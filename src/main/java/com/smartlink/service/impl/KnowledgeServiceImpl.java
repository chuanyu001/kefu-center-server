package com.smartlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlink.common.PageResult;
import com.smartlink.dto.request.QaCreateReq;
import com.smartlink.dto.request.QaQueryReq;
import com.smartlink.dto.response.*;
import com.smartlink.entity.*;
import com.smartlink.mapper.*;
import com.smartlink.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库服务实现
 *
 * @author smartlink
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final DocumentMapper documentMapper;
    private final QaPairMapper qaPairMapper;
    private final ReviewMapper reviewMapper;
    private final IterationMapper iterationMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ===== 文档管理 =====

    @Override
    public PageResult<DocumentVO> documentList(Integer pageNum, Integer pageSize, String category, String parseStatus) {
        try {
            LambdaQueryWrapper<DocumentEntity> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(category)) {
                wrapper.eq(DocumentEntity::getCategory, category);
            }
            if (StringUtils.hasText(parseStatus)) {
                wrapper.eq(DocumentEntity::getParseStatus, parseStatus);
            }
            wrapper.orderByDesc(DocumentEntity::getUploadTime);

            Page<DocumentEntity> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
            IPage<DocumentEntity> result = documentMapper.selectPage(page, wrapper);

            List<DocumentVO> records = result.getRecords().stream()
                    .map(this::docToVO)
                    .collect(Collectors.toList());

            return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
        } catch (Exception e) {
            log.warn("数据库查询文档失败，返回Mock数据: {}", e.getMessage());
            return getMockDocumentList(pageNum, pageSize, category, parseStatus);
        }
    }

    @Override
    public void documentCreate(Map<String, Object> data) {
        try {
            DocumentEntity entity = new DocumentEntity();
            entity.setTitle((String) data.getOrDefault("title", ""));
            entity.setFormat((String) data.getOrDefault("format", ""));
            entity.setCategory((String) data.getOrDefault("category", ""));
            entity.setSubcategory((String) data.getOrDefault("subcategory", ""));
            entity.setFileSize(data.get("fileSize") != null ? Long.valueOf(data.get("fileSize").toString()) : 0L);
            entity.setParseStatus("PENDING");
            documentMapper.insert(entity);
            log.info("文档创建成功: {}", entity.getId());
        } catch (Exception e) {
            log.error("创建文档失败: {}", e.getMessage());
            throw new RuntimeException("创建文档失败: " + e.getMessage());
        }
    }

    // ===== 问答对管理 =====

    @Override
    public PageResult<QaPairVO> qaList(QaQueryReq req) {
        try {
            LambdaQueryWrapper<QaPairEntity> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(req.getKeyword())) {
                wrapper.and(w -> w
                        .like(QaPairEntity::getQuestion, req.getKeyword())
                        .or()
                        .like(QaPairEntity::getAnswer, req.getKeyword()));
            }
            if (StringUtils.hasText(req.getCategory())) {
                wrapper.eq(QaPairEntity::getCategory, req.getCategory());
            }
            if (StringUtils.hasText(req.getStatus())) {
                wrapper.eq(QaPairEntity::getStatus, req.getStatus());
            }
            wrapper.orderByDesc(QaPairEntity::getCreatedAt);

            Page<QaPairEntity> page = new Page<>(req.getPage(), req.getPageSize());
            IPage<QaPairEntity> result = qaPairMapper.selectPage(page, wrapper);

            List<QaPairVO> records = result.getRecords().stream()
                    .map(this::qaToVO)
                    .collect(Collectors.toList());

            return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
        } catch (Exception e) {
            log.warn("数据库查询问答失败，返回Mock数据: {}", e.getMessage());
            return getMockQaList(req);
        }
    }

    @Override
    public QaPairVO qaDetail(String id) {
        try {
            QaPairEntity entity = qaPairMapper.selectById(id);
            if (entity != null) return qaToVO(entity);
        } catch (Exception e) {
            log.warn("数据库查询问答详情失败，返回Mock数据: {}", e.getMessage());
        }
        return getMockQaDetail(id);
    }

    @Override
    public void qaCreate(QaCreateReq req) {
        try {
            QaPairEntity entity = new QaPairEntity();
            entity.setQuestion(req.getQuestion());
            entity.setAnswer(req.getAnswer());
            entity.setAnswerBrief(req.getAnswerBrief());
            entity.setCategory(req.getCategory());
            entity.setStatus("DRAFT");
            if (req.getTags() != null) {
                entity.setTags(objectMapper.writeValueAsString(req.getTags()));
            }
            qaPairMapper.insert(entity);
            log.info("问答对创建成功: {}", entity.getId());
        } catch (Exception e) {
            log.error("创建问答对失败: {}", e.getMessage());
            throw new RuntimeException("创建问答对失败: " + e.getMessage());
        }
    }

    @Override
    public void qaUpdate(String id, Map<String, Object> data) {
        try {
            QaPairEntity entity = qaPairMapper.selectById(id);
            if (entity == null) throw new RuntimeException("问答对不存在");
            if (data.containsKey("question")) entity.setQuestion((String) data.get("question"));
            if (data.containsKey("answer")) entity.setAnswer((String) data.get("answer"));
            if (data.containsKey("answerBrief")) entity.setAnswerBrief((String) data.get("answerBrief"));
            if (data.containsKey("category")) entity.setCategory((String) data.get("category"));
            if (data.containsKey("status")) entity.setStatus((String) data.get("status"));
            if (data.containsKey("tags")) entity.setTags(objectMapper.writeValueAsString(data.get("tags")));
            qaPairMapper.updateById(entity);
            log.info("问答对 {} 更新成功", id);
        } catch (Exception e) {
            log.error("更新问答对失败: {}", e.getMessage());
            throw new RuntimeException("更新失败: " + e.getMessage());
        }
    }

    @Override
    public void qaDelete(String id) {
        try {
            qaPairMapper.deleteById(id);
            log.info("问答对 {} 删除成功", id);
        } catch (Exception e) {
            log.error("删除问答对失败: {}", e.getMessage());
            throw new RuntimeException("删除失败: " + e.getMessage());
        }
    }

    // ===== 审核管理 =====

    @Override
    public PageResult<ReviewVO> reviewList(Integer pageNum, Integer pageSize, String type, String status) {
        try {
            LambdaQueryWrapper<ReviewEntity> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(type)) wrapper.eq(ReviewEntity::getType, type);
            if (StringUtils.hasText(status)) wrapper.eq(ReviewEntity::getStatus, status);
            wrapper.orderByDesc(ReviewEntity::getSubmittedAt);

            Page<ReviewEntity> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
            IPage<ReviewEntity> result = reviewMapper.selectPage(page, wrapper);

            List<ReviewVO> records = result.getRecords().stream()
                    .map(this::reviewToVO)
                    .collect(Collectors.toList());

            return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
        } catch (Exception e) {
            log.warn("数据库查询审核失败，返回Mock数据: {}", e.getMessage());
            return getMockReviewList(pageNum, pageSize, type, status);
        }
    }

    @Override
    public void reviewUpdate(String id, Map<String, Object> data) {
        try {
            ReviewEntity entity = reviewMapper.selectById(id);
            if (entity == null) throw new RuntimeException("审核记录不存在");
            if (data.containsKey("status")) entity.setStatus((String) data.get("status"));
            if (data.containsKey("reviewer")) entity.setReviewer((String) data.get("reviewer"));
            if (data.containsKey("reviewComment")) entity.setReviewComment((String) data.get("reviewComment"));
            reviewMapper.updateById(entity);
            log.info("审核 {} 更新成功", id);
        } catch (Exception e) {
            log.error("更新审核失败: {}", e.getMessage());
            throw new RuntimeException("更新失败: " + e.getMessage());
        }
    }

    // ===== 迭代记录管理 =====

    @Override
    public PageResult<IterationVO> iterationList(Integer pageNum, Integer pageSize, String status) {
        try {
            LambdaQueryWrapper<IterationEntity> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(status)) wrapper.eq(IterationEntity::getStatus, status);
            wrapper.orderByDesc(IterationEntity::getCreatedAt);

            Page<IterationEntity> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
            IPage<IterationEntity> result = iterationMapper.selectPage(page, wrapper);

            List<IterationVO> records = result.getRecords().stream()
                    .map(this::iterationToVO)
                    .collect(Collectors.toList());

            return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
        } catch (Exception e) {
            log.warn("数据库查询迭代失败，返回Mock数据: {}", e.getMessage());
            return getMockIterationList(pageNum, pageSize, status);
        }
    }

    @Override
    public void iterationUpdate(String id, Map<String, Object> data) {
        try {
            IterationEntity entity = iterationMapper.selectById(id);
            if (entity == null) throw new RuntimeException("迭代记录不存在");
            if (data.containsKey("status")) entity.setStatus((String) data.get("status"));
            iterationMapper.updateById(entity);
            log.info("迭代记录 {} 更新成功", id);
        } catch (Exception e) {
            log.error("更新迭代失败: {}", e.getMessage());
            throw new RuntimeException("更新失败: " + e.getMessage());
        }
    }

    // ===== 分析概览 =====

    @Override
    public Map<String, Object> analytics() {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            // 尝试从数据库统计
            try {
                result.put("totalSessions", sessionMapper != null ? sessionMapper.selectCount(null) : 0);
                result.put("totalQa", qaPairMapper.selectCount(null));
                result.put("totalDocuments", documentMapper.selectCount(null));
                result.put("totalReports", reportMapper != null ? reportMapper.selectCount(null) : 0);
                result.put("pendingReviews", reviewMapper.selectCount(
                        new LambdaQueryWrapper<ReviewEntity>().eq(ReviewEntity::getStatus, "PENDING")));
            } catch (Exception ignored) {
                // 使用Mock数据
            }
            return result;
        } catch (Exception e) {
            log.warn("获取分析数据失败，返回Mock数据: {}", e.getMessage());
        }
        return getMockAnalytics();
    }

    // 额外的mapper引用（延迟注入会导致问题，使用try-catch保护）
    @javax.annotation.Resource
    private com.smartlink.mapper.SessionMapper sessionMapper;
    @javax.annotation.Resource
    private com.smartlink.mapper.ReportMapper reportMapper;

    // ===== 实体转VO =====

    private DocumentVO docToVO(DocumentEntity e) {
        DocumentVO vo = new DocumentVO();
        vo.setId(e.getId()); vo.setTitle(e.getTitle()); vo.setFormat(e.getFormat());
        vo.setCategory(e.getCategory()); vo.setSubcategory(e.getSubcategory());
        vo.setFileSize(e.getFileSize());
        if (e.getUploadTime() != null) vo.setUploadTime(e.getUploadTime().format(DTF));
        vo.setParseStatus(e.getParseStatus()); vo.setContent(e.getContent());
        return vo;
    }

    private QaPairVO qaToVO(QaPairEntity e) {
        QaPairVO vo = new QaPairVO();
        vo.setId(e.getId()); vo.setQuestion(e.getQuestion()); vo.setAnswer(e.getAnswer());
        vo.setAnswerBrief(e.getAnswerBrief()); vo.setSourceDocId(e.getSourceDocId());
        vo.setCategory(e.getCategory());
        vo.setTags(parseJsonToObject(e.getTags()));
        vo.setStatus(e.getStatus());
        if (e.getConfidence() != null) vo.setConfidence(e.getConfidence().doubleValue());
        vo.setMultiDimensional(parseJsonToMap(e.getMultiDimensional()));
        vo.setSyncStatus(e.getSyncStatus());
        if (e.getSyncedAt() != null) vo.setSyncedAt(e.getSyncedAt().format(DTF));
        if (e.getCreatedAt() != null) vo.setCreatedAt(e.getCreatedAt().format(DTF));
        if (e.getUpdatedAt() != null) vo.setUpdatedAt(e.getUpdatedAt().format(DTF));
        return vo;
    }

    private ReviewVO reviewToVO(ReviewEntity e) {
        ReviewVO vo = new ReviewVO();
        vo.setId(e.getId()); vo.setType(e.getType()); vo.setTargetId(e.getTargetId());
        vo.setTitle(e.getTitle()); vo.setSubmitter(e.getSubmitter());
        if (e.getSubmittedAt() != null) vo.setSubmittedAt(e.getSubmittedAt().format(DTF));
        vo.setStatus(e.getStatus()); vo.setReviewer(e.getReviewer());
        vo.setReviewComment(e.getReviewComment());
        return vo;
    }

    private IterationVO iterationToVO(IterationEntity e) {
        IterationVO vo = new IterationVO();
        vo.setId(e.getId()); vo.setSourceSessionId(e.getSourceSessionId());
        vo.setSessionSummary(e.getSessionSummary()); vo.setExtractedQuestion(e.getExtractedQuestion());
        vo.setExtractedAnswer(e.getExtractedAnswer()); vo.setExistingQaId(e.getExistingQaId());
        vo.setComparison(parseJsonToMap(e.getComparison()));
        if (e.getCreatedAt() != null) vo.setCreatedAt(e.getCreatedAt().format(DTF));
        vo.setStatus(e.getStatus());
        return vo;
    }

    private Map<String, Object> parseJsonToMap(String json) {
        try {
            if (StringUtils.hasText(json)) {
                return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) { log.warn("JSON解析失败: {}", e.getMessage()); }
        return new LinkedHashMap<>();
    }

    private Object parseJsonToObject(String json) {
        try {
            if (StringUtils.hasText(json)) return objectMapper.readTree(json);
        } catch (Exception e) { log.warn("JSON解析失败: {}", e.getMessage()); }
        return null;
    }

    // ==================== Mock数据 ====================

    private PageResult<DocumentVO> getMockDocumentList(Integer pageNum, Integer pageSize, String category, String parseStatus) {
        List<DocumentVO> all = buildMockDocuments();
        List<DocumentVO> filtered = all.stream()
                .filter(d -> {
                    if (StringUtils.hasText(category) && !category.equals(d.getCategory())) return false;
                    if (StringUtils.hasText(parseStatus) && !parseStatus.equals(d.getParseStatus())) return false;
                    return true;
                }).collect(Collectors.toList());
        int page = pageNum != null ? pageNum : 1;
        int size = pageSize != null ? pageSize : 10;
        long total = filtered.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, (int) total);
        if (from >= total) return PageResult.of(Collections.emptyList(), total, page, size);
        return PageResult.of(filtered.subList(from, to), total, page, size);
    }

    private List<DocumentVO> buildMockDocuments() {
        List<DocumentVO> list = new ArrayList<>();
        list.add(doc("DOC20260701001", "一汽解放J7系列用户手册", "PDF", "产品手册", "整车文档", 15200000L, "2026-06-15 10:00:00", "PARSED", "一汽解放J7系列重卡用户手册完整内容。"));
        list.add(doc("DOC20260701002", "T-Box V3终端安装与调试指南", "PDF", "技术文档", "终端设备", 8600000L, "2026-06-16 14:30:00", "PARSED", "FAW-TBox-V3车载终端安装手册。"));
        list.add(doc("DOC20260702003", "DPF再生操作常见问题FAQ", "DOCX", "知识库", "故障处理", 2400000L, "2026-06-20 09:00:00", "PARSED", "DPF颗粒捕集器相关知识文档。"));
        list.add(doc("DOC20260702004", "解放卡车保养周期与项目表", "XLSX", "知识库", "保养维修", 1800000L, "2026-06-22 11:00:00", "PARSED", "一汽解放全系车型保养周期表。"));
        list.add(doc("DOC20260703005", "车队管理平台V3.2操作手册", "PDF", "产品手册", "平台文档", 12500000L, "2026-06-25 16:00:00", "PARSED", "鱼快创领车队管理平台操作指南。"));
        list.add(doc("DOC20260704006", "国六排放标准与SCR系统维修手册", "PDF", "技术文档", "排放系统", 9800000L, "2026-07-01 08:00:00", "PARSED", "解放国六车型SCR维修手册。"));
        list.add(doc("DOC20260705007", "2026年夏季高温天气车辆保养要点", "DOCX", "知识库", "季节性文档", 1200000L, "2026-07-05 13:00:00", "PENDING", "夏季高温天气车辆保养注意事项。"));
        list.add(doc("DOC20260706008", "车载终端故障代码大全V5.2", "PDF", "技术文档", "终端设备", 6200000L, "2026-07-08 10:00:00", "PARSED", "FAW-TBox故障代码手册，收录200+故障码。"));
        return list;
    }

    private DocumentVO doc(String id, String title, String format, String category, String subcategory, Long fileSize, String uploadTime, String parseStatus, String content) {
        DocumentVO vo = new DocumentVO();
        vo.setId(id); vo.setTitle(title); vo.setFormat(format); vo.setCategory(category);
        vo.setSubcategory(subcategory); vo.setFileSize(fileSize); vo.setUploadTime(uploadTime);
        vo.setParseStatus(parseStatus); vo.setContent(content);
        return vo;
    }

    private PageResult<QaPairVO> getMockQaList(QaQueryReq req) {
        List<QaPairVO> all = buildMockQas();
        List<QaPairVO> filtered = all.stream()
                .filter(q -> {
                    if (StringUtils.hasText(req.getCategory()) && !req.getCategory().equals(q.getCategory())) return false;
                    if (StringUtils.hasText(req.getStatus()) && !req.getStatus().equals(q.getStatus())) return false;
                    if (StringUtils.hasText(req.getKeyword())) {
                        String kw = req.getKeyword().toLowerCase();
                        return (q.getQuestion() != null && q.getQuestion().contains(kw))
                                || (q.getAnswer() != null && q.getAnswer().contains(kw));
                    }
                    return true;
                }).collect(Collectors.toList());
        int page = req.getPage(); int size = req.getPageSize();
        long total = filtered.size();
        int from = (page - 1) * size; int to = Math.min(from + size, (int) total);
        if (from >= total) return PageResult.of(Collections.emptyList(), total, page, size);
        return PageResult.of(filtered.subList(from, to), total, page, size);
    }

    private QaPairVO getMockQaDetail(String id) {
        return buildMockQas().stream().filter(q -> q.getId().equals(id)).findFirst().orElse(null);
    }

    private List<QaPairVO> buildMockQas() {
        List<QaPairVO> list = new ArrayList<>();
        list.add(qa("QA20260701001", "J7卡车DPF灯亮了怎么办？还能继续开吗？",
                "当DPF（颗粒捕集器）报警灯亮起时，说明DPF中积碳已达到需要进行再生的程度。建议处理方案：1. 如果车辆动力正常，可以继续行驶但应尽快进行DPF再生；2. 降低车速至80km/h以下；3. 前往最近的服务站进行强制再生；4. 如果动力明显下降或伴有发动机故障灯，建议立即靠边停车并联系客服救援。不建议长期无视DPF报警继续行驶，否则可能导致DPF堵塞损坏，维修费用高达1-2万元。",
                "DPF灯亮应尽快进行再生处理，降低车速前往服务站，不可长期无视。", "DOC20260702003", "故障处理", "[\"DPF\",\"颗粒捕集器\",\"再生\",\"报警灯\",\"J7\"]", "PUBLISHED", 0.9520, "SYNCED", "2026-07-15 08:00:00", "2026-06-20 09:30:00", "2026-07-01 10:00:00"));
        list.add(qa("QA20260701002", "解放卡车10万公里大保养需要做哪些项目？费用多少？",
                "解放卡车10万公里大保养属于重要保养节点，主要包括以下项目：1. 更换发动机机油和机油滤芯；2. 更换柴油滤芯（粗滤+精滤）；3. 更换空气滤芯；4. 更换变速箱油和后桥齿轮油；5. 检查并调整气门间隙；6. 检查刹车片磨损情况；7. 检查转向系统和悬挂系统；8. 检查T-Box终端工作状态。全套费用约3500-5000元，保养时间约3-4小时。",
                "10万公里保养包含机油三滤、变速箱油、后桥油、气门间隙等，费用3500-5000元，耗时3-4小时。", "DOC20260702004", "保养维修", "[\"保养\",\"10万公里\",\"大保养\",\"费用\"]", "PUBLISHED", 0.9380, "SYNCED", "2026-07-15 08:30:00", "2026-06-22 14:00:00", "2026-07-02 08:30:00"));
        list.add(qa("QA20260702003", "T-Box设备离线了怎么恢复？",
                "T-Box离线恢复步骤：1. 首先确认车辆是否在地下室、隧道等信号弱区域；2. 检查车辆电瓶电压是否正常（应大于24V）；3. 熄火状态下等待5分钟后重新通电；4. 通过车队管理平台发送远程重启指令；5. 如果以上方法无效，可能需要远程固件升级或服务站检测硬件。常见原因：SIM卡欠费、天线松动、固件版本过低、电源模块故障。",
                "T-Box离线先确认信号和电源，可远程重启或升级固件，无法恢复需服务站检测。", "DOC20260701002", "终端设备", "[\"T-Box\",\"离线\",\"远程诊断\",\"固件升级\"]", "PUBLISHED", 0.9210, "SYNCED", "2026-07-16 09:00:00", "2026-06-25 10:00:00", "2026-07-03 09:00:00"));
        list.add(qa("QA20260703004", "如何通过车队管理平台查看车辆油耗分析？",
                "油耗分析操作步骤：1. 登录车队管理平台（PC端或APP端）；2. 在左侧导航栏选择[油耗管理]模块；3. 选择查看维度：单车油耗/车队油耗/月度趋势；4. 选择时间范围；5. 查看百公里油耗曲线图、怠速油耗占比等；6. 支持导出油耗报表（PDF/Excel格式）。平台还提供异常油耗自动告警功能。",
                "登录平台→油耗管理→选择车辆和时间→查看油耗曲线和统计分析→可导出报表。", "DOC20260703005", "平台操作", "[\"油耗分析\",\"车队管理\",\"平台操作\"]", "PUBLISHED", 0.8950, "NOT_SYNCED", null, "2026-06-28 15:00:00", "2026-07-04 11:00:00"));
        list.add(qa("QA20260704005", "国六SCR系统故障怎么排查？尿素消耗快正常吗？",
                "SCR系统故障排查步骤：1. 检查尿素液位是否充足（低于10%会触发报警）；2. 检查尿素质量是否符合GB29518标准；3. 读取故障码判断具体问题；4. 检查尿素管路是否泄漏或结晶。尿素正常消耗量为柴油消耗量的3%-5%，即每100L柴油消耗3-5L尿素。如果消耗明显偏高，可能是排温传感器故障或尿素喷嘴雾化不良。",
                "SCR系统故障先检查尿素液位和质量，读取故障码定位问题。尿素正常消耗为柴油的3%-5%。", "DOC20260704006", "排放系统", "[\"SCR\",\"尿素\",\"国六\",\"排放\"]", "PUBLISHED", 0.9080, "SYNCED", "2026-07-17 10:00:00", "2026-07-02 09:00:00", "2026-07-05 14:00:00"));
        list.add(qa("QA20260705006", "车队管理系统如何批量导入车辆？",
                "批量导入车辆流程：1. 准备车辆信息Excel模板（VIN码、车牌号、T-Box ICCID、车辆型号、所属车队）；2. 登录平台→车辆管理→批量导入→下载模板→填写数据→上传文件；3. 系统自动校验VIN码和ICCID有效性；4. 校验通过后一键导入；5. 等待T-Box上线验证数据。注意：VIN码17位，ICCID 20位，单次最多导入500台车。",
                "下载Excel模板→填写VIN/ICCID→上传→系统自动校验→一键导入→等待激活。", "DOC20260703005", "平台操作", "[\"批量导入\",\"车辆管理\",\"车队\"]", "PUBLISHED", 0.8860, "NOT_SYNCED", null, "2026-07-05 16:00:00", "2026-07-06 10:00:00"));
        list.add(qa("QA20260706007", "夏季高温天气解放卡车需要注意哪些保养事项？",
                "夏季高温保养要点：1. 空调系统：检查制冷剂压力，清洗冷凝器散热片；2. 冷却系统：检查冷却液液位和冰点；3. 轮胎：适当降低胎压（比标准低5%-10%）；4. 电瓶：检查电瓶液位，清洁接线柱氧化物；5. 线路防火：检查发动机舱线路老化情况；6. 油品：使用夏季标号机油（如15W-40）；7. 驾驶习惯：避免长时间高转速行驶。",
                "夏季保养重点：空调、冷却液、轮胎气压、电瓶、线路检查、夏季机油、避免高温长时间行驶。", "DOC20260705007", "季节性文档", "[\"夏季保养\",\"高温\",\"空调\",\"冷却液\"]", "PUBLISHED", 0.8720, "NOT_SYNCED", null, "2026-07-06 08:00:00", "2026-07-07 09:00:00"));
        list.add(qa("QA20260707008", "车辆ECU通讯丢失怎么办？什么原因导致的？",
                "ECU通讯丢失故障处理：1. 首先检查电瓶电压是否正常（24V系统电压应大于22V）；2. 检查ECU供电保险丝是否熔断；3. 检查电瓶负极搭铁线是否松动或腐蚀；4. 检查CAN总线终端电阻；5. 如果以上都正常，可能是ECU本体故障。紧急处理时可尝试断开电瓶负极1分钟后重新连接。",
                "检查电瓶电压→保险丝→搭铁线→CAN总线电阻→尝试断电重启→仍不行联系服务站。", "DOC20260706008", "故障处理", "[\"ECU\",\"通讯故障\",\"电瓶\",\"保险丝\"]", "PUBLISHED", 0.9140, "NOT_SYNCED", null, "2026-07-08 11:00:00", "2026-07-08 16:00:00"));
        return list;
    }

    private QaPairVO qa(String id, String question, String answer, String brief, String srcDocId, String category, String tagsJson, String status, double confidence, String syncStatus, String syncedAt, String createdAt, String updatedAt) {
        QaPairVO vo = new QaPairVO();
        vo.setId(id); vo.setQuestion(question); vo.setAnswer(answer); vo.setAnswerBrief(brief);
        vo.setSourceDocId(srcDocId); vo.setCategory(category);
        try { vo.setTags(objectMapper.readTree(tagsJson)); } catch (Exception ignored) {}
        vo.setStatus(status); vo.setConfidence(confidence);
        vo.setSyncStatus(syncStatus); vo.setSyncedAt(syncedAt);
        vo.setCreatedAt(createdAt); vo.setUpdatedAt(updatedAt);
        Map<String, Object> multi = new LinkedHashMap<>();
        multi.put("vehicleModels", Arrays.asList("全系"));
        multi.put("difficulty", "中等");
        vo.setMultiDimensional(multi);
        return vo;
    }

    private PageResult<ReviewVO> getMockReviewList(Integer pageNum, Integer pageSize, String type, String status) {
        List<ReviewVO> all = buildMockReviews();
        List<ReviewVO> filtered = all.stream()
                .filter(r -> {
                    if (StringUtils.hasText(type) && !type.equals(r.getType())) return false;
                    if (StringUtils.hasText(status) && !status.equals(r.getStatus())) return false;
                    return true;
                }).collect(Collectors.toList());
        int page = pageNum != null ? pageNum : 1; int size = pageSize != null ? pageSize : 10;
        long total = filtered.size(); int from = (page - 1) * size; int to = Math.min(from + size, (int) total);
        if (from >= total) return PageResult.of(Collections.emptyList(), total, page, size);
        return PageResult.of(filtered.subList(from, to), total, page, size);
    }

    private List<ReviewVO> buildMockReviews() {
        List<ReviewVO> list = new ArrayList<>();
        list.add(review("RV20260701001", "问答审核", "QA20260701001", "DPF灯亮处理方案审核", "李娜", "2026-07-01 11:00:00", "APPROVED", "王主管", "内容准确，操作步骤清晰，审核通过。"));
        list.add(review("RV20260702002", "问答审核", "QA20260702003", "T-Box离线恢复方案审核", "刘芳", "2026-07-02 14:00:00", "APPROVED", "王主管", "覆盖了常见原因，远程诊断流程描述完整，通过。"));
        list.add(review("RV20260703003", "文档审核", "DOC20260704006", "国六SCR维修手册审核", "张伟", "2026-07-03 09:00:00", "PENDING", "", ""));
        list.add(review("RV20260704004", "迭代审核", "IT20260701001", "DPF问答优化迭代审核", "李娜", "2026-07-04 10:00:00", "APPROVED", "赵经理", "迭代方案合理，从真实工单中提取的问答对质量较高。"));
        list.add(review("RV20260705005", "问答审核", "QA20260705006", "批量导入车辆方案审核", "陈静", "2026-07-05 17:00:00", "APPROVED", "王主管", "操作步骤详细，注意事项完整。"));
        list.add(review("RV20260706006", "文档审核", "DOC20260705007", "夏季保养要点文档审核", "张伟", "2026-07-06 11:00:00", "REJECTED", "赵经理", "内容覆盖面不够全，缺少轮胎防火和线路老化检查部分。"));
        list.add(review("RV20260707007", "问答审核", "QA20260707008", "ECU通讯丢失方案审核", "刘芳", "2026-07-08 10:00:00", "PENDING", "", ""));
        return list;
    }

    private ReviewVO review(String id, String type, String targetId, String title, String submitter, String submittedAt, String status, String reviewer, String comment) {
        ReviewVO vo = new ReviewVO();
        vo.setId(id); vo.setType(type); vo.setTargetId(targetId); vo.setTitle(title);
        vo.setSubmitter(submitter); vo.setSubmittedAt(submittedAt); vo.setStatus(status);
        vo.setReviewer(reviewer); vo.setReviewComment(comment);
        return vo;
    }

    private PageResult<IterationVO> getMockIterationList(Integer pageNum, Integer pageSize, String status) {
        List<IterationVO> all = buildMockIterations();
        List<IterationVO> filtered = all.stream()
                .filter(i -> !StringUtils.hasText(status) || status.equals(i.getStatus()))
                .collect(Collectors.toList());
        int page = pageNum != null ? pageNum : 1; int size = pageSize != null ? pageSize : 10;
        long total = filtered.size(); int from = (page - 1) * size; int to = Math.min(from + size, (int) total);
        if (from >= total) return PageResult.of(Collections.emptyList(), total, page, size);
        return PageResult.of(filtered.subList(from, to), total, page, size);
    }

    private List<IterationVO> buildMockIterations() {
        List<IterationVO> list = new ArrayList<>();

        IterationVO i1 = new IterationVO();
        i1.setId("IT20260701001"); i1.setSourceSessionId("SE20260701001");
        i1.setSessionSummary("客户张建国反馈J7卡车DPF灯亮和发动机故障灯亮，动力有所下降。客服李娜建议降低车速前往长春服务站做DPF再生。");
        i1.setExtractedQuestion("J7卡车DPF灯亮且动力下降，应该怎么应急处理？");
        i1.setExtractedAnswer("DPF灯亮伴有动力下降时，建议：1. 降低车速至80km/h以下；2. 避免急加速和高转速；3. 尽快前往最近的服务站进行DPF强制再生。");
        i1.setExistingQaId("QA20260701001");
        Map<String, Object> comp1 = new LinkedHashMap<>();
        comp1.put("similarity", 0.92); comp1.put("improvement", "原问答未强调动力下降时的紧急性，建议补充费用对比信息");
        i1.setComparison(comp1);
        i1.setCreatedAt("2026-07-01 14:00:00"); i1.setStatus("PROCESSED");
        list.add(i1);

        IterationVO i2 = new IterationVO();
        i2.setId("IT20260702002"); i2.setSourceSessionId("SE20260703003");
        i2.setSessionSummary("客户李国强反馈车队鹰途T-Box连续两天离线。客服刘芳通过远程诊断发现是固件版本过低导致，执行远程升级后设备恢复正常。");
        i2.setExtractedQuestion("T-Box连续多天离线，远程升级固件能解决吗？");
        i2.setExtractedAnswer("T-Box长时间离线可能是固件版本问题。V3终端固件版本低于4.2.1的设备存在已知的掉线bug，可通过平台远程推送固件升级解决。");
        i2.setExistingQaId("QA20260702003");
        Map<String, Object> comp2 = new LinkedHashMap<>();
        comp2.put("similarity", 0.88); comp2.put("improvement", "原问答未提及特定版本号的已知问题，建议补充版本相关信息");
        i2.setComparison(comp2);
        i2.setCreatedAt("2026-07-03 10:30:00"); i2.setStatus("PROCESSED");
        list.add(i2);

        IterationVO i3 = new IterationVO();
        i3.setId("IT20260703003"); i3.setSourceSessionId("SE20260704004");
        i3.setSessionSummary("客户赵大勇J6P自卸车突然熄火无法启动。客服李娜通过远程诊断分析故障码U0100，指导客户检查电瓶和保险丝。最终发现是电瓶负极松动导致。");
        i3.setExtractedQuestion("J6P突然熄火打不着，故障码U0100，应该检查哪里？");
        i3.setExtractedAnswer("故障码U0100表示ECU通讯丢失，排查步骤：1. 优先检查电瓶负极线是否松动（最常见原因）；2. 检查ECU供电保险丝；3. 尝试断电重启。");
        i3.setExistingQaId("");
        Map<String, Object> comp3 = new LinkedHashMap<>();
        comp3.put("similarity", 0.75); comp3.put("improvement", "可新增一条针对工程车辆电瓶检查的问答");
        i3.setComparison(comp3);
        i3.setCreatedAt("2026-07-04 17:00:00"); i3.setStatus("NEW_QA_CREATED");
        list.add(i3);

        IterationVO i4 = new IterationVO();
        i4.setId("IT20260704004"); i4.setSourceSessionId("SE20260705005");
        i4.setSessionSummary("沈阳恒通物流车队长孙明辉咨询35台解放J7接入车队管理系统。客服陈静介绍了批量导入流程和费用方案，成功签约企业版套餐。");
        i4.setExtractedQuestion("车队批量接入智能车队管理系统的完整流程是什么？");
        i4.setExtractedAnswer("批量接入流程：1. 提供VIN清单和ICCID清单；2. 后台批量注册；3. 远程推送配置参数；4. 等待上线验证（约1-2小时）。企业版费用：每台车600元/年。");
        i4.setExistingQaId("QA20260705006");
        Map<String, Object> comp4 = new LinkedHashMap<>();
        comp4.put("similarity", 0.82); comp4.put("improvement", "原问答缺少费用和折扣信息，建议补充商务信息");
        i4.setComparison(comp4);
        i4.setCreatedAt("2026-07-05 13:00:00"); i4.setStatus("PROCESSED");
        list.add(i4);

        IterationVO i5 = new IterationVO();
        i5.setId("IT20260705005"); i5.setSourceSessionId("SE20260708008");
        i5.setSessionSummary("客户郑伟明查询DPF总成配件。客服李娜提供了配件号、价格、库存信息，同时建议了清洗再生的替代方案。");
        i5.setExtractedQuestion("DPF总成更换与清洗再生该如何选择？费用差多少？");
        i5.setExtractedAnswer("DPF故障处理：1. 清洗再生：适用于轻度至中度堵塞，费用约1500元，成功率约80%；2. 总成更换：适用于严重堵塞，费用约12800元（原厂）。建议先尝试清洗再生。");
        i5.setExistingQaId("");
        Map<String, Object> comp5 = new LinkedHashMap<>();
        comp5.put("similarity", 0.70); comp5.put("improvement", "建议新建一条DPF清洗vs更换的对比问答");
        i5.setComparison(comp5);
        i5.setCreatedAt("2026-07-08 16:30:00"); i5.setStatus("PENDING");
        list.add(i5);

        return list;
    }

    private Map<String, Object> getMockAnalytics() {
        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("totalSessions", 8);
        analytics.put("totalQa", 8);
        analytics.put("totalDocuments", 8);
        analytics.put("totalReports", 5);
        analytics.put("totalReviews", 7);
        analytics.put("totalIterations", 5);
        analytics.put("pendingReviews", 2);
        analytics.put("completionRate", "87.2%");
        analytics.put("avgConfidence", 0.923);
        analytics.put("topCategories", Arrays.asList("故障处理", "保养维修", "终端设备"));
        analytics.put("satisfactionRate", "4.6/5.0");
        return analytics;
    }
}
