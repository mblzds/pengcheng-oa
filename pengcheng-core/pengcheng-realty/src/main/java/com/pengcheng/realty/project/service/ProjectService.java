package com.pengcheng.realty.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目管理服务
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectCommissionRuleMapper commissionRuleMapper;

    /**
     * 创建项目
     */
    @Transactional
    public Long createProject(ProjectCreateDTO dto) {
        validateAgencyDates(dto.getAgencyStartDate(), dto.getAgencyEndDate());

        Project project = Project.builder()
                .projectName(dto.getProjectName())
                .developerName(dto.getDeveloperName())
                .address(dto.getAddress())
                .projectType(dto.getProjectType())
                .status(dto.getStatus() != null ? dto.getStatus() : 1)
                .district(dto.getDistrict())
                .agencyStartDate(dto.getAgencyStartDate())
                .agencyEndDate(dto.getAgencyEndDate())
                .contactPerson(dto.getContactPerson())
                .contactPhone(dto.getContactPhone())
                .description(dto.getDescription())
                .build();
        projectMapper.insert(project);
        return project.getId();
    }

    /**
     * 编辑项目
     */
    @Transactional
    public void updateProject(ProjectCreateDTO dto) {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }
        validateAgencyDates(dto.getAgencyStartDate(), dto.getAgencyEndDate());

        Project project = projectMapper.selectById(dto.getId());
        if (project == null) {
            throw new IllegalArgumentException("项目不存在");
        }

        project.setProjectName(dto.getProjectName());
        project.setDeveloperName(dto.getDeveloperName());
        project.setAddress(dto.getAddress());
        project.setProjectType(dto.getProjectType());
        if (dto.getStatus() != null) {
            project.setStatus(dto.getStatus());
        }
        project.setDistrict(dto.getDistrict());
        project.setAgencyStartDate(dto.getAgencyStartDate());
        project.setAgencyEndDate(dto.getAgencyEndDate());
        project.setContactPerson(dto.getContactPerson());
        project.setContactPhone(dto.getContactPhone());
        project.setDescription(dto.getDescription());
        projectMapper.updateById(project);
    }

    /**
     * 分页查询项目（支持按名称/片区/类型/状态筛选）
     */
    public PageResult<ProjectVO> pageProjects(ProjectQueryDTO query) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getProjectName())) {
            wrapper.like(Project::getProjectName, query.getProjectName());
        }
        if (StringUtils.hasText(query.getDistrict())) {
            wrapper.eq(Project::getDistrict, query.getDistrict());
        }
        if (query.getProjectType() != null) {
            wrapper.eq(Project::getProjectType, query.getProjectType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Project::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Project::getCreateTime);

        IPage<Project> page = projectMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<ProjectVO> voList = page.getRecords().stream()
                .map(ProjectVO::fromEntity)
                .toList();
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 获取项目详情
     */
    public ProjectVO getProject(Long id) {
        Project project = projectMapper.selectById(id);
        return ProjectVO.fromEntity(project);
    }

    /**
     * 录入/更新佣金规则（版本化管理）
     * <p>
     * 更新时保留旧版本记录（标记为已失效），新版本号递增，状态设为待审批。
     */
    @Transactional
    public Long saveCommissionRule(ProjectCommissionRuleDTO dto) {
        if (dto.getProjectId() == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }

        // 查找当前生效的规则
        ProjectCommissionRule currentRule = getActiveCommissionRule(dto.getProjectId());

        int newVersion = 1;
        if (currentRule != null) {
            // 将旧版本标记为已失效
            currentRule.setStatus(3); // 3-已失效
            commissionRuleMapper.updateById(currentRule);
            newVersion = currentRule.getVersion() + 1;
        }

        // 创建新版本规则，状态为待审批
        ProjectCommissionRule newRule = ProjectCommissionRule.builder()
                .projectId(dto.getProjectId())
                .baseRate(dto.getBaseRate())
                .jumpPointRules(dto.getJumpPointRules())
                .cashReward(dto.getCashReward())
                .firstDealReward(dto.getFirstDealReward())
                .platformReward(dto.getPlatformReward())
                .version(newVersion)
                .status(2) // 2-待审批
                .build();
        commissionRuleMapper.insert(newRule);
        return newRule.getId();
    }

    /**
     * 审批佣金规则（通过后生效）
     */
    @Transactional
    public void approveCommissionRule(Long ruleId, boolean approved) {
        ProjectCommissionRule rule = commissionRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("佣金规则不存在");
        }
        if (rule.getStatus() != 2) {
            throw new IllegalArgumentException("仅待审批状态的规则可审批");
        }
        rule.setStatus(approved ? 1 : 3); // 1-生效, 3-已失效
        commissionRuleMapper.updateById(rule);
    }

    /**
     * 获取项目当前生效的佣金规则
     */
    public ProjectCommissionRule getActiveCommissionRule(Long projectId) {
        LambdaQueryWrapper<ProjectCommissionRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectCommissionRule::getProjectId, projectId)
                .eq(ProjectCommissionRule::getStatus, 1) // 1-生效
                .orderByDesc(ProjectCommissionRule::getVersion)
                .last("LIMIT 1");
        return commissionRuleMapper.selectOne(wrapper);
    }

    /**
     * 获取项目所有佣金规则版本
     */
    public List<ProjectCommissionRule> getCommissionRuleVersions(Long projectId) {
        LambdaQueryWrapper<ProjectCommissionRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectCommissionRule::getProjectId, projectId)
                .orderByDesc(ProjectCommissionRule::getVersion);
        return commissionRuleMapper.selectList(wrapper);
    }

    /**
     * 标记到期项目
     * <p>
     * 查找代理结束时间已过且状态不是"已到期"的项目，将其状态更新为"已到期"(4)。
     *
     * @return 标记为到期的项目列表
     */
    public List<Project> markExpiredProjects() {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(Project::getAgencyEndDate, LocalDate.now())
                .ne(Project::getStatus, 4) // 排除已到期
                .isNotNull(Project::getAgencyEndDate);
        List<Project> expiredProjects = projectMapper.selectList(wrapper);

        for (Project project : expiredProjects) {
            project.setStatus(4); // 4-已到期
            projectMapper.updateById(project);
        }
        return expiredProjects;
    }

    /**
     * 校验代理时间：开始时间必须早于结束时间
     */
    public void validateAgencyDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && !startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("代理开始时间必须早于代理结束时间");
        }
    }
}
