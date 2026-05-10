package com.pengcheng.hr.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.event.DataChangeEvent;
import com.pengcheng.hr.approval.constant.ApprovalConstants;
import com.pengcheng.hr.approval.entity.ApprovalBusinessType;
import com.pengcheng.hr.approval.entity.ApprovalRecordNode;
import com.pengcheng.hr.approval.entity.GeneralApprovalRequest;
import com.pengcheng.hr.approval.event.ApprovalFinalizedEvent;
import com.pengcheng.hr.approval.mapper.GeneralApprovalRequestMapper;
import com.pengcheng.hr.approval.service.ApprovalBusinessTypeService;
import com.pengcheng.hr.approval.service.ApprovalFlowService;
import com.pengcheng.hr.approval.service.GeneralApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GeneralApprovalServiceImpl implements GeneralApprovalService {

    private final GeneralApprovalRequestMapper mapper;
    private final ApprovalBusinessTypeService businessTypeService;
    private final ApprovalFlowService approvalFlowService;
    private final ApplicationEventPublisher eventPublisher;

    /** 内置业务类型在 GeneralApprovalRequest 这条线没有承接表，这里硬保护下 */
    private static final Set<String> BUILTIN_TYPES = Set.of(
            ApprovalConstants.BUSINESS_TYPE_LEAVE,
            ApprovalConstants.BUSINESS_TYPE_COMPENSATE,
            ApprovalConstants.BUSINESS_TYPE_EXPENSE,
            ApprovalConstants.BUSINESS_TYPE_ADVANCE,
            ApprovalConstants.BUSINESS_TYPE_PREPAY);

    @Override
    @Transactional
    public Long submit(String businessType, Long applicantId, String title, String description) {
        if (businessType == null || businessType.isBlank()) {
            throw new IllegalArgumentException("业务类型不能为空");
        }
        if (BUILTIN_TYPES.contains(businessType)) {
            throw new IllegalArgumentException("内置业务类型「" + businessType + "」请使用专属提交入口");
        }
        ApprovalBusinessType bt = businessTypeService.getByKey(businessType);
        if (bt == null || bt.getDeleted() != null && bt.getDeleted() == 1
                || bt.getEnabled() != null && bt.getEnabled() == 0) {
            throw new IllegalArgumentException("业务类型不存在或已停用：" + businessType);
        }
        if (applicantId == null) {
            throw new IllegalArgumentException("申请人不能为空");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("申请标题不能为空");
        }

        GeneralApprovalRequest req = new GeneralApprovalRequest();
        req.setBusinessType(businessType);
        req.setApplicantId(applicantId);
        req.setTitle(title.trim());
        req.setDescription(description != null ? description.trim() : null);
        req.setStatus(ApprovalConstants.STATUS_PENDING);
        mapper.insert(req);

        // 实例化审批流
        approvalFlowService.start(businessType, req.getId(), applicantId);

        // 启动事件可能在 start() 内部跳过所有节点（候选人都=申请人）→ 自动通过 → 引擎已经发布
        // ApprovalFinalizedEvent；监听器把 status 同步翻成 STATUS_APPROVED；这里不再手动改

        eventPublisher.publishEvent(new DataChangeEvent(this, "create", "general-approval", req.getId()));
        return req.getId();
    }

    @Override
    @Transactional
    public void cancel(Long requestId, Long applicantId) {
        if (requestId == null || applicantId == null) {
            throw new IllegalArgumentException("撤销参数不完整");
        }
        GeneralApprovalRequest req = mapper.selectById(requestId);
        if (req == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        if (!Objects.equals(req.getApplicantId(), applicantId)) {
            throw new IllegalStateException("仅申请人可撤销自己的申请");
        }
        if (req.getStatus() == null || req.getStatus() != ApprovalConstants.STATUS_PENDING) {
            throw new IllegalStateException("当前申请已审批完成或已撤销，不可撤销");
        }
        approvalFlowService.cancelPendingNodes(req.getBusinessType(), requestId, applicantId);
        req.setStatus(ApprovalConstants.STATUS_CANCELLED);
        mapper.updateById(req);
        eventPublisher.publishEvent(new DataChangeEvent(this, "update", "general-approval", requestId));
    }

    @Override
    @Transactional
    public void approve(Long requestId, Long approverId, boolean approved, String remark) {
        if (requestId == null) throw new IllegalArgumentException("申请ID不能为空");
        GeneralApprovalRequest req = mapper.selectById(requestId);
        if (req == null) throw new IllegalArgumentException("申请不存在");
        if (req.getStatus() == null || req.getStatus() != ApprovalConstants.STATUS_PENDING) {
            throw new IllegalStateException("该申请已完成审批或已撤销，不可重复操作");
        }
        ApprovalRecordNode current = approvalFlowService.getCurrentNode(req.getBusinessType(), requestId);
        if (current == null) {
            throw new IllegalStateException("该申请已无可处理审批节点");
        }
        approvalFlowService.approve(current.getId(), approverId, approved, remark);
        // 由 listener 在 finalize 事件里回写 status；中间态 (审批通过但还有下一节点) 仍然是 PENDING
    }

    @Override
    public GeneralApprovalRequest getById(Long id) {
        if (id == null) return null;
        return mapper.selectById(id);
    }

    @Override
    public IPage<GeneralApprovalRequest> pageMine(Long applicantId, String businessType, Integer status,
                                                  long page, long pageSize) {
        LambdaQueryWrapper<GeneralApprovalRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GeneralApprovalRequest::getApplicantId, applicantId);
        if (businessType != null && !businessType.isBlank()) {
            wrapper.eq(GeneralApprovalRequest::getBusinessType, businessType);
        }
        if (status != null) {
            wrapper.eq(GeneralApprovalRequest::getStatus, status);
        }
        wrapper.orderByDesc(GeneralApprovalRequest::getCreateTime);
        return mapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 引擎终态事件 → 把 general_approval_request.status 同步翻转。
     * 仅处理非内置 business_type；内置类型由 PaymentService / leaveRequest 各自直接 setStatus。
     */
    @EventListener
    @Transactional
    public void onApprovalFinalized(ApprovalFinalizedEvent event) {
        String bt = event.getBusinessType();
        if (bt == null || BUILTIN_TYPES.contains(bt)) return;
        GeneralApprovalRequest req = mapper.selectOne(
                new LambdaQueryWrapper<GeneralApprovalRequest>()
                        .eq(GeneralApprovalRequest::getId, event.getBusinessId())
                        .eq(GeneralApprovalRequest::getBusinessType, bt)
                        .last("LIMIT 1"));
        if (req == null) return;
        // 已撤销的不被覆盖（撤销路径在 cancel() 里同步事务里改了 status，但 listener 异步可能晚到也无所谓）
        if (req.getStatus() != null && req.getStatus() == ApprovalConstants.STATUS_CANCELLED) return;
        req.setStatus(event.isApproved() ? ApprovalConstants.STATUS_APPROVED : ApprovalConstants.STATUS_REJECTED);
        mapper.updateById(req);
        eventPublisher.publishEvent(new DataChangeEvent(this, "update", "general-approval", req.getId()));
    }
}
