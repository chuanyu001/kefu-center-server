package com.smartlink.controller;

import com.smartlink.common.PageResult;
import com.smartlink.common.Result;
import com.smartlink.dto.response.FeedbackVO;
import com.smartlink.dto.response.ReportVO;
import com.smartlink.service.ReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 报告管理控制器
 *
 * @author smartlink
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Api(tags = "报告管理")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/reports")
    @ApiOperation("分页查询报告列表")
    public Result<PageResult<ReportVO>> list(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String pushStatus) {
        try {
            return Result.ok(reportService.list(page, pageSize, reportType, pushStatus));
        } catch (Exception e) {
            log.error("查询报告列表失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/reports/{id}")
    @ApiOperation("查询报告详情")
    public Result<ReportVO> detail(
            @ApiParam(value = "报告ID", required = true) @PathVariable String id) {
        try {
            ReportVO vo = reportService.detail(id);
            if (vo == null) {
                return Result.notFound("报告不存在");
            }
            return Result.ok(vo);
        } catch (Exception e) {
            log.error("查询报告详情失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/reports/{id}/push")
    @ApiOperation("推送报告")
    public Result<Void> push(
            @ApiParam(value = "报告ID", required = true) @PathVariable String id) {
        try {
            reportService.push(id);
            return Result.ok(null, "推送成功");
        } catch (Exception e) {
            log.error("推送报告失败", e);
            return Result.fail("推送失败: " + e.getMessage());
        }
    }

    @GetMapping("/feedback")
    @ApiOperation("分页查询反馈列表")
    public Result<PageResult<FeedbackVO>> feedbackList(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        try {
            return Result.ok(reportService.feedbackList(page, pageSize, status));
        } catch (Exception e) {
            log.error("查询反馈列表失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    @PutMapping("/feedback/{id}")
    @ApiOperation("更新反馈")
    public Result<Void> updateFeedback(
            @ApiParam(value = "反馈ID", required = true) @PathVariable String id,
            @RequestBody Map<String, Object> data) {
        try {
            reportService.updateFeedback(id, data);
            return Result.ok(null, "更新成功");
        } catch (Exception e) {
            log.error("更新反馈失败", e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }
}
