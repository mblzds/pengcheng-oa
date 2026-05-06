package com.pengcheng.system.visit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 销售拜访记录实体
 */
@Data
@TableName("sys_sales_visit")
public class SalesVisit {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 销售人员 ID */
    private Long userId;

    /** 关联客户 ID */
    private Long customerId;

    /** 客户姓名（冗余） */
    private String customerName;

    /** 关联项目 ID */
    private Long projectId;

    /** 项目名称（冗余） */
    private String projectName;

    /** 拜访类型: field=实地/phone=电话/online=线上 */
    private String visitType;

    /** 拜访时间 */
    private LocalDateTime visitTime;

    /** 拜访时长（分钟） */
    private Integer duration;

    /** 拜访地点 */
    private String location;

    /** 拜访目的 */
    private String purpose;

    /** 拜访总结 */
    private String summary;

    /** 录音文件 URL */
    private String audioUrl;

    /** ASR 转写文本 */
    private String transcript;

    /** AI 分析结果 JSON */
    private String aiAnalysis;

    /** AI 拜访质量评分 */
    private Integer aiScore;

    /** 跟进事项 */
    private String followUp;

    /** 下次拜访计划 */
    private String nextPlan;

    /** 状态: 0=草稿 1=已完成 2=已取消 */
    private Integer status;

    /** 所属部门 */
    private Long deptId;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
