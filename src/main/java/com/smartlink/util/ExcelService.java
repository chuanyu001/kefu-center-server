package com.smartlink.util;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.smartlink.entity.SessionEntity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelService {

    private static final Map<String, String> HEADER_TO_FIELD = new LinkedHashMap<>();
    private static final Map<String, String> FIELD_TO_HEADER = new LinkedHashMap<>();

    static {
        HEADER_TO_FIELD.put("VIN", "vin");
        HEADER_TO_FIELD.put("SIM卡号", "simCard");
        HEADER_TO_FIELD.put("行车记录仪设备ID", "recorderDeviceId");
        HEADER_TO_FIELD.put("受理人", "agentName");
        HEADER_TO_FIELD.put("创建时间", "sessionTime");
        HEADER_TO_FIELD.put("状态", "qiyuTicketStatus");
        HEADER_TO_FIELD.put("工单号", "qiyuTicketCategory");

        FIELD_TO_HEADER.put("vin", "VIN");
        FIELD_TO_HEADER.put("simCard", "SIM卡号");
        FIELD_TO_HEADER.put("recorderDeviceId", "行车记录仪设备ID");
        FIELD_TO_HEADER.put("agentName", "受理人");
        FIELD_TO_HEADER.put("sessionTime", "创建时间");
        FIELD_TO_HEADER.put("qiyuTicketStatus", "状态");
        FIELD_TO_HEADER.put("qiyuTicketCategory", "工单号");
        FIELD_TO_HEADER.put("carModel", "车型");
        FIELD_TO_HEADER.put("fuelType", "燃油类型");
        FIELD_TO_HEADER.put("customerName", "客户姓名");
        FIELD_TO_HEADER.put("customerPhone", "客户电话");
        FIELD_TO_HEADER.put("workRecordType", "工单类型");
        FIELD_TO_HEADER.put("agentName", "受理人");
        FIELD_TO_HEADER.put("iccid", "ICCID");
        FIELD_TO_HEADER.put("terminalNumber", "终端号");
        FIELD_TO_HEADER.put("antennaPosition", "天线位置");
        FIELD_TO_HEADER.put("noPositionReason", "未定位原因");
        FIELD_TO_HEADER.put("noPositionIssue", "未定位问题");
        FIELD_TO_HEADER.put("antennaDamaged", "天线损坏");
        FIELD_TO_HEADER.put("consultationScenario", "咨询场景");
        FIELD_TO_HEADER.put("problemType", "问题类型");
        FIELD_TO_HEADER.put("temporarySolution", "临时解决方案");
        FIELD_TO_HEADER.put("specialNotes", "特殊备注");
        FIELD_TO_HEADER.put("qiyuTicketCategory", "企域工单分类");
    }

    private static final Map<String, Integer> STATUS_MAP = new HashMap<>();

    static {
        STATUS_MAP.put("已完结", 20);
        STATUS_MAP.put("受理中", 10);
        STATUS_MAP.put("处理中", 10);
        STATUS_MAP.put("已挂起", 5);
        STATUS_MAP.put("待申领", 5);
        STATUS_MAP.put("已提交", 1);
        STATUS_MAP.put("未受理", 1);
    }

    public List<SessionEntity> parseExcel(MultipartFile file) {
        List<SessionEntity> result = new ArrayList<>();
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return result;
            }

            // Map column index to field name
            Map<Integer, String> colFieldMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String header = cell.getStringCellValue().trim();
                    String field = HEADER_TO_FIELD.get(header);
                    if (field != null) {
                        colFieldMap.put(i, field);
                    }
                }
            }

            // Read data rows
            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }

                SessionEntity entity = new SessionEntity();
                entity.setWorkRecordType("recorder_register");

                for (Map.Entry<Integer, String> entry : colFieldMap.entrySet()) {
                    int colIdx = entry.getKey();
                    String fieldName = entry.getValue();
                    Cell cell = row.getCell(colIdx);
                    if (cell == null) {
                        continue;
                    }

                    String value = getCellValueAsString(cell);

                    if ("qiyuTicketStatus".equals(fieldName)) {
                        Integer status = STATUS_MAP.get(value.trim());
                        if (status != null) {
                            entity.setQiyuTicketStatus(status);
                        }
                    } else {
                        try {
                            Field field = SessionEntity.class.getDeclaredField(fieldName);
                            field.setAccessible(true);
                            field.set(entity, value);
                        } catch (Exception ignored) {
                        }
                    }
                }

                result.add(entity);
            }
        } catch (Exception e) {
            throw new RuntimeException("Excel解析失败: " + e.getMessage(), e);
        }
        return result;
    }

    public byte[] exportExcel(List<SessionEntity> list, List<String> columns) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sessions");

            // Header row
            Row headerRow = sheet.createRow(0);
            List<String> selectedHeaders = new ArrayList<>();
            for (int i = 0; i < columns.size(); i++) {
                String col = columns.get(i);
                String header = FIELD_TO_HEADER.getOrDefault(col, col);
                selectedHeaders.add(header);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(header);
            }

            // Data rows
            for (int rowIdx = 0; rowIdx < list.size(); rowIdx++) {
                Row row = sheet.createRow(rowIdx + 1);
                SessionEntity entity = list.get(rowIdx);

                for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
                    String fieldName = columns.get(colIdx);
                    Cell cell = row.createCell(colIdx);

                    try {
                        Field field = SessionEntity.class.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        Object value = field.get(entity);
                        if (value != null) {
                            if ("qiyuTicketStatus".equals(fieldName) && value instanceof Integer) {
                                cell.setCellValue(statusToString((Integer) value));
                            } else {
                                cell.setCellValue(String.valueOf(value));
                            }
                        }
                    } catch (Exception e) {
                        cell.setCellValue("");
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel导出失败: " + e.getMessage(), e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    private String statusToString(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 20: return "已完结";
            case 10: return "受理中";
            case 5: return "已挂起";
            case 1: return "已提交";
            default: return String.valueOf(status);
        }
    }
}
