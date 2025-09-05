package com.portfolio.common.system.util;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

public class ExcelUtils {
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
