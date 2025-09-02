package com.portfolio.common.system.paging;

import org.springframework.data.domain.PageRequest;

/**
 * ThreadLocal을 사용하여 현재 요청 스레드의 페이징 정보를 관리하는 클래스.
 * 서비스 계층에서 페이징을 시작하고, MyBatis 인터셉터에서 이 정보를 읽어 사용합니다.
 */
public class PagingContext {

    // ThreadLocal 변수는 각 스레드마다 독립적인 저장 공간을 가집니다.
    private static final ThreadLocal<PageDto.Request> PAGE_REQUEST_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Long> TOTAL_COUNT_HOLDER = new ThreadLocal<>();

    /**
     * 현재 스레드의 페이징 요청 정보를 설정합니다.
     * @param pageRequest 페이징 정보 DTO
     */
    public static void setPageRequest(PageDto.Request pageRequest) {
        PAGE_REQUEST_HOLDER.set(pageRequest);
    }

    /**
     * 현재 스레드의 페이징 요청 정보를 가져옵니다.
     * @return PageRequest DTO
     */
    public static PageDto.Request getPageRequest() {
        return PAGE_REQUEST_HOLDER.get();
    }

    /**
     * 현재 스레드의 전체 데이터 개수(total count)를 설정합니다.
     * 이 값은 PagingInterceptor가 카운트 쿼리를 실행한 후 설정합니다.
     * @param totalCount 전체 데이터 개수
     */
    public static void setTotalCount(Long totalCount) {
        TOTAL_COUNT_HOLDER.set(totalCount);
    }

    /**
     * 현재 스레드의 전체 데이터 개수를 가져옵니다.
     * @return 전체 데이터 개수
     */
    public static Long getTotalCount() {
        return TOTAL_COUNT_HOLDER.get();
    }

    /**
     * 현재 스레드에 저장된 모든 페이징 정보를 반드시 제거합니다.
     * 스레드 풀 환경에서 이전 요청의 정보가 다음 요청에 영향을 주지 않도록 하기 위함입니다.
     */
    public static void clear() {
        PAGE_REQUEST_HOLDER.remove();
        TOTAL_COUNT_HOLDER.remove();
    }
}
