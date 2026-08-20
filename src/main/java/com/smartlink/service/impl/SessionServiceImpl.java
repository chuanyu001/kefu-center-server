package com.smartlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlink.common.PageResult;
import com.smartlink.dto.request.SessionQueryReq;
import com.smartlink.dto.request.SessionUpdateReq;
import com.smartlink.dto.response.SessionVO;
import com.smartlink.entity.SessionEntity;
import com.smartlink.mapper.SessionMapper;
import com.smartlink.service.SessionService;
import com.smartlink.service.VehicleInfoService;
import com.smartlink.util.ExcelService;
import com.smartlink.util.ImportFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class SessionServiceImpl implements SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private ExcelService excelService;

    @Autowired
    private ImportFileService importFileService;

    @Autowired
    private VehicleInfoService vehicleInfoService;

    @Override
    public PageResult<SessionVO> list(SessionQueryReq req) {
        LambdaQueryWrapper<SessionEntity> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(req.getKeyword())) {
            wrapper.and(w -> w
                    .like(SessionEntity::getCustomerName, req.getKeyword())
                    .or()
                    .like(SessionEntity::getCustomerPhone, req.getKeyword())
                    .or()
                    .like(SessionEntity::getVin, req.getKeyword())
                    .or()
                    .like(SessionEntity::getAgentName, req.getKeyword())
                    .or()
                    .like(SessionEntity::getCarModel, req.getKeyword())
                    .or()
                    .like(SessionEntity::getRecorderDeviceId, req.getKeyword())
                    .or()
                    .like(SessionEntity::getSimCard, req.getKeyword())
            );
        }

        if (StringUtils.isNotBlank(req.getWorkRecordType())) {
            wrapper.eq(SessionEntity::getWorkRecordType, req.getWorkRecordType());
        }

        wrapper.orderByDesc(SessionEntity::getSessionTime);

        Page<SessionEntity> page = new Page<>(req.getPage(), req.getPageSize());
        Page<SessionEntity> result = sessionMapper.selectPage(page, wrapper);

        List<SessionVO> voList = result.getRecords().stream()
                .map(this::entityToVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), req.getPage(), req.getPageSize());
    }

    @Override
    public SessionVO detail(String id) {
        SessionEntity entity = sessionMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        return entityToVO(entity);
    }

    @Override
    @Transactional
    public void update(String id, SessionUpdateReq req) {
        SessionEntity entity = sessionMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("记录不存在: " + id);
        }

        List<Map<String, Object>> history = readModificationHistory(entity.getModificationHistory());
        boolean changed = false;

        changed |= applyChange(history, "sessionTime", "时间", entity.getSessionTime(),
                req.getSessionTime(), "人工编辑", entity::setSessionTime);

        String normalizedVin = req.getVin() == null ? null : normalizeVin(req.getVin());
        boolean vinChanged = applyChange(history, "vin", "VIN码", entity.getVin(),
                normalizedVin, "人工编辑", entity::setVin);
        changed |= vinChanged;

        changed |= applyChange(history, "recorderDeviceId", "ID号", entity.getRecorderDeviceId(),
                req.getRecorderDeviceId(), "人工编辑", entity::setRecorderDeviceId);
        changed |= applyChange(history, "simCard", "SIM号", entity.getSimCard(),
                req.getSimCard(), "人工编辑", entity::setSimCard);
        changed |= applyChange(history, "iccid", "ICCID", entity.getIccid(),
                req.getIccid(), "人工编辑", entity::setIccid);
        changed |= applyChange(history, "consultationScenario", "咨询场景", entity.getConsultationScenario(),
                req.getConsultationScenario(), "人工编辑", entity::setConsultationScenario);
        changed |= applyChange(history, "problemType", "问题类型", entity.getProblemType(),
                req.getProblemType(), "人工编辑", entity::setProblemType);
        changed |= applyChange(history, "temporarySolution", "临时解决方案", entity.getTemporarySolution(),
                req.getTemporarySolution(), "人工编辑", entity::setTemporarySolution);
        changed |= applyChange(history, "specialNotes", "特殊备注", entity.getSpecialNotes(),
                req.getSpecialNotes(), "人工编辑", entity::setSpecialNotes);
        changed |= applyChange(history, "antennaPosition", "天线位置", entity.getAntennaPosition(),
                req.getAntennaPosition(), "人工编辑", entity::setAntennaPosition);
        changed |= applyChange(history, "noPositionReason", "未定位原因", entity.getNoPositionReason(),
                req.getNoPositionReason(), "人工编辑", entity::setNoPositionReason);
        changed |= applyChange(history, "noPositionIssue", "未定位问题", entity.getNoPositionIssue(),
                req.getNoPositionIssue(), "人工编辑", entity::setNoPositionIssue);
        changed |= applyChange(history, "antennaDamaged", "天线是否损坏", entity.getAntennaDamaged(),
                req.getAntennaDamaged(), "人工编辑", entity::setAntennaDamaged);
        changed |= applyChange(history, "manufacturer", "厂家", entity.getManufacturer(),
                req.getManufacturer(), "人工编辑", entity::setManufacturer);
        changed |= applyChange(history, "recorderModel", "记录仪型号", entity.getRecorderModel(),
                req.getRecorderModel(), "人工编辑", entity::setRecorderModel);

        if (vinChanged) {
            // VIN 改变时旧车型已不再可靠，必须按新 VIN 重新匹配；无匹配则保持为空。
            changed |= applyChange(history, "carModel", "车型", entity.getCarModel(), "",
                    "VIN变更清理", entity::setCarModel);
            changed |= applyChange(history, "fuelType", "燃料类型", entity.getFuelType(), "",
                    "VIN变更清理", entity::setFuelType);
            VehicleInfoService.LookupResult lookup = vehicleInfoService.lookup(entity.getVin());
            if (lookup.isMatched()) {
                String actor = "mock".equals(lookup.getSource()) ? "VIN测试映射" : "VIN接口补全";
                changed |= applyChange(history, "carModel", "车型", entity.getCarModel(),
                        lookup.getCarModel(), actor, entity::setCarModel);
                changed |= applyChange(history, "fuelType", "燃料类型", entity.getFuelType(),
                        lookup.getFuelType(), actor, entity::setFuelType);
            }
        } else {
            changed |= applyChange(history, "carModel", "车型", entity.getCarModel(),
                    req.getCarModel(), "人工编辑", entity::setCarModel);
            changed |= applyChange(history, "fuelType", "燃料类型", entity.getFuelType(),
                    req.getFuelType(), "人工编辑", entity::setFuelType);
        }

        if (changed) {
            entity.setModificationHistory(writeModificationHistory(history));
            entity.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(entity);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> importFile(MultipartFile file) {
        ImportFileService.ParseResult parsed = importFileService.parse(file);
        List<SessionEntity> parsedList = parsed.getRecords();
        Map<String, VehicleInfoService.LookupResult> vehicleCache = new HashMap<>();
        int inserted = 0;
        int skipped = 0;
        int vehicleEnriched = 0;
        int recorderEnriched = 0;

        for (SessionEntity entity : parsedList) {
            boolean enrichedCar = false;
            boolean enrichedRecorder = false;
            if (!isBlank(entity.getVin())) {
                String vin = normalizeVin(entity.getVin());
                entity.setVin(vin);
                VehicleInfoService.LookupResult lookup = vehicleCache.computeIfAbsent(
                        vin, vehicleInfoService::lookup);
                if (lookup.isMatched()) {
                    if (isBlank(entity.getCarModel()) && !isBlank(lookup.getCarModel())) {
                        entity.setCarModel(blankToNull(lookup.getCarModel()));
                        enrichedCar = true;
                    }
                    if (isBlank(entity.getFuelType()) && !isBlank(lookup.getFuelType())) {
                        entity.setFuelType(blankToNull(lookup.getFuelType()));
                        enrichedCar = true;
                    }
                    if (isBlank(entity.getManufacturer()) && !isBlank(lookup.getManufacturer())) {
                        entity.setManufacturer(blankToNull(lookup.getManufacturer()));
                        enrichedCar = true;
                    }
                    if (isBlank(entity.getRecorderModel()) && !isBlank(lookup.getRecorderModel())) {
                        entity.setRecorderModel(blankToNull(lookup.getRecorderModel()));
                        enrichedCar = true;
                    }
                    if (isBlank(entity.getRecorderDeviceId()) && !isBlank(lookup.getRecorderDeviceId())) {
                        entity.setRecorderDeviceId(blankToNull(lookup.getRecorderDeviceId()));
                        enrichedRecorder = true;
                    }
                    if (isBlank(entity.getSimCard()) && !isBlank(lookup.getSimCard())) {
                        entity.setSimCard(blankToNull(lookup.getSimCard()));
                        enrichedRecorder = true;
                    }
                }
            }
            if (existsDuplicate(entity)) {
                skipped++;
                continue;
            }
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            sessionMapper.insert(entity);
            inserted++;
            if (enrichedCar) {
                vehicleEnriched++;
            }
            if (enrichedRecorder) {
                recorderEnriched++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", parsedList.size());
        result.put("inserted", inserted);
        result.put("newCount", inserted);
        result.put("skipped", skipped);
        result.put("recognizedFields", parsed.getRecognizedFields());
        Map<String, Integer> missingFields = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : parsed.getRecognizedFields().entrySet()) {
            missingFields.put(entry.getKey(), parsedList.size() - entry.getValue());
        }
        result.put("missingFields", missingFields);
        result.put("vehicleEnriched", vehicleEnriched);
        result.put("recorderEnriched", recorderEnriched);
        result.put("vehicleMode", vehicleInfoService.getMode());
        result.put("fileType", parsed.getFileType());
        result.put("success", true);
        return result;
    }

    @Override
    public byte[] exportExcel(List<String> ids, List<String> columns) {
        List<SessionEntity> list;
        if (ids != null && !ids.isEmpty()) {
            list = sessionMapper.selectBatchIds(ids);
        } else {
            list = sessionMapper.selectList(null);
        }

        if (columns == null || columns.isEmpty()) {
            columns = Arrays.asList("vin", "customerName", "customerPhone", "carModel",
                    "fuelType", "agentName", "sessionTime", "workRecordType",
                    "qiyuTicketStatus", "consultationScenario", "problemType");
        }

        return excelService.exportExcel(list, columns);
    }

    @Override
    @Transactional
    public Map<String, Object> syncVehicleInfo() {
        LambdaQueryWrapper<SessionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SessionEntity::getWorkRecordType, "recorder_register")
                .isNotNull(SessionEntity::getVin)
                .ne(SessionEntity::getVin, "")
                .and(w -> w
                        .isNull(SessionEntity::getCarModel).or().eq(SessionEntity::getCarModel, "")
                        .or().isNull(SessionEntity::getFuelType).or().eq(SessionEntity::getFuelType, "")
                        .or().isNull(SessionEntity::getManufacturer).or().eq(SessionEntity::getManufacturer, "")
                        .or().isNull(SessionEntity::getRecorderModel).or().eq(SessionEntity::getRecorderModel, "")
                        .or().isNull(SessionEntity::getRecorderDeviceId).or().eq(SessionEntity::getRecorderDeviceId, "")
                        .or().isNull(SessionEntity::getSimCard).or().eq(SessionEntity::getSimCard, ""));

        List<SessionEntity> needSyncList = sessionMapper.selectList(wrapper);
        Map<String, VehicleInfoService.LookupResult> detailCache = new LinkedHashMap<>();
        Map<String, VehicleInfoService.LookupResult> recorderCache = new LinkedHashMap<>();
        int updated = 0;
        for (SessionEntity entity : needSyncList) {
            String vin = normalizeVin(entity.getVin());
            boolean changed = false;

            // 车型/燃料/厂家/记录仪型号：预留运营平台 API（现为模拟数据），无需外呼
            boolean needDetail = isBlank(entity.getCarModel()) || isBlank(entity.getFuelType())
                    || isBlank(entity.getManufacturer()) || isBlank(entity.getRecorderModel());
            if (needDetail) {
                VehicleInfoService.LookupResult detail = detailCache.computeIfAbsent(
                        vin, vehicleInfoService::lookupDetail);
                if (detail.isMatched()) {
                    if (isBlank(entity.getCarModel()) && !isBlank(detail.getCarModel())) {
                        entity.setCarModel(detail.getCarModel());
                        changed = true;
                    }
                    if (isBlank(entity.getFuelType()) && !isBlank(detail.getFuelType())) {
                        entity.setFuelType(detail.getFuelType());
                        changed = true;
                    }
                    if (isBlank(entity.getManufacturer()) && !isBlank(detail.getManufacturer())) {
                        entity.setManufacturer(detail.getManufacturer());
                        changed = true;
                    }
                    if (isBlank(entity.getRecorderModel()) && !isBlank(detail.getRecorderModel())) {
                        entity.setRecorderModel(detail.getRecorderModel());
                        changed = true;
                    }
                }
            }

            // ID号/SIM号：运营平台真实接口，仅在缺失时外呼
            boolean needRecorder = isBlank(entity.getRecorderDeviceId()) || isBlank(entity.getSimCard());
            if (needRecorder) {
                VehicleInfoService.LookupResult recorder = recorderCache.computeIfAbsent(
                        vin, vehicleInfoService::lookupRecorder);
                if (recorder.isMatched()) {
                    if (isBlank(entity.getRecorderDeviceId()) && !isBlank(recorder.getRecorderDeviceId())) {
                        entity.setRecorderDeviceId(recorder.getRecorderDeviceId());
                        changed = true;
                    }
                    if (isBlank(entity.getSimCard()) && !isBlank(recorder.getSimCard())) {
                        entity.setSimCard(recorder.getSimCard());
                        changed = true;
                    }
                }
            }

            if (changed) {
                entity.setUpdatedAt(LocalDateTime.now());
                sessionMapper.updateById(entity);
                updated++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("mode", vehicleInfoService.getMode());
        result.put("totalVins", needSyncList.size());
        result.put("updated", updated);
        result.put("syncedCount", updated);
        return result;
    }

    @Override
    public Map<String, Object> analytics(String range) {
        Map<String, Object> result = new HashMap<>();

        // 时间范围：all / 30d / month → 起始日期字符串（session_time 前10位比较）
        String from = null;
        LocalDateTime now = LocalDateTime.now();
        if ("30d".equals(range)) {
            from = now.minusDays(30).toLocalDate().toString();
        } else if ("month".equals(range)) {
            from = now.withDayOfMonth(1).toLocalDate().toString();
        }

        result.put("range", from == null ? "all" : range);
        result.put("total", sessionMapper.countTotal(from));
        result.put("byAgent", sessionMapper.countByAgent(from));
        result.put("byQiyuStatus", decorateStatus(sessionMapper.countByQiyuStatus(from)));
        result.put("byProblemType", sessionMapper.countByProblemType(from));
        result.put("byCategory", sessionMapper.countByCategory(from));
        result.put("byMonth", sessionMapper.countByMonth(from));
        result.put("byCarModel", sessionMapper.countByCarModel(from));
        result.put("byFuelType", sessionMapper.countByFuelType(from));
        return result;
    }

    /** 七鱼工单状态码转中文（1-已提交 5-待申领 10-受理中 20-已完结） */
    private List<Map<String, Object>> decorateStatus(List<Map<String, Object>> rows) {
        Map<Object, String> statusMap = new HashMap<>();
        statusMap.put(1, "已提交");
        statusMap.put(5, "待申领");
        statusMap.put(10, "受理中");
        statusMap.put(20, "已完结");
        for (Map<String, Object> row : rows) {
            Object status = row.get("status");
            row.put("name", statusMap.getOrDefault(status, "未知状态"));
            row.remove("status");
        }
        return rows;
    }

    private boolean existsDuplicate(SessionEntity entity) {
        LambdaQueryWrapper<SessionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SessionEntity::getWorkRecordType, "recorder_register");
        boolean hasIdentityField = false;
        if (!isBlank(entity.getSessionTime())) {
            wrapper.eq(SessionEntity::getSessionTime, entity.getSessionTime());
            hasIdentityField = true;
        }
        if (!isBlank(entity.getVin())) {
            wrapper.eq(SessionEntity::getVin, entity.getVin());
            hasIdentityField = true;
        }
        if (!isBlank(entity.getRecorderDeviceId())) {
            wrapper.eq(SessionEntity::getRecorderDeviceId, entity.getRecorderDeviceId());
            hasIdentityField = true;
        }
        if (!isBlank(entity.getSimCard())) {
            wrapper.eq(SessionEntity::getSimCard, entity.getSimCard());
            hasIdentityField = true;
        }
        return hasIdentityField && sessionMapper.selectCount(wrapper) > 0;
    }

    private boolean applyChange(List<Map<String, Object>> history, String field, String fieldLabel,
                                String oldValue, String newValue, String modifiedBy,
                                Consumer<String> setter) {
        if (newValue == null || Objects.equals(nullToEmpty(oldValue), nullToEmpty(newValue))) {
            return false;
        }
        setter.accept(newValue);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", UUID.randomUUID().toString());
        item.put("field", field);
        item.put("fieldLabel", fieldLabel);
        item.put("oldValue", nullToEmpty(oldValue));
        item.put("newValue", nullToEmpty(newValue));
        item.put("modifiedBy", modifiedBy);
        item.put("modifiedAt", LocalDateTime.now().toString());
        history.add(item);
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readModificationHistory(String json) {
        if (isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(objectMapper.readValue(json, List.class));
        } catch (Exception e) {
            log.warn("Invalid modification history JSON, start a new history", e);
            return new ArrayList<>();
        }
    }

    private String writeModificationHistory(List<Map<String, Object>> history) {
        try {
            List<Map<String, Object>> retained = history;
            if (history.size() > 200) {
                retained = new ArrayList<>(history.subList(history.size() - 200, history.size()));
            }
            return objectMapper.writeValueAsString(retained);
        } catch (Exception e) {
            throw new RuntimeException("修改记录保存失败", e);
        }
    }

    private String normalizeVin(String vin) {
        return vin == null ? "" : vin.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    // ==================== Helper Methods ====================

    private SessionVO entityToVO(SessionEntity entity) {
        SessionVO vo = new SessionVO();
        BeanUtils.copyProperties(entity, vo);
        // Parse chatMessages JSON string to List
        if (entity.getChatMessages() != null) {
            try {
                vo.setChatMessages(objectMapper.readValue(entity.getChatMessages(), List.class));
            } catch (Exception e) {
                vo.setChatMessages(Collections.emptyList());
            }
        }
        // Parse modificationHistory JSON string to List
        if (entity.getModificationHistory() != null) {
            try {
                vo.setModificationHistory(objectMapper.readValue(entity.getModificationHistory(), List.class));
            } catch (Exception e) {
                vo.setModificationHistory(Collections.emptyList());
            }
        }
        return vo;
    }
}
