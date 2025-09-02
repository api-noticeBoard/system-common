package com.portfolio.common.system.paging;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

/**
 * MyBatis 쿼리 실행을 가로채서 페이징 처리를 자동으로 수행하는 인터셉터.
 */
@Slf4j
@Intercepts({@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class PagingInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // PagingContext에 페이징 정보가 설정되어 있는지 확인
        PageDto.Request pageRequest = PagingContext.getPageRequest();
        if (pageRequest == null) {
            // 페이징 정보가 없으면 원래 로직 그대로 실행
            return invocation.proceed();
        }

        log.debug("MyBatis Paging Interceptor started.");

        // --- 1. 원본 쿼리 정보 가져오기 ---
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];
        Executor executor = (Executor) invocation.getTarget();

        BoundSql boundSql = ms.getBoundSql(parameter);
        String originalSql = boundSql.getSql();

        try {
            // --- 2. 전체 카운트 쿼리 실행 ---
            String countSql = generateCountSql(originalSql);
            // 카운트 쿼리를 위한 새로운 MappedStatement 생성
            MappedStatement countMs = new MappedStatement.Builder(ms.getConfiguration(), ms.getId() + "_count", ms.getSqlSource(), ms.getSqlCommandType())
                    .resultMaps(List.of(new org.apache.ibatis.mapping.ResultMap.Builder(ms.getConfiguration(), "countResult", Long.class, List.of()).build()))
                    .build();

            List<Object> countResult = executor.query(countMs, parameter, RowBounds.DEFAULT, null, null, ms.getBoundSql(parameter));
            Long totalCount = (Long) countResult.get(0);
            PagingContext.setTotalCount(totalCount);

            // 전체 카운트가 0이면, 목록 조회는 의미 없으므로 빈 리스트 반환
            if (totalCount == 0) {
                return List.of();
            }

            // --- 3. 페이징 쿼리 실행 ---
            String pagingSql = generatePagingSql(originalSql, pageRequest);
            // 페이징 쿼리를 위한 BoundSql을 새로 생성
            BoundSql pagingBoundSql = new BoundSql(ms.getConfiguration(), pagingSql, boundSql.getParameterMappings(), parameter);

            // 원본 쿼리 실행을 가로채서, 페이징 쿼리를 대신 실행
            CacheKey cacheKey = executor.createCacheKey(ms, parameter, RowBounds.DEFAULT, pagingBoundSql);
            return executor.query(ms, parameter, RowBounds.DEFAULT, (ResultHandler) args[3], cacheKey, pagingBoundSql);

        } finally {
            // 중요: 인터셉터 작업이 끝나면 반드시 ThreadLocal 정보를 제거
            log.debug("MyBatis Paging Interceptor finished.");
            // PagingContext.clear(); // 서비스 계층에서 명시적으로 호출하는 것이 더 안전
        }
    }

    // 원본 SQL을 카운트 쿼리로 변환하는 헬퍼 메서드 (간단한 버전)
    private String generateCountSql(String originalSql) {
//        return "SELECT count(*) FROM (" + originalSql + ") AS total";
        // 1. 원본 SQL에서 ORDER BY 절을 제거합니다.
        String countSql = originalSql.replaceAll("(?i)order\\s+by[\\s\\S]+", "");

        // 2. 원본 SQL의 SELECT ... FROM 부분을 SELECT count(*) FROM 으로 교체합니다.
        //    JOIN으로 인해 카운트가 부풀려지는 것을 막기 위해, 기준 테이블의 PK를 카운트하는 것이 더 정확합니다.
        //    (예: "SELECT count(p.id) FROM post p LEFT JOIN ...")
        //    여기서는 간단하게 FROM 앞부분을 잘라내는 방식을 사용합니다.
        int fromIndex = countSql.toLowerCase().indexOf("from");
        countSql = "SELECT count(*) " + countSql.substring(fromIndex);

        return countSql;
    }

    // 원본 SQL을 페이징 쿼리로 변환하는 헬퍼 메서드 (MySQL/H2 기준)
    private String generatePagingSql(String originalSql, PageDto.Request pageRequest) {
        return originalSql + " LIMIT " + pageRequest.getSize() + " OFFSET " + pageRequest.getOffset();
    }
}
