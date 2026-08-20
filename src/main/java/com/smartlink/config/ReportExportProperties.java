package com.smartlink.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 报表导出图片配置
 * 前缀: report.export
 */
@Data
@Component
@ConfigurationProperties(prefix = "report.export")
public class ReportExportProperties {

    /** Node 可执行文件路径；支持用 REPORT_EXPORT_NODE_PATH 覆盖 */
    private String nodePath = "node";

    /** report-export.cjs 脚本绝对路径 */
    private String scriptPath;

    /** 脚本工作目录（用于解析 playwright 依赖） */
    private String workDir;

    /** 渲染超时（秒） */
    private int timeoutSeconds = 90;
}
