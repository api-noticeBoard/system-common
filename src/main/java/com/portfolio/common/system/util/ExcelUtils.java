package com.portfolio.common.system.util;

import com.portfolio.common.system.exception.BusinessException;
import com.portfolio.common.system.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.Styles;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class ExcelUtils {

    /**
     * 엑셀 파일을 업로드하여 DTO 리스트로 변환합니다.
     * SAX 파싱 방식을 사용하여 대용량 파일(수십만 건 이상)도 메모리 문제 없이 처리할 수 있습니다.
     *
     * @param file     클라이언트로부터 업로드된 MultipartFile 객체
     * @param dtoClass 변환할 DTO의 클래스 타입 (e.g., PostDto.UploadRequest.class)
     * @param <T>      DTO의 제네릭 타입
     * @return DTO 객체 리스트
     */
    public static <T> List<T> uploadExcel(MultipartFile file, Class<T> dtoClass) {
        List<T> resultList = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream();
             OPCPackage opcPackage = OPCPackage.open(inputStream)) {

            // 1. 엑셀 파일(.xlsx)의 기본 구성 요소를 읽어옵니다.
            XSSFReader xssfReader = new XSSFReader(opcPackage);
            Styles styles = xssfReader.getStylesTable(); // 셀 스타일 정보
            SharedStrings sharedStrings = xssfReader.getSharedStringsTable(); // 중복 문자열 정보

            // 2. XML을 파싱할 SAX 파서를 설정합니다.
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            // 보안 설정: XXE(XML External Entity) 공격 방지
            /** 엑셀 시트 내부의 XML은 특정 네임스페이스(Namespace)를 사용하여 정의되는데, SAX 파서가 이 네임스페이스를 인지하도록 설정되지 않으면 <row>나 <c> 같은 태그들을 인식하지 못하고 그냥 건너뛰게 됩 */
            saxParserFactory.setNamespaceAware(true); // 🤬🤬🤬🤬🤬🤬네임스페이스 인지하도록 설정
            saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
//            saxParserFactory.setFeature("http://xml/org/sax/features/external-parameter-entities", false);

            SAXParser saxParser = saxParserFactory.newSAXParser();
            XMLReader sheetParser = saxParser.getXMLReader();

            // 3. 엑셀 파일 내의 모든 시트를 순회하며 처리합니다.
            XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
            while (sheetIterator.hasNext()) {
                try (InputStream sheetInputStream = sheetIterator.next()) {
                    String sheetName = sheetIterator.getSheetName();
                    log.info("엑셀 시트 처리 시작: '{}'", sheetName);

                    // 4. 실제 파싱 로직을 담당할 커스텀 핸들러를 생성합니다.
                    ContentHandler handler = new XSSFSheetXMLHandler(
                            styles,
                            null, // ✅ 올바른 위치: CommentsTable은 보통 null입니다.
                            sharedStrings, // ✅ 올바른 위치: 세 번째 인자가 SharedStrings 자리입니다.
                            new RowProcessingHandler<>(dtoClass, resultList), // ✅ 올바른 위치: 네 번째가 실제 핸들러 자리입니다.
                            new DataFormatter(),
                            false
                    );
                    sheetParser.setContentHandler(handler);

                    // 5. 시트 파싱을 실행합니다.
                    InputSource sheetSource = new InputSource(sheetInputStream);
                    sheetParser.parse(sheetSource);

                    // 6. 데이터가 있는 첫 번째 시트만 처리하고 중단합니다.
                    if (!resultList.isEmpty()) {
                        log.info("시트 '{}'에서 데이터를 성공적으로 파싱했습니다. 업로드를 종료합니다.", sheetName);
                        break;
                    }
                }
            }

            log.info("엑셀 파싱 완료. 총 {}개의 데이터를 변환했습니다.", resultList.size());

        } catch (Exception e) {
            log.error("엑셀 파일 파싱 중 심각한 오류가 발생했습니다.", e);
            throw new RuntimeException("엑셀 파일 처리 중 오류가 발생했습니다. 파일 형식이나 내용을 확인해주세요.", e);
        }
        return resultList;
    }

    /**
     * 엑셀의 각 행(Row)을 DTO로 변환하는 SAX 이벤트 핸들러 클래스입니다.
     */
    private static class RowProcessingHandler<T> implements XSSFSheetXMLHandler.SheetContentsHandler {

        private static final int HEADER_ROW_COUNT = 1; // 건너뛸 헤더 행의 수

        private final Class<T> dtoClass;
        private final List<T> resultList;
        private final Map<Integer, Field> fieldMap; // {엑셀 컬럼 인덱스: DTO 필드}

        private Map<Integer, String> currentRowData; // 현재 처리 중인 행의 데이터를 임시 저장 {컬럼 인덱스: 셀 값}
        private int currentRowNum = -1; // 현재 행 번호를 추적하기 위한 필드 추가

        public RowProcessingHandler(Class<T> dtoClass, List<T> resultList) {
            this.dtoClass = dtoClass;
            this.resultList = resultList;
            this.fieldMap = mapFieldsToColumnIndex(dtoClass);
        }

        /**
         * DTO 클래스를 분석하여 @ExcelColumn 어노테이션의 colIndex와 필드를 미리 매핑합니다.
         * 이 작업은 성능 향상을 위해 생성자에서 한 번만 수행됩니다.
         */
        private Map<Integer, Field> mapFieldsToColumnIndex(Class<T> clazz) {
            Map<Integer, Field> mappedFields = new HashMap<>();
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(ExcelColumn.class)) {
                    ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
                    if (annotation.colIndex() >= 0) {
                        field.setAccessible(true); // private 필드에 접근 가능하도록 설정
                        mappedFields.put(annotation.colIndex(), field);
                    }
                }
            }
            return mappedFields;
        }

        /**
         * 행(Row)의 시작을 처리하는 이벤트입니다.
         * 데이터 행인 경우, 데이터를 담을 임시 저장소를 초기화합니다.
         */
        @Override
        public void startRow(int rowNum) {
            this.currentRowNum = rowNum;
            // 헤더 행은 건너뛰고, 데이터 행부터 처리합니다.
            if (rowNum >= HEADER_ROW_COUNT) {
                this.currentRowData = new HashMap<>();
                log.info("[Handler] 데이터 행 시작: {}", rowNum + 1);
            } else {
                log.info("[Handler] 헤더 행 건너뜀: {}", rowNum + 1);
            }
        }

        /**
         * 셀(Cell)을 처리하는 이벤트입니다.
         * 현재 행의 임시 저장소에 셀 데이터를 추가합니다.
         */
        @Override
        public void cell(String cellReference, String formattedValue, org.apache.poi.xssf.usermodel.XSSFComment comment) {
            // startRow에서 데이터 행 처리가 시작된 경우에만 (currentRowData != null) 동작합니다.
            if (currentRowData != null && formattedValue != null && !formattedValue.isBlank()) {
                int colIndex = new org.apache.poi.ss.util.CellReference(cellReference).getCol();
                currentRowData.put(colIndex, formattedValue);
                log.info("[Handler] 셀 데이터 읽음 (행 {}): ref={}, 값='{}'", currentRowNum + 1, cellReference, formattedValue);
            }
        }

        /**
         * 행(Row)의 끝을 처리하는 이벤트입니다.
         * 수집된 행 데이터를 바탕으로 DTO 객체를 생성하고 결과 리스트에 추가합니다.
         */
        @Override
        public void endRow(int rowNum) {
            // 데이터가 있는 데이터 행이 끝났을 때만 DTO 변환을 시도합니다.
            if (isProcessableDataRow(rowNum)) {
                log.info("[Handler] 데이터 행 종료 (행 {}): 수집된 데이터={}", rowNum + 1, currentRowData);
                try {
                    T dto = createDtoFromRowData();
                    resultList.add(dto);
                    log.info("[Handler] DTO 변환 성공 (행 {})", rowNum + 1);
                } catch (Exception e) {
                    log.error("[Handler] DTO 변환 실패 (행 {})", rowNum + 1, e);
                }
            }
            // 다음 행을 위해 현재 행 데이터는 초기화합니다.
            this.currentRowData = null;
        }

        /**
         * 현재 행이 DTO로 변환할 가치가 있는 데이터 행인지 확인합니다.
         */
        private boolean isProcessableDataRow(int rowNum) {
            return rowNum >= HEADER_ROW_COUNT && currentRowData != null && !currentRowData.isEmpty();
        }

        /**
         * 수집된 행 데이터를 바탕으로 DTO 객체를 생성하고 필드 값을 채웁니다.
         */
        private T createDtoFromRowData() throws ReflectiveOperationException {
            T dtoInstance = dtoClass.getDeclaredConstructor().newInstance();

            for (Map.Entry<Integer, String> dataEntry : currentRowData.entrySet()) {
                Field field = fieldMap.get(dataEntry.getKey());
                if (field != null) {
                    setFieldValue(dtoInstance, field, dataEntry.getValue());
                }
            }
            return dtoInstance;
        }

        /**
         * 리플렉션을 사용하여 DTO 객체의 특정 필드에 값을 설정합니다.
         * 값의 타입 변환도 이 메서드에서 처리합니다.
         */
        private void setFieldValue(T dtoInstance, Field field, String value) {
            try {
                Object convertedValue = convertValueToFieldType(value, field.getType());
                field.set(dtoInstance, convertedValue);
            } catch (Exception e) {
                // 특정 필드 값 설정에 실패하더라도 전체 행의 변환이 멈추지 않도록 경고만 기록합니다.
                log.warn("필드 '{}'에 값 '{}'을(를) 설정하는 데 실패했습니다. 원인: {}", field.getName(), value, e.getMessage());
            }
        }

        /**
         * 엑셀에서 읽은 문자열 값을 DTO 필드의 실제 타입으로 변환합니다.
         */
        private Object convertValueToFieldType(String value, Class<?> fieldType) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                if (fieldType.equals(String.class)) return value;
                if (fieldType.equals(Long.class) || fieldType.equals(long.class)) return Long.parseLong(value.trim());
                if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) return Integer.parseInt(value.trim());
                if (fieldType.equals(Double.class) || fieldType.equals(double.class)) return Double.parseDouble(value.trim());
                if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) return Boolean.parseBoolean(value.trim());
                // TODO: 필요에 따라 날짜(LocalDateTime) 등 다른 타입 변환 로직을 추가할 수 있습니다.
            } catch (NumberFormatException e) {
                log.warn("'{}' 값을 숫자 타입({})으로 변환할 수 없습니다. null로 처리됩니다.", value, fieldType.getSimpleName());
                return null;
            }
            return value;
        }
    }

    /**
     * Java 객체 리스트를 엑셀 파일로 변환하여 HttpServletResponse에 바로 다운로드합니다. (XSSFWorkbook 방식)
     * @param data 엑셀로 만들 데이터 목록 (e.g., List<PostDto.Response>)
     * @param dtoClass headers, fields를 합침
     * // @param headers 엑셀의 헤더로 사용할 이름 목록 (e.g., ["ID", "제목", "작성자"])
     * // @param fields DTO 객체에서 헤더 순서에 맞게 값을 추출할 필드 이름 목록 (e.g., ["id", "title", "createdByName"])
     * @param fileName 다운로드될 파일 이름
     * @param response HttpServletResponse 객체
     * @param <T> 데이터 객체의 타입
     */
    public static <T> void downloadExcel(
            List<T> data
            , Class<T> dtoClass
            , String fileName
            , HttpServletResponse response) throws IOException {

        // --- 1. DTO 클래스에서 @ExcelColumn 정보를 추출하여 헤더와 필드 목록 생성 ---
        List<Field> excelFields = new ArrayList<>();
        for (Field field : dtoClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(ExcelColumn.class)) {
                excelFields.add(field);
            }
        }
        // 'order' 속성 기준으로 필드 정렬
        excelFields.sort(Comparator.comparingInt(f -> f.getAnnotation(ExcelColumn.class).order()));

        // 정렬된 필드에서 헤더 이름과 필드 이름 목록 추출
        List<String>headers = excelFields.stream()
                .map(f -> f.getAnnotation(ExcelColumn.class).headerName())
                .toList();

        // try-with-resources 구문을 사용하여 Workbook 객체가 자동으로 닫힘
        try (SXSSFWorkbook workbook = new SXSSFWorkbook()){
            // 이 옵션을 켜면 성능이 약간 저하될 수 있지만, 너비 자동 조정을 가능하게 합니다.
            workbook.setCompressTempFiles(true); // 임시 파일을 압축하여 디스크 공간 절약
            // sheet 생성
            Sheet sheet = workbook.createSheet("Data");
            ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();    // SXSSFSheet 타입으로 캐스팅해야 함

            /** 스타일 객체 생성 */
            // 헤더 전용 스타일
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());    // 배경색: 로얄 블루
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);               // 배경색 단색으로 채우는 패턴
            headerStyle.setAlignment(HorizontalAlignment.CENTER);                       // 수평 가운데 정렬
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);                 // 수직 가운데 정렬
            // 헤더 테두리 적용
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            // 헤더 폰트 적용
            Font headerFont = workbook.createFont();
            headerFont.setFontName("맑은 고딕");                                          // 폰트 적용
            headerFont.setBold(true);                                                    // 폰트 두께 적용
            headerFont.setColor(IndexedColors.WHITE.getIndex());                         // 폰트 색상 적용
            headerStyle.setFont(headerFont);                                             // 위에 폰트 스타일 적용

            // 본문 전용 스타일
            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setAlignment(HorizontalAlignment.LEFT); // 수평 왼쪽 정렬
            bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            // 본문 테두리 적용
            bodyStyle.setBorderTop(BorderStyle.THIN);
            bodyStyle.setBorderBottom(BorderStyle.THIN);
            bodyStyle.setBorderLeft(BorderStyle.THIN);
            bodyStyle.setBorderRight(BorderStyle.THIN);
            // 본문 폰트 적용
            Font bodyFont = workbook.createFont();
            bodyFont.setFontName("맑은 고딕");
            bodyStyle.setFont(bodyFont);

            // 2. 헤더 생성 및 스타일 적용
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(headers.get(i));        // 헤더 이름과 필드값 적용
                headerCell.setCellStyle(headerStyle);           // 헤더 스타일 적용
            }

            // 3. 데이터 생성
            int rowCnt = data.size();
            for (int i = 0; i < rowCnt; i++) {
                Row dataRow = sheet.createRow(i + 1);
                T dto = data.get(i);
                for (int j = 0; j < excelFields.size(); j++) {
                    Cell dataCell = dataRow.createCell(j);
                    try {
                        Field field = excelFields.get(j);
                        field.setAccessible(true);              // private 필드 접근 허용
                        Object value = field.get(dto);          // 리플렉션으로 필드값 적용
                        if (value != null){
                            dataCell.setCellValue(value.toString());
                        }

                    }catch (IllegalAccessException e){
                        // 필드를 못찾거나 접근할 수 없는 경우
                        log.error("Failed to get field value via ref lection", e);
                        e.printStackTrace();    // 실제로 로깅 처리
                    }
                    dataCell.setCellStyle(bodyStyle);           // 본문 스타일 적용
                }
            }

            // --- 컬럼 너비 자동 조정 ---
            for (int i = 0; i < headers.size(); i++){
                sheet.autoSizeColumn(i);                                        // 내용에 맞게 각 커럼 너비 자동 조정
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 512);   // 컬럼 너비에 여유 "512"= 두개 문자너비 추가
            }

            // --- 자동 필터 적용 ---
            int firstRow = 0;                   // 헤더 행 번호
            int lastRow = rowCnt;               // 마지막 데이터 행 번호 (데이터가 없으면 0)
            int firstCol = 0;                   // 첫 번째 컬럼 번호
            int lastCol = headers.size() - 1;   // 마지막 컬럼 번호
            // 데이터 존재 시 필터 적용 (헤더만 있는 경우 제외)
            if (lastRow > firstRow && lastCol >= firstCol) {
                CellRangeAddress cellAddresses = new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);
                sheet.setAutoFilter(cellAddresses);     // 시트에 자동 필터 기능 설정
            }


            // --- 4. 파일 다운로드를 위한 HTTP 헤더 설정 ---
            // 파일 이름에 한글이 포함될 경우를 대비하여 UTF-8로 인코딩.
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            // 응답의 Content-Type을 엑셀 파일 형식(xlsx)으로 지정.
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            // Content-Disposition 헤더를 'attachment'로 설정하여 브라우저가 파일을 다운로드
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + ".xlsx\"");
            // 생성된 엑셀(Workbook)의 내용을 HTTP 응답의 출력 스트림(OutputStream)에 씀
            workbook.write(response.getOutputStream());
        }
    }

    /**
     * 데이터를 받아 CSV 파일(.csv)로 만들어 다운로드.
     * Apache POI를 사용하지 않고 직접 문자열을 조합.
     * @param response HttpServletResponse 객체
     * @param fileName 다운로드될 파일 이름
     * @param headers  헤더 리스트
     * @param dataRows 데이터 리스트
     * @throws IOException 파일 작성 실패 시
     */
    public static void downloadCsv(HttpServletResponse response, String fileName, List<String> headers, List<List<String>> dataRows) throws IOException {

        // HTTP 헤더 설정(URLEncoder.encode()는 공백(' ')을 '+' 문자로 변환한 것을 웹 환경 공백 "%20"로 다시 변환)
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        // ContentType을 text/csv로 변경하고, 문자 인코딩을 명시(한글 깨짐 방지)
        response.setContentType("text/csv; charset=UTF-8");
        // 파일 확장자를 .csv로 변경
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + ".csv\"");
        // CSV 파일임을 명시하기 위해 BOM(Byte Order Mark) 추가 (Excel에서 한글 깨짐 방지)
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("\uFEFF");

        try (PrintWriter writer = response.getWriter()) {
            // 헤더 쓰기 (쉼표로 join)
            writer.println(String.join(",", headers));

            // 데이터 행 쓰기
            for (List<String> rowData : dataRows) {
                // 각 셀 데이터에 쉼표(,)나 줄바꿈이 포함될 경우를 대비하여 큰따옴표(")로 감싸주는 로직이 필요할 수 있음
                writer.println(String.join(",", rowData));
            }
            writer.flush();
        }
    }
}
