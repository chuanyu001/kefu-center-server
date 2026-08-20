package com.smartlink.service;

import com.smartlink.config.ReportExportProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 报表导出图片：通过 ProcessBuilder 调用 Node + Playwright 渲染 BI 看板并自动裁剪。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final ReportExportProperties props;

    public Map<String, Object> exportImage(String url, String title) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("看板地址不能为空");
        }
        File tmp = null;
        File logFile = null;
        try {
            tmp = File.createTempFile("report-", ".png");
            logFile = File.createTempFile("report-", ".log");

            String nodeExecutable = resolveNodeExecutable();

            ProcessBuilder pb = new ProcessBuilder(
                    nodeExecutable,
                    props.getScriptPath(),
                    "--url", url,
                    "--out", tmp.getAbsolutePath(),
                    "--title", title == null ? "" : title);
            if (props.getWorkDir() != null && !props.getWorkDir().isEmpty()) {
                pb.directory(new File(props.getWorkDir()));
            }
            pb.redirectErrorStream(true);
            pb.redirectOutput(logFile);

            log.info("[ReportExport] 使用 Node 可执行文件：{}", nodeExecutable);

            Process process = pb.start();
            boolean finished = process.waitFor(props.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("报表渲染超时（>" + props.getTimeoutSeconds() + "秒）");
            }

            int exitCode = process.exitValue();
            String logText = new String(Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8).trim();
            if (exitCode != 0 || !tmp.exists() || tmp.length() == 0) {
                log.warn("[ReportExport] node 脚本失败 exit={}, log={}", exitCode, logText);
                throw new IllegalStateException("报表导出失败：" + logText);
            }

            byte[] bytes = Files.readAllBytes(tmp.toPath());
            String base64 = Base64.getEncoder().encodeToString(bytes);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("image", base64);
            result.put("mime", "image/png");
            result.put("title", title);
            result.put("size", bytes.length);
            return result;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[ReportExport] 导出异常", e);
            throw new IllegalStateException("报表导出失败：" + e.getMessage());
        } finally {
            if (tmp != null && tmp.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
            if (logFile != null && logFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                logFile.delete();
            }
        }
    }

    /**
     * 后端可能由 IDE、Windows 服务或旧终端启动，这些进程的 PATH 不一定包含新安装的 Node。
     * 优先使用显式配置；配置不可用时再检查 Windows/Linux/macOS 的常见安装位置。
     */
    private String resolveNodeExecutable() {
        String configured = trimToNull(props.getNodePath());
        List<String> candidates = new ArrayList<>();

        if (configured != null && looksLikePath(configured)) {
            candidates.add(configured);
        }

        addNodeCandidate(candidates, System.getenv("REPORT_EXPORT_NODE_PATH"));
        addNodeCandidate(candidates, joinPath(System.getenv("ProgramFiles"), "nodejs", "node.exe"));
        addNodeCandidate(candidates, joinPath(System.getenv("ProgramFiles(x86)"), "nodejs", "node.exe"));
        addNodeCandidate(candidates, joinPath(System.getenv("LOCALAPPDATA"), "Programs", "nodejs", "node.exe"));
        candidates.add("/usr/local/bin/node");
        candidates.add("/usr/bin/node");
        candidates.add("/opt/homebrew/bin/node");

        for (String candidate : candidates) {
            File file = new File(candidate);
            if (file.isFile()) {
                return file.getAbsolutePath();
            }
        }

        // 保留对 PATH 中 node 的兼容；当前配置为简单命令时由 ProcessBuilder 最后尝试。
        if (configured != null && !looksLikePath(configured)) {
            return configured;
        }

        throw new IllegalStateException(
                "未找到 Node.js 可执行文件，请安装 Node.js，或配置环境变量 REPORT_EXPORT_NODE_PATH");
    }

    private void addNodeCandidate(List<String> candidates, String value) {
        String candidate = trimToNull(value);
        if (candidate != null && !candidates.contains(candidate)) {
            candidates.add(candidate);
        }
    }

    private String joinPath(String root, String... children) {
        String value = trimToNull(root);
        if (value == null) {
            return null;
        }
        File file = new File(value);
        for (String child : children) {
            file = new File(file, child);
        }
        return file.getPath();
    }

    private boolean looksLikePath(String value) {
        return new File(value).isAbsolute() || value.contains("/") || value.contains("\\");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
