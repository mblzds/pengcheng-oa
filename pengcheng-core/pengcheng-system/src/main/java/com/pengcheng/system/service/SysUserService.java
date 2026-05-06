package com.pengcheng.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.system.entity.SysUser;

import java.util.List;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 分页查询用户
     */
    PageResult<SysUser> page(Integer page, Integer pageSize, String username, Integer status, String userType, Long deptId, Long postId);

    /**
     * 获取用户详情（包含角色和岗位）
     */
    SysUser getDetail(Long id);

    /**
     * 创建用户
     */
    void create(SysUser user, List<Long> roleIds, List<Long> postIds);

    /**
     * 更新用户
     */
    void update(SysUser user, List<Long> roleIds, List<Long> postIds);

    /**
     * 删除用户
     */
    void delete(Long id);

    /**
     * 根据用户名获取用户
     */
    SysUser getByUsername(String username);

    /**
     * 获取用户角色编码列表
     */
    List<String> getRoleCodes(Long userId);

    /**
     * 获取用户权限标识列表
     */
    List<String> getPermissions(Long userId);

    /**
     * 修改密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 重置密码
     */
    void resetPassword(Long userId);

    /**
     * 更新个人信息
     */
    void updateProfile(Long userId, SysUser user);

    /**
     * 获取所有用户列表
     */
    List<SysUser> listAll();

    /**
     * 获取用户岗位ID列表
     */
    List<Long> getPostIds(Long userId);

    /**
     * 根据微信openId获取用户
     */
    SysUser getByOpenId(String openId);
}
