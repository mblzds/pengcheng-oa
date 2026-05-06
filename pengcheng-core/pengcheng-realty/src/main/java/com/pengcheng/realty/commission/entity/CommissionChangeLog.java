package com.pengcheng.realty.commission.entity;

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
 * 佣金变更日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("commission_change_log")
public class CommissionChangeLog implements Serializable {

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 佣金ID
     */
    private Long commissionId;

    /**
     * 变更字段
     */
    private String fieldName;

    /**
     * 变更前值
     */
    private String oldValue;

    /**
     * 变更后值
     */
    private String newValue;

    /**
     * 变更人ID
     */
    private Long operatorId;

    /**
     * 变更时间
     */
    private LocalDateTime changeTime;
}
