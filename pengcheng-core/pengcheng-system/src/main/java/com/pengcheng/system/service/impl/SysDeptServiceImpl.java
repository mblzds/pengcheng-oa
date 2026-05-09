package com.pengcheng.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.system.entity.SysDept;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.mapper.SysDeptMapper;
import com.pengcheng.system.mapper.SysUserMapper;
import com.pengcheng.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门服务实现
 */
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    private final SysUserMapper userMapper;

    @Override
    public List<SysDept> tree(String deptName, Integer status) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(deptName), SysDept::getDeptName, deptName)
                .eq(status != null, SysDept::getStatus, status)
                .orderByAsc(SysDept::getSort);
        List<SysDept> depts = this.list(wrapper);
        return buildTree(depts);
    }

    @Override
    public List<SysDept> listAll() {
        return this.list(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort));
    }

    @Override
    public void create(SysDept dept) {
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
            dept.setAncestors("0");
        }
        validateSiblingName(dept.getDeptName(), dept.getParentId(), null);
        validateLeader(dept);
        if (dept.getParentId() > 0) {
            SysDept parent = this.getById(dept.getParentId());
            if (parent == null) {
                throw new BusinessException("父部门不存在");
            }
            dept.setAncestors(parent.getAncestors() + "," + dept.getParentId());
        } else {
            dept.setAncestors("0");
        }
        this.save(dept);
    }

    @Override
    public void update(SysDept dept) {
        SysDept existDept = this.getById(dept.getId());
        if (existDept == null) {
            throw new BusinessException("部门不存在");
        }
        if (dept.getParentId() != null && dept.getParentId().equals(dept.getId())) {
            throw new BusinessException("上级部门不能选择自己");
        }
        Long effectiveParentId = dept.getParentId() != null ? dept.getParentId() : existDept.getParentId();
        validateSiblingName(dept.getDeptName(), effectiveParentId, dept.getId());
        validateLeader(dept);
        // 更新祖级列表
        if (dept.getParentId() != null && !dept.getParentId().equals(existDept.getParentId())) {
            if (dept.getParentId() == 0) {
                dept.setAncestors("0");
            } else {
                SysDept parent = this.getById(dept.getParentId());
                if (parent == null) {
                    throw new BusinessException("父部门不存在");
                }
                dept.setAncestors(parent.getAncestors() + "," + dept.getParentId());
            }
        }
        this.updateById(dept);
    }

    /**
     * 同父部门下 dept_name 不可重名（忽略首尾空格、忽略已删除）
     */
    private void validateSiblingName(String deptName, Long parentId, Long excludeId) {
        if (!StringUtils.hasText(deptName)) {
            throw new BusinessException("部门名称不能为空");
        }
        String trimmed = deptName.trim();
        long count = this.count(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, parentId == null ? 0L : parentId)
                .eq(SysDept::getDeptName, trimmed)
                .ne(excludeId != null, SysDept::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("同一上级部门下已存在同名部门");
        }
    }

    /**
     * 校验负责人：可选；填了则要求用户存在且启用，并用 nickname 回填 leader 字段。
     * 留空允许：新建部门时通常还没成员可选，先建后再编辑。
     */
    private void validateLeader(SysDept dept) {
        if (dept.getLeaderId() == null) {
            // 允许留空，但同步把 legacy 文本字段也清掉避免不一致
            dept.setLeader(null);
            return;
        }
        SysUser leader = userMapper.selectById(dept.getLeaderId());
        if (leader == null) {
            throw new BusinessException("所选负责人不存在或已删除");
        }
        if (leader.getStatus() != null && leader.getStatus() != 1) {
            throw new BusinessException("所选负责人未启用，无法担任部门负责人");
        }
        if (StringUtils.hasText(leader.getNickname())) {
            dept.setLeader(leader.getNickname());
        } else if (StringUtils.hasText(leader.getUsername())) {
            dept.setLeader(leader.getUsername());
        }
    }

    @Override
    public void delete(Long id) {
        // 检查是否有子部门
        long count = this.count(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (count > 0) {
            throw new BusinessException("存在子部门，无法删除");
        }
        // 检查是否有成员，避免软删除后留下"鬼魂部门"（用户 dept_id 指向已删除部门）
        long memberCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (memberCount > 0) {
            throw new BusinessException("部门下还有 " + memberCount
                    + " 名成员，请先到用户管理把这些成员转移到其他部门后再删除");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, Long parentId, Integer sort) {
        SysDept dept = this.getById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        if (id.equals(parentId)) {
            throw new BusinessException("上级部门不能选择自己");
        }

        // 修改父ID和排序
        dept.setParentId(parentId);
        if (sort != null) {
            dept.setSort(sort);
        }
        
        // 更新祖级列表
        if (parentId == 0) {
            dept.setAncestors("0");
        } else {
            SysDept parent = this.getById(parentId);
            if (parent == null) {
                throw new BusinessException("父部门不存在");
            }
            dept.setAncestors(parent.getAncestors() + "," + parentId);
        }
        
        // 显式更新，确保字段变更被持久化
        this.updateById(dept);
        
        // 递归更新子部门的 ancestors
        updateChildAncestors(dept);
    }

    /**
     * 递归更新子部门的祖级列表
     */
    private void updateChildAncestors(SysDept parentDept) {
        List<SysDept> children = this.list(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, parentDept.getId()));
        for (SysDept child : children) {
            child.setAncestors(parentDept.getAncestors() + "," + parentDept.getId());
            this.updateById(child);
            updateChildAncestors(child);
        }
    }

    /**
     * 构建部门树
     */
    private List<SysDept> buildTree(List<SysDept> depts) {
        List<SysDept> tree = new ArrayList<>();
        for (SysDept dept : depts) {
            if (dept.getParentId() == null || dept.getParentId() == 0) {
                dept.setChildren(getChildren(dept.getId(), depts));
                tree.add(dept);
            }
        }
        return tree;
    }

    /**
     * 递归获取子部门
     */
    private List<SysDept> getChildren(Long parentId, List<SysDept> depts) {
        List<SysDept> children = new ArrayList<>();
        for (SysDept dept : depts) {
            if (parentId.equals(dept.getParentId())) {
                dept.setChildren(getChildren(dept.getId(), depts));
                children.add(dept);
            }
        }
        return children.isEmpty() ? null : children;
    }
}
