package com.pengcheng.db.interceptor;

import com.pengcheng.common.annotation.DataScope;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.Serializable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DataPermissionInterceptor")
class DataPermissionInterceptorTest {

    private final DataPermissionInterceptor interceptor = new DataPermissionInterceptor();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static void bindRequest(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("房产业务角色过滤: 驻场 / 联盟商负责人 / 总监")
    void buildRealtyDataScopeFilterMatchesRoleRules() {
        FakeUserMapper userMapper = new FakeUserMapper(user(1L));
        FakeRoleMapper residentMapper = new FakeRoleMapper(List.of(role("resident", null)));
        DataScope residentScope = scope("", "", "alliance_id", "id");

        String residentFilter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 99L, residentScope, residentMapper, userMapper, false);
        assertThat(residentFilter)
                .contains("id IN")
                .contains("SELECT cp.customer_id FROM customer_project cp")
                .contains("contact_person = (SELECT nickname FROM sys_user WHERE id = 99)");

        FakeRoleMapper allianceManagerMapper = new FakeRoleMapper(List.of(role("alliance_manager", null)));
        String allianceFilter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 88L, residentScope, allianceManagerMapper, userMapper, false);
        assertThat(allianceFilter)
                .contains("alliance_id IN")
                .contains("user_id = 88");

        FakeRoleMapper directorMapper = new FakeRoleMapper(List.of(role("resident_director", null)));
        String directorFilter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 66L, residentScope, directorMapper, userMapper, false);
        assertThat(directorFilter).isEmpty();
    }

    @Test
    @DisplayName("房产业务角色过滤: 无匹配角色时拒绝访问")
    void buildRealtyDataScopeFilterDeniesUnknownRole() {
        FakeRoleMapper roleMapper = new FakeRoleMapper(List.of(role("guest", null)));

        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 77L,
                scope("", "", "alliance_id", "id"), roleMapper, new FakeUserMapper(user(1L)), false);

        assertThat(filter).isEqualTo("1=0");
    }

    @Test
    @DisplayName("房产业务角色过滤: 异常路径 fail closed")
    void buildRealtyDataScopeFilterFailsClosedOnError() {
        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 55L,
                scope("", "", "alliance_id", "id"), new ExplodingRoleMapper(), new FakeUserMapper(user(1L)), false);

        assertThat(filter).isEqualTo("1=0");
    }

    @Test
    @DisplayName("房产业务数据权限: 普通员工默认仅看本人创建的（不受 data_scope=3/4 影响）")
    void buildRealtyDataScopeFilterDefaultsToOwnerOnly() {
        // 普通员工 user 角色, data_scope=3 本部门 —— 在房产分支不生效
        FakeRoleMapper userMapper = new FakeRoleMapper(List.of(role("user", 3)));
        DataScope customerScope = scope("", "creator_id", "alliance_id", "id");

        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 1001L, customerScope, userMapper, new FakeUserMapper(user(10L)), false);

        // 应有: 仅本人 + 组织驱动子查询。不应有: 通用 data_scope=3 的"本部门所有创建者"扩展
        assertThat(filter)
                .contains("creator_id = 1001")
                .doesNotContain("SELECT id FROM sys_user WHERE dept_id = 10");
    }

    @Test
    @DisplayName("房产业务数据权限: 非 /app/* 上下文中 data_scope=1 授予全部权限（admin/总经理后台视图）")
    void buildRealtyDataScopeFilterAppliesDataScopeOne() {
        FakeRoleMapper gmMapper = new FakeRoleMapper(List.of(role("general_manager", 1)));
        DataScope salesScope = scope("", "creator_id", "alliance_id", "id");

        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 3003L, salesScope, gmMapper, new FakeUserMapper(user(30L)), false);

        assertThat(filter).isEmpty();
    }

    @Test
    @DisplayName("房产业务数据权限: /app/* 上下文中 data_scope=1 不再短路，admin 仍按本人 + 组织驱动过滤")
    void buildRealtyDataScopeFilterDoesNotShortCircuitDataScopeOneOnApp() {
        // admin 角色，sys_role.data_scope=1，但请求落在小程序 /app/*：
        // 不能短路，必须按个人视角（本人 + leader 子查询）过滤。
        FakeRoleMapper adminMapper = new FakeRoleMapper(List.of(role("admin", 1)));
        DataScope customerScope = scope("", "creator_id", "alliance_id", "id");

        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 18L, customerScope, adminMapper, new FakeUserMapper(user(110L)), true);

        assertThat(filter)
                .isNotEmpty()
                .contains("creator_id = 18")
                .contains("d1.leader_id = 18");
    }

    @Test
    @DisplayName("房产业务数据权限: 组织驱动 —— 部门负责人(leader_id)自动看本部门及以下")
    void buildRealtyDataScopeFilterAppliesOrganizationDrivenLeader() {
        // 普通员工角色，靠 sys_dept.leader_id 自动放宽到本部门及以下
        FakeRoleMapper userMapper = new FakeRoleMapper(List.of(role("user", 3)));
        DataScope customerScope = scope("", "creator_id", "alliance_id", "id");

        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 4004L, customerScope, userMapper, new FakeUserMapper(user(40L)), false);

        // 应同时包含: 仅本人条件 + 部门负责人下钻子查询
        assertThat(filter)
                .contains("creator_id = 4004")
                .contains("WHERE d1.leader_id = 4004")
                .contains("FIND_IN_SET(d1.id, d2.ancestors)");
    }

    @Test
    @DisplayName("房产业务数据权限: 仅 userAlias 缺失时不叠加组织驱动子查询")
    void buildRealtyDataScopeFilterSkipsOrgDrivenWhenUserAliasMissing() {
        // userAlias 为空 — 即使是部门负责人也无法叠加（依赖归属销售字段）
        FakeRoleMapper residentMapper = new FakeRoleMapper(List.of(role("resident", null)));
        DataScope residentScopeNoUserAlias = scope("", "", "alliance_id", "id");

        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 5005L,
                residentScopeNoUserAlias, residentMapper, new FakeUserMapper(user(50L)), false);

        assertThat(filter).doesNotContain("d1.leader_id");
    }

    @Test
    @DisplayName("通用数据权限过滤: 本部门 / 仅本人 / 全部")
    void buildDataScopeFilterHandlesCommonScopes() {
        FakeUserMapper userMapper = new FakeUserMapper(user(300L));

        String deptFilter = ReflectionTestUtils.invokeMethod(
                interceptor,
                "buildDataScopeFilter",
                11L,
                scope("dept_id", "create_by", "", ""),
                new FakeRoleMapper(List.of(role("staff", 3))),
                userMapper
        );
        assertThat(deptFilter).contains("dept_id = 300");

        String selfFilter = ReflectionTestUtils.invokeMethod(
                interceptor,
                "buildDataScopeFilter",
                22L,
                scope("dept_id", "create_by", "", ""),
                new FakeRoleMapper(List.of(role("staff", 5))),
                userMapper
        );
        assertThat(selfFilter).contains("create_by = 22");

        String allFilter = ReflectionTestUtils.invokeMethod(
                interceptor,
                "buildDataScopeFilter",
                33L,
                scope("dept_id", "create_by", "", ""),
                new FakeRoleMapper(List.of(role("admin", 1))),
                userMapper
        );
        assertThat(allFilter).isEmpty();
    }

    @Test
    @DisplayName("通用数据权限过滤: 异常路径 fail closed")
    void buildDataScopeFilterFailsClosedOnError() {
        String filter = ReflectionTestUtils.invokeMethod(
                interceptor,
                "buildDataScopeFilter",
                44L,
                scope("dept_id", "create_by", "", ""),
                new ExplodingRoleMapper(),
                new FakeUserMapper(user(300L))
        );

        assertThat(filter).isEqualTo("1=0");
    }

    @Test
    @DisplayName("isAppEndpoint: 识别 /app/* 与 /api/app/* (ApiPrefixConfig 注入 /api 前缀)")
    void isAppEndpointDetectsAppUri() {
        // 无请求上下文 → false（定时任务 / 异步线程 / 测试）
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(interceptor, "isAppEndpoint")).isFalse();

        // 实际线上路径：/api/app/* (ApiPrefixConfig 全局加 /api)
        bindRequest("/api/app/realty/customer/listPage");
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(interceptor, "isAppEndpoint")).isTrue();

        // 兼容：未加 /api 前缀的 /app/* 也算（防御性，比如未来移除前缀或测试场景）
        bindRequest("/app/realty/customer/listPage");
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(interceptor, "isAppEndpoint")).isTrue();

        // 管理后台 / 其他接口 → false
        bindRequest("/api/admin/realty/customer/listPage");
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(interceptor, "isAppEndpoint")).isFalse();

        bindRequest("/api/sys/user/info");
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(interceptor, "isAppEndpoint")).isFalse();
    }

    @Test
    @DisplayName("isAppEndpoint: 仅前缀匹配 — 路径里含 /app/ 但开头不一致不算 app 端")
    void isAppEndpointMatchesOnlyPrefix() {
        bindRequest("/api/admin/app/diagnostic");
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(interceptor, "isAppEndpoint")).isFalse();
    }

    @Test
    @DisplayName("getDataScope 能读取 Mapper 方法上的注解")
    void getDataScopeReadsMapperAnnotation() {
        String statementId = AnnotatedMapper.class.getName() + ".selectWithScope";
        Configuration configuration = new Configuration();
        MappedStatement ms = new MappedStatement.Builder(
                configuration,
                statementId,
                new StaticSqlSource(configuration, "SELECT 1"),
                SqlCommandType.SELECT
        ).build();

        DataScope dataScope = ReflectionTestUtils.invokeMethod(interceptor, "getDataScope", ms);

        assertThat(dataScope).isNotNull();
        assertThat(dataScope.allianceAlias()).isEqualTo("a_id");
        assertThat(dataScope.projectAlias()).isEqualTo("p_id");
    }

    private static DataScope scope(String deptAlias, String userAlias, String allianceAlias, String projectAlias) {
        return new DataScope() {
            @Override
            public String deptAlias() {
                return deptAlias;
            }

            @Override
            public String userAlias() {
                return userAlias;
            }

            @Override
            public String allianceAlias() {
                return allianceAlias;
            }

            @Override
            public String projectAlias() {
                return projectAlias;
            }

            @Override
            public Class<DataScope> annotationType() {
                return DataScope.class;
            }
        };
    }

    private static FakeRole role(String code, Integer dataScope) {
        FakeRole role = new FakeRole();
        role.setCode(code);
        role.setDataScope(dataScope);
        role.setId(1L);
        return role;
    }

    private static FakeUser user(Long deptId) {
        FakeUser user = new FakeUser();
        user.setDeptId(deptId);
        return user;
    }

    interface AnnotatedMapper {
        @DataScope(allianceAlias = "a_id", projectAlias = "p_id")
        void selectWithScope();
    }

    static class FakeRoleMapper {
        private final List<FakeRole> roles;

        FakeRoleMapper(List<FakeRole> roles) {
            this.roles = roles;
        }

        public List<FakeRole> selectRolesByUserId(Long userId) {
            return roles;
        }
    }

    static class ExplodingRoleMapper {
        public List<FakeRole> selectRolesByUserId(Long userId) {
            throw new IllegalStateException("boom");
        }
    }

    static class FakeUserMapper {
        private final FakeUser user;

        FakeUserMapper(FakeUser user) {
            this.user = user;
        }

        public FakeUser selectById(Serializable id) {
            return user;
        }
    }

    static class FakeRole {
        private Long id;
        private String code;
        private Integer dataScope;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Integer getDataScope() {
            return dataScope;
        }

        public void setDataScope(Integer dataScope) {
            this.dataScope = dataScope;
        }
    }

    static class FakeUser {
        private Long deptId;

        public Long getDeptId() {
            return deptId;
        }

        public void setDeptId(Long deptId) {
            this.deptId = deptId;
        }
    }
}
