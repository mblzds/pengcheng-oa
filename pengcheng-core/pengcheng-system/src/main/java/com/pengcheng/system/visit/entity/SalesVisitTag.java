package com.pengcheng.system.visit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拜访分析标签实体
 */
@Data
@TableName("sys_sales_visit_tag")
public class SalesVisitTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拜访记录 ID */
    private Long visitId;

    /** 标签类型: need/objection/commitment/competitor/risk */
    private String tagType;

    /** 标签内容 */
    private String tagContent;

    /** 置信度 */
    private BigDecimal confidence;

    private LocalDateTime createTime;
}
