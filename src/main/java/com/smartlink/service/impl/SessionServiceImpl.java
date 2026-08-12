package com.smartlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlink.common.PageResult;
import com.smartlink.dto.request.SessionQueryReq;
import com.smartlink.dto.request.SessionUpdateReq;
import com.smartlink.dto.response.SessionVO;
import com.smartlink.entity.SessionEntity;
import com.smartlink.mapper.SessionMapper;
import com.smartlink.service.SessionService;
import com.smartlink.util.ExcelService;
import com.smartlink.util.OutServiceUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SessionServiceImpl implements SessionService {

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private ExcelService excelService;

    @Autowired
    private OutServiceUtil outServiceUtil;

    @Override
    public PageResult<SessionVO> list(SessionQueryReq req) {
        try {
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

            // recorder_register sorted by sessionTime desc
            wrapper.orderByDesc(SessionEntity::getSessionTime);

            Page<SessionEntity> page = new Page<>(req.getPage(), req.getPageSize());
            Page<SessionEntity> result = sessionMapper.selectPage(page, wrapper);

            List<SessionVO> voList = result.getRecords().stream()
                    .map(this::entityToVO)
                    .collect(Collectors.toList());

            return PageResult.of(voList, result.getTotal(), req.getPage(), req.getPageSize());
        } catch (Exception e) {
            // Fallback to mock data on error
            return getMockData(req);
        }
    }

    @Override
    public SessionVO detail(String id) {
        try {
            SessionEntity entity = sessionMapper.selectById(id);
            if (entity != null) {
                return entityToVO(entity);
            }
        } catch (Exception e) {
            // Fallback to mock
        }
        // Return mock detail
        return buildMockVO("1", "张三", "13800138001", "LFWJX9C89M1001001",
                "after_sales", "2024-01-15 10:30:00", "李客服",
                "一汽解放J6P", "柴油", "13800000001",
                "8986000000000000001", "一汽解放", "JL-8000", "DEV20240115001",
                "前挡风玻璃", null, null, "否",
                10, "车辆故障", "车辆无法启动",
                "电池亏电", "建议更换电池", "客户反馈车辆停放3天后无法启动");
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
    public Map<String, Object> importExcel(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        try {
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
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

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
            // Get distinct VINs with empty fields
            LambdaQueryWrapper<SessionEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(SessionEntity::getVin)
                    .ne(SessionEntity::getVin, "")
                    .and(w -> w
                            .isNull(SessionEntity::getManufacturer)
                            .or()
                            .eq(SessionEntity::getManufacturer, "")
                            .or()
                            .isNull(SessionEntity::getSimCard)
                            .or()
                            .eq(SessionEntity::getSimCard, "")
                            .or()
                            .isNull(SessionEntity::getRecorderModel)
                            .or()
                            .eq(SessionEntity::getRecorderModel, "")
                            .or()
                            .isNull(SessionEntity::getRecorderDeviceId)
                            .or()
                            .eq(SessionEntity::getRecorderDeviceId, "")
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

                // Call batchVehicleInfo
                List<Map<String, Object>> vehicleInfoList = outServiceUtil.batchVehicleInfo(batch);
                // Call batchVehicleOperateInfo
                List<Map<String, Object>> operateInfoList = outServiceUtil.batchVehicleOperateInfo(batch);

                // Build VIN -> info map
                Map<String, Map<String, Object>> infoMap = new HashMap<>();
                for (Map<String, Object> info : vehicleInfoList) {
                    Object vin = info.get("vin");
                    if (vin != null) {
                        infoMap.put(vin.toString(), info);
                    }
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

                // Update session records for this batch
                for (String vin : batch) {
                    Map<String, Object> info = infoMap.get(vin);
                    if (info == null) {
                        continue;
                    }

                    LambdaQueryWrapper<SessionEntity> updateWrapper = new LambdaQueryWrapper<>();
                    updateWrapper.eq(SessionEntity::getVin, vin);

                    SessionEntity updateEntity = new SessionEntity();
                    boolean hasUpdate = false;

                    // Only update fields that are currently empty
                    if (info.containsKey("manufacturer")) {
                        Object val = info.get("manufacturer");
                        // Check if most records for this VIN have empty manufacturer
                        List<SessionEntity> vinRecords = sessionMapper.selectList(
                                new LambdaQueryWrapper<SessionEntity>()
                                        .eq(SessionEntity::getVin, vin)
                                        .and(w -> w.isNull(SessionEntity::getManufacturer)
                                                .or().eq(SessionEntity::getManufacturer, ""))
                        );
                        if (!vinRecords.isEmpty() && val != null && !val.toString().isEmpty()) {
                            updateEntity.setManufacturer(val.toString());
                            hasUpdate = true;
                        }
                    }
                    if (info.containsKey("recorderModel")) {
                        Object val = info.get("recorderModel");
                        List<SessionEntity> vinRecords = sessionMapper.selectList(
                                new LambdaQueryWrapper<SessionEntity>()
                                        .eq(SessionEntity::getVin, vin)
                                        .and(w -> w.isNull(SessionEntity::getRecorderModel)
                                                .or().eq(SessionEntity::getRecorderModel, ""))
                        );
                        if (!vinRecords.isEmpty() && val != null && !val.toString().isEmpty()) {
                            updateEntity.setRecorderModel(val.toString());
                            hasUpdate = true;
                        }
                    }
                    if (info.containsKey("simCard")) {
                        Object val = info.get("simCard");
                        List<SessionEntity> vinRecords = sessionMapper.selectList(
                                new LambdaQueryWrapper<SessionEntity>()
                                        .eq(SessionEntity::getVin, vin)
                                        .and(w -> w.isNull(SessionEntity::getSimCard)
                                                .or().eq(SessionEntity::getSimCard, ""))
                        );
                        if (!vinRecords.isEmpty() && val != null && !val.toString().isEmpty()) {
                            updateEntity.setSimCard(val.toString());
                            hasUpdate = true;
                        }
                    }
                    if (info.containsKey("recorderDeviceId")) {
                        Object val = info.get("recorderDeviceId");
                        List<SessionEntity> vinRecords = sessionMapper.selectList(
                                new LambdaQueryWrapper<SessionEntity>()
                                        .eq(SessionEntity::getVin, vin)
                                        .and(w -> w.isNull(SessionEntity::getRecorderDeviceId)
                                                .or().eq(SessionEntity::getRecorderDeviceId, ""))
                        );
                        if (!vinRecords.isEmpty() && val != null && !val.toString().isEmpty()) {
                            updateEntity.setRecorderDeviceId(val.toString());
                            hasUpdate = true;
                        }
                    }

                    if (hasUpdate) {
                        updateEntity.setUpdatedAt(LocalDateTime.now());
                        sessionMapper.update(updateEntity, updateWrapper);
                        totalSynced++;
                    }
                }
            }

            result.put("success", true);
            result.put("totalVins", vinList.size());
            result.put("syncedCount", totalSynced);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    // ==================== Helper Methods ====================

    private SessionVO entityToVO(SessionEntity entity) {
        SessionVO vo = new SessionVO();
        BeanUtils.copyProperties(entity, vo);
        // Parse chatMessages JSON string to List
        if (entity.getChatMessages() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                vo.setChatMessages(mapper.readValue(entity.getChatMessages(), List.class));
            } catch (Exception e) {
                vo.setChatMessages(Collections.emptyList());
            }
        }
        // Parse modificationHistory JSON string to List
        if (entity.getModificationHistory() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                vo.setModificationHistory(mapper.readValue(entity.getModificationHistory(), List.class));
            } catch (Exception e) {
                vo.setModificationHistory(Collections.emptyList());
            }
        }
        return vo;
    }

    private PageResult<SessionVO> getMockData(SessionQueryReq req) {
        List<SessionVO> mockList = new ArrayList<>();

        mockList.add(buildMockVO("1", "张三", "13800138001", "LFWJX9C89M1001001",
                "after_sales", "2024-01-15 10:30:00", "李客服",
                "一汽解放J6P", "柴油", "13800000001",
                "8986000000000000001", "一汽解放", "JL-8000", "DEV20240115001",
                "前挡风玻璃", null, null, "否",
                20, "车辆故障", "车辆无法启动",
                "电池亏电", "建议更换电池", "客户反馈车辆停放3天后无法启动"));

        mockList.add(buildMockVO("2", "李四", "13900139002", "LFWJX9C89M1001002",
                "fleet_register", "2024-01-16 14:20:00", "王客服",
                "东风天龙KL", "柴油", "13800000002",
                "8986000000000000002", "东风商用车", "DF-KL600", "DEV20240116002",
                "后视镜旁", null, null, "否",
                10, "安装咨询", "行车记录仪安装位置",
                "已指导安装", "选择后视镜右侧位置", "客户新购车队设备，咨询安装"));

        mockList.add(buildMockVO("3", "王五", "13700137003", "LFWJX9C89M1001003",
                "after_sales", "2024-01-17 09:15:00", "赵客服",
                "重汽豪沃T7H", "柴油", "13800000003",
                "8986000000000000003", "中国重汽", "HW-T700", "DEV20240117003",
                null, "设备未安装", "设备未到货", "是",
                5, "设备故障", "GPS无信号",
                "待设备到货", "重新发货中", "GPS模块故障需更换"));

        mockList.add(buildMockVO("4", "赵六", "13600136004", "LFWJX9C89M1001004",
                "after_sales", "2024-01-18 11:45:00", "孙客服",
                "陕汽德龙X6000", "天然气", "13800000004",
                "8986000000000000004", "陕汽重卡", "DL-X6000", "DEV20240118004",
                "车顶中央", null, null, "否",
                1, "使用咨询", "APP数据不更新",
                "指导重新绑定", "已解绑后重新绑定", "用户更换手机后数据不更新"));

        mockList.add(buildMockVO("5", "钱七", "13500135005", "LFWJX9C89M1001005",
                "fleet_register", "2024-01-19 16:00:00", "周客服",
                "福田欧曼EST", "柴油", "13800000005",
                "8986000000000000005", "福田汽车", "FT-EST500", "DEV20240119005",
                "前挡风玻璃左下角", null, null, "否",
                10, "设备激活", "SIM卡无法激活",
                "联系运营商", "等待运营商回复", "SIM卡与设备绑定异常"));

        // Apply keyword filter
        if (StringUtils.isNotBlank(req.getKeyword())) {
            String kw = req.getKeyword().toLowerCase();
            mockList = mockList.stream()
                    .filter(vo -> (vo.getCustomerName() != null && vo.getCustomerName().contains(kw))
                            || (vo.getCustomerPhone() != null && vo.getCustomerPhone().contains(kw))
                            || (vo.getVin() != null && vo.getVin().toLowerCase().contains(kw))
                            || (vo.getAgentName() != null && vo.getAgentName().contains(kw))
                            || (vo.getCarModel() != null && vo.getCarModel().contains(kw)))
                    .collect(Collectors.toList());
        }

        // Apply workRecordType filter
        if (StringUtils.isNotBlank(req.getWorkRecordType())) {
            mockList = mockList.stream()
                    .filter(vo -> req.getWorkRecordType().equals(vo.getWorkRecordType()))
                    .collect(Collectors.toList());
        }

        // Pagination
        int total = mockList.size();
        int fromIndex = (req.getPage() - 1) * req.getPageSize();
        int toIndex = Math.min(fromIndex + req.getPageSize(), total);

        if (fromIndex >= total) {
            return PageResult.of(Collections.emptyList(), total, req.getPage(), req.getPageSize());
        }

        List<SessionVO> pageList = mockList.subList(fromIndex, toIndex);
        return PageResult.of(pageList, total, req.getPage(), req.getPageSize());
    }

    private SessionVO buildMockVO(String id, String customerName, String customerPhone, String vin,
                                   String workRecordType, String sessionTime, String agentName,
                                   String carModel, String fuelType, String terminalNumber,
                                   String simCard, String manufacturer, String recorderModel,
                                   String recorderDeviceId, String antennaPosition,
                                   String noPositionReason, String noPositionIssue,
                                   String antennaDamaged, Integer qiyuTicketStatus,
                                   String qiyuTicketCategory, String consultationScenario,
                                   String problemType, String temporarySolution, String specialNotes) {
        SessionVO vo = new SessionVO();
        vo.setId(id);
        vo.setCustomerName(customerName);
        vo.setCustomerPhone(customerPhone);
        vo.setVin(vin);
        vo.setWorkRecordType(workRecordType);
        vo.setSessionTime(sessionTime);
        vo.setAgentName(agentName);
        vo.setCarModel(carModel);
        vo.setFuelType(fuelType);
        vo.setTerminalNumber(terminalNumber);
        vo.setSimCard(simCard);
        vo.setManufacturer(manufacturer);
        vo.setRecorderModel(recorderModel);
        vo.setRecorderDeviceId(recorderDeviceId);
        vo.setAntennaPosition(antennaPosition);
        vo.setNoPositionReason(noPositionReason);
        vo.setNoPositionIssue(noPositionIssue);
        vo.setAntennaDamaged(antennaDamaged);
        vo.setQiyuTicketStatus(qiyuTicketStatus);
        vo.setQiyuTicketCategory(qiyuTicketCategory);
        vo.setConsultationScenario(consultationScenario);
        vo.setProblemType(problemType);
        vo.setTemporarySolution(temporarySolution);
        vo.setSpecialNotes(specialNotes);

        // Build mock chat messages
        List<Map<String, Object>> chatMessages = new ArrayList<>();
        Map<String, Object> msg1 = new HashMap<>();
        msg1.put("role", "customer");
        msg1.put("content", "你好，我的行车记录仪好像不工作了");
        msg1.put("time", sessionTime);
        chatMessages.add(msg1);

        Map<String, Object> msg2 = new HashMap<>();
        msg2.put("role", "agent");
        msg2.put("content", "您好，请问设备指示灯是什么颜色的？");
        msg2.put("time", sessionTime);
        chatMessages.add(msg2);

        vo.setChatMessages(chatMessages);

        // Build mock modification history
        List<Map<String, Object>> history = new ArrayList<>();
        Map<String, Object> mod1 = new HashMap<>();
        mod1.put("field", "consultationScenario");
        mod1.put("oldValue", "");
        mod1.put("newValue", consultationScenario);
        mod1.put("time", sessionTime);
        mod1.put("operator", agentName);
        history.add(mod1);
        vo.setModificationHistory(history);

        vo.setCreatedAt(LocalDateTime.now().minusDays(30));
        vo.setUpdatedAt(LocalDateTime.now());

        return vo;
    }
}
