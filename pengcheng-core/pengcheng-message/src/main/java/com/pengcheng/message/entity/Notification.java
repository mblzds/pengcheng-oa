package com.pengcheng.message.entity;

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
 * 系统通知消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_notification")
public class Notification implements Serializable {

    /**
     * 通知ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 接收人ID
     */
    private Long userId;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知类型：1=客户状态变更 2=审批状态变更 3=新审批到达
     */
    private Integer type;

    /**
     * 业务类型：customer/approval/payment
     */
    private String bizType;

    /**
     * 业务ID（客户ID/审批ID等）
     */
    private Long bizId;

    /**
     * 读取状态：0=未读 1=已读
     */
    private Integer readStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
