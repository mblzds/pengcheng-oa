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
        FakeRoleMapper residentMapper = new FakeRoleMapper(List.of(role("resident", null)));
        DataScope residentScope = scope("", "", "alliance_id", "id");

        String residentFilter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 99L, residentScope, residentMapper);
        assertThat(residentFilter)
                .contains("id IN")
                .contains("SELECT cp.customer_id FROM customer_project cp")
                .contains("contact_person = (SELECT nickname FROM sys_user WHERE id = 99)");

        FakeRoleMapper allianceManagerMapper = new FakeRoleMapper(List.of(role("alliance_manager", null)));
        String allianceFilter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 88L, residentScope, allianceManagerMapper);
        assertThat(allianceFilter)
                .contains("alliance_id IN")
                .contains("user_id = 88");

        FakeRoleMapper directorMapper = new FakeRoleMapper(List.of(role("resident_director", null)));
        String directorFilter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 66L, residentScope, directorMapper);
        assertThat(directorFilter).isEmpty();
    }

    @Test
    @DisplayName("房产业务角色过滤: 无匹配角色时拒绝访问")
    void buildRealtyDataScopeFilterDeniesUnknownRole() {
        FakeRoleMapper roleMapper = new FakeRoleMapper(List.of(role("guest", null)));

        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 77L, scope("", "", "alliance_id", "id"), roleMapper);

        assertThat(filter).isEqualTo("1=0");
    }

    @Test
    @DisplayName("房产业务角色过滤: 异常路径 fail closed")
    void buildRealtyDataScopeFilterFailsClosedOnError() {
        String filter = ReflectionTestUtils.invokeMethod(
                interceptor, "buildRealtyDataScopeFilter", 55L, scope("", "", "alliance_id", "id"), new ExplodingRoleMapper());

        assertThat(filter).isEqualTo("1=0");
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
