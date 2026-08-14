package com.smartlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("custom_views")
public class CustomViewEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String pageKey;
    private String sectionKey;
    private String type;
    private String title;
    /** 数据生成规则(JSON文本): {dimension, range} */
    private String dataRule;
    /** JSON 数组文本 */
    private String labels;
    private String dataJson;
    private String columnsJson;
    private String valueStr;
    private String subtitle;
    private String content;
    /** JSON 数组文本 */
    private String itemsJson;
    private String tone;
    private String width;
    private String sourceRef;
    private String src;
    private String caption;
    private String createdBy;
    private LocalDateTime createdAt;
}
