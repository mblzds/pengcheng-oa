package com.pengcheng.system.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.system.entity.SysPost;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.entity.SysUserPost;
import com.pengcheng.system.entity.SysUserRole;
import com.pengcheng.system.mapper.SysMenuMapper;
import com.pengcheng.system.mapper.SysUserMapper;
import com.pengcheng.system.mapper.SysUserPostMapper;
import com.pengcheng.system.mapper.SysUserRoleMapper;
import com.pengcheng.system.config.StpInterfaceImpl;
import com.pengcheng.system.service.SysUserService;
import com.pengcheng.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysUserPostMapper userPostMapper;
    private final com.pengcheng.system.mapper.SysPostMapper postMapper;
    private final SysMenuMapper menuMapper;
    private final SystemConfigHelper configHelper;

    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    public PageResult<SysUser> page(Integer page, Integer pageSize, String username, Integer status, String userType, Long deptId, Long postId, Long roleId) {
        Page<SysUser> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // sys_user u LEFT JOIN sys_dept d 后，status / create_time / deleted 等列在两表都存在，
        // 必须显式加 u. 前缀，否则 MySQL 会抛 "Column 'X' is ambiguous"
        wrapper.apply("u.deleted = 0")
                .apply(status != null, "u.status = {0}", status);
        wrapper.like(StringUtils.hasText(username), SysUser::getUsername, username)
                .eq(StringUtils.hasText(userType), SysUser::getUserType, userType)
                .eq(deptId != null, SysUser::getDeptId, deptId);

        // 按岗位 / 按角色：取关联用户 ID 的交集后注入 IN
        // 用字符串拼接而非 {0} 占位符 —— 后者会被 MP 当作单个参数绑定，IN ('1,2,3') 永远不匹配多 ID
        Set<Long> filterUserIds = null;
        if (postId != null) {
            filterUserIds = new HashSet<>(userPostMapper.selectUserIdsByPostId(postId));
        }
        if (roleId != null) {
            List<Long> roleUserIds = userRoleMapper.selectUserIdsByRoleId(roleId);
            if (filterUserIds == null) {
                filterUserIds = new HashSet<>(roleUserIds);
            } else {
                filterUserIds.retainAll(roleUserIds);
            }
        }
        if (filterUserIds != null) {
            if (filterUserIds.isEmpty()) {
                return PageResult.empty();
            }
            String idList = filterUserIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            wrapper.apply("u.id IN (" + idList + ")");
        }

        wrapper.last("ORDER BY u.create_time DESC");

        // 切换到自定义 @DataScope 拦截器
        IPage<SysUser> result = baseMapper.selectUserPage(pageParam, wrapper);

        // 获取岗位映射
        List<SysPost> allPosts = postMapper.selectList(null);
        Map<Long, String> postMap = allPosts.stream().collect(Collectors.toMap(SysPost::getId, SysPost::getPostName));

        // 清空密码，填充部门名称
        result.getRecords().forEach(user -> {
            user.setPassword(null);
            List<Long> userPostIds = getPostIds(user.getId());
            if (userPostIds != null && !userPostIds.isEmpty()) {
                String postNames = userPostIds.stream()
                        .map(postMap::get)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.joining(","));
                user.setPostNames(postNames);
            }
        });
        return PageResult.of(result);
    }

    @Override
    public SysUser getDetail(Long id) {
        SysUser user = this.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(SysUser user, List<Long> roleIds, List<Long> postIds) {
        checkUsernameUnique(user.getUsername(), null);
        checkPhoneUnique(user.getPhone(), null);
        checkOpenIdUnique(user.getOpenId(), null);
        // 工号统一由系统自动生成 EMP+id 4 位补零，HR 不录入；以防前端误传，先清空
        user.setEmployeeNo(null);
        // 加密密码
        String password = StringUtils.hasText(user.getPassword()) ? user.getPassword() : DEFAULT_PASSWORD;
        user.setPassword(BCrypt.hashpw(password));
        this.save(user);
        SysUser patch = new SysUser();
        patch.setId(user.getId());
        patch.setEmployeeNo(String.format("EMP%04d", user.getId()));
        this.updateById(patch);
        user.setEmployeeNo(patch.getEmployeeNo());
        // 保存用户角色关联
        saveUserRoles(user.getId(), roleIds);
        // 保存用户岗位关联
        saveUserPosts(user.getId(), postIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysUser user, List<Long> roleIds, List<Long> postIds) {
        SysUser existUser = this.getById(user.getId());
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }
        checkUsernameUnique(user.getUsername(), user.getId());
        checkPhoneUnique(user.getPhone(), user.getId());
        checkOpenIdUnique(user.getOpenId(), user.getId());
        if (StringUtils.hasText(user.getEmployeeNo())) {
            checkEmployeeNoUnique(user.getEmployeeNo(), user.getId());
        }
        // 不更新密码
        user.setPassword(null);
        this.updateById(user);
        // 更新用户角色关联
        userRoleMapper.deleteByUserId(user.getId());
        saveUserRoles(user.getId(), roleIds);
        // 更新用户岗位关联
        userPostMapper.deleteByUserId(user.getId());
        saveUserPosts(user.getId(), postIds);
        // 角色变更，清除该用户的权限缓存
        StpInterfaceImpl.clearPermissionCache(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        this.removeById(id);
        userRoleMapper.deleteByUserId(id);
        userPostMapper.deleteByUserId(id);
        // 用户删除，清除权限缓存
        StpInterfaceImpl.clearPermissionCache(id);
    }

    @Override
    public SysUser getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        return baseMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public List<String> getPermissions(Long userId) {
        List<String> roleCodes = baseMapper.selectRoleCodesByUserId(userId);
        // 超级管理员(admin)拥有所有权限，不依赖 sys_role_menu 是否分配
        if (roleCodes != null && roleCodes.contains("admin")) {
            return menuMapper.selectAllPermissions();
        }
        return baseMapper.selectPermissionsByUserId(userId);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        // 验证新密码规则
        configHelper.validatePassword(newPassword);

        user.setPassword(BCrypt.hashpw(newPassword));
        this.updateById(user);
    }

    @Override
    public void resetPassword(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(BCrypt.hashpw(DEFAULT_PASSWORD));
        this.updateById(user);
    }

    @Override
    public void updateProfile(Long userId, SysUser profile) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        checkPhoneUnique(profile.getPhone(), userId);
        // 只允许更新昵称、邮箱、手机号、头像
        user.setNickname(profile.getNickname());
        user.setEmail(profile.getEmail());
        user.setPhone(profile.getPhone());
        user.setAvatar(profile.getAvatar());
        this.updateById(user);
    }

    @Override
    public List<SysUser> listAll() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getStatus, 1);
        wrapper.orderByAsc(SysUser::getId);
        List<SysUser> users = this.list(wrapper);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @Override
    public SysUser getByOpenId(String openId) {
        return this.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getOpenId, openId));
    }

    @Override
    public List<Long> getPostIds(Long userId) {
        return userPostMapper.selectList(new LambdaQueryWrapper<SysUserPost>()
                .eq(SysUserPost::getUserId, userId))
                .stream()
                .map(SysUserPost::getPostId)
                .collect(Collectors.toList());
    }

    private void checkUsernameUnique(String username, Long excludeId) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        if (baseMapper.countByUsernameAll(username, excludeId) > 0) {
            throw new BusinessException("用户名已被占用");
        }
    }

    private void checkPhoneUnique(String phone, Long excludeId) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        if (baseMapper.countByPhoneAll(phone, excludeId) > 0) {
            throw new BusinessException("手机号已被占用");
        }
    }

    private void checkOpenIdUnique(String openId, Long excludeId) {
        if (!StringUtils.hasText(openId)) {
            return;
        }
        if (baseMapper.countByOpenIdAll(openId, excludeId) > 0) {
            throw new BusinessException("微信账号已被绑定到其他用户");
        }
    }

    private void checkEmployeeNoUnique(String employeeNo, Long excludeId) {
        if (!StringUtils.hasText(employeeNo)) {
            return;
        }
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser> w =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmployeeNo, employeeNo);
        if (excludeId != null) w.ne(SysUser::getId, excludeId);
        if (baseMapper.selectCount(w) > 0) {
            throw new BusinessException("工号已被占用：" + employeeNo);
        }
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }
    }

    private void saveUserPosts(Long userId, List<Long> postIds) {
        if (postIds != null && !postIds.isEmpty()) {
            for (Long postId : postIds) {
                SysUserPost userPost = new SysUserPost();
                userPost.setUserId(userId);
                userPost.setPostId(postId);
                userPostMapper.insert(userPost);
            }
        }
    }
}
