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
import com.smartlink.util.ExcelService;
import com.smartlink.util.OutServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
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
    private OutServiceUtil outServiceUtil;

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
    public void update(String id, SessionUpdateReq req) {
        SessionEntity entity = sessionMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("记录不存在: " + id);
        }

        if (StringUtils.isNotBlank(req.getIccid())) {
            entity.setIccid(req.getIccid());
        }
        if (StringUtils.isNotBlank(req.getConsultationScenario())) {
            entity.setConsultationScenario(req.getConsultationScenario());
        }
        if (StringUtils.isNotBlank(req.getProblemType())) {
            entity.setProblemType(req.getProblemType());
        }
        if (StringUtils.isNotBlank(req.getTemporarySolution())) {
            entity.setTemporarySolution(req.getTemporarySolution());
        }
        if (StringUtils.isNotBlank(req.getSpecialNotes())) {
            entity.setSpecialNotes(req.getSpecialNotes());
        }
        if (StringUtils.isNotBlank(req.getAntennaPosition())) {
            entity.setAntennaPosition(req.getAntennaPosition());
        }
        if (StringUtils.isNotBlank(req.getNoPositionReason())) {
            entity.setNoPositionReason(req.getNoPositionReason());
        }
        if (StringUtils.isNotBlank(req.getNoPositionIssue())) {
            entity.setNoPositionIssue(req.getNoPositionIssue());
        }
        if (StringUtils.isNotBlank(req.getAntennaDamaged())) {
            entity.setAntennaDamaged(req.getAntennaDamaged());
        }
        if (StringUtils.isNotBlank(req.getCarModel())) {
            entity.setCarModel(req.getCarModel());
        }
        if (StringUtils.isNotBlank(req.getFuelType())) {
            entity.setFuelType(req.getFuelType());
        }
        if (StringUtils.isNotBlank(req.getManufacturer())) {
            entity.setManufacturer(req.getManufacturer());
        }
        if (StringUtils.isNotBlank(req.getRecorderModel())) {
            entity.setRecorderModel(req.getRecorderModel());
        }

        entity.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(entity);
    }

    @Override
    @Transactional
    public Map<String, Object> importExcel(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        List<SessionEntity> parsedList = excelService.parseExcel(file);

        int inserted = 0;
        int skipped = 0;

        for (SessionEntity entity : parsedList) {
            // Dedup by vin + sessionTime
            LambdaQueryWrapper<SessionEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SessionEntity::getVin, entity.getVin())
                    .eq(SessionEntity::getSessionTime, entity.getSessionTime());
            Long count = sessionMapper.selectCount(wrapper);

            if (count == 0) {
                entity.setCreatedAt(LocalDateTime.now());
                entity.setUpdatedAt(LocalDateTime.now());
                sessionMapper.insert(entity);
                inserted++;
            } else {
                skipped++;
            }
        }

        result.put("total", parsedList.size());
        result.put("inserted", inserted);
        result.put("skipped", skipped);
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
    public Map<String, Object> syncVehicleInfo() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. 一次性查出所有需要同步的 VIN 及其对应记录
            LambdaQueryWrapper<SessionEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(SessionEntity::getVin)
                    .ne(SessionEntity::getVin, "")
                    .and(w -> w
                            .isNull(SessionEntity::getManufacturer).or().eq(SessionEntity::getManufacturer, "")
                            .or().isNull(SessionEntity::getSimCard).or().eq(SessionEntity::getSimCard, "")
                            .or().isNull(SessionEntity::getRecorderModel).or().eq(SessionEntity::getRecorderModel, "")
                            .or().isNull(SessionEntity::getRecorderDeviceId).or().eq(SessionEntity::getRecorderDeviceId, "")
                    );

            List<SessionEntity> needSyncList = sessionMapper.selectList(wrapper);
            Set<String> vinSet = needSyncList.stream()
                    .map(SessionEntity::getVin)
                    .filter(v -> v != null && !v.isEmpty())
                    .collect(Collectors.toSet());

            List<String> vinList = new ArrayList<>(vinSet);
            int totalSynced = 0;
            int batchSize = 50;

            for (int i = 0; i < vinList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, vinList.size());
                List<String> batch = vinList.subList(i, end);

                List<Map<String, Object>> vehicleInfoList = outServiceUtil.batchVehicleInfo(batch);
                List<Map<String, Object>> operateInfoList = outServiceUtil.batchVehicleOperateInfo(batch);

                // Build VIN -> merged info map
                Map<String, Map<String, Object>> infoMap = new HashMap<>();
                for (Map<String, Object> info : vehicleInfoList) {
                    Object vin = info.get("vin");
                    if (vin != null) infoMap.put(vin.toString(), info);
                }
                for (Map<String, Object> info : operateInfoList) {
                    Object vin = info.get("vin");
                    if (vin != null) {
                        infoMap.merge(vin.toString(), info, (old, newVal) -> {
                            old.putAll(newVal);
                            return old;
                        });
                    }
                }

                // 2. 批量更新：一次性查出该批次所有 VIN 的记录，收集更新后批量提交
                List<SessionEntity> batchRecords = sessionMapper.selectList(
                        new LambdaQueryWrapper<SessionEntity>().in(SessionEntity::getVin, batch));

                List<SessionEntity> toUpdate = new ArrayList<>();
                for (SessionEntity record : batchRecords) {
                    Map<String, Object> info = infoMap.get(record.getVin());
                    if (info == null) continue;

                    boolean changed = applyVehicleInfo(record, info);
                    if (changed) {
                        record.setUpdatedAt(LocalDateTime.now());
                        toUpdate.add(record);
                    }
                }

                for (SessionEntity entity : toUpdate) {
                    sessionMapper.updateById(entity);
                    totalSynced++;
                }
            }

            result.put("success", true);
            result.put("totalVins", vinList.size());
            result.put("syncedCount", totalSynced);
        } catch (Exception e) {
            log.error("syncVehicleInfo error", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * Apply vehicle info to entity fields that are currently empty.
     * @return true if any field was updated
     */
    private boolean applyVehicleInfo(SessionEntity entity, Map<String, Object> info) {
        boolean changed = false;

        if (isBlank(entity.getManufacturer())) {
            Object val = info.get("manufacturer");
            if (val != null && !val.toString().isEmpty()) {
                entity.setManufacturer(val.toString());
                changed = true;
            }
        }
        if (isBlank(entity.getRecorderModel())) {
            Object val = info.get("recorderModel");
            if (val != null && !val.toString().isEmpty()) {
                entity.setRecorderModel(val.toString());
                changed = true;
            }
        }
        if (isBlank(entity.getSimCard())) {
            Object val = info.get("simCard");
            if (val != null && !val.toString().isEmpty()) {
                entity.setSimCard(val.toString());
                changed = true;
            }
        }
        if (isBlank(entity.getRecorderDeviceId())) {
            Object val = info.get("recorderDeviceId");
            if (val != null && !val.toString().isEmpty()) {
                entity.setRecorderDeviceId(val.toString());
                changed = true;
            }
        }
        return changed;
    }

    private boolean isBlank(String str) {
        return str == null || str.isEmpty();
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
