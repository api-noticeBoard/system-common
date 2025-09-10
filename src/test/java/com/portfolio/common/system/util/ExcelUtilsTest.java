package com.portfolio.common.system.util;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString; // DTO 객체 로그 출력을 위해 추가
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
public class ExcelUtilsTest {

    /**
     * 테스트용 DTO 클래스입니다.
     * 실제 DTO와 동일한 구조(@ExcelColumn)를 가져야 합니다.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @ToString // 객체 내용을 로그로 쉽게 확인하기 위해 추가
    public static class UploadRequest {
        @ExcelColumn(colIndex = 0) // A열
        private String title;
        @ExcelColumn(colIndex = 1) // B열
        private String content;
        @ExcelColumn(colIndex = 2) // C열
        private Long categoryId;
    }

    @Test
    @DisplayName("성공: 유효한 엑셀 파일 업로드 시 DTO 리스트로 정확하게 변환되어야 한다")
    void uploadExcel_withValidFile_shouldReturnCorrectDtoList() throws IOException {
        // given: 테스트 준비
        // 1. src/test/resources 폴더에서 정상적인 엑셀 파일을 로드합니다.
        MultipartFile validExcelFile = createMockMultipartFile("excel-upload.xlsx");

        // when: 테스트 대상 메서드 실행
        // 2. ExcelUtils.uploadExcel 메서드를 호출하여 DTO 리스트로 변환합니다.
        List<UploadRequest> resultList = ExcelUtils.uploadExcel(validExcelFile, UploadRequest.class);

        // then: 결과 검증
        // 3. 변환 결과가 예상과 일치하는지 확인합니다.
        assertThat(resultList).isNotNull();
//        assertThat(resultList).hasSize(3); // 데이터 행이 3개이므로, DTO 객체도 3개여야 합니다.

        // 첫 번째 DTO 객체의 내용을 상세히 검증합니다.
        UploadRequest firstDto = resultList.get(0);
        assertThat(firstDto.getTitle()).isEqualTo("업로드 테스트1");
        assertThat(firstDto.getContent()).isEqualTo("업로드 테스트");
        assertThat(firstDto.getCategoryId()).isEqualTo(1L);

        // 두 번째 DTO 객체의 내용을 상세히 검증합니다.
        UploadRequest secondDto = resultList.get(1);
        assertThat(secondDto.getTitle()).isEqualTo("업로드 테스트2");
        assertThat(secondDto.getContent()).isEqualTo("업로드 테스트업로드 테스트");
        assertThat(secondDto.getCategoryId()).isEqualTo(2L);

        System.out.println("성공 케이스 결과: " + resultList);
    }

    @Test
    @DisplayName("성공: 데이터가 없는 (헤더만 있는) 엑셀 파일 업로드 시 빈 리스트를 반환해야 한다")
    void uploadExcel_withEmptyFile_shouldReturnEmptyList() throws IOException {
        // given: 헤더만 있는 엑셀 파일을 준비합니다.
        MultipartFile emptyExcelFile = createMockMultipartFile("빈문서.xlsx");

        // when: uploadExcel 메서드를 실행합니다.
        List<UploadRequest> resultList = ExcelUtils.uploadExcel(emptyExcelFile, UploadRequest.class);

        // then: 결과가 비어있는 리스트인지 검증합니다.
        assertThat(resultList).isNotNull();
        assertThat(resultList).isEmpty();

        System.out.println("빈 파일 케이스 결과: " + resultList);
    }

    @Test
    @DisplayName("실패: 엑셀 형식이 아닌 파일 업로드 시 RuntimeException이 발생해야 한다")
    void uploadExcel_withInvalidFileFormat_shouldThrowRuntimeException() throws IOException {
        // given: .txt 파일과 같이 잘못된 형식의 파일을 준비합니다.
        MultipartFile invalidFile = createMockMultipartFile("upload_test_invalid.txt", "text/plain");

        // when & then: 예외가 발생하는지 검증합니다.
        // assertThrows를 사용하여 특정 예외(RuntimeException)가 발생하는 것을 기대하고 테스트를 수행합니다.
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ExcelUtils.uploadExcel(invalidFile, UploadRequest.class);
        });

        // 예외 메시지가 예상과 일치하는지 추가로 검증할 수 있습니다.
        assertThat(exception.getMessage()).contains("엑셀 파일 처리 중 오류가 발생했습니다");

        System.out.println("예외 케이스 발생 성공: " + exception.getMessage());
    }


    /**
     * 테스트를 위한 MockMultipartFile 객체를 생성하는 헬퍼 메서드입니다.
     * @param fileName src/test/resources 에 위치한 파일 이름
     * @return 생성된 MockMultipartFile 객체
     */
    private MockMultipartFile createMockMultipartFile(String fileName) throws IOException {
        return createMockMultipartFile(fileName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    /**
     * 테스트를 위한 MockMultipartFile 객체를 생성하는 헬퍼 메서드입니다. (Content-Type 지정 가능)
     * @param fileName src/test/resources 에 위치한 파일 이름
     * @param contentType 파일의 MIME 타입
     * @return 생성된 MockMultipartFile 객체
     */
    private MockMultipartFile createMockMultipartFile(String fileName, String contentType) throws IOException {
        ClassPathResource resource = new ClassPathResource(fileName);
        InputStream inputStream = resource.getInputStream();
        return new MockMultipartFile(
                "file",               // 컨트롤러에서 받을 파라미터 이름 ("@RequestParam("file")")
                fileName,             // 원본 파일 이름
                contentType,          // 파일의 Content-Type
                inputStream           // 파일의 실제 내용 (Stream)
        );
    }

    @Test
    @DisplayName("유효CSV 시 DTO리스트 변환")
    void uploadCSVTest() throws IOException{
        // give : 테스트 파일 준비
        MultipartFile file = createMockMultipartFile("upload_csv.csv", "test/csv");

        // when : uploadCSV 메서드 실행
        List<UploadRequest> resultList  = ExcelUtils.uploadCsv(file, UploadRequest.class);

        // then : 결과 검증
        log.info("resultList  : {}", resultList );

        assertThat(resultList).isNotNull();
        assertThat(resultList).hasSize(2);

        // 첫 번째 DTO 객체의 내용을 상세히 검증합니다.
        UploadRequest firstDto = resultList.get(0);
        assertThat(firstDto.getTitle()).isEqualTo("CSV테스트1");
        assertThat(firstDto.getContent()).isEqualTo("csv테스트");
        assertThat(firstDto.getCategoryId()).isEqualTo(65L);

        // ✨ [핵심 검증] 두 번째 DTO 객체의 내용을 상세히 검증합니다.
        // Apache Commons CSV 덕분에 따옴표로 묶인 필드 안의 쉼표가 올바르게 처리됩니다.
        UploadRequest secondDto = resultList.get(1);
        assertThat(secondDto.getTitle()).isEqualTo("CSV테스트2");
        assertThat(secondDto.getContent()).isEqualTo("csv테스트,csv테스트");
        assertThat(secondDto.getCategoryId()).isEqualTo(65L);
    }
}
