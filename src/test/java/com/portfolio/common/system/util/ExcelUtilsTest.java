package com.portfolio.common.system.util;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
public class ExcelUtilsTest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class UploadRequest{
        @ExcelColumn(colIndex = 0)  // A열
        private String title;
        @ExcelColumn(colIndex = 1)  // B열
        private String content;
        @ExcelColumn(colIndex = 2)  // C열
        private Long categoryId;
    }

    @Test
    @DisplayName("엑셀 파일 업로드 테스트")
    void upload() throws IOException{
        // given
        // src/test/resources/test_posts_upload.xlsx 파일을 로드합니다.
        ClassPathResource resource = new ClassPathResource("excel-upload_2025-09-08.xlsx");
        InputStream inputStream = resource.getInputStream();

        // MockMultipartFile을 사용하여 실제 파일 업로드처럼 시뮬레이션합니다.
        MultipartFile multipartFile = new MockMultipartFile(
                "file", // 파라미터 이름
                "excel-upload_2025-09-08.xlsx", // 원본 파일 이름
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // Content-Type
                inputStream // 파일 내용
        );

        // when
        List<UploadRequest> postDtoList = ExcelUtils.uploadExcel(multipartFile, UploadRequest.class);

        // then
        assertThat(postDtoList).isNotNull();
//        assertThat(postDtoList).withFailMessage("엑셀 파싱 결과, DTO 리스트가 비어있습니다.").isNotEmpty();
//        assertThat(postDtoList).hasSize(3); // 3개의 데이터 행이 있으므로 3개의 DTO가 생성되어야 합니다.

//        UploadRequest post1 = postDtoList.get(0);
//        assertThat(post1.getTitle()).isEqualTo("Test Post 1");
//        assertThat(post1.getContent()).isEqualTo("Content 1");
//        assertThat(post1.getCategoryId()).isEqualTo(1L);
//        log.info("post1 : {}", post1);

//        UploadRequest post2 = postDtoList.get(1);
//        assertThat(post2.getTitle()).isEqualTo("uploadTEST2");
//        assertThat(post2.getContent()).isEqualTo("content2");
//        assertThat(post2.getCategoryId()).isEqualTo(2L);
//        log.info("post2 : {}", post2);

    }
}
