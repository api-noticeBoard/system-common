package com.portfolio.common.system.paging;

/**
 * ThreadLocal을 사용하여 현재 요청 스레드의 페이징 정보를 관리하는 클래스.
 * 서비스 계층에서 페이징을 시작하고, MyBatis 인터셉터에서 이 정보를 읽어 사용합니다.
 */
public class PagingContext {

    private static final ThreadLocal<PageDto.Request> PAGE_REQUEST_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Long> TOTAL_COUNT_HOLDER = new ThreadLocal<>();

    // 서비스에서 페이징 시작을 알리기 위해 호출
    public static void setPageRequest(PageDto.Request pageRequest) {
        PAGE_REQUEST_HOLDER.set(pageRequest);
    }
    public static PageDto.Request getPageRequest() {
        return PAGE_REQUEST_HOLDER.get();
    }
    // 인터셉터가 계산한 totalCount를 저장하기 위해 호출
    public static void setTotalCount(Long totalCount) {
        TOTAL_COUNT_HOLDER.set(totalCount);
    }
    // 서비스에서 totalCount를 가져오기 위해 호출
    public static Long getTotalCount() {
        return TOTAL_COUNT_HOLDER.get();
    }
    // 서비스에서 작업 완료 후 반드시 호출하여 ThreadLocal 정리
    public static void clear() {
        PAGE_REQUEST_HOLDER.remove();
        TOTAL_COUNT_HOLDER.remove();
    }
}
