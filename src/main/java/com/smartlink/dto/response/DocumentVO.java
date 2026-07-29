package com.smartlink.dto.response;

import lombok.Data;

/**
 * 文档视图对象
 *
 * @author smartlink
 */
@Data
public class DocumentVO {

    /** 主键ID */
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
    private String uploadTime;

    /** 解析状态 */
    private String parseStatus;

    /** 文档内容 */
    private String content;
}
