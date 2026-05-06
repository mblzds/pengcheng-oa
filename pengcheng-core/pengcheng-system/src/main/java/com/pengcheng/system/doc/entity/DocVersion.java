package com.pengcheng.system.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_doc_version")
public class DocVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long docId;
    private Integer version;
    private String title;
    private String content;
    private Long editorId;
    private String changeSummary;
    private LocalDateTime createdAt;
}
