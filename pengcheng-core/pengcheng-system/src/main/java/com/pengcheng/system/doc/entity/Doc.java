package com.pengcheng.system.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档实体（支持 Markdown 内容和目录树结构）
 */
@Data
@TableName("sys_doc")
public class Doc {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long spaceId;
    private Long parentId;
    private String title;
    private String content;
    /** markdown / folder */
    private String docType;
    private Integer sortOrder;
    private Long creatorId;
    private Long lastEditorId;
    private Integer wordCount;
    private Integer version;
    private Integer pinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
