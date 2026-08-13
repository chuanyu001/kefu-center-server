package com.smartlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("documents")
public class DocumentEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String title;
    private String format;
    private String category;
    private String subcategory;
    private Long fileSize;
    private LocalDateTime uploadTime;
    private LocalDateTime updatedAt;
    private String parseStatus;
    private Integer version;
    private String updatedBy;
    private String content;
}
