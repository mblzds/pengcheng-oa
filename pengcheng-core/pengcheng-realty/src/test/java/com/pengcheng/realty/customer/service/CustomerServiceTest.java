package com.pengcheng.realty.customer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.event.DataChangeEvent;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.realty.alliance.entity.Alliance;
import com.pengcheng.realty.alliance.mapper.AllianceMapper;
import com.pengcheng.realty.common.exception.AllianceDisabledException;
import com.pengcheng.realty.common.exception.CustomerDuplicateException;
import com.pengcheng.realty.customer.dto.CustomerCreateDTO;
import com.pengcheng.realty.customer.dto.CustomerCreateResultVO;
import com.pengcheng.realty.customer.dto.CustomerQueryDTO;
import com.pengcheng.realty.customer.dto.CustomerVO;
import com.pengcheng.realty.customer.dto.PoolStatsVO;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerProject;
import com.pengcheng.realty.customer.mapper.CustomerPoolEventLogMapper;
import com.pengcheng.realty.customer.mapper.CustomerProjectMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.project.entity.Project;
import com.pengcheng.realty.project.mapper.ProjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CustomerService")
class CustomerServiceTest {

    private RealtyCustomerMapper customerMapper;
    private CustomerProjectMapper customerProjectMapper;
    private CustomerPoolEventLogMapper customerPoolEventLogMapper;
    private ProjectMapper projectMapper;
    private AllianceMapper allianceMapper;
    private ApplicationEventPublisher eventPublisher;
    private CustomerDuplicateChecker duplicateChecker;
    private CustomerService service;

    @BeforeEach
    void setUp() {
        customerMapper = mock(RealtyCustomerMapper.class);
        customerProjectMapper = mock(CustomerProjectMapper.class);
        customerPoolEventLogMapper = mock(CustomerPoolEventLogMapper.class);
        projectMapper = mock(ProjectMapper.class);
        allianceMapper = mock(AllianceMapper.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        duplicateChecker = mock(CustomerDuplicateChecker.class);

        service = new CustomerService(
                customerMapper,
                customerProjectMapper,
                customerPoolEventLogMapper,
                projectMapper,
                allianceMapper,
                eventPublisher
        );
        ReflectionTestUtils.setField(service, "duplicateChecker", duplicateChecker);
    }

    @Test
    @DisplayName("createCustomer 在联盟商启用且无保护期重复时创建客户、项目关联并发布事件")
    void createCustomerCreatesCustomerProjectsAndEvent() {
        CustomerCreateDTO dto = validCreateDto();
        Alliance alliance = new Alliance();
        alliance.setId(100L);
        alliance.setStatus(1);

        when(allianceMapper.selectById(100L)).thenReturn(alliance);
        when(customerMapper.selectList(any())).thenReturn(List.of());
        when(duplicateChecker.checkDuplicate("13800000000")).thenReturn(
                new CustomerDuplicateChecker.DuplicateCheckResult(
                        true,
                        List.of(CustomerCreateResultVO.ExistingCustomerInfo.builder().id(9L).customerName("重复客户").build()),
                        "发现重复客户"
                )
        );
        doAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(9527L);
            return 1;
        }).when(customerMapper).insert(any(Customer.class));

        CustomerCreateResultVO result = service.createCustomer(dto);

        assertThat(result.getCustomerId()).isEqualTo(9527L);
        assertThat(result.isHasDuplicate()).isTrue();
        assertThat(result.getAnalysisMessage()).isEqualTo("发现重复客户");
        assertThat(result.getExistingCustomers()).hasSize(1);

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerMapper).insert(customerCaptor.capture());
        Customer saved = customerCaptor.getValue();
        assertThat(saved.getReportNo()).startsWith("BP");
        assertThat(saved.getPhoneMasked()).isEqualTo("138****0000");
        assertThat(saved.getStatus()).isEqualTo(1);
        assertThat(saved.getPoolType()).isEqualTo(2);

        verify(customerProjectMapper, times(2)).insert(any(CustomerProject.class));

        ArgumentCaptor<DataChangeEvent> eventCaptor = ArgumentCaptor.forClass(DataChangeEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getBizId()).isEqualTo(9527L);
        assertThat(eventCaptor.getValue().getBizType()).isEqualTo("customer");
    }

    @Test
    @DisplayName("createCustomer 联盟商停用时拒绝创建")
    void createCustomerRejectsDisabledAlliance() {
        CustomerCreateDTO dto = validCreateDto();
        Alliance alliance = new Alliance();
        alliance.setStatus(0);
        when(allianceMapper.selectById(dto.getAllianceId())).thenReturn(alliance);

        assertThatThrownBy(() -> service.createCustomer(dto))
                .isInstanceOf(AllianceDisabledException.class)
                .hasMessageContaining("已停用");
    }

    @Test
    @DisplayName("createCustomer 保护期内重复报备时拒绝创建")
    void createCustomerRejectsProtectedDuplicate() {
        CustomerCreateDTO dto = validCreateDto();
        Alliance alliance = new Alliance();
        alliance.setStatus(1);

        Customer protectedCustomer = Customer.builder()
                .phone(dto.getPhone())
                .protectionExpireTime(LocalDateTime.now().plusDays(1))
                .build();
        protectedCustomer.setId(11L);

        when(allianceMapper.selectById(dto.getAllianceId())).thenReturn(alliance);
        when(customerMapper.selectList(any())).thenReturn(List.of(protectedCustomer));
        when(customerProjectMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.createCustomer(dto))
                .isInstanceOf(CustomerDuplicateException.class)
                .hasMessageContaining("保护期");
    }

    @Test
    @DisplayName("isInProtectionPeriod 在同项目存在有效客户时返回 true")
    void isInProtectionPeriodReturnsTrueWhenSameProjectExists() {
        Customer existing = Customer.builder()
                .phone("13800000000")
                .protectionExpireTime(LocalDateTime.now().plusHours(2))
                .build();
        existing.setId(7L);
        when(customerMapper.selectList(any())).thenReturn(List.of(existing));
        when(customerProjectMapper.selectCount(any())).thenReturn(1L);

        assertThat(service.isInProtectionPeriod("13800000000", 200L)).isTrue();
    }

    @Test
    @DisplayName("pageCustomers 返回分页后的脱敏客户列表")
    void pageCustomersMapsPageResult() {
        Customer customer = Customer.builder()
                .reportNo("BP202604220001")
                .customerName("王")
                .phone("13800000000")
                .agentPhone("13900000000")
                .status(1)
                .poolType(2)
                .build();
        customer.setId(1L);
        customer.setCreateTime(LocalDateTime.now());
        IPage<Customer> page = new Page<Customer>(1, 10).setRecords(List.of(customer));
        page.setTotal(1);
        when(customerMapper.selectPageWithScope(any(Page.class), any())).thenReturn(page);

        PageResult<CustomerVO> result = service.pageCustomers(CustomerQueryDTO.builder().page(1).pageSize(10).build());

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getPhoneMasked()).isEqualTo("138****0000");
        assertThat(result.getList().get(0).getAgentPhoneMasked()).isEqualTo("139****0000");
    }

    @Test
    @DisplayName("searchProjects / searchAlliances / getPoolStats 返回查询结果与统计")
    void searchAndPoolStatsWork() {
        Project project = Project.builder().projectName("望京壹号").status(1).build();
        project.setId(200L);
        Alliance alliance = new Alliance();
        alliance.setId(300L);
        alliance.setCompanyName("鹏诚链家");
        alliance.setStatus(1);

        when(projectMapper.selectList(any())).thenReturn(List.of(project));
        when(allianceMapper.selectList(any())).thenReturn(List.of(alliance));
        when(customerMapper.selectCount(any())).thenReturn(5L, 2L);
        when(customerPoolEventLogMapper.selectCount(any())).thenReturn(3L, 1L);

        assertThat(service.searchProjects("望京")).extracting(Project::getProjectName).containsExactly("望京壹号");
        assertThat(service.searchAlliances("鹏诚")).extracting(Alliance::getCompanyName).containsExactly("鹏诚链家");
        PoolStatsVO stats = service.getPoolStats();
        assertThat(stats.getTotal()).isEqualTo(5);
        assertThat(stats.getTodayNew()).isEqualTo(2);
        assertThat(stats.getTodayClaimed()).isEqualTo(3);
        assertThat(stats.getTodayRecycled()).isEqualTo(1);
        verify(customerPoolEventLogMapper, times(2)).selectCount(any());
    }

    private CustomerCreateDTO validCreateDto() {
        return CustomerCreateDTO.builder()
                .projectIds(List.of(20001L, 20002L))
                .customerName("张")
                .phone("13800000000")
                .visitCount(2)
                .visitTime(LocalDateTime.now())
                .allianceId(100L)
                .agentName("陈经理")
                .agentPhone("13900000000")
                .build();
    }
}
