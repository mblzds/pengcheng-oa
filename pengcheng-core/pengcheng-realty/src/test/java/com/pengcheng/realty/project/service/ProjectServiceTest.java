package com.pengcheng.realty.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.realty.project.dto.ProjectCommissionRuleDTO;
import com.pengcheng.realty.project.dto.ProjectCreateDTO;
import com.pengcheng.realty.project.dto.ProjectQueryDTO;
import com.pengcheng.realty.project.dto.ProjectVO;
import com.pengcheng.realty.project.entity.Project;
import com.pengcheng.realty.project.entity.ProjectCommissionRule;
import com.pengcheng.realty.project.mapper.ProjectCommissionRuleMapper;
import com.pengcheng.realty.project.mapper.ProjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ProjectService")
class ProjectServiceTest {

    private ProjectMapper projectMapper;
    private ProjectCommissionRuleMapper commissionRuleMapper;
    private ProjectService service;

    @BeforeEach
    void setUp() {
        projectMapper = mock(ProjectMapper.class);
        commissionRuleMapper = mock(ProjectCommissionRuleMapper.class);
        service = new ProjectService(projectMapper, commissionRuleMapper);
    }

    @Test
    @DisplayName("createProject 默认状态为在售并返回主键")
    void createProjectUsesDefaultStatus() {
        ProjectCreateDTO dto = validProjectDto();
        dto.setStatus(null);
        doAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(1001L);
            return 1;
        }).when(projectMapper).insert(any(Project.class));

        Long id = service.createProject(dto);

        assertThat(id).isEqualTo(1001L);
    }

    @Test
    @DisplayName("updateProject 更新已存在项目字段")
    void updateProjectUpdatesExistingProject() {
        ProjectCreateDTO dto = validProjectDto();
        dto.setId(2001L);
        Project existing = Project.builder().projectName("旧项目").status(1).build();
        existing.setId(2001L);
        when(projectMapper.selectById(2001L)).thenReturn(existing);

        service.updateProject(dto);

        assertThat(existing.getProjectName()).isEqualTo(dto.getProjectName());
        assertThat(existing.getDeveloperName()).isEqualTo(dto.getDeveloperName());
        verify(projectMapper).updateById(existing);
    }

    @Test
    @DisplayName("pageProjects 返回分页后的 VO")
    void pageProjectsReturnsPageResult() {
        Project project = Project.builder().projectName("望京壹号").status(1).district("朝阳").build();
        project.setId(1L);
        Page<Project> page = new Page<Project>(1, 10).setRecords(List.of(project));
        page.setTotal(1);
        when(projectMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<ProjectVO> result = service.pageProjects(ProjectQueryDTO.builder().page(1).pageSize(10).build());

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).extracting(ProjectVO::getProjectName).containsExactly("望京壹号");
    }

    @Test
    @DisplayName("saveCommissionRule 会使旧版本失效并创建待审批新版本")
    void saveCommissionRuleVersionsRules() {
        ProjectCommissionRule current = ProjectCommissionRule.builder().projectId(2001L).version(2).status(1).build();
        current.setId(3001L);
        when(commissionRuleMapper.selectOne(any())).thenReturn(current);
        doAnswer(invocation -> {
            ProjectCommissionRule rule = invocation.getArgument(0);
            rule.setId(3002L);
            return 1;
        }).when(commissionRuleMapper).insert(any(ProjectCommissionRule.class));

        Long newId = service.saveCommissionRule(ProjectCommissionRuleDTO.builder()
                .projectId(2001L)
                .baseRate(new BigDecimal("0.018"))
                .jumpPointRules("[]")
                .cashReward(new BigDecimal("2000"))
                .firstDealReward(new BigDecimal("3000"))
                .platformReward(new BigDecimal("500"))
                .build());

        assertThat(newId).isEqualTo(3002L);
        assertThat(current.getStatus()).isEqualTo(3);
        verify(commissionRuleMapper).updateById(current);
    }

    @Test
    @DisplayName("approveCommissionRule 与 markExpiredProjects 可更新状态")
    void approveRuleAndMarkExpiredProjects() {
        ProjectCommissionRule pendingRule = ProjectCommissionRule.builder().status(2).build();
        pendingRule.setId(5001L);
        when(commissionRuleMapper.selectById(5001L)).thenReturn(pendingRule);

        Project expired = Project.builder()
                .projectName("到期项目")
                .agencyEndDate(LocalDate.now().minusDays(1))
                .status(1)
                .build();
        expired.setId(7001L);
        when(projectMapper.selectList(any())).thenReturn(List.of(expired));

        service.approveCommissionRule(5001L, true);
        List<Project> expiredProjects = service.markExpiredProjects();

        assertThat(pendingRule.getStatus()).isEqualTo(1);
        assertThat(expiredProjects).hasSize(1);
        assertThat(expired.getStatus()).isEqualTo(4);
        verify(projectMapper).updateById(expired);
    }

    @Test
    @DisplayName("validateAgencyDates 阻止开始时间晚于结束时间")
    void validateAgencyDatesRejectsInvalidRange() {
        assertThatThrownBy(() -> service.validateAgencyDates(LocalDate.now(), LocalDate.now().minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("开始时间必须早于");
    }

    private ProjectCreateDTO validProjectDto() {
        ProjectCreateDTO dto = new ProjectCreateDTO();
        dto.setProjectName("望京壹号");
        dto.setDeveloperName("鹏诚地产");
        dto.setAddress("朝阳区");
        dto.setProjectType(1);
        dto.setStatus(1);
        dto.setDistrict("朝阳");
        dto.setAgencyStartDate(LocalDate.of(2026, 1, 1));
        dto.setAgencyEndDate(LocalDate.of(2026, 12, 31));
        dto.setContactPerson("赵驻场");
        dto.setContactPhone("13900000000");
        dto.setDescription("项目描述");
        return dto;
    }
}
