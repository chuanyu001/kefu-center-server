package com.smartlink.util;

import com.smartlink.entity.SessionEntity;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportFileServiceTest {

    private final ImportFileService service = new ImportFileService();

    @Test
    void parsesExcelHeadersAndKeepsMissingFieldEmpty() throws Exception {
        byte[] bytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Row header = workbook.createSheet("工单").createRow(0);
            header.createCell(0).setCellValue("时间");
            header.createCell(1).setCellValue("VIN码");
            header.createCell(2).setCellValue("ID号");
            Row data = header.getSheet().createRow(1);
            data.createCell(0).setCellValue("2026-08-17 10:00:00");
            data.createCell(1).setCellValue("TESTVIN00000000001");
            data.createCell(2).setCellValue("DEVICE-001");
            workbook.write(output);
            bytes = output.toByteArray();
        }

        ImportFileService.ParseResult result = service.parse(new MockMultipartFile(
                "file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes));

        assertEquals(1, result.getRecords().size());
        SessionEntity record = result.getRecords().get(0);
        assertEquals("TESTVIN00000000001", record.getVin());
        assertEquals("DEVICE-001", record.getRecorderDeviceId());
        assertNull(record.getSimCard());
    }

    @Test
    void parsesDocxTableByExplicitHeaders() throws Exception {
        byte[] bytes;
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFTable table = document.createTable(2, 4);
            table.getRow(0).getCell(0).setText("创建时间");
            table.getRow(0).getCell(1).setText("VIN");
            table.getRow(0).getCell(2).setText("行车记录仪设备ID");
            table.getRow(0).getCell(3).setText("SIM卡号");
            table.getRow(1).getCell(0).setText("2026-08-17 11:00:00");
            table.getRow(1).getCell(1).setText("TESTVIN00000000002");
            table.getRow(1).getCell(2).setText("DEVICE-002");
            table.getRow(1).getCell(3).setText("SIM-002");
            document.write(output);
            bytes = output.toByteArray();
        }

        ImportFileService.ParseResult result = service.parse(new MockMultipartFile(
                "file", "test.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bytes));

        assertEquals(1, result.getRecords().size());
        assertEquals("SIM-002", result.getRecords().get(0).getSimCard());
    }

    @Test
    void parsesLabelledPdfWithoutInventingTime() throws Exception {
        byte[] bytes;
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(50, 700);
                content.showText("VIN: TESTVIN00000000003 ID: DEVICE-003 SIM: SIM-003");
                content.endText();
            }
            document.save(output);
            bytes = output.toByteArray();
        }

        ImportFileService.ParseResult result = service.parse(new MockMultipartFile(
                "file", "test.pdf", "application/pdf", bytes));

        assertEquals(1, result.getRecords().size());
        SessionEntity record = result.getRecords().get(0);
        assertEquals("TESTVIN00000000003", record.getVin());
        assertEquals("DEVICE-003", record.getRecorderDeviceId());
        assertEquals("SIM-003", record.getSimCard());
        assertNull(record.getSessionTime());
    }

    @Test
    void unrelatedDocxProducesNoRecords() throws Exception {
        byte[] bytes;
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("这是一份与工单字段无关的说明文档。");
            document.write(output);
            bytes = output.toByteArray();
        }

        ImportFileService.ParseResult result = service.parse(new MockMultipartFile(
                "file", "unrelated.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bytes));

        assertTrue(result.getRecords().isEmpty());
    }
}
