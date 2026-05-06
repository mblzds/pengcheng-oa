package com.pengcheng.hr.performance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 同事评估关系 — 对应 V33__360_review.sql 的 kpi_peer_review 表
 */
@Data
@TableName("kpi_peer_review")
public class KpiPeerReview implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 同事 ID */
    private Long peerId;

    /** 考核周期 ID */
    private Long periodId;

    /** 部门 ID */
    private Long deptId;

    /** 状态：1-有效 0-无效 */
    private Integer status;

    private LocalDateTime createTime;
}
