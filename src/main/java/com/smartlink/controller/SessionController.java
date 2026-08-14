package com.smartlink.controller;

import com.smartlink.common.PageResult;
import com.smartlink.common.Result;
import com.smartlink.dto.request.SessionQueryReq;
import com.smartlink.dto.request.SessionUpdateReq;
import com.smartlink.dto.response.SessionVO;
import com.smartlink.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @GetMapping
    public Result<PageResult<SessionVO>> list(SessionQueryReq req) {
        PageResult<SessionVO> result = sessionService.list(req);
        return Result.ok(result);
    }

    @GetMapping("/{id}")
    public Result<SessionVO> detail(@PathVariable String id) {
        SessionVO vo = sessionService.detail(id);
        if (vo == null) {
            return Result.notFound("记录不存在");
        }
        return Result.ok(vo);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody SessionUpdateReq req) {
        sessionService.update(id, req);
        return Result.ok(null, "更新成功");
    }

    @PostMapping("/import-excel")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = sessionService.importExcel(file);
        return Result.ok(result);
    }

    @PostMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) params.get("ids");
        @SuppressWarnings("unchecked")
        List<String> columns = (List<String>) params.get("columns");

        byte[] data = sessionService.exportExcel(ids, columns);

        String fileName = "sessions_export.xlsx";
        String encodedFileName;
        try {
            encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
        } catch (Exception e) {
            encodedFileName = "sessions_export.xlsx";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/sync-vehicle")
    public Result<Map<String, Object>> syncVehicleInfo() {
        Map<String, Object> result = sessionService.syncVehicleInfo();
        return Result.ok(result);
    }

    /** 工单真实数据聚合（供 AI 内容设计使用） */
    @GetMapping("/analytics")
    public Result<Map<String, Object>> analytics(@RequestParam(defaultValue = "all") String range) {
        return Result.ok(sessionService.analytics(range));
    }
}
