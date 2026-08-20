package com.smartlink.service;

import com.smartlink.entity.DocumentChunkEntity;
import com.smartlink.mapper.DocumentChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识库检索：把用户问题匹配到 document_chunks 中最相关的切片，供 AI 助手回答时引用。
 * 检索分两步：先用 MySQL ngram 全文索引召回候选，不足再用关键词 LIKE 兜底，最后按关键词命中重排。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final DocumentChunkMapper chunkMapper;

    private static final int CANDIDATE_LIMIT = 30;
    private static final int TOP_K = 6;
    private static final int MAX_CHUNK_CHARS = 1500;
    private static final int TOTAL_BUDGET = 9000;

    /** 检索结果片段 */
    public static class Retrieved {
        public final String docTitle;
        public final String section;
        public final String text;

        Retrieved(String docTitle, String section, String text) {
            this.docTitle = docTitle;
            this.section = section;
            this.text = text;
        }
    }

    public List<Retrieved> retrieve(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String q = query.trim();

        List<DocumentChunkEntity> candidates = fulltextCandidates(q);
        if (candidates.size() < 3) {
            List<String> kws = extractKeywords(q);
            if (!kws.isEmpty()) {
                try {
                    List<DocumentChunkEntity> extra = chunkMapper.likeSearch(kws, CANDIDATE_LIMIT);
                    Set<Long> seen = new HashSet<>();
                    for (DocumentChunkEntity c : candidates) {
                        seen.add(c.getId());
                    }
                    for (DocumentChunkEntity c : extra) {
                        if (c.getId() != null && seen.add(c.getId())) {
                            candidates.add(c);
                        }
                    }
                } catch (Exception e) {
                    log.warn("[Knowledge] LIKE 检索失败: {}", e.getMessage());
                }
            }
        }
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        return rank(candidates, q);
    }

    private List<DocumentChunkEntity> fulltextCandidates(String q) {
        try {
            return chunkMapper.fulltextSearch(q, CANDIDATE_LIMIT);
        } catch (Exception e) {
            log.warn("[Knowledge] 全文检索失败(可能尚未建索引): {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Retrieved> rank(List<DocumentChunkEntity> candidates, String query) {
        List<String> kws = extractKeywords(query);
        List<double[]> scored = new ArrayList<>(); // [score, index]
        for (int i = 0; i < candidates.size(); i++) {
            DocumentChunkEntity c = candidates.get(i);
            String text = c.getChunkText() == null ? "" : c.getChunkText();
            String title = c.getDocTitle() == null ? "" : c.getDocTitle();
            double score = 0;
            for (String kw : kws) {
                if (text.contains(kw)) {
                    score += kw.length();
                }
            }
            for (String kw : kws) {
                if (title.contains(kw)) {
                    score += kw.length() * 2;
                }
            }
            scored.add(new double[]{score, i});
        }
        scored.sort((a, b) -> Double.compare(b[0], a[0]));

        List<Retrieved> out = new ArrayList<>();
        int budget = 0;
        for (double[] s : scored) {
            if (s[0] <= 0) {
                break;
            }
            DocumentChunkEntity c = candidates.get((int) s[1]);
            String text = c.getChunkText() == null ? "" : c.getChunkText().trim();
            if (text.isEmpty()) {
                continue;
            }
            if (text.length() > MAX_CHUNK_CHARS) {
                text = text.substring(0, MAX_CHUNK_CHARS);
            }
            if (budget + text.length() > TOTAL_BUDGET && !out.isEmpty()) {
                break;
            }
            out.add(new Retrieved(c.getDocTitle(), c.getSection(), text));
            budget += text.length();
            if (out.size() >= TOP_K) {
                break;
            }
        }
        return out;
    }

    /** 提取检索关键词：英文数字按整词，中文按二元组切分；长词优先、去重、最多 12 个。 */
    static List<String> extractKeywords(String q) {
        List<String> terms = new ArrayList<>();
        Matcher ascii = Pattern.compile("[A-Za-z0-9]+").matcher(q);
        while (ascii.find()) {
            String t = ascii.group().toLowerCase(Locale.ROOT);
            if (t.length() >= 2) {
                terms.add(t);
            }
        }
        Matcher cjk = Pattern.compile("[\\u4e00-\\u9fff]+").matcher(q);
        while (cjk.find()) {
            String seg = cjk.group();
            if (seg.length() == 1) {
                terms.add(seg);
                continue;
            }
            if (seg.length() <= 6) {
                terms.add(seg); // 完整短语，权重更高
            }
            for (int i = 0; i + 2 <= seg.length(); i++) {
                terms.add(seg.substring(i, i + 2));
            }
        }
        terms.sort((a, b) -> b.length() - a.length());
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        for (String t : terms) {
            if (seen.add(t)) {
                out.add(t);
            }
            if (out.size() >= 12) {
                break;
            }
        }
        return out;
    }
}
