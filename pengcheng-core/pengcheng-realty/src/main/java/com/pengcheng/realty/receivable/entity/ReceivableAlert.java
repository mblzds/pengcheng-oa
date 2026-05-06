package com.pengcheng.realty.receivable.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 回款告警（逾期 / 即将到期）。按 plan_id + alert_type 唯一。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("receivable_alert")
public class ReceivableAlert {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划分期 ID */
    private Long planId;

    /** 告警类型：1-逾期未回款 2-即将到期 */
    private Integer alertType;

    /** 首次告警时间 */
    private LocalDateTime alertTime;

    /** 最后一次通知时间 */
    private LocalDateTime lastNotified;

    /** 累计通知次数 */
    private Integer notifyCount;

    /** 处理状态：0-未处理 1-已处理 */
    private Integer handled;

    private Long handledBy;
    private LocalDateTime handledAt;
    private String handledRemark;
    private LocalDateTime createTime;

    public static final int TYPE_OVERDUE = 1;
    public static final int TYPE_UPCOMING = 2;
    public static final int HANDLED_NO = 0;
    public static final int HANDLED_YES = 1;
}
