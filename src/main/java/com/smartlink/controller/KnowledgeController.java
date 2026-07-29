package com.smartlink.controller;

import com.smartlink.common.PageResult;
import com.smartlink.common.Result;
import com.smartlink.dto.request.QaCreateReq;
import com.smartlink.dto.request.QaQueryReq;
import com.smartlink.dto.response.*;
import com.smartlink.service.KnowledgeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 知识库管理控制器
 *
 * @author smartlink
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Api(tags = "知识库管理")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    // ===== 文档管理 =====

    @GetMapping("/documents")
    @ApiOperation("分页查询文档列表")
    public Result<PageResult<DocumentVO>> documentList(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String parseStatus) {
        try {
            return Result.ok(knowledgeService.documentList(page, pageSize, category, parseStatus));
        } catch (Exception e) {
            log.error("查询文档列表失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/documents")
    @ApiOperation("创建文档")
    public Result<Void> documentCreate(@RequestBody Map<String, Object> data) {
        try {
            knowledgeService.documentCreate(data);
            return Result.ok(null, "创建成功");
        } catch (Exception e) {
            log.error("创建文档失败", e);
            return Result.fail("创建失败: " + e.getMessage());
        }
    }

    // ===== 问答对管理 =====

    @GetMapping("/qa")
    @ApiOperation("分页查询问答对列表")
    public Result<PageResult<QaPairVO>> qaList(@Valid QaQueryReq req) {
        try {
            return Result.ok(knowledgeService.qaList(req));
        } catch (Exception e) {
            log.error("查询问答列表失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/qa")
    @ApiOperation("创建问答对")
    public Result<Void> qaCreate(@RequestBody @Valid QaCreateReq req) {
        try {
            knowledgeService.qaCreate(req);
            return Result.ok(null, "创建成功");
        } catch (Exception e) {
            log.error("创建问答对失败", e);
            return Result.fail("创建失败: " + e.getMessage());
        }
    }

    @GetMapping("/qa/{id}")
    @ApiOperation("查询问答对详情")
    public Result<QaPairVO> qaDetail(
            @ApiParam(value = "问答对ID", required = true) @PathVariable String id) {
        try {
            QaPairVO vo = knowledgeService.qaDetail(id);
            if (vo == null) {
                return Result.notFound("问答对不存在");
            }
            return Result.ok(vo);
        } catch (Exception e) {
            log.error("查询问答详情失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    @PutMapping("/qa/{id}")
    @ApiOperation("更新问答对")
    public Result<Void> qaUpdate(
            @ApiParam(value = "问答对ID", required = true) @PathVariable String id,
            @RequestBody Map<String, Object> data) {
        try {
            knowledgeService.qaUpdate(id, data);
            return Result.ok(null, "更新成功");
        } catch (Exception e) {
            log.error("更新问答对失败", e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/qa/{id}")
    @ApiOperation("删除问答对")
    public Result<Void> qaDelete(
            @ApiParam(value = "问答对ID", required = true) @PathVariable String id) {
        try {
            knowledgeService.qaDelete(id);
            return Result.ok(null, "删除成功");
        } catch (Exception e) {
            log.error("删除问答对失败", e);
            return Result.fail("删除失败: " + e.getMessage());
        }
    }

    // ===== 审核管理 =====

    @GetMapping("/reviews")
    @ApiOperation("分页查询审核列表")
    public Result<PageResult<ReviewVO>> reviewList(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        try {
            return Result.ok(knowledgeService.reviewList(page, pageSize, type, status));
        } catch (Exception e) {
            log.error("查询审核列表失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    @PutMapping("/reviews/{id}")
    @ApiOperation("更新审核")
    public Result<Void> reviewUpdate(
            @ApiParam(value = "审核ID", required = true) @PathVariable String id,
            @RequestBody Map<String, Object> data) {
        try {
            knowledgeService.reviewUpdate(id, data);
            return Result.ok(null, "更新成功");
        } catch (Exception e) {
            log.error("更新审核失败", e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }

    // ===== 迭代记录管理 =====

    @GetMapping("/iterations")
    @ApiOperation("分页查询迭代记录列表")
    public Result<PageResult<IterationVO>> iterationList(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        try {
            return Result.ok(knowledgeService.iterationList(page, pageSize, status));
        } catch (Exception e) {
            log.error("查询迭代记录失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    @PutMapping("/iterations/{id}")
    @ApiOperation("更新迭代记录")
    public Result<Void> iterationUpdate(
            @ApiParam(value = "迭代记录ID", required = true) @PathVariable String id,
            @RequestBody Map<String, Object> data) {
        try {
            knowledgeService.iterationUpdate(id, data);
            return Result.ok(null, "更新成功");
        } catch (Exception e) {
            log.error("更新迭代记录失败", e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }

    // ===== 分析概览 =====

    @GetMapping("/analytics")
    @ApiOperation("获取分析概览数据")
    public Result<Map<String, Object>> analytics() {
        try {
            return Result.ok(knowledgeService.analytics());
        } catch (Exception e) {
            log.error("获取分析数据失败", e);
            return Result.fail("获取分析数据失败: " + e.getMessage());
        }
    }
}
