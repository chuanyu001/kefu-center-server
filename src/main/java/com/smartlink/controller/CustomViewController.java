package com.smartlink.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlink.common.Result;
import com.smartlink.entity.CustomViewEntity;
import com.smartlink.mapper.CustomViewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 生成的自定义视图：服务端存储（P1），替代前端 localStorage。
 */
@Slf4j
@RestController
@RequestMapping("/api/custom-views")
@RequiredArgsConstructor
public class CustomViewController {

    private final CustomViewMapper customViewMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 查询全部（前端按 pageKey/sectionKey 自行过滤） */
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        try {
            List<CustomViewEntity> list = customViewMapper.selectList(
                    new LambdaQueryWrapper<CustomViewEntity>().orderByAsc(CustomViewEntity::getCreatedAt));
            return Result.ok(list.stream().map(this::entityToVO).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("查询自定义视图失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /** 批量保存（一次设计可能生成多个内容块） */
    @PostMapping
    public Result<List<Map<String, Object>>> save(@RequestBody Map<String, Object> body) {
        try {
            String pageKey = str(body.get("pageKey"));
            String sectionKey = str(body.get("sectionKey"));
            String createdBy = str(body.getOrDefault("createdBy", ""));
            Object viewsObj = body.get("views");
            if (!(viewsObj instanceof List)) {
                return Result.fail("views 不能为空");
            }

            List<Map<String, Object>> saved = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<Object> views = (List<Object>) viewsObj;
            for (Object viewObj : views) {
                if (!(viewObj instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> view = (Map<String, Object>) viewObj;
                CustomViewEntity entity = new CustomViewEntity();
                entity.setPageKey(pageKey);
                entity.setSectionKey(sectionKey);
                entity.setType(str(view.get("type")));
                entity.setTitle(str(view.get("title")));
                entity.setDataRule(toJson(view.get("dataRule")));
                entity.setLabels(toJson(view.get("labels")));
                entity.setDataJson(toJson(view.get("data")));
                entity.setColumnsJson(toJson(view.get("columns")));
                entity.setValueStr(str(view.get("value")));
                entity.setSubtitle(str(view.get("subtitle")));
                entity.setContent(str(view.get("content")));
                entity.setItemsJson(toJson(view.get("items")));
                entity.setTone(str(view.get("tone")));
                entity.setWidth(str(view.getOrDefault("width", "full")));
                entity.setSourceRef(str(view.get("sourceRef")));
                entity.setSrc(str(view.get("src")));
                entity.setCaption(str(view.get("caption")));
                entity.setCreatedBy(createdBy);
                customViewMapper.insert(entity);
                saved.add(entityToVO(entity));
            }
            return Result.ok(saved);
        } catch (Exception e) {
            log.error("保存自定义视图失败", e);
            return Result.fail("保存失败: " + e.getMessage());
        }
    }

    /** 局部更新（微调标题 / 数据范围 / 编辑面板全字段） */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            CustomViewEntity entity = customViewMapper.selectById(id);
            if (entity == null) {
                return Result.notFound("视图不存在");
            }
            // 统一走 wrapper 显式 set（含 null），避免 updateById 跳过 null 字段；
            // 同一请求中 dataRule 清空与其他字段修改需一并落库
            LambdaUpdateWrapper<CustomViewEntity> wrapper = new LambdaUpdateWrapper<CustomViewEntity>()
                    .eq(CustomViewEntity::getId, id);
            boolean changed = false;
            if (body.containsKey("title")) {
                wrapper.set(CustomViewEntity::getTitle, str(body.get("title")));
                changed = true;
            }
            if (body.containsKey("dataRule")) {
                wrapper.set(CustomViewEntity::getDataRule,
                        body.get("dataRule") == null ? null : toJson(body.get("dataRule")));
                changed = true;
            }
            // 编辑面板白名单字段：取值方式与 POST 保存一致
            if (body.containsKey("type")) {
                wrapper.set(CustomViewEntity::getType, str(body.get("type")));
                changed = true;
            }
            if (body.containsKey("labels")) {
                wrapper.set(CustomViewEntity::getLabels, toJson(body.get("labels")));
                changed = true;
            }
            if (body.containsKey("data")) {
                wrapper.set(CustomViewEntity::getDataJson, toJson(body.get("data")));
                changed = true;
            }
            if (body.containsKey("columns")) {
                wrapper.set(CustomViewEntity::getColumnsJson, toJson(body.get("columns")));
                changed = true;
            }
            if (body.containsKey("value")) {
                wrapper.set(CustomViewEntity::getValueStr, str(body.get("value")));
                changed = true;
            }
            if (body.containsKey("subtitle")) {
                wrapper.set(CustomViewEntity::getSubtitle, str(body.get("subtitle")));
                changed = true;
            }
            if (body.containsKey("content")) {
                wrapper.set(CustomViewEntity::getContent, str(body.get("content")));
                changed = true;
            }
            if (body.containsKey("items")) {
                wrapper.set(CustomViewEntity::getItemsJson, toJson(body.get("items")));
                changed = true;
            }
            if (body.containsKey("tone")) {
                wrapper.set(CustomViewEntity::getTone, str(body.get("tone")));
                changed = true;
            }
            if (body.containsKey("width")) {
                wrapper.set(CustomViewEntity::getWidth, str(body.get("width")));
                changed = true;
            }
            if (body.containsKey("sourceRef")) {
                wrapper.set(CustomViewEntity::getSourceRef, str(body.get("sourceRef")));
                changed = true;
            }
            if (body.containsKey("src")) {
                wrapper.set(CustomViewEntity::getSrc, str(body.get("src")));
                changed = true;
            }
            if (body.containsKey("caption")) {
                wrapper.set(CustomViewEntity::getCaption, str(body.get("caption")));
                changed = true;
            }
            if (changed) {
                customViewMapper.update(null, wrapper);
            }
            return Result.ok(null, "更新成功");
        } catch (Exception e) {
            log.error("更新自定义视图失败", e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        try {
            customViewMapper.deleteById(id);
            return Result.ok(null, "删除成功");
        } catch (Exception e) {
            log.error("删除自定义视图失败", e);
            return Result.fail("删除失败: " + e.getMessage());
        }
    }

    /** 实体转 VO：JSON 文本字段反序列化回对象，前端保持原数据形状 */
    private Map<String, Object> entityToVO(CustomViewEntity entity) {
        Map<String, Object> vo = new java.util.LinkedHashMap<>();
        vo.put("id", entity.getId());
        vo.put("pageKey", entity.getPageKey());
        vo.put("sectionKey", entity.getSectionKey());
        vo.put("type", entity.getType());
        vo.put("title", entity.getTitle());
        putJson(vo, "dataRule", entity.getDataRule());
        putJson(vo, "labels", entity.getLabels());
        putJson(vo, "data", entity.getDataJson());
        putJson(vo, "columns", entity.getColumnsJson());
        vo.put("value", entity.getValueStr());
        vo.put("subtitle", entity.getSubtitle());
        vo.put("content", entity.getContent());
        putJson(vo, "items", entity.getItemsJson());
        vo.put("tone", entity.getTone());
        vo.put("width", entity.getWidth());
        vo.put("sourceRef", entity.getSourceRef());
        vo.put("src", entity.getSrc());
        vo.put("caption", entity.getCaption());
        vo.put("createdBy", entity.getCreatedBy());
        vo.put("createdAt", entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString());
        return vo;
    }

    private void putJson(Map<String, Object> vo, String key, String json) {
        if (json == null || json.isEmpty()) return;
        try {
            vo.put(key, objectMapper.readValue(json, Object.class));
        } catch (Exception e) {
            // JSON 解析失败时原样透传字符串
            vo.put(key, json);
        }
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }
}
