package com.pengcheng.realty.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 客户-项目关联实体（多对多中间表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("customer_project")
public class CustomerProject {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private Long projectId;

    private LocalDateTime createTime;
}
