package com.portfolio.common.system.paging;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class PageDto {

    @Getter
    @Setter
    public static class Request {
        @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
        private int page = 1;
        @Schema(description = "페이지 당 데이터 수", example = "10", defaultValue = "10")
        private int size = 10;

        /**
         * 인터셉터가 계산한 전체 데이터 개수를 저장하기 위한 필드.
         * 서버 내부에서만 사용되며, 클라이언트가 보내는 값은 무시됩니다.
         */
        @Schema(hidden = true) // 이 필드는 클라이언트가 보내는 값이 아니므로 Swagger UI에서 숨깁니다.
        private long totalCount;

        // JPA용 변환 메서드
        public Pageable toPageable() {
            return PageRequest.of(page - 1, size);
        }

        // MyBatis용 OFFSET 계산 메서드
        public long getOffset() {
            return (long) (page - 1) * size;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class Response<T> {
        private List<T> content;
        private int pageNumber;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean isLast;

        // JPA용 생성자
        public Response(Page<T> page) {
            this.content = page.getContent();
            this.pageNumber = page.getNumber() + 1;
            this.pageSize = page.getSize();
            this.totalElements = page.getTotalElements();
            this.totalPages = page.getTotalPages();
            this.isLast = page.isLast();
        }

        /**
         * MyBatis 페이징 결과를 위한 최종 생성자.
         * @param content 현재 페이지의 데이터 목록
         * @param pageRequest 인터셉터가 totalCount를 채워준 페이징 요청 정보
         */
        public Response(List<T> content, Request pageRequest) {
            this.content = content;
            this.pageNumber = pageRequest.getPage();
            this.pageSize = pageRequest.getSize();
            this.totalElements = pageRequest.getTotalCount(); // 인터셉터가 설정해준 값을 사용

            if (this.pageSize > 0) {
                this.totalPages = (int) Math.ceil((double) this.totalElements / this.pageSize);
            }else {
                this.totalPages = 0;
            }

            this.isLast = this.pageNumber >= this.totalPages;
        }
    }
}
