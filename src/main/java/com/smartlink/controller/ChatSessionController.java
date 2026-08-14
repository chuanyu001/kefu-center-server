package com.smartlink.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlink.common.Result;
import com.smartlink.entity.ChatMessageEntity;
import com.smartlink.entity.ChatSessionEntity;
import com.smartlink.mapper.ChatMessageMapper;
import com.smartlink.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 智能助手会话记录（会话列表 / 消息落库 / 历史恢复）。
 */
@Slf4j
@RestController
@RequestMapping("/api/chat-sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    /** 会话列表（最近更新的在前） */
    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(defaultValue = "") String createdBy) {
        try {
            List<ChatSessionEntity> list = sessionMapper.selectList(
                    new LambdaQueryWrapper<ChatSessionEntity>()
                            .eq(!"".equals(createdBy), ChatSessionEntity::getCreatedBy, createdBy)
                            .orderByDesc(ChatSessionEntity::getUpdatedAt));
            return Result.ok(list.stream().map(s -> {
                Map<String, Object> vo = new LinkedHashMap<>();
                vo.put("id", s.getId());
                vo.put("title", s.getTitle());
                vo.put("kind", s.getKind());
                vo.put("createdAt", s.getCreatedAt() == null ? null : s.getCreatedAt().toString());
                vo.put("updatedAt", s.getUpdatedAt() == null ? null : s.getUpdatedAt().toString());
                return vo;
            }).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("查询会话列表失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /** 新建会话，返回会话ID */
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        try {
            ChatSessionEntity session = new ChatSessionEntity();
            session.setTitle(body.get("title") == null ? "" : String.valueOf(body.get("title")));
            session.setKind(body.get("kind") == null ? "chat" : String.valueOf(body.get("kind")));
            session.setCreatedBy(body.get("createdBy") == null ? "" : String.valueOf(body.get("createdBy")));
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.insert(session);
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", session.getId());
            vo.put("title", session.getTitle());
            return Result.ok(vo);
        } catch (Exception e) {
            log.error("创建会话失败", e);
            return Result.fail("创建失败: " + e.getMessage());
        }
    }

    /** 会话消息列表（时间正序） */
    @GetMapping("/{id}/messages")
    public Result<List<Map<String, Object>>> messages(@PathVariable String id) {
        try {
            List<ChatMessageEntity> list = messageMapper.selectList(
                    new LambdaQueryWrapper<ChatMessageEntity>()
                            .eq(ChatMessageEntity::getSessionId, id)
                            .orderByAsc(ChatMessageEntity::getCreatedAt));
            return Result.ok(list.stream().map(m -> {
                Map<String, Object> vo = new LinkedHashMap<>();
                vo.put("id", m.getId());
                vo.put("role", m.getRole());
                vo.put("content", m.getContent());
                vo.put("createdAt", m.getCreatedAt() == null ? null : m.getCreatedAt().toString());
                return vo;
            }).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("查询会话消息失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /** 追加消息；若会话标题为空且是用户消息，用其前20字补标题 */
    @PostMapping("/{id}/messages")
    public Result<Map<String, Object>> append(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            ChatSessionEntity session = sessionMapper.selectById(id);
            if (session == null) {
                return Result.notFound("会话不存在");
            }
            String role = body.get("role") == null ? "user" : String.valueOf(body.get("role"));
            String content = body.get("content") == null ? "" : String.valueOf(body.get("content"));

            ChatMessageEntity message = new ChatMessageEntity();
            message.setSessionId(id);
            message.setRole(role);
            message.setContent(content);
            message.setCreatedAt(LocalDateTime.now());
            messageMapper.insert(message);

            session.setUpdatedAt(LocalDateTime.now());
            if ((session.getTitle() == null || session.getTitle().isEmpty()) && "user".equals(role)) {
                String title = content.replaceAll("\\s+", " ").trim();
                session.setTitle(title.length() > 20 ? title.substring(0, 20) : title);
            }
            sessionMapper.updateById(session);

            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", message.getId());
            return Result.ok(vo);
        } catch (Exception e) {
            log.error("追加消息失败", e);
            return Result.fail("保存失败: " + e.getMessage());
        }
    }

    /** 删除会话（级联删除消息） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        try {
            messageMapper.delete(new LambdaQueryWrapper<ChatMessageEntity>().eq(ChatMessageEntity::getSessionId, id));
            sessionMapper.deleteById(id);
            return Result.ok(null, "删除成功");
        } catch (Exception e) {
            log.error("删除会话失败", e);
            return Result.fail("删除失败: " + e.getMessage());
        }
    }
}
