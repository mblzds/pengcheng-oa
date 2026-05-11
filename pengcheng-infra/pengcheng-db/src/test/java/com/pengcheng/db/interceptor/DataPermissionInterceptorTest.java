package com.pengcheng.db.interceptor;

import com.pengcheng.common.annotation.DataScope;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DataPermissionInterceptor")
class DataPermissionInterceptorTest {

    private final DataPermissionInterceptor interceptor = new DataPermissionInterceptor();

    @Test
    @DisplayName("房产业务角色过滤: 驻场 / 联盟商负责人 / 总监")
    void buildRealtyDataScopeFilterMatchesRoleRules() {
        FakeUserMapper userMapper = new FakeUserMapper(user(1L));
        FakeRoleMapper residentMapper = new FakeRoleMapper(List.of(role("resident", null)));
        DataScope residentScope = scope("", "", "alliance_id", "id");

        String residentFilter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 99L, residentScope, residentMapper, userMapper);
        assertThat(residentFilter)
                .contains("id IN")
                .contains("SELECT cp.customer_id FROM customer_project cp")
                .contains("contact_person = (SELECT nickname FROM sys_user WHERE id = 99)");

        FakeRoleMapper allianceManagerMapper = new FakeRoleMapper(List.of(role("alliance_manager", null)));
        String allianceFilter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 88L, residentScope, allianceManagerMapper, userMapper);
        assertThat(allianceFilter)
                .contains("alliance_id IN")
                .contains("user_id = 88");

        FakeRoleMapper directorMapper = new FakeRoleMapper(List.of(role("resident_director", null)));
        String directorFilter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 66L, residentScope, directorMapper, userMapper);
        assertThat(directorFilter).isEmpty();
    }

    @Test
    @DisplayName("房产业务角色过滤: 无匹配角色时拒绝访问")
    void buildRealtyDataScopeFilterDeniesUnknownRole() {
        FakeRoleMapper roleMapper = new FakeRoleMapper(List.of(role("guest", null)));

        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 77L,
                scope("", "", "alliance_id", "id"), roleMapper, new FakeUserMapper(user(1L)));

        assertThat(filter).isEqualTo("1=0");
    }

    @Test
    @DisplayName("房产业务角色过滤: 异常路径 fail closed")
    void buildRealtyDataScopeFilterFailsClosedOnError() {
        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 55L,
                scope("", "", "alliance_id", "id"), new ExplodingRoleMapper(), new FakeUserMapper(user(1L)));

        assertThat(filter).isEqualTo("1=0");
    }

    @Test
    @DisplayName("房产业务数据权限: sys_role.data_scope=5 仅本人（叠加 userAlias）")
    void buildRealtyDataScopeFilterAppliesDataScopeFive() {
        FakeRoleMapper saleMapper = new FakeRoleMapper(List.of(role("sales", 5)));
        DataScope salesScope = scope("", "creator_id", "alliance_id", "id");

        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 1001L, salesScope, saleMapper, new FakeUserMapper(user(10L)));

        assertThat(filter).contains("creator_id = 1001");
    }

    @Test
    @DisplayName("房产业务数据权限: sys_role.data_scope=4 本部门及以下")
    void buildRealtyDataScopeFilterAppliesDataScopeFour() {
        FakeRoleMapper managerMapper = new FakeRoleMapper(List.of(role("sales_manager", 4)));
        DataScope salesScope = scope("", "creator_id", "alliance_id", "id");

        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 2002L, salesScope, managerMapper, new FakeUserMapper(user(20L)));

        assertThat(filter)
                .contains("creator_id IN (SELECT id FROM sys_user")
                .contains("FIND_IN_SET(20, ancestors)");
    }

    @Test
    @DisplayName("房产业务数据权限: sys_role.data_scope=1 全部不加过滤")
    void buildRealtyDataScopeFilterAppliesDataScopeOne() {
        FakeRoleMapper gmMapper = new FakeRoleMapper(List.of(role("general_manager", 1)));
        DataScope salesScope = scope("", "creator_id", "alliance_id", "id");

        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 3003L, salesScope, gmMapper, new FakeUserMapper(user(30L)));

        assertThat(filter).isEmpty();
    }

    @Test
    @DisplayName("房产业务数据权限: 组织驱动 —— 部门负责人(leader_id)自动看本部门及以下")
    void buildRealtyDataScopeFilterAppliesOrganizationDrivenLeader() {
        // 仅挂基础销售员角色（仅本人），靠 sys_dept.leader_id 自动放宽
        FakeRoleMapper saleMapper = new FakeRoleMapper(List.of(role("sales", 5)));
        DataScope salesScope = scope("", "creator_id", "alliance_id", "id");

        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 4004L, salesScope, saleMapper, new FakeUserMapper(user(40L)));

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
                residentScopeNoUserAlias, residentMapper, new FakeUserMapper(user(50L)));

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
