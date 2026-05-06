package com.pengcheng.system.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目成员实体
 */
@Data
@TableName("pm_project_member")
public class PmProjectMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long userId;
    /** owner/admin/member */
    private String role;
    private LocalDateTime joinTime;
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
