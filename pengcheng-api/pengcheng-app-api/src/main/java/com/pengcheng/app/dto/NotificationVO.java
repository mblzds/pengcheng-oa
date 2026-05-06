package com.pengcheng.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知消息响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {

    /** 通知ID */
    private Long id;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 通知类型：1=客户状态变更 2=审批状态变更 3=新审批到达 */
    private Integer type;

    /** 业务类型：customer/approval/payment */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 读取状态：0=未读 1=已读 */
    private Integer readStatus;

    /** 创建时间 */
    private LocalDateTime createTime;
}
