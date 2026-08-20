package com.smartlink.controller;

import com.smartlink.common.Result;
import com.smartlink.service.ReportExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportExportController {

    @Autowired
    private ReportExportService reportExportService;

    @PostMapping("/export-image")
    public Result<Map<String, Object>> exportImage(@RequestBody Map<String, Object> params) {
        String url = params.get("url") == null ? null : params.get("url").toString();
        String title = params.get("title") == null ? null : params.get("title").toString();
        try {
            Map<String, Object> result = reportExportService.exportImage(url, title);
            return Result.ok(result);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
}
