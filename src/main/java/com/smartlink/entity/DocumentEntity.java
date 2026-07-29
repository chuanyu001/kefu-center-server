package com.smartlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档实体
 *
 * @author smartlink
 */
@Data
@TableName("documents")
public class DocumentEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 文档标题 */
    private String title;

    /** 文档格式 */
    private String format;

    /** 分类 */
    private String category;

    /** 子分类 */
    private String subcategory;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 解析状态 */
    private String parseStatus;

    /** 版本号 */
    private Integer version;

    /** 更新人 */
    private String updatedBy;

    /** 文档内容 */
    private String content;
}
