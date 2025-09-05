package com.portfolio.common.system.util;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
// HSSFWorkbook (xls), XSSFWorkbook (xlsx)과 달리,
// SXSSFWorkbook은 대용량 데이터를 처리할 때 메모리 사용량을 최소화하기 위한 스트리밍 방식의 Workbook.
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mozilla.universalchardet.UniversalDetector;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * [Utility Class]
 * Apache POI 라이브러리를 사용하여 대용량 엑셀 데이터(.xlsx)를 다운로드하는 기능을 제공하는 유틸리티 클래스.
 *
 * 이 클래스의 핵심 특징은 `SXSSFWorkbook`을 사용하여 OutOfMemoryError를 방지.
 * 일반 `XSSFWorkbook`은 모든 셀 데이터를 메모리에 로드하므로, 수십만 건의 데이터를 처리할 때
 * 메모리 부족 오류가 발생할 수 있음. `SXSSFWorkbook`은 일정량의 행만 메모리에 유지하고
 * 나머지는 디스크의 임시 파일에 기록하는 스트리밍 방식을 사용하여 이 문제를 해결.
 */
public final class OldExcelUtils {

    /**
     * 유틸리티 클래스는 인스턴스화할 필요가 없으므로 private 생성자로 막음.
     */
    private OldExcelUtils() {}

    /**
     * 엑셀 파일을 업로드하여 내용을 2차원 문자열 리스트로 변환.
     * 첫 번째 행은 헤더로 간주하고 건너뛸 수 있음.
     *
     * @param multipartFile 컨트롤러에서 받은 업로드 파일
     * @param skipHeader    첫 번째 행(헤더)을 건너뛸지 여부
     * @return 엑셀 데이터가 담긴 List<List<String>>. 각 내부 리스트는 하나의 행(Row)에 해당.
     * @throws IOException 파일 읽기 실패 시
     */
    public static List<List<String>> parseExcel(MultipartFile multipartFile, boolean skipHeader) throws IOException {
        // 반환할 데이터 구조 생성
        List<List<String>> data = new ArrayList<>();

        // 파일의 InputStream을 얻어옴
        try (InputStream inputStream = multipartFile.getInputStream();
             // 업로드된 파일로부터 XSSFWorkbook 객체 생성 (xlsx 형식)
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            // 첫 번째 시트를 가져옴
            Sheet sheet = workbook.getSheetAt(0);

            // 시작할 행의 인덱스를 결정
            int startRow = skipHeader ? 1 : 0;

            // 시트의 모든 행을 순회 (첫 행부터 마지막 행까지)
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                // 행이 비어있지 않은 경우에만 처리
                if (row != null) {
                    List<String> rowData = new ArrayList<>();

                    // 해당 행의 모든 셀을 순회 (첫 셀부터 마지막 셀까지)
                    for (int j = 0; j < row.getLastCellNum(); j++) {
                        Cell cell = row.getCell(j);
                        // 셀의 값을 문자열로 변환하여 리스트에 추가
                        rowData.add(getCellValueAsString(cell));
                    }
                    // 완성된 행 데이터를 전체 데이터 리스트에 추가
                    data.add(rowData);
                }
            }
        } catch (IOException e) {
            throw new IOException("엑셀 파일 파싱 중 오류가 발생했습니다.", e);
        }

        return data;
    }

    /**
     * 셀의 타입을 확인하고, 어떤 타입이든 관계없이 항상 문자열(String)로 변환하여 반환하는 헬퍼 메서드입니다.
     * @param cell 변환할 셀 객체
     * @return 셀의 값을 나타내는 문자열
     */
    private static String getCellValueAsString(Cell cell) {
        // 셀이 null이거나 비어있으면 빈 문자열("")을 반환
        if (cell == null) {
            return "";
        }

        // 셀의 데이터 타입에 따라 분기 처리
        switch (cell.getCellType()) {
            case STRING:
                // 문자열 타입이면 그대로 반환
                return cell.getStringCellValue();

            case NUMERIC:
                // 숫자 타입일 경우, 날짜 형식인지 일반 숫자인지 확인
                if (DateUtil.isCellDateFormatted(cell)) {
                    // 날짜 형식이면 "yyyy-MM-dd" 형태의 문자열로 변환
                    return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                } else {
                    // 일반 숫자면 소수점 없이 정수 부분만 문자열로 변환
                    // (만약 소수점이 필요하다면 new DecimalFormat("#.##").format(cell.getNumericCellValue()) 등을 사용)
                    return String.valueOf((long) cell.getNumericCellValue());
                }

            case BOOLEAN:
                // 불리언 타입이면 "true" 또는 "false" 문자열로 변환
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                // 수식 타입이면 수식의 계산 결과를 문자열로 변환
                return cell.getStringCellValue();

            // 비어있는 셀(BLANK)이나 에러(ERROR) 타입은 빈 문자열로 처리
            case BLANK:
            case ERROR:
            default:
                return "";
        }
    }

    /**
     * 데이터를 받아 엑셀 파일(XLSX)로 만들어 HttpServletResponse를 통해 클라이언트에게 다운로드.
     * 컨트롤러에서 이 메서드를 호출하여 서비스 로직과 엑셀 생성 로직을 분리할 수 있음.
     *
     * @param response HttpServletResponse 객체. 이 객체의 OutputStream에 엑셀 파일 스트림을 씀.
     * @param fileName 다운로드될 파일의 이름 (확장자 '.xlsx'는 자동으로 붙음.)
     * @param headers  엑셀의 헤더(첫 번째 행)가 될 문자열 리스트. (예: ["ID", "이름", "이메일"])
     * @param dataRows 엑셀에 채워질 데이터 행들의 리스트. 각 행은 셀 데이터의 리스트.
     *                 (예: [ ["1", "홍길동", "test@test.com"], ["2", "김철수", "kim@test.com"] ])
     * @throws IOException 파일 작성 또는 네트워크 전송 중 오류 발생 시
     */
    public static void downloadExcel(HttpServletResponse response, String fileName
            , List<String> headers, List<List<String>> dataRows) throws IOException {

        // try-with-resources 구문을 사용하여 Workbook 객체가 끝나면 자동으로 close()를 호출하여
        // 디스크에 생성된 임시 파일 등 관련 리소스를 안전하게 정리.
        // new SXSSFWorkbook(-1) : 메모리에 행을 전혀 유지하지 않고, 생성되는 즉시 디스크에 flush하여 메모리 사용량을 극도로 최소화.
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(-1)) {
            Sheet sheet = workbook.createSheet(); // 새로운 시트를 생성합니다. (시트 이름은 기본값으로 설정됨)

            // --- 1. 헤더 생성 ---
            // 시트의 첫 번째 행(0번 인덱스)에 헤더를 생성합니다.
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));

                /**
                 * 필요하다면 여기에 CellStyle을 적용하여 헤더를 꾸밀 수 있음. (예: 배경색, 폰트 굵게)
                 *  --- 1. 헤더 폰트 스타일 생성 ---
                 *  Workbook에서 새로운 Font 객체를 생성.
                 */
                 Font headerFont = workbook.createFont();
                 headerFont.setBold(true); // 폰트를 굵게 설정
                 headerFont.setFontHeightInPoints((short) 12); // 폰트 크기 설정 (필요시)
                 headerFont.setColor(IndexedColors.WHITE.getIndex()); // 폰트 색상 설정 (필요시)

                // --- 2. 헤더 셀 스타일 생성 ---
                // Workbook에서 새로운 CellStyle 객체를 생성.
                 CellStyle headerCellStyle = workbook.createCellStyle();
                // 위에서 만든 폰트를 이 셀 스타일에 적용.
                 headerCellStyle.setFont(headerFont);
                // 배경색을 설정합니다. IndexedColors Enum에 미리 정의된 색상들을 사용.
                 headerCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                // 채우기 패턴을 설정합니다. SOLID_FOREGROUND는 셀 전체를 색으로 채우는 패턴.
                 headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                // 정렬 설정 (필요시)
                 headerCellStyle.setAlignment(HorizontalAlignment.CENTER); // 가운데 정렬
                 headerCellStyle.setVerticalAlignment(VerticalAlignment.CENTER); // 수직 가운데 정렬

                // 테두리 설정 (필요시)
                 headerCellStyle.setBorderTop(BorderStyle.THIN);
                 headerCellStyle.setBorderBottom(BorderStyle.THIN);
                 headerCellStyle.setBorderLeft(BorderStyle.THIN);
                 headerCellStyle.setBorderRight(BorderStyle.THIN);

                // --- 3. 헤더 생성 및 스타일 적용 ---
                 headerRow = sheet.createRow(0);
                 for (int j = 0; j < headers.size(); j++) {
                     Cell cell = headerRow.createCell(j);
                     cell.setCellValue(headers.get(j));
                     // 생성된 모든 헤더 셀에 위에서 만든 CellStyle 객체를 적용.
                     cell.setCellStyle(headerCellStyle);
                 }

                // --- 5. 열 너비 자동 조정 (선택 사항) ---
                // 이 작업은 성능에 영향을 줄 수 있으므로, 데이터 양이 매우 많을 때는 주의해야 함.
                // SXSSFWorkbook에서 autoSizeColumn은 제한적으로 동작.
                 for (int i = 0; i < headers.size(); i++) {
                  sheet.autoSizeColumn(i);
                 }
            }

            // --- 2. 데이터 행 생성 ---
            // 데이터는 두 번째 행(1번 인덱스)부터 순서대로 채움.
            for (int i = 0; i < dataRows.size(); i++) {
                Row dataRow = sheet.createRow(i + 1); // 헤더 다음 행부터 생성
                List<String> rowData = dataRows.get(i);
                for (int j = 0; j < rowData.size(); j++) {
                    // 각 행에 셀을 생성하고 데이터를 입력합니다.
                    dataRow.createCell(j).setCellValue(rowData.get(j));
                }
            }

            // --- 3. 브라우저가 파일을 다운로드하도록 HTTP 응답 헤더 설정 ---
            // 파일 이름이 한글 등 비-ASCII 문자를 포함할 경우 깨지지 않도록 UTF-8로 인코딩.
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            // 응답의 컨텐츠 타입을 엑셀 파일(XLSX) 형식으로 지정.
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            // Content-Disposition 헤더를 'attachment'로 설정하여 브라우저가 이 응답을 '다운로드'하도록 지시.
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + ".xlsx\"");

            // --- 4. 생성된 엑셀 데이터를 응답 스트림(OutputStream)에 씀. ---
            // 이 코드가 실행되면, 서버에서 생성된 엑셀 파일 데이터가 네트워크를 통해 클라이언트(브라우저)로 전송.
            workbook.write(response.getOutputStream());

        } catch (IOException e) {
            // 이 메서드를 호출한 컨트롤러 단에서 예외를 처리할 수 있도록, 혹은 GlobalExceptionHandler가 처리하도록
            // IOException을 다시 던짐.
            throw new IOException("Excel 파일 생성 및 다운로드 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * OpenCSV 라이브러리를 사용하여 CSV 파일을 업로드하고 파싱.
     * 셀 데이터에 쉼표, 큰따옴표, 줄바꿈이 포함된 복잡한 CSV도 안정적으로 처리할 수 있음.
     *
     * @param multipartFile 컨트롤러에서 받은 업로드 파일
     * @param skipHeader    첫 번째 행(헤더)을 건너뛸지 여부
     * @return CSV 데이터가 담긴 List<List<String>>
     * @throws IOException 파일 읽기 실패 시
     * @throws CsvException CSV 형식 오류 발생 시
     */
    public static List<List<String>> parseCsvWithOpenCsv(MultipartFile multipartFile, boolean skipHeader) throws IOException, CsvException {

        // --- 1. 인코딩 감지 ---
        String detectedCharset;
        // InputStream은 한 번만 읽을 수 있으므로, 인코딩 감지 후 다시 읽기 위해 try-with-resources를 두 번 사용합니다.
        try (BufferedInputStream bis = new BufferedInputStream(multipartFile.getInputStream())) {
            // UniversalDetector 인스턴스 생성
            UniversalDetector detector = new UniversalDetector(null);

            // 파일의 바이트 데이터를 읽으면서 인코딩을 추측합니다.
            byte[] buf = new byte[4096];
            int nread;
            while ((nread = bis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
            // 데이터 처리를 완료하고 감지를 마칩니다.
            detector.dataEnd();

            // 감지된 인코딩 이름을 가져옵니다.
            detectedCharset = detector.getDetectedCharset();

            // 감지에 실패한 경우, 기본값으로 UTF-8을 사용합니다.
            if (detectedCharset == null) {
                detectedCharset = "UTF-8";
            }

            // 감지기 리셋
            detector.reset();
        } catch (IOException e) {
            throw new IOException("파일 인코딩 감지 중 오류가 발생했습니다.", e);
        }

        // --- 2. 감지된 인코딩으로 파일 파싱 ---
        List<List<String>> data = new ArrayList<>();

        // InputStreamReader를 사용하여 인코딩 지정
        try (CSVReader reader = new CSVReader(new InputStreamReader(multipartFile.getInputStream(), StandardCharsets.UTF_8))) {

            // readAll() 메서드는 파일의 모든 내용을 읽어 List<String[]> 형태로 반환.
            List<String[]> allRows = reader.readAll();

            // 시작할 행의 인덱스를 결정
            int startRow = skipHeader ? 1 : 0;

            // 배열 리스트를 List<List<String>> 형태로 변환
            for (int i = startRow; i < allRows.size(); i++) {
                data.add(List.of(allRows.get(i)));
            }

        } catch (IOException | CsvException e) {
            // CsvException은 OpenCSV에서 발생하는 예외.
            throw e;
        }

        return data;
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

        // HTTP 헤더 설정
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

    /**
     * Java 객체 리스트를 엑셀 파일로 변환하여 HttpServletResponse에 바로 다운로드합니다.
     * @param data 엑셀로 만들 데이터 목록 (e.g., List<PostDto.Response>)
     * @param headers 엑셀의 헤더로 사용할 이름 목록 (e.g., ["ID", "제목", "작성자"])
     * @param fields DTO 객체에서 헤더 순서에 맞게 값을 추출할 필드 이름 목록 (e.g., ["id", "title", "createdByName"])
     * @param fileName 다운로드될 파일 이름
     * @param response HttpServletResponse 객체
     * @param <T> 데이터 객체의 타입
     */
    public static <T> void downloadExcel(
            List<T> data
            , List<String> headers
            , List<String> fields
            , String fileName
            , HttpServletResponse response) throws IOException {

        try (Workbook workbook = new XSSFWorkbook()){
            Sheet sheet = workbook.createSheet("Data");

            // 1. 헤더 생성
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                // TODO: 헤더 스타일링 (폰트, 배경색 등)
            }

            // 2. 데이터 생성
            for (int i = 0; i < data.size(); i++) {
                Row dataRow = sheet.createRow(i + 1);
                T dto = data.get(i);
                for (int j = 0; j < data.size(); j++) {
                    try {
                        Field field = dto.getClass().getDeclaredField(fields.get(j));
                        field.setAccessible(true);  // private 필드 접근 허용
                        Object value = field.get(dto);
                        Cell cell = dataRow.createCell(j);
                        if (value != null){
                            cell.setCellValue(value.toString());
                        }
                    }catch (NoSuchFieldException | IllegalAccessException e){
                        // 필드를 못찾거나 접근할 수 없는 경우
                        e.printStackTrace();    // 실제로 로깅 처리
                    }
                }
            }

            // 3. 파일 다운로드를 위한 HTTP 헤더 설정
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");

            // 4. Workbook을 response의 OutputStream에 사용
            workbook.write(response.getOutputStream());
        }

        // TODO: 엑셀 업로드(파일을 읽어 List<DTO>로 변환) 기능도 여기에 추가
    }
}
