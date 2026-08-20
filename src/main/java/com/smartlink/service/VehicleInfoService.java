package com.smartlink.service;

import com.smartlink.util.OutServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * VIN 车辆信息查询层。
 *
 * 当前分工：
 *  - ID号(recorderId)、SIM号(autoTerminal)：调用运营平台 batchVehicleInfo / batchVehicleOperateInfo（真实）。
 *  - 车型(carModel)、燃料类型(fuelType)、厂家(manufacturer)、记录仪型号(recorderModel)：
 *    预留运营平台 API（带 key）的位置，暂未提供，先用模拟数据占位。
 *    待拿到 API 地址与 key 后，替换 {@link #lookupVehicleDetail(String)} 的实现即可，
 *    无需改动调用方。
 */
@Service
public class VehicleInfoService {

    private static final Logger log = LoggerFactory.getLogger(VehicleInfoService.class);

    /** 占位模拟数据（后续接入运营平台 API 后删除） */
    private static final String[] MOCK_MODELS = {
            "解放J7 6x4牵引车", "解放JH6 6x4牵引车", "解放鹰途 4x2牵引车", "解放J6P 8x4自卸车"
    };
    private static final String[] MOCK_FUELS = {
            "柴油", "汽油", "纯电动", "混合动力"
    };
    private static final String[] MOCK_MANUFACTURERS = {
            "一汽解放", "一汽解放青岛", "一汽解放长春", "一汽解放成都"
    };
    private static final String[] MOCK_RECORDER_MODELS = {
            "部标机", "单北斗", "双北斗", "智能记录仪"
    };

    private final OutServiceUtil outServiceUtil;

    public VehicleInfoService(OutServiceUtil outServiceUtil) {
        this.outServiceUtil = outServiceUtil;
    }

    @Value("${vehicle-info.mode:api}")
    private String mode;

    /** 预留：车型/燃料/厂家/记录仪型号 的运营平台 API 地址与 key，拿到后在此接入。 */
    @Value("${vehicle-info.detail-url:}")
    private String detailUrl;

    @Value("${vehicle-info.detail-key:}")
    private String detailKey;

    public LookupResult lookup(String rawVin) {
        String vin = normalizeVin(rawVin);
        if (vin.isEmpty()) {
            return LookupResult.empty(vin, getMode());
        }
        if (isMockMode()) {
            Map<String, Object> detail = lookupVehicleDetail(vin);
            return new LookupResult(vin,
                    str(detail, "carModel"), str(detail, "fuelType"),
                    str(detail, "manufacturer"), str(detail, "recorderModel"),
                    "", "", "mock", true);
        }
        return lookupFromApi(vin);
    }

    public String getMode() {
        return isMockMode() ? "mock" : "api";
    }

    public boolean isMockMode() {
        return "mock".equalsIgnoreCase(mode == null ? "" : mode.trim());
    }

    /**
     * 仅查询 车型/燃料/厂家/记录仪型号（预留运营平台 API，现为模拟数据）。
     * 不发起运营平台 ID号/SIM号 外呼，适合批量回填已有记录。
     */
    public LookupResult lookupDetail(String rawVin) {
        String vin = normalizeVin(rawVin);
        if (vin.isEmpty()) {
            return LookupResult.empty(vin, getMode());
        }
        Map<String, Object> detail = lookupVehicleDetail(vin);
        return new LookupResult(vin,
                str(detail, "carModel"), str(detail, "fuelType"),
                str(detail, "manufacturer"), str(detail, "recorderModel"),
                "", "", getMode(), true);
    }

    /**
     * 仅查询 ID号(recorderId)/SIM号(autoTerminal)（运营平台真实接口）。
     * mock 模式下无真实数据，返回空。
     */
    public LookupResult lookupRecorder(String rawVin) {
        String vin = normalizeVin(rawVin);
        if (vin.isEmpty() || isMockMode()) {
            return LookupResult.empty(vin, getMode());
        }
        try {
            Map<String, Object> info = firstByVin(
                    outServiceUtil.batchVehicleInfo(Collections.singletonList(vin)), vin);
            Map<String, Object> operate = firstByVin(
                    outServiceUtil.batchVehicleOperateInfo(Collections.singletonList(vin)), vin);
            Map<String, Object> merged = new LinkedHashMap<>();
            merged.putAll(info);
            merged.putAll(operate);
            String recorderDeviceId = firstString(merged, "recorderId", "recorderDeviceId", "记录仪ID");
            String simCard = firstString(merged, "autoTerminal", "simCard", "终端号");
            boolean matched = !recorderDeviceId.isEmpty() || !simCard.isEmpty();
            return new LookupResult(vin, "", "", "", "",
                    recorderDeviceId, simCard, "api", matched);
        } catch (Exception e) {
            log.error("Vehicle recorder lookup failed for VIN {}", vin, e);
            return LookupResult.empty(vin, "api");
        }
    }

    private LookupResult lookupFromApi(String vin) {
        try {
            // 记录仪 ID号/SIM号：现有运营平台接口（真实）
            Map<String, Object> info = firstByVin(
                    outServiceUtil.batchVehicleInfo(Collections.singletonList(vin)), vin);
            Map<String, Object> operate = firstByVin(
                    outServiceUtil.batchVehicleOperateInfo(Collections.singletonList(vin)), vin);
            Map<String, Object> merged = new LinkedHashMap<>();
            merged.putAll(info);
            merged.putAll(operate);
            String recorderDeviceId = firstString(merged, "recorderId", "recorderDeviceId", "记录仪ID");
            String simCard = firstString(merged, "autoTerminal", "simCard", "终端号");

            // 车型/燃料/厂家/记录仪型号：预留运营平台 API（带 key），暂用模拟数据
            Map<String, Object> detail = lookupVehicleDetail(vin);
            String carModel = str(detail, "carModel");
            String fuelType = str(detail, "fuelType");
            String manufacturer = str(detail, "manufacturer");
            String recorderModel = str(detail, "recorderModel");

            boolean matched = !recorderDeviceId.isEmpty() || !simCard.isEmpty()
                    || !carModel.isEmpty() || !fuelType.isEmpty();
            return new LookupResult(vin, carModel, fuelType, manufacturer, recorderModel,
                    recorderDeviceId, simCard, "api", matched);
        } catch (Exception e) {
            log.error("Vehicle info API lookup failed for VIN {}", vin, e);
            return LookupResult.empty(vin, "api");
        }
    }

    /**
     * 预留位置：车型 / 燃料类型 / 厂家 / 记录仪型号 后续由运营平台 API（带 key）按 VIN 查询。
     * 当前运营平台未提供该接口，先用模拟数据占位；拿到 {@code detail-url} 与 {@code detail-key} 后，
     * 在本方法内改成真实 HTTP 查询即可，调用方无需改动。
     */
    private Map<String, Object> lookupVehicleDetail(String vin) {
        if (isNotBlank(detailUrl)) {
            // TODO 接入运营平台 API：POST detail-url，带 detail-key，按 VIN 返回
            //       carModel / fuelType / manufacturer / recorderModel 后解析填入。
            log.warn("vehicle-info.detail-url 已配置但尚未接入查询实现，暂用模拟数据：{}", detailUrl);
        }
        int index = Math.floorMod(vin.hashCode(), MOCK_MODELS.length);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("carModel", MOCK_MODELS[index]);
        detail.put("fuelType", MOCK_FUELS[index]);
        detail.put("manufacturer", MOCK_MANUFACTURERS[index]);
        detail.put("recorderModel", MOCK_RECORDER_MODELS[index]);
        return detail;
    }

    private Map<String, Object> firstByVin(List<Map<String, Object>> list, String vin) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }
        for (Map<String, Object> row : list) {
            String rowVin = firstString(row, "vin", "VIN");
            if (vin.equals(normalizeVin(rowVin))) {
                return row;
            }
        }
        return list.get(0);
    }

    private String str(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private String firstString(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        }
        return "";
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeVin(String vin) {
        return vin == null ? "" : vin.trim().toUpperCase(Locale.ROOT);
    }

    public static class LookupResult {
        private final String vin;
        private final String carModel;
        private final String fuelType;
        private final String manufacturer;
        private final String recorderModel;
        private final String recorderDeviceId;
        private final String simCard;
        private final String source;
        private final boolean matched;

        public LookupResult(String vin, String carModel, String fuelType,
                            String manufacturer, String recorderModel,
                            String recorderDeviceId, String simCard,
                            String source, boolean matched) {
            this.vin = vin;
            this.carModel = carModel == null ? "" : carModel;
            this.fuelType = fuelType == null ? "" : fuelType;
            this.manufacturer = manufacturer == null ? "" : manufacturer;
            this.recorderModel = recorderModel == null ? "" : recorderModel;
            this.recorderDeviceId = recorderDeviceId == null ? "" : recorderDeviceId;
            this.simCard = simCard == null ? "" : simCard;
            this.source = source;
            this.matched = matched;
        }

        public static LookupResult empty(String vin, String source) {
            return new LookupResult(vin, "", "", "", "", "", "", source, false);
        }

        public String getVin() { return vin; }
        public String getCarModel() { return carModel; }
        public String getFuelType() { return fuelType; }
        public String getManufacturer() { return manufacturer; }
        public String getRecorderModel() { return recorderModel; }
        public String getRecorderDeviceId() { return recorderDeviceId; }
        public String getSimCard() { return simCard; }
        public String getSource() { return source; }
        public boolean isMatched() { return matched; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("vin", vin);
            map.put("carModel", carModel);
            map.put("fuelType", fuelType);
            map.put("manufacturer", manufacturer);
            map.put("recorderModel", recorderModel);
            map.put("recorderDeviceId", recorderDeviceId);
            map.put("simCard", simCard);
            map.put("source", source);
            map.put("matched", matched);
            return map;
        }
    }
}
