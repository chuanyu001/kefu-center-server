package com.smartlink.controller;

import com.smartlink.common.PageResult;
import com.smartlink.common.Result;
import com.smartlink.dto.request.SessionQueryReq;
import com.smartlink.dto.request.SessionUpdateReq;
import com.smartlink.dto.response.SessionVO;
import com.smartlink.service.SessionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 工作表管理控制器
 *
 * @author smartlink
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Api(tags = "工作表管理")
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    @ApiOperation("分页查询工作表列表")
    public Result<PageResult<SessionVO>> list(@Valid SessionQueryReq req) {
        try {
            return Result.ok(sessionService.list(req));
        } catch (Exception e) {
            log.error("查询工作表列表失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @ApiOperation("查询工作表详情")
    public Result<SessionVO> detail(
            @ApiParam(value = "工作表ID", required = true) @PathVariable String id) {
        try {
            SessionVO vo = sessionService.detail(id);
            if (vo == null) {
                return Result.notFound("工作表不存在");
            }
            return Result.ok(vo);
        } catch (Exception e) {
            log.error("查询工作表详情失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @ApiOperation("更新工作表")
    public Result<Void> update(
            @ApiParam(value = "工作表ID", required = true) @PathVariable String id,
            @RequestBody @Valid SessionUpdateReq req) {
        try {
            sessionService.update(id, req);
            return Result.ok(null, "更新成功");
        } catch (Exception e) {
            log.error("更新工作表失败", e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }
}
