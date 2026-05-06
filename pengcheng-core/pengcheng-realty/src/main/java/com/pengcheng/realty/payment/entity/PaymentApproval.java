package com.pengcheng.realty.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 付款审批记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("payment_approval")
public class PaymentApproval implements Serializable {

    /**
     * 审批记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 付款申请ID
     */
    private Long requestId;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批结果：1-通过 2-驳回
     */
    private Integer result;

    /**
     * 审批备注
     */
    private String remark;

    /**
     * 审批顺序
     */
    private Integer approvalOrder;

    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
