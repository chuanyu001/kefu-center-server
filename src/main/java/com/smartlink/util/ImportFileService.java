package com.smartlink.util;

import com.smartlink.entity.SessionEntity;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 严格文件识别器：只根据明确的表头或“字段名: 值”标签提取数据。
 * 没有识别到的字段保持为空，不使用位置猜测、默认值或 AI 补写。
 */
@Service
public class ImportFileService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Map<String, String> HEADER_TO_FIELD = new LinkedHashMap<>();
    private static final Set<String> EMPTY_MARKERS = new LinkedHashSet<>(
            Arrays.asList("", "-", "--", "/", "无", "未提供", "未填写", "NULL", "N/A"));

    private static final String LABEL_EXPRESSION =
            "创建时间|工单时间|会话时间|受理时间|时间|车辆VIN|VIN码|车架号|VIN|" +
            "行车记录仪设备ID|记录仪设备ID|设备ID|终端ID|ID号|ID|SIM卡号|SIM号|SIM";
    private static final Pattern LABELLED_VALUE = Pattern.compile(
            "(?i)(" + LABEL_EXPRESSION + ")\\s*[:：=]\\s*(.*?)" +
                    "(?=\\s+(?:" + LABEL_EXPRESSION + ")\\s*[:：=]|$)");

    /** 17 位车架号（VIN 不含 I/O/Q，含数字 0/1）。用于从「客户-外部」等富文本里抽取 VIN。 */
    private static final Pattern VIN_PATTERN =
            Pattern.compile("(?<![A-Z0-9])([A-HJ-NPR-Z0-9]{17})(?![A-Z0-9])");

    static {
        register("sessionTime", "时间", "创建时间", "工单时间", "会话时间", "受理时间");
        // 七鱼工单导出真实表头：VIN 可能在「客户-VIN」，也可能内嵌在「客户-外部」文本里
        register("vin", "VIN", "VIN码", "车辆VIN", "车架号", "客户-VIN", "客户VIN", "客户-外部");
        register("recorderDeviceId", "ID", "ID号", "设备ID", "终端ID", "记录仪ID",
                "记录仪设备ID", "行车记录仪设备ID");
        register("simCard", "SIM", "SIM号", "SIM卡号", "卡号");
        register("carModel", "车型", "车型号", "客户-车型", "客户-车型号");
        register("customerPhone", "手机号码", "手机号", "客户-手机号", "联系电话");
        register("customerName", "客户姓名", "姓名", "客户名称");
        register("agentName", "受理人", "客服", "坐席");
    }

    public ParseResult parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择需要导入的文件");
        }

        String extension = extensionOf(file.getOriginalFilename());
        List<SessionEntity> records;
        try {
            switch (extension) {
                case "xlsx":
                case "xls":
                    records = parseWorkbook(file);
                    break;
                case "docx":
                    records = parseDocx(file);
                    break;
                case "doc":
                    records = parseDoc(file);
                    break;
                case "pdf":
                    records = parsePdf(file);
                    break;
                default:
                    throw new IllegalArgumentException("仅支持 Excel、Word 和 PDF 文件（xlsx、xls、docx、doc、pdf）");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("文件解析失败，请确认文件未损坏且不是扫描图片：" + e.getMessage(), e);
        }

        List<SessionEntity> uniqueRecords = deduplicate(records);
        Map<String, Integer> recognizedFields = new LinkedHashMap<>();
        recognizedFields.put("sessionTime", 0);
        recognizedFields.put("vin", 0);
        recognizedFields.put("recorderDeviceId", 0);
        recognizedFields.put("simCard", 0);
        for (SessionEntity record : uniqueRecords) {
            incrementIfPresent(recognizedFields, "sessionTime", record.getSessionTime());
            incrementIfPresent(recognizedFields, "vin", record.getVin());
            incrementIfPresent(recognizedFields, "recorderDeviceId", record.getRecorderDeviceId());
            incrementIfPresent(recognizedFields, "simCard", record.getSimCard());
        }
        return new ParseResult(extension, uniqueRecords, recognizedFields);
    }

    private List<SessionEntity> parseWorkbook(MultipartFile file) throws Exception {
        List<SessionEntity> result = new ArrayList<>();
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                int headerIndex = -1;
                Map<Integer, String> columnMap = Collections.emptyMap();
                int scanEnd = Math.min(sheet.getLastRowNum(), 20);
                for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= scanEnd; rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    Map<Integer, String> candidate = mapExcelHeader(row);
                    if (candidate.size() > columnMap.size()) {
                        headerIndex = rowIndex;
                        columnMap = candidate;
                    }
                }
                if (headerIndex < 0 || columnMap.isEmpty()) {
                    continue;
                }
                for (int rowIndex = headerIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }
                    SessionEntity entity = newRecorderEntity();
                    for (Map.Entry<Integer, String> entry : columnMap.entrySet()) {
                        setField(entity, entry.getValue(), excelCellValue(row.getCell(entry.getKey())));
                    }
                    addIfRecognized(result, entity);
                }
            }
        }
        return result;
    }

    private List<SessionEntity> parseDocx(MultipartFile file) throws Exception {
        List<SessionEntity> result = new ArrayList<>();
        StringBuilder paragraphText = new StringBuilder();
        try (InputStream input = file.getInputStream(); XWPFDocument document = new XWPFDocument(input)) {
            for (XWPFTable table : document.getTables()) {
                parseDocxTable(table, result);
            }
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                paragraphText.append(paragraph.getText()).append('\n');
            }
        }
        result.addAll(parseStructuredText(paragraphText.toString()));
        return result;
    }

    private void parseDocxTable(XWPFTable table, List<SessionEntity> result) {
        List<XWPFTableRow> rows = table.getRows();
        int headerIndex = -1;
        Map<Integer, String> columnMap = Collections.emptyMap();
        for (int rowIndex = 0; rowIndex < Math.min(rows.size(), 20); rowIndex++) {
            Map<Integer, String> candidate = new LinkedHashMap<>();
            List<XWPFTableCell> cells = rows.get(rowIndex).getTableCells();
            for (int columnIndex = 0; columnIndex < cells.size(); columnIndex++) {
                String field = fieldForHeader(cells.get(columnIndex).getText());
                if (field != null) {
                    candidate.put(columnIndex, field);
                }
            }
            if (candidate.size() > columnMap.size()) {
                headerIndex = rowIndex;
                columnMap = candidate;
            }
        }
        if (headerIndex < 0 || columnMap.isEmpty()) {
            return;
        }
        for (int rowIndex = headerIndex + 1; rowIndex < rows.size(); rowIndex++) {
            List<XWPFTableCell> cells = rows.get(rowIndex).getTableCells();
            SessionEntity entity = newRecorderEntity();
            for (Map.Entry<Integer, String> entry : columnMap.entrySet()) {
                if (entry.getKey() < cells.size()) {
                    setField(entity, entry.getValue(), cells.get(entry.getKey()).getText());
                }
            }
            addIfRecognized(result, entity);
        }
    }

    private List<SessionEntity> parseDoc(MultipartFile file) throws Exception {
        try (InputStream input = file.getInputStream();
             HWPFDocument document = new HWPFDocument(input);
             WordExtractor extractor = new WordExtractor(document)) {
            return parseStructuredText(extractor.getText());
        }
    }

    private List<SessionEntity> parsePdf(MultipartFile file) throws Exception {
        try (InputStream input = file.getInputStream(); PDDocument document = PDDocument.load(input)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("暂不支持加密 PDF");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return parseStructuredText(stripper.getText(document));
        }
    }

    private List<SessionEntity> parseStructuredText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<SessionEntity> result = new ArrayList<>();
        result.addAll(parseDelimitedTables(text));
        result.addAll(parseLabelledText(text));
        return deduplicate(result);
    }

    private List<SessionEntity> parseDelimitedTables(String text) {
        List<SessionEntity> result = new ArrayList<>();
        String[] lines = text.replace("\r", "").split("\n");
        Map<Integer, String> columnMap = Collections.emptyMap();
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                columnMap = Collections.emptyMap();
                continue;
            }
            String[] cells = line.split("(?:\\t+|\\s{2,})");
            Map<Integer, String> candidate = mapTextHeader(cells);
            if (!candidate.isEmpty()) {
                columnMap = candidate;
                continue;
            }
            if (columnMap.isEmpty()) {
                continue;
            }
            SessionEntity entity = newRecorderEntity();
            for (Map.Entry<Integer, String> entry : columnMap.entrySet()) {
                if (entry.getKey() < cells.length) {
                    setField(entity, entry.getValue(), cells[entry.getKey()]);
                }
            }
            addIfRecognized(result, entity);
        }
        return result;
    }

    private List<SessionEntity> parseLabelledText(String text) {
        List<SessionEntity> result = new ArrayList<>();
        SessionEntity current = newRecorderEntity();
        String[] lines = text.replace("\r", "").split("\n", -1);
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                if (hasRecognizedField(current)) {
                    result.add(current);
                    current = newRecorderEntity();
                }
                continue;
            }
            Matcher matcher = LABELLED_VALUE.matcher(line.trim());
            while (matcher.find()) {
                String field = fieldForHeader(matcher.group(1));
                String value = cleanValue(matcher.group(2));
                if (field == null || value.isEmpty()) {
                    continue;
                }
                if (hasValue(current, field)) {
                    result.add(current);
                    current = newRecorderEntity();
                }
                setField(current, field, value);
            }
        }
        addIfRecognized(result, current);
        return result;
    }

    private Map<Integer, String> mapExcelHeader(Row row) {
        if (row == null) {
            return Collections.emptyMap();
        }
        Map<Integer, String> result = new LinkedHashMap<>();
        for (int index = 0; index < row.getLastCellNum(); index++) {
            String field = fieldForHeader(excelCellValue(row.getCell(index)));
            if (field != null) {
                result.put(index, field);
            }
        }
        return result;
    }

    private Map<Integer, String> mapTextHeader(String[] cells) {
        Map<Integer, String> result = new LinkedHashMap<>();
        for (int index = 0; index < cells.length; index++) {
            String field = fieldForHeader(cells[index]);
            if (field != null) {
                result.put(index, field);
            }
        }
        return result;
    }

    private String excelCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().format(DATE_TIME_FORMATTER);
        }
        return new org.apache.poi.ss.usermodel.DataFormatter(Locale.CHINA).formatCellValue(cell).trim();
    }

    private void setField(SessionEntity entity, String field, String rawValue) {
        String value = cleanValue(rawValue);
        if (value.isEmpty()) {
            return;
        }
        if ("sessionTime".equals(field)) {
            entity.setSessionTime(value);
        } else if ("vin".equals(field)) {
            entity.setVin(extractVin(value));
        } else if ("recorderDeviceId".equals(field)) {
            entity.setRecorderDeviceId(value);
        } else if ("simCard".equals(field)) {
            entity.setSimCard(value);
        } else if ("carModel".equals(field)) {
            entity.setCarModel(value);
        } else if ("customerPhone".equals(field)) {
            entity.setCustomerPhone(value);
        } else if ("customerName".equals(field)) {
            entity.setCustomerName(value);
        } else if ("agentName".equals(field)) {
            entity.setAgentName(value);
        }
    }

    /** 提取 VIN：整格为纯字母数字（VIN 或占位串）直接返回；富文本则用正则抽取 17 位车架号。 */
    private String extractVin(String value) {
        if (value == null) {
            return "";
        }
        String upper = value.trim().toUpperCase(Locale.ROOT);
        if (upper.matches("[A-Z0-9]+")) {
            return upper;
        }
        Matcher matcher = VIN_PATTERN.matcher(upper);
        return matcher.find() ? matcher.group(1) : "";
    }

    private boolean hasValue(SessionEntity entity, String field) {
        if ("sessionTime".equals(field)) return !isBlank(entity.getSessionTime());
        if ("vin".equals(field)) return !isBlank(entity.getVin());
        if ("recorderDeviceId".equals(field)) return !isBlank(entity.getRecorderDeviceId());
        if ("simCard".equals(field)) return !isBlank(entity.getSimCard());
        return false;
    }

    private boolean hasRecognizedField(SessionEntity entity) {
        return !isBlank(entity.getSessionTime()) || !isBlank(entity.getVin())
                || !isBlank(entity.getRecorderDeviceId()) || !isBlank(entity.getSimCard());
    }

    private void addIfRecognized(List<SessionEntity> result, SessionEntity entity) {
        if (hasRecognizedField(entity)) {
            result.add(entity);
        }
    }

    private SessionEntity newRecorderEntity() {
        SessionEntity entity = new SessionEntity();
        entity.setWorkRecordType("recorder_register");
        return entity;
    }

    private List<SessionEntity> deduplicate(List<SessionEntity> records) {
        Map<String, SessionEntity> unique = new LinkedHashMap<>();
        for (SessionEntity entity : records) {
            if (!hasRecognizedField(entity)) {
                continue;
            }
            String key = safe(entity.getSessionTime()) + "|" + safe(entity.getVin()) + "|"
                    + safe(entity.getRecorderDeviceId()) + "|" + safe(entity.getSimCard());
            unique.putIfAbsent(key, entity);
        }
        return new ArrayList<>(unique.values());
    }

    private static void register(String field, String... headers) {
        for (String header : headers) {
            HEADER_TO_FIELD.put(normalizeHeader(header), field);
        }
    }

    private String fieldForHeader(String header) {
        return HEADER_TO_FIELD.get(normalizeHeader(header));
    }

    private static String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("[\\s:：=_-]", "").toUpperCase(Locale.ROOT);
    }

    private String cleanValue(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        return EMPTY_MARKERS.contains(cleaned.toUpperCase(Locale.ROOT)) ? "" : cleaned;
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void incrementIfPresent(Map<String, Integer> counts, String field, String value) {
        if (!isBlank(value)) {
            counts.put(field, counts.get(field) + 1);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static class ParseResult {
        private final String fileType;
        private final List<SessionEntity> records;
        private final Map<String, Integer> recognizedFields;

        public ParseResult(String fileType, List<SessionEntity> records,
                           Map<String, Integer> recognizedFields) {
            this.fileType = fileType;
            this.records = records;
            this.recognizedFields = recognizedFields;
        }

        public String getFileType() {
            return fileType;
        }

        public List<SessionEntity> getRecords() {
            return records;
        }

        public Map<String, Integer> getRecognizedFields() {
            return recognizedFields;
        }
    }
}
