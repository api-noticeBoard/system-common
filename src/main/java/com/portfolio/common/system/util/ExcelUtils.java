package com.portfolio.common.system.util;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExcelUtils {
    /**
     * Java 객체 리스트를 엑셀 파일로 변환하여 HttpServletResponse에 바로 다운로드합니다.
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

        try (Workbook workbook = new XSSFWorkbook()){
            Sheet sheet = workbook.createSheet("Data");

            // 2. 헤더 생성
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
                // TODO: 헤더 스타일링 (폰트, 배경색 등)
            }

            // 3. 데이터 생성
            for (int i = 0; i < data.size(); i++) {
                Row dataRow = sheet.createRow(i + 1);
                T dto = data.get(i);
                for (int j = 0; j < excelFields.size(); j++) {
                    try {
                        Field field = excelFields.get(j);
                        field.setAccessible(true);  // private 필드 접근 허용
                        Object value = field.get(dto);
                        Cell cell = dataRow.createCell(j);
                        if (value != null){
                            cell.setCellValue(value.toString());
                        }
                    }catch (IllegalAccessException e){
                        // 필드를 못찾거나 접근할 수 없는 경우
                        e.printStackTrace();    // 실제로 로깅 처리
                    }
                }
            }
            // --- 4. 파일 다운로드를 위한 HTTP 헤더 설정 ---
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + ".xlsx\"");

            workbook.write(response.getOutputStream());
        }

        // TODO: 엑셀 업로드(파일을 읽어 List<DTO>로 변환) 기능도 여기에 추가
    }
}
