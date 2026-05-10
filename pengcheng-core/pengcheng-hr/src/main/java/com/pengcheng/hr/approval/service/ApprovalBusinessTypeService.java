package com.pengcheng.hr.approval.service;

import com.pengcheng.hr.approval.entity.ApprovalBusinessType;

import java.util.List;

/**
 * 业务类型注册表服务（管理后台 tab 列表的源头）。
 * 解耦 ApprovalFlowServiceImpl 里硬编码的 LinkedHashMap，让运营增删类型不再需要改代码。
 */
public interface ApprovalBusinessTypeService {

    /** 列出全部启用 + 未删除的业务类型，按 sort 升序 */
    List<ApprovalBusinessType> listAll();

    /** 按 key 查询（含已禁用 / 已删除时返回 null） */
    ApprovalBusinessType getByKey(String businessType);

    /** 新建一种业务类型；business_type 必须满足 {@code [a-z][a-z0-9_]{0,63}} 且不重复 */
    Long create(ApprovalBusinessType bean);

    /** 更新 label / description / sort / enabled。内置类型禁止改 business_type key */
    void update(ApprovalBusinessType bean);

    /** 软删除某业务类型；内置类型禁止删除 */
    void delete(Long id);
}
