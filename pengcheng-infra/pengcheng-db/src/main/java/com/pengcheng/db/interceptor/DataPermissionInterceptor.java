package com.pengcheng.db.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.pengcheng.common.annotation.DataScope;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据权限拦截器 (支持通用部门权限 + 房产业务角色权限)
 */
@Slf4j
@Component
public class DataPermissionInterceptor implements InnerInterceptor, ApplicationContextAware {

    private static final String DENY_ALL_FILTER = "1=0";

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    // 房产业务角色编码常量（避免对 pengcheng-realty 的编译依赖）
    private static final String ROLE_RESIDENT = "resident";
    private static final String ROLE_CHANNEL = "channel";
    private static final String ROLE_RESIDENT_DIRECTOR = "resident_director";
    private static final String ROLE_CHANNEL_DIRECTOR = "channel_director";
    private static final String ROLE_ADMIN_DIRECTOR = "admin_director";
    private static final String ROLE_ADMIN_CLERK = "admin_clerk";
    private static final String ROLE_ALLIANCE_MANAGER = "alliance_manager";

    @Override
    @SuppressWarnings("rawtypes")
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        if (SqlCommandType.SELECT != ms.getSqlCommandType()) {
            return;
        }

        DataScope dataScope = getDataScope(ms);
        if (dataScope == null) {
            return;
        }

        if (!StpUtil.isLogin()) {
            return;
        }

        Long userId = StpUtil.getLoginIdAsLong();

        // 动态获取 Mapper 解决循环依赖
        Object roleMapper = applicationContext.getBean("sysRoleMapper");
        Object userMapper = applicationContext.getBean("sysUserMapper");

        if (isAdmin(userId, roleMapper)) {
            return;
        }

        String originalSql = boundSql.getSql();
        String filterSql;

        // 判断是否为房产业务数据权限（有 allianceAlias 或 projectAlias）
        boolean isRealtyScope = StringUtils.hasText(dataScope.allianceAlias())
                || StringUtils.hasText(dataScope.projectAlias());

        if (isRealtyScope) {
            filterSql = buildRealtyDataScopeFilter(userId, dataScope, roleMapper);
        } else {
            filterSql = buildDataScopeFilter(userId, dataScope, roleMapper, userMapper);
        }

        if (StringUtils.hasText(filterSql)) {
            String newSql = "SELECT * FROM (" + originalSql + ") temp_data_scope WHERE " + filterSql;
            try {
                java.lang.reflect.Field field = boundSql.getClass().getDeclaredField("sql");
                field.setAccessible(true);
                field.set(boundSql, newSql);
            } catch (Exception e) {
                throw new SQLException("修改数据权限SQL失败", e);
            }
        }
    }

    private DataScope getDataScope(MappedStatement ms) {
        try {
            String id = ms.getId();
            String className = id.substring(0, id.lastIndexOf("."));
            String methodName = id.substring(id.lastIndexOf(".") + 1);
            Class<?> clazz = Class.forName(className);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName) && method.isAnnotationPresent(DataScope.class)) {
                    return method.getAnnotation(DataScope.class);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isAdmin(Long userId, Object roleMapper) {
        try {
            Method method = roleMapper.getClass().getMethod("selectRolesByUserId", Long.class);
            List<?> roles = (List<?>) method.invoke(roleMapper, userId);
            for (Object role : roles) {
                Method getCode = role.getClass().getMethod("getCode");
                if ("admin".equals(getCode.invoke(role))) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * 构建房产业务数据权限过滤条件。
     * <p>
     * 角色-数据范围映射：
     * <ul>
     *   <li>驻场 → 仅负责项目的客户（通过 customer_project + project 关联 creator_id）</li>
     *   <li>渠道 → 仅对接联盟商的客户（通过 alliance.channel_user_id）</li>
     *   <li>驻场总监/渠道总监/行政总监/行政文员 → 全部（不加过滤）</li>
     *   <li>联盟商负责人 → 仅本联盟商数据（通过 alliance.user_id）</li>
     * </ul>
     */
    private String buildRealtyDataScopeFilter(Long userId, DataScope dataScope, Object roleMapper) {
        try {
            Method selectRoles = roleMapper.getClass().getMethod("selectRolesByUserId", Long.class);
            List<?> roles = (List<?>) selectRoles.invoke(roleMapper, userId);
            if (CollectionUtils.isEmpty(roles)) {
                return DENY_ALL_FILTER;
            }

            List<String> roleCodes = new ArrayList<>();
            for (Object role : roles) {
                Method getCode = role.getClass().getMethod("getCode");
                String code = (String) getCode.invoke(role);
                if (code != null) {
                    roleCodes.add(code);
                }
            }

            // 总监/行政角色拥有全部数据权限，不加过滤
            if (roleCodes.contains(ROLE_RESIDENT_DIRECTOR)
                    || roleCodes.contains(ROLE_CHANNEL_DIRECTOR)
                    || roleCodes.contains(ROLE_ADMIN_DIRECTOR)
                    || roleCodes.contains(ROLE_ADMIN_CLERK)) {
                return "";
            }

            List<String> conditions = new ArrayList<>();

            // 驻场角色：仅负责项目的客户
            if (roleCodes.contains(ROLE_RESIDENT) && StringUtils.hasText(dataScope.projectAlias())) {
                String projectAlias = dataScope.projectAlias();
                // 通过 customer.id -> customer_project.customer_id -> project 关联筛选驻场负责项目的客户
                conditions.add(projectAlias + " IN ("
                        + "SELECT cp.customer_id FROM customer_project cp "
                        + "INNER JOIN project p ON cp.project_id = p.id "
                        + "WHERE p.contact_person = (SELECT nickname FROM sys_user WHERE id = " + userId + ")"
                        + " AND p.deleted = 0"
                        + ")");
            }

            // 渠道角色：仅对接联盟商的客户
            if (roleCodes.contains(ROLE_CHANNEL) && StringUtils.hasText(dataScope.allianceAlias())) {
                String allianceAlias = dataScope.allianceAlias();
                conditions.add(allianceAlias + " IN ("
                        + "SELECT id FROM alliance WHERE channel_user_id = " + userId
                        + " AND deleted = 0"
                        + ")");
            }

            // 联盟商负责人：仅本联盟商数据
            if (roleCodes.contains(ROLE_ALLIANCE_MANAGER) && StringUtils.hasText(dataScope.allianceAlias())) {
                String allianceAlias = dataScope.allianceAlias();
                conditions.add(allianceAlias + " IN ("
                        + "SELECT id FROM alliance WHERE user_id = " + userId
                        + " AND deleted = 0"
                        + ")");
            }

            if (conditions.isEmpty()) {
                // 角色不匹配任何规则时，拒绝访问
                return DENY_ALL_FILTER;
            }

            return "(" + String.join(" OR ", conditions) + ")";
        } catch (Exception e) {
            log.warn("构建房产业务数据权限过滤失败，按拒绝访问处理: userId={}", userId, e);
            return DENY_ALL_FILTER;
        }
    }

    /**
     * 构建通用数据权限过滤条件（基于部门的数据范围）
     */
    private String buildDataScopeFilter(Long userId, DataScope dataScope, Object roleMapper, Object userMapper) {
        try {
            Method selectRoles = roleMapper.getClass().getMethod("selectRolesByUserId", Long.class);
            List<?> roles = (List<?>) selectRoles.invoke(roleMapper, userId);
            if (CollectionUtils.isEmpty(roles)) return DENY_ALL_FILTER;

            List<String> conditions = new ArrayList<>();
            Long userDeptId = null;

            // 获取用户部门ID
            Method selectUser = userMapper.getClass().getMethod("selectById", java.io.Serializable.class);
            Object user = selectUser.invoke(userMapper, userId);
            if (user != null) {
                userDeptId = (Long) user.getClass().getMethod("getDeptId").invoke(user);
            }

            for (Object role : roles) {
                Integer scope = (Integer) role.getClass().getMethod("getDataScope").invoke(role);
                Long roleId = (Long) role.getClass().getMethod("getId").invoke(role);

                if (scope == null || scope == 1) return "";

                String deptAlias = StringUtils.hasText(dataScope.deptAlias()) ? dataScope.deptAlias() : "dept_id";
                if (scope == 2) {
                    conditions.add(deptAlias + " IN (SELECT dept_id FROM sys_role_dept WHERE role_id = " + roleId + ")");
                } else if (scope == 3 && userDeptId != null) {
                    conditions.add(deptAlias + " = " + userDeptId);
                } else if (scope == 4 && userDeptId != null) {
                    conditions.add(deptAlias + " IN (SELECT id FROM sys_dept WHERE id = " + userDeptId + " OR FIND_IN_SET(" + userDeptId + ", ancestors))");
                } else if (scope == 5) {
                    String userAlias = StringUtils.hasText(dataScope.userAlias()) ? dataScope.userAlias() : "create_by";
                    conditions.add(userAlias + " = " + userId);
                }
            }
            return conditions.isEmpty() ? DENY_ALL_FILTER : "(" + String.join(" OR ", conditions) + ")";
        } catch (Exception e) {
            log.warn("构建通用数据权限过滤失败，按拒绝访问处理: userId={}", userId, e);
            return DENY_ALL_FILTER;
        }
    }
}
