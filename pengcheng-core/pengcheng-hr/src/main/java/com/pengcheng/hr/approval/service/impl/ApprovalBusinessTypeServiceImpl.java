package com.pengcheng.hr.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.approval.entity.ApprovalBusinessType;
import com.pengcheng.hr.approval.mapper.ApprovalBusinessTypeMapper;
import com.pengcheng.hr.approval.service.ApprovalBusinessTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ApprovalBusinessTypeServiceImpl implements ApprovalBusinessTypeService {

    private final ApprovalBusinessTypeMapper mapper;

    /** business_type key 命名约束：以小写字母开头，可含小写字母/数字/下划线；1~64 字符 */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");

    @Override
    public List<ApprovalBusinessType> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<ApprovalBusinessType>()
                .eq(ApprovalBusinessType::getEnabled, 1)
                .orderByAsc(ApprovalBusinessType::getSort)
                .orderByAsc(ApprovalBusinessType::getId));
    }

    @Override
    public ApprovalBusinessType getByKey(String businessType) {
        if (businessType == null || businessType.isBlank()) return null;
        return mapper.selectOne(new LambdaQueryWrapper<ApprovalBusinessType>()
                .eq(ApprovalBusinessType::getBusinessType, businessType)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional
    public Long create(ApprovalBusinessType bean) {
        validate(bean, true);
        if (getByKey(bean.getBusinessType()) != null) {
            throw new IllegalArgumentException("业务类型 key 已存在：" + bean.getBusinessType());
        }
        // 新建的业务类型一律 builtin=0；只有 V62 seed 行 builtin=1
        bean.setBuiltin(0);
        if (bean.getEnabled() == null) bean.setEnabled(1);
        if (bean.getSort() == null) bean.setSort(100);
        mapper.insert(bean);
        return bean.getId();
    }

    @Override
    @Transactional
    public void update(ApprovalBusinessType bean) {
        if (bean.getId() == null) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        ApprovalBusinessType existing = mapper.selectById(bean.getId());
        if (existing == null) {
            throw new IllegalArgumentException("业务类型不存在");
        }
        validate(bean, false);
        // 内置类型禁改 key（label/description/sort/enabled 仍可改）
        if (existing.getBuiltin() != null && existing.getBuiltin() == 1
                && bean.getBusinessType() != null
                && !bean.getBusinessType().equals(existing.getBusinessType())) {
            throw new IllegalArgumentException("内置业务类型不允许修改 key");
        }
        // 改 key 时检查唯一性
        if (bean.getBusinessType() != null
                && !bean.getBusinessType().equals(existing.getBusinessType())) {
            ApprovalBusinessType clash = getByKey(bean.getBusinessType());
            if (clash != null && !clash.getId().equals(existing.getId())) {
                throw new IllegalArgumentException("业务类型 key 已存在：" + bean.getBusinessType());
            }
            existing.setBusinessType(bean.getBusinessType());
        }
        if (bean.getLabel() != null) existing.setLabel(bean.getLabel());
        if (bean.getDescription() != null) existing.setDescription(bean.getDescription());
        if (bean.getSort() != null) existing.setSort(bean.getSort());
        if (bean.getEnabled() != null) existing.setEnabled(bean.getEnabled());
        // builtin 不允许通过此接口修改，防越权
        mapper.updateById(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null) return;
        ApprovalBusinessType existing = mapper.selectById(id);
        if (existing == null) return;
        if (existing.getBuiltin() != null && existing.getBuiltin() == 1) {
            throw new IllegalArgumentException("内置业务类型「" + existing.getLabel() + "」不允许删除");
        }
        // 软删除（@TableLogic 自动）
        mapper.deleteById(id);
    }

    private void validate(ApprovalBusinessType bean, boolean isCreate) {
        if (bean == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (isCreate || bean.getBusinessType() != null) {
            if (bean.getBusinessType() == null || bean.getBusinessType().isBlank()) {
                throw new IllegalArgumentException("业务类型 key 不能为空");
            }
            if (!KEY_PATTERN.matcher(bean.getBusinessType()).matches()) {
                throw new IllegalArgumentException("业务类型 key 仅支持小写字母 / 数字 / 下划线，且必须以字母开头");
            }
        }
        if (isCreate) {
            if (bean.getLabel() == null || bean.getLabel().isBlank()) {
                throw new IllegalArgumentException("显示名不能为空");
            }
        } else if (bean.getLabel() != null && bean.getLabel().isBlank()) {
            throw new IllegalArgumentException("显示名不能为空");
        }
    }
}
