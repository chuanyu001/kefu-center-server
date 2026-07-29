package com.smartlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报告实体
 *
 * @author smartlink
 */
@Data
@TableName("reports")
public class ReportEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 客户名称 */
    private String customerName;

    /** 报告类型 */
    private String reportType;

    /** 统计开始日期 */
    private LocalDate dateStart;

    /** 统计结束日期 */
    private LocalDate dateEnd;

    /** 生成时间 */
    private LocalDateTime generatedTime;

    /** 推送状态 */
    private String pushStatus;

    /** 摘要文本 */
    private String summaryText;

    /** 异常备注 */
    private String anomalyNotes;

    /** 图表数据(JSON) */
    private String chartData;

    /** 表格数据(JSON) */
    private String tableData;

    /** 推送话术 */
    private String pushScript;
}
