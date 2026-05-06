package com.pengcheng.system.todo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 待办事项（支持手动创建和从聊天消息中 AI 自动提取）
 */
@Data
@TableName("sys_todo")
public class Todo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String description;
    /** manual / chat / ai */
    private String sourceType;
    private Long sourceId;
    private String sourceChatType;
    /** 0=普通 1=重要 2=紧急 */
    private Integer priority;
    /** 0=待办 1=进行中 2=已完成 3=已取消 */
    private Integer status;
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;
    private Long customerId;
    private Long assigneeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
