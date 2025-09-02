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
    /**
     * 클라이언트로부터 페이징 요청 파라미터(page, size)를 받기 위한 DTO.
     */
    @Getter
    @Setter
    public static class Request {
        @Schema(description = "페이지 번호 (1부터 시작)", example = "1", defaultValue = "1")
        private int page = 1;
        @Schema(description = "페이지 당 데이터 수", example = "10", defaultValue = "10")
        private int size = 10; // 기본 페이지 크기

        /**
         * Spring Data JPA의 Pageable 객체로 변환하는 메서드.
         * @return Pageable 객체
         */
        public Pageable toPageable() {
            // JPA의 PageRequest는 페이지 번호가 0부터 시작하므로, 클라이언트가 보낸 page에서 1을 빼줍니다.
            return PageRequest.of(page - 1, size);
        }

        /**
         * MyBatis 페이징 쿼리에서 사용할 OFFSET 값을 계산하는 메서드.
         * OFFSET은 건너뛸 데이터의 수를 의미합니다.
         * @return OFFSET 값
         */
        @Schema(hidden = true)
        public long getOffset() {
            return (long) (page - 1) * size;
        }
    }

    /**
     * 페이징 처리된 결과를 클라이언트에게 응답하기 위한 공통 DTO.
     * 제네릭(<T>)을 사용하여 어떤 타입의 데이터 목록이든 담을 수 있습니다. (e.g., PostDto, UserDto 등)
     */
    @Getter
    @NoArgsConstructor // ✨ 기본 생성자 추가 (필요 시)
    public static class Response<T> {
        private List<T> content;      // 현재 페이지의 데이터 목록
        private int pageNumber;       // 현재 페이지 번호 (1부터 시작)
        private int pageSize;         // 페이지 당 데이터 수
        private long totalElements;   // 전체 데이터 개수
        private int totalPages;       // 전체 페이지 수
        private boolean isLast;       // 마지막 페이지 여부

        /**
         * ✨ [수정] 생성자는 클래스 이름과 동일해야 하며, 반환 타입(void)을 가질 수 없습니다.
         * Spring Data JPA의 Page 객체를 이 공통 응답 DTO로 변환하는 생성자.
         */
        public Response(Page<T> page) {
            this.content = page.getContent();
            this.pageNumber = page.getNumber() + 1; // 클라이언트는 1부터 시작하는 페이지 번호를 받음
            this.pageSize = page.getSize();
            this.totalElements = page.getTotalElements();
            this.totalPages = page.getTotalPages();
            this.isLast = page.isLast();
        }

        /**
         * MyBatis 페이징 결과로부터 이 공통 응답 DTO를 생성하는 생성자.
         */
        public Response(List<T> content, PageDto.Request pageRequest, long totalElements) {
            this.content = content;
            this.pageNumber = pageRequest.getPage();
            this.pageSize = pageRequest.getSize();
            this.totalElements = totalElements;
            this.totalPages = (int) Math.ceil((double) totalElements / this.pageSize);
            this.isLast = this.pageNumber >= this.totalPages;
        }
    }
}
