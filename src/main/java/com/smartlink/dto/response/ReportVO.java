package com.smartlink.dto.response;

import lombok.Data;

import java.util.Map;

/**
 * 报告视图对象
 *
 * @author smartlink
 */
@Data
public class ReportVO {

    /** 主键ID */
    private String id;

    /** 客户名称 */
    private String customerName;

    /** 报告类型 */
    private String reportType;

    /** 统计开始日期 */
    private String dateStart;

    /** 统计结束日期 */
    private String dateEnd;

    /** 生成时间 */
    private String generatedTime;

    /** 推送状态 */
    private String pushStatus;

    /** 摘要文本 */
    private String summaryText;

    /** 异常备注 */
    private String anomalyNotes;

    /** 图表数据(已解析) */
    private Map<String, Object> chartData;

    /** 表格数据(已解析) */
    private Object tableData;

    /** 推送话术 */
    private String pushScript;
}
