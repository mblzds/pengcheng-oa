package com.pengcheng.realty.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.event.DataChangeEvent;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.hr.approval.constant.ApprovalConstants;
import com.pengcheng.hr.approval.entity.ApprovalRecordNode;
import com.pengcheng.hr.approval.service.ApprovalFlowService;
import com.pengcheng.realty.alliance.entity.Alliance;
import com.pengcheng.realty.alliance.mapper.AllianceMapper;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.common.exception.ApprovalFlowException;
import com.pengcheng.realty.payment.dto.*;
import com.pengcheng.realty.payment.entity.PayNotifyLog;
import com.pengcheng.realty.payment.entity.PaymentApproval;
import com.pengcheng.realty.payment.entity.PaymentRequest;
import com.pengcheng.realty.payment.mapper.PayNotifyLogMapper;
import com.pengcheng.realty.payment.mapper.PaymentApprovalMapper;
import com.pengcheng.realty.payment.mapper.PaymentRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 付款申请管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRequestMapper paymentRequestMapper;
    private final PaymentApprovalMapper paymentApprovalMapper;
    private final PayNotifyLogMapper payNotifyLogMapper;
    private final CustomerDealMapper customerDealMapper;
    private final AllianceMapper allianceMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ApprovalFlowService approvalFlowService;

    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 支付状态：未付款 */
    public static final int PAY_STATUS_UNPAID = 0;
    /** 支付状态：付款中 */
    public static final int PAY_STATUS_PAYING = 1;
    /** 支付状态：已付款 */
    public static final int PAY_STATUS_PAID = 2;
    /** 支付状态：已退款 */
    public static final int PAY_STATUS_REFUNDED = 3;
    /** 支付状态：失败 */
    public static final int PAY_STATUS_FAILED = 4;

    /** 申请类型：费用报销 */
    public static final int TYPE_EXPENSE = 1;
    /** 申请类型：垫佣 */
    public static final int TYPE_ADVANCE_COMMISSION = 2;
    /** 申请类型：预付佣 */
    public static final int TYPE_PREPAY_COMMISSION = 3;

    /** 审批状态：待审批 */
    public static final int STATUS_PENDING = 1;
    /** 审批状态：审批中 */
    public static final int STATUS_IN_PROGRESS = 2;
    /** 审批状态：已通过 */
    public static final int STATUS_APPROVED = 3;
    /** 审批状态：已驳回 */
    public static final int STATUS_REJECTED = 4;
    /** 审批状态：申请人主动撤销（仅审批中可撤；已通过且未付款不在此功能范围内） */
    public static final int STATUS_CANCELLED = 5;

    /** 审批结果：通过 */
    public static final int APPROVAL_RESULT_PASS = 1;
    /** 审批结果：驳回 */
    public static final int APPROVAL_RESULT_REJECT = 2;

    /**
     * 创建付款申请
     */
    @Transactional
    public Long createPaymentRequest(PaymentRequestDTO dto) {
        validatePaymentRequest(dto);

        PaymentRequest request = PaymentRequest.builder()
                .orderNo(generateOrderNo())
                .payStatus(PAY_STATUS_UNPAID)
                .applicantId(dto.getApplicantId())
                .requestType(dto.getRequestType())
                .expenseType(dto.getExpenseType())
                .amount(dto.getAmount())
                .description(dto.getDescription())
                .relatedDealId(dto.getRelatedDealId())
                .relatedAllianceId(dto.getRelatedAllianceId())
                .attachments(dto.getAttachments())
                .status(STATUS_PENDING)
                .build();
        paymentRequestMapper.insert(request);
        // 实例化审批流（按 approval_flow_node 模板生成 record_node 链，路由到首位审批人）
        approvalFlowService.start(businessTypeOf(dto.getRequestType()), request.getId(), dto.getApplicantId());
        // 广播数据变更事件
        eventPublisher.publishEvent(new DataChangeEvent(this, "create", "payment", request.getId()));
        return request.getId();
    }

    /**
     * request_type → approval_flow_node.business_type 映射
     */
    public static String businessTypeOf(Integer requestType) {
        if (requestType == null) {
            throw new IllegalArgumentException("申请类型不能为空");
        }
        return switch (requestType) {
            case TYPE_EXPENSE -> ApprovalConstants.BUSINESS_TYPE_EXPENSE;
            case TYPE_ADVANCE_COMMISSION -> ApprovalConstants.BUSINESS_TYPE_ADVANCE;
            case TYPE_PREPAY_COMMISSION -> ApprovalConstants.BUSINESS_TYPE_PREPAY;
            default -> throw new IllegalArgumentException("无效的申请类型：" + requestType);
        };
    }

    /**
     * 审批付款申请（通过/驳回）
     * 改造后：节点流转完全由 ApprovalFlowService 引擎驱动（按 approval_flow_node 模板路由到候选人，
     * 并校验 approver 必须是当前节点候选人）。本方法只负责把"引擎判定的整单状态"回写到 payment_request.status：
     *   驳回 → STATUS_REJECTED；通过且后续无节点 → STATUS_APPROVED；通过但还有后续节点 → STATUS_IN_PROGRESS。
     */
    @Transactional
    public void approvePaymentRequest(PaymentApprovalDTO dto) {
        if (dto.getRequestId() == null) {
            throw new IllegalArgumentException("付款申请ID不能为空");
        }
        if (dto.getApproverId() == null) {
            throw new IllegalArgumentException("审批人ID不能为空");
        }
        if (dto.getApproved() == null) {
            throw new IllegalArgumentException("审批结果不能为空");
        }
        if (!dto.getApproved() && (dto.getRemark() == null || dto.getRemark().isBlank())) {
            throw new IllegalArgumentException("驳回时必须填写驳回原因");
        }

        PaymentRequest request = paymentRequestMapper.selectById(dto.getRequestId());
        if (request == null) {
            throw new IllegalArgumentException("付款申请不存在");
        }
        if (request.getStatus() == STATUS_APPROVED
                || request.getStatus() == STATUS_REJECTED
                || request.getStatus() == STATUS_CANCELLED) {
            throw new ApprovalFlowException("该申请已完成审批或已撤销，不可重复操作");
        }

        String businessType = businessTypeOf(request.getRequestType());
        ApprovalRecordNode current = approvalFlowService.getCurrentNode(businessType, request.getId());
        if (current == null) {
            throw new ApprovalFlowException("该申请已无可处理审批节点");
        }
        // 引擎内部完成：候选人校验、写节点 result/approver_id/remark/approval_time、若末节点通过则在 finalizeBusiness 发事件
        approvalFlowService.approve(current.getId(), dto.getApproverId(), dto.getApproved(), dto.getRemark());

        // 由引擎当前剩余节点状态推导整单状态（finalizeBusiness 不再回写付款表）
        if (!dto.getApproved()) {
            request.setStatus(STATUS_REJECTED);
        } else {
            ApprovalRecordNode next = approvalFlowService.getCurrentNode(businessType, request.getId());
            request.setStatus(next == null ? STATUS_APPROVED : STATUS_IN_PROGRESS);
        }
        paymentRequestMapper.updateById(request);
        // 广播数据变更事件
        eventPublisher.publishEvent(new DataChangeEvent(this, "update", "payment", request.getId()));
    }

    /**
     * 申请人主动撤销付款申请。
     * 仅当 status ∈ {待审批, 审批中} 且尚未实际付款（pay_status=未付款）时允许撤销。
     */
    @Transactional
    public void cancelPaymentRequest(Long requestId, Long applicantId) {
        if (requestId == null || applicantId == null) {
            throw new IllegalArgumentException("撤销参数不完整");
        }
        PaymentRequest request = paymentRequestMapper.selectById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("付款申请不存在");
        }
        if (!applicantId.equals(request.getApplicantId())) {
            throw new ApprovalFlowException("仅申请人可撤销自己的申请");
        }
        if (request.getStatus() == null
                || (request.getStatus() != STATUS_PENDING && request.getStatus() != STATUS_IN_PROGRESS)) {
            throw new ApprovalFlowException("当前申请已审批完成或已撤销，不可撤销");
        }
        // 已发起支付（即使审批未完成，正常流程也不会走到这里）必须拦住，避免与回调入账并发
        if (request.getPayStatus() != null && request.getPayStatus() != PAY_STATUS_UNPAID) {
            throw new ApprovalFlowException("已发起或完成付款，不可撤销");
        }
        // 把引擎里仍未审批的节点终态化为「已撤销」，使其从所有审批人待办列表消失
        approvalFlowService.cancelPendingNodes(businessTypeOf(request.getRequestType()), request.getId(), applicantId);
        request.setStatus(STATUS_CANCELLED);
        paymentRequestMapper.updateById(request);
        eventPublisher.publishEvent(new DataChangeEvent(this, "update", "payment", request.getId()));
    }

    /**
     * 分页查询付款申请
     */
    public PageResult<PaymentVO> pagePaymentRequests(PaymentQueryDTO query) {
        LambdaQueryWrapper<PaymentRequest> wrapper = new LambdaQueryWrapper<>();
        if (query.getRequestType() != null) {
            wrapper.eq(PaymentRequest::getRequestType, query.getRequestType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(PaymentRequest::getStatus, query.getStatus());
        }
        if (query.getApplicantId() != null) {
            wrapper.eq(PaymentRequest::getApplicantId, query.getApplicantId());
        }
        wrapper.orderByDesc(PaymentRequest::getCreateTime);

        IPage<PaymentRequest> page = paymentRequestMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<PaymentVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 按主键查询付款申请。
     */
    public PaymentRequest getPaymentRequestById(Long requestId) {
        if (requestId == null) {
            return null;
        }
        return paymentRequestMapper.selectById(requestId);
    }

    /**
     * 查询付款申请的审批记录。改造后审批由 ApprovalFlowService 引擎驱动，
     * 历史从 approval_record_node 取（仅返回已审批节点 result IS NOT NULL）。
     * 改造前的旧申请没有 record_node，回退到 payment_approval 表保留可读性。
     */
    public List<PaymentApproval> getApprovalHistory(Long requestId) {
        if (requestId == null) {
            return java.util.Collections.emptyList();
        }
        PaymentRequest request = paymentRequestMapper.selectById(requestId);
        if (request != null && request.getRequestType() != null) {
            String businessType = businessTypeOf(request.getRequestType());
            List<ApprovalRecordNode> nodes = approvalFlowService.listRecordNodes(businessType, requestId);
            List<PaymentApproval> fromEngine = nodes.stream()
                    .filter(n -> n.getResult() != null)
                    .map(n -> PaymentApproval.builder()
                            .requestId(requestId)
                            .approverId(n.getApproverId())
                            .result(n.getResult())
                            .remark(n.getRemark())
                            .approvalOrder(n.getSeq())
                            .approvalTime(n.getApprovalTime())
                            .build())
                    .toList();
            if (!fromEngine.isEmpty()) {
                return fromEngine;
            }
        }
        // 回退：改造前旧数据走 payment_approval 表
        return paymentApprovalMapper.selectList(
                new LambdaQueryWrapper<PaymentApproval>()
                        .eq(PaymentApproval::getRequestId, requestId)
                        .orderByAsc(PaymentApproval::getApprovalOrder));
    }

    /**
     * 校验付款申请参数
     */
    public void validatePaymentRequest(PaymentRequestDTO dto) {
        if (dto.getApplicantId() == null) {
            throw new IllegalArgumentException("申请人ID不能为空");
        }
        if (dto.getRequestType() == null) {
            throw new IllegalArgumentException("申请类型不能为空");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("金额必须大于0");
        }

        switch (dto.getRequestType()) {
            case TYPE_EXPENSE:
                if (dto.getExpenseType() == null) {
                    throw new IllegalArgumentException("费用报销时报销类型不能为空");
                }
                break;
            case TYPE_ADVANCE_COMMISSION:
                if (dto.getRelatedDealId() == null) {
                    throw new IllegalArgumentException("垫佣申请必须关联成交记录");
                }
                validateDealExists(dto.getRelatedDealId());
                break;
            case TYPE_PREPAY_COMMISSION:
                if (dto.getRelatedDealId() == null) {
                    throw new IllegalArgumentException("预付佣申请必须关联成交记录");
                }
                if (dto.getRelatedAllianceId() == null) {
                    throw new IllegalArgumentException("预付佣申请必须关联联盟商");
                }
                validateDealExists(dto.getRelatedDealId());
                validateAllianceExists(dto.getRelatedAllianceId());
                break;
            default:
                throw new IllegalArgumentException("无效的申请类型：" + dto.getRequestType());
        }
    }

    /**
     * 校验成交记录是否存在
     */
    private void validateDealExists(Long dealId) {
        CustomerDeal deal = customerDealMapper.selectById(dealId);
        if (deal == null) {
            throw new IllegalArgumentException("关联的成交记录不存在：" + dealId);
        }
    }

    /**
     * 校验联盟商是否存在
     */
    private void validateAllianceExists(Long allianceId) {
        Alliance alliance = allianceMapper.selectById(allianceId);
        if (alliance == null) {
            throw new IllegalArgumentException("关联的联盟商不存在：" + allianceId);
        }
    }


    private PaymentVO toVO(PaymentRequest request) {
        PaymentVO vo = PaymentVO.fromEntity(request);
        vo.setApprovals(getApprovalHistory(request.getId()));
        // 审批中（待审批/审批中）才暴露当前节点名，小程序列表展示「当前节点：财务」
        if (request.getStatus() != null
                && (request.getStatus() == STATUS_PENDING || request.getStatus() == STATUS_IN_PROGRESS)
                && request.getRequestType() != null) {
            ApprovalRecordNode current = approvalFlowService.getCurrentNode(
                    businessTypeOf(request.getRequestType()), request.getId());
            if (current != null) {
                vo.setCurrentNodeName(current.getNodeName());
            }
        }
        return vo;
    }

    /**
     * 预下单成功后，把支付状态推进到“付款中”。
     * 不覆盖已付款状态，避免下游重复创建订单时篡改结果。
     */
    @Transactional
    public void markPaying(Long requestId, String payChannel) {
        PaymentRequest request = paymentRequestMapper.selectById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("付款申请不存在");
        }
        if (request.getPayStatus() != null && request.getPayStatus() == PAY_STATUS_PAID) {
            return;
        }
        request.setPayStatus(PAY_STATUS_PAYING);
        request.setPayChannel(payChannel);
        paymentRequestMapper.updateById(request);
    }

    /**
     * 生成对外支付标题，优先保留业务语义，便于支付平台侧排查。
     */
    public String buildPayDescription(PaymentRequest request) {
        if (request == null) {
            return "付款申请";
        }
        return switch (request.getRequestType() == null ? 0 : request.getRequestType()) {
            case TYPE_EXPENSE -> "费用报销-" + request.getId();
            case TYPE_ADVANCE_COMMISSION -> "垫佣申请-" + request.getId();
            case TYPE_PREPAY_COMMISSION -> "预付佣申请-" + request.getId();
            default -> "付款申请-" + request.getId();
        };
    }

    // ==================== P0-1 支付通道回调 ====================

    /**
     * 生成对外业务订单号。
     * 格式：PAY + yyyyMMddHHmmss + 6 位随机数，共 23 位。
     */
    String generateOrderNo() {
        return "PAY" + LocalDateTime.now().format(ORDER_NO_FMT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    /**
     * 支付通道（支付宝/微信）异步回调入口。
     * <p>
     * 语义：根据业务订单号 {@code orderNo} 将 PaymentRequest 标记为已付款，
     * 并落地第三方交易号、渠道、支付时间；同时写入审计日志以保留原始通知。
     * <p>
     * 幂等保证：
     * <ol>
     *   <li>Controller 层 verifyService.isProcessed 做一次通知去重</li>
     *   <li>本方法向 pay_notify_log 插入记录，notify_id 唯一索引兜底</li>
     *   <li>已是 PAID 的订单重复回调直接返回 true（避免抛异常让通道重试）</li>
     * </ol>
     *
     * @param orderNo   业务订单号（payment_request.order_no）
     * @param tradeNo   第三方交易号
     * @param channel   渠道标识：alipay / wechat
     * @param amount    实际到账金额（元）
     * @return 是否成功处理；false 会让通道稍后重试
     */
    @Transactional
    public boolean updatePayStatus(String orderNo, String tradeNo, String channel, Double amount) {
        return updatePayStatus(orderNo, tradeNo, channel, amount, null, null);
    }

    /**
     * 含审计上下文的回调处理重载。
     *
     * @param notifyId   第三方通知ID（幂等键，可为 null 表示由调用方保证幂等）
     * @param rawPayload 原始回调报文，入审计库
     */
    @Transactional
    public boolean updatePayStatus(String orderNo,
                                   String tradeNo,
                                   String channel,
                                   Double amount,
                                   String notifyId,
                                   String rawPayload) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("业务订单号不能为空");
        }
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("支付渠道不能为空");
        }
        BigDecimal amountBd = amount == null ? BigDecimal.ZERO : BigDecimal.valueOf(amount);

        PaymentRequest request = paymentRequestMapper.selectOne(
                new LambdaQueryWrapper<PaymentRequest>().eq(PaymentRequest::getOrderNo, orderNo));

        if (request == null) {
            log.warn("[支付回调] 订单不存在：orderNo={}, channel={}", orderNo, channel);
            logNotify(notifyId, channel, orderNo, tradeNo, amountBd, rawPayload,
                    PayNotifyLog.RESULT_FAILURE, "订单不存在");
            return false;
        }

        // 已付过款，不重复处理（通道重复回调返回 true 让其停止重试）
        if (request.getPayStatus() != null && request.getPayStatus() == PAY_STATUS_PAID) {
            log.info("[支付回调] 订单已处于已付款状态，忽略重复通知：orderNo={}", orderNo);
            logNotify(notifyId, channel, orderNo, tradeNo, amountBd, rawPayload,
                    PayNotifyLog.RESULT_DUPLICATE, null);
            return true;
        }

        // 审批未通过的订单不允许记账
        if (request.getStatus() == null || request.getStatus() != STATUS_APPROVED) {
            log.warn("[支付回调] 订单审批未通过不可入账：orderNo={}, status={}",
                    orderNo, request.getStatus());
            logNotify(notifyId, channel, orderNo, tradeNo, amountBd, rawPayload,
                    PayNotifyLog.RESULT_FAILURE, "审批未通过");
            return false;
        }

        // 金额一致性校验（误差 1 分以内视为相等）
        if (request.getAmount() != null
                && request.getAmount().subtract(amountBd).abs().compareTo(new BigDecimal("0.01")) > 0) {
            log.error("[支付回调] 金额不一致：orderNo={}, expect={}, actual={}",
                    orderNo, request.getAmount(), amountBd);
            logNotify(notifyId, channel, orderNo, tradeNo, amountBd, rawPayload,
                    PayNotifyLog.RESULT_FAILURE, "金额不一致");
            return false;
        }

        request.setPayChannel(channel);
        request.setThirdTradeNo(tradeNo);
        request.setPayStatus(PAY_STATUS_PAID);
        request.setPaidTime(LocalDateTime.now());
        paymentRequestMapper.updateById(request);

        eventPublisher.publishEvent(
                new DataChangeEvent(this, "pay", "payment", request.getId()));

        logNotify(notifyId, channel, orderNo, tradeNo, amountBd, rawPayload,
                PayNotifyLog.RESULT_SUCCESS, null);

        log.info("[支付回调] 订单入账成功：orderNo={}, channel={}, tradeNo={}", orderNo, channel, tradeNo);
        return true;
    }

    /**
     * 记录支付回调审计日志。notifyId 重复时（数据库唯一索引抛 DuplicateKeyException）直接忽略，
     * 以此作为幂等兜底——即使上游 verifyService 失效也不会重复入账。
     */
    private void logNotify(String notifyId,
                           String channel,
                           String orderNo,
                           String tradeNo,
                           BigDecimal amount,
                           String rawPayload,
                           String result,
                           String errorMsg) {
        if (notifyId == null || notifyId.isBlank()) {
            return;
        }
        try {
            payNotifyLogMapper.insert(PayNotifyLog.builder()
                    .notifyId(notifyId)
                    .channel(channel)
                    .orderNo(orderNo)
                    .thirdTradeNo(tradeNo)
                    .amount(amount)
                    .rawPayload(rawPayload)
                    .processResult(result)
                    .errorMsg(errorMsg)
                    .build());
        } catch (DuplicateKeyException ignore) {
            log.info("[支付回调] notifyId 已存在，幂等兜底忽略：{}", notifyId);
        }
    }
}
