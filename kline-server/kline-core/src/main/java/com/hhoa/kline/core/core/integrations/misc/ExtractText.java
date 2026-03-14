package com.hhoa.kline.core.core.integrations.misc;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.mozilla.universalchardet.UniversalDetector;

/**
 * 文本提取工具类 支持多种文件格式的文本提取：PDF, DOCX, IPYNB, XLSX 等
 *
 * @author hhoa
 */
@Slf4j
public class ExtractText {

    private static final long MAX_FILE_SIZE = 20 * 1000 * 1024L;

    private static final int MAX_EXCEL_ROWS = 50000;

    private static final Tika tika = new Tika();

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    public static String detectEncoding(byte[] fileBuffer, String fileExtension) {
        if (fileBuffer.length >= 3
                && fileBuffer[0] == (byte) 0xEF
                && fileBuffer[1] == (byte) 0xBB
                && fileBuffer[2] == (byte) 0xBF) {
            return StandardCharsets.UTF_8.name();
        }

        if (fileBuffer.length >= 2
                && fileBuffer[0] == (byte) 0xFF
                && fileBuffer[1] == (byte) 0xFE) {
            return "UTF-16LE";
        }

        if (fileBuffer.length >= 2
                && fileBuffer[0] == (byte) 0xFE
                && fileBuffer[1] == (byte) 0xFF) {
            return "UTF-16BE";
        }

        try {
            UniversalDetector detector = new UniversalDetector(null);
            detector.handleData(fileBuffer, 0, fileBuffer.length);
            detector.dataEnd();

            String detectedEncoding = detector.getDetectedCharset();
            if (detectedEncoding != null && !detectedEncoding.isEmpty()) {
                detector.reset();
                return detectedEncoding;
            }
            detector.reset();
        } catch (Exception e) {
            log.debug("Failed to detect encoding using juniversalchardet", e);
        }

        if (fileExtension != null && isBinaryFile(fileBuffer)) {
            throw new IllegalArgumentException("Cannot read text for file type: " + fileExtension);
        }

        return StandardCharsets.UTF_8.name();
    }

    public static String extractTextFromFile(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                throw new IOException("File not found: " + filePath);
            }
            return callTextExtractionFunctions(filePath);
        } catch (Exception e) {
            log.error("Error extracting text from file: {}", filePath, e);
            throw new RuntimeException("Error extracting text from file: " + e.getMessage(), e);
        }
    }

    public static String callTextExtractionFunctions(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        String extension = getFileExtension(fileName);

        return switch (extension) {
            case "pdf" -> extractTextFromPDF(filePath);
            case "docx" -> extractTextFromDOCX(filePath);
            case "ipynb" -> extractTextFromIPYNB(filePath);
            case "xlsx" -> extractTextFromExcel(filePath);
            default -> {
                try {
                    byte[] fileBuffer = Files.readAllBytes(filePath);
                    if (fileBuffer.length > MAX_FILE_SIZE) {
                        throw new IOException("File is too large to read into context.");
                    }
                    String encoding = detectEncoding(fileBuffer, extension);
                    Charset charset = Charset.forName(encoding);
                    yield new String(fileBuffer, charset);
                } catch (IOException e) {
                    log.error("Error reading file: {}", filePath, e);
                    throw new RuntimeException("Error reading file: " + e.getMessage(), e);
                }
            }
        };
    }

    private static String extractTextFromPDF(Path filePath) {
        try {
            return tika.parseToString(filePath);
        } catch (IOException | TikaException e) {
            log.error("Error extracting text from PDF: {}", filePath, e);
            throw new RuntimeException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }

    private static String extractTextFromDOCX(Path filePath) {
        try {
            return tika.parseToString(filePath);
        } catch (IOException | TikaException e) {
            log.error("Error extracting text from DOCX: {}", filePath, e);
            throw new RuntimeException("Failed to extract text from DOCX: " + e.getMessage(), e);
        }
    }

    private static String extractTextFromIPYNB(Path filePath) {
        try {
            byte[] fileBuffer = Files.readAllBytes(filePath);
            String encoding = detectEncoding(fileBuffer, null);
            Charset charset = Charset.forName(encoding);
            String content = new String(fileBuffer, charset);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode notebook = mapper.readTree(content);

            StringBuilder extractedText = new StringBuilder();
            if (notebook.has("cells")) {
                for (JsonNode cell : notebook.get("cells")) {
                    String cellType = cell.has("cell_type") ? cell.get("cell_type").asText() : "";
                    if (("markdown".equals(cellType) || "code".equals(cellType))
                            && cell.has("source")) {
                        List<String> sourceLines = new ArrayList<>();
                        for (JsonNode sourceLine : cell.get("source")) {
                            sourceLines.add(sourceLine.asText());
                        }
                        extractedText.append(String.join("\n", sourceLines)).append("\n");
                    }
                }
            }

            return extractedText.toString();
        } catch (Exception e) {
            log.error("Error extracting text from IPYNB: {}", filePath, e);
            throw new RuntimeException("Failed to extract text from IPYNB: " + e.getMessage(), e);
        }
    }

    private static String extractTextFromExcel(Path filePath) {
        try (Workbook workbook = WorkbookFactory.create(filePath.toFile())) {
            StringBuilder excelText = new StringBuilder();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);

                if (workbook.isSheetHidden(i) || workbook.isSheetVeryHidden(i)) {
                    continue;
                }

                excelText.append("--- Sheet: ").append(sheet.getSheetName()).append(" ---\n");

                Iterator<Row> rowIterator = sheet.iterator();
                boolean truncated = false;

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    int rowNumber = row.getRowNum() + 1;

                    if (rowNumber > MAX_EXCEL_ROWS) {
                        excelText
                                .append("[... truncated at row ")
                                .append(rowNumber)
                                .append(" ...]\n");
                        truncated = true;
                        break;
                    }

                    List<String> rowTexts = new ArrayList<>();
                    boolean hasContent = false;

                    int lastCellNum = row.getLastCellNum();
                    if (lastCellNum >= 0) {
                        for (int colIndex = 0; colIndex <= lastCellNum; colIndex++) {
                            Cell cell = row.getCell(colIndex);
                            String cellText = formatCellValue(cell);
                            if (StrUtil.isNotBlank(cellText)) {
                                hasContent = true;
                            }
                            rowTexts.add(cellText);
                        }
                    }

                    if (hasContent) {
                        excelText.append(String.join("\t", rowTexts)).append("\n");
                    }

                    if (truncated) {
                        break;
                    }
                }

                excelText.append("\n");
            }

            return excelText.toString().trim();
        } catch (Exception e) {
            log.error("Error extracting text from Excel: {}", filePath, e);
            throw new RuntimeException("Failed to extract text from Excel: " + e.getMessage(), e);
        }
    }

    private static String formatCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        CellType cellType = cell.getCellType();

        if (cellType == CellType.FORMULA) {
            try {
                switch (cell.getCachedFormulaResultType()) {
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            Date date = cell.getDateCellValue();
                            return DATE_FORMAT.get().format(date);
                        } else {
                            double numericValue = cell.getNumericCellValue();
                            if (numericValue == (long) numericValue) {
                                return String.valueOf((long) numericValue);
                            }
                            return String.valueOf(numericValue);
                        }
                    case STRING:
                        return cell.getStringCellValue();
                    case BOOLEAN:
                        return String.valueOf(cell.getBooleanCellValue());
                    case ERROR:
                        return "[Error: " + cell.getErrorCellValue() + "]";
                    default:
                        return "[Formula: " + cell.getCellFormula() + "]";
                }
            } catch (Exception e) {
                return "[Formula: " + cell.getCellFormula() + "]";
            }
        }

        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return DATE_FORMAT.get().format(date);
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    }
                    return String.valueOf(numericValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case ERROR:
                return "[Error: " + cell.getErrorCellValue() + "]";
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    public static String processFilesIntoText(List<Path> filePaths) {
        List<String> fileContents = new ArrayList<>();

        for (Path filePath : filePaths) {
            try {
                String content = extractTextFromFile(filePath);
                String posixPath = filePath.toString().replace('\\', '/');
                fileContents.add(
                        String.format(
                                "<file_content path=\"%s\">\n%s\n</file_content>",
                                posixPath, content));
            } catch (Exception e) {
                log.error("Error processing file: {}", filePath, e);
                String posixPath = filePath.toString().replace('\\', '/');
                fileContents.add(
                        String.format(
                                "<file_content path=\"%s\">\nError fetching content: %s\n</file_content>",
                                posixPath, e.getMessage()));
            }
        }

        String validFileContents = String.join("\n\n", fileContents);

        if (StrUtil.isNotBlank(validFileContents)) {
            return "Files attached by the user:\n\n" + validFileContents;
        }

        return "";
    }

    private static String getFileExtension(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private static boolean isBinaryFile(byte[] buffer) {
        if (buffer.length == 0) {
            return false;
        }

        int nullCount = 0;
        int checkLength = Math.min(buffer.length, 512);
        for (int i = 0; i < checkLength; i++) {
            if (buffer[i] == 0) {
                nullCount++;
            }
        }

        return (nullCount * 100.0 / checkLength) > 5.0;
    }
}
