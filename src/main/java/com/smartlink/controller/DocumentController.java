package com.smartlink.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlink.common.Result;
import com.smartlink.entity.DocumentEntity;
import com.smartlink.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentMapper documentMapper;

    @GetMapping
    public Result<List<DocumentEntity>> list() {
        try {
            List<DocumentEntity> list = documentMapper.selectList(
                new LambdaQueryWrapper<DocumentEntity>().orderByDesc(DocumentEntity::getUpdatedAt));
            return Result.ok(list);
        } catch (Exception e) {
            log.error("查询文档列表失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<DocumentEntity> detail(@PathVariable String id) {
        try {
            DocumentEntity doc = documentMapper.selectById(id);
            return doc != null ? Result.ok(doc) : Result.notFound("文档不存在");
        } catch (Exception e) {
            log.error("查询文档详情失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            DocumentEntity doc = documentMapper.selectById(id);
            if (doc == null) return Result.notFound("文档不存在");

            if (body.containsKey("title")) doc.setTitle((String) body.get("title"));
            if (body.containsKey("content")) doc.setContent((String) body.get("content"));
            if (body.containsKey("category")) doc.setCategory((String) body.get("category"));
            doc.setUpdatedAt(LocalDateTime.now());
            doc.setVersion((doc.getVersion() == null ? 0 : doc.getVersion()) + 1);

            documentMapper.updateById(doc);
            return Result.ok(null, "更新成功");
        } catch (Exception e) {
            log.error("更新文档失败", e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }
}
