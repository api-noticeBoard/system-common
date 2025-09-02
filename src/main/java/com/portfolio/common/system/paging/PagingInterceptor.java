package com.portfolio.common.system.paging;

import lombok.extern.slf4j.Slf4j;
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
import java.util.Map;

@Slf4j
@Intercepts({@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class PagingInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object parameterObject = invocation.getArgs()[1];
        PageDto.Request pageRequest = findPageRequest(parameterObject);

        if (pageRequest == null) {
            return invocation.proceed(); // 페이징 객체가 없으면 통과
        }

        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Executor executor = (Executor) invocation.getTarget();
        BoundSql boundSql = ms.getBoundSql(parameterObject);
        String originalSql = boundSql.getSql();

        // 1. 전체 카운트 쿼리 실행
        Long totalCount = executeCountQuery(executor, ms, parameterObject);
        pageRequest.setTotalCount(totalCount); // ✨ 파라미터 객체에 직접 count 설정

        if (totalCount == 0) {
            return List.of(); // 결과가 없으면 목록 조회 없이 빈 리스트 반환
        }

        // 2. 페이징 쿼리 생성 및 실행
        String pagingSql = generatePagingSql(originalSql, pageRequest);
        BoundSql pagingBoundSql = new BoundSql(ms.getConfiguration(), pagingSql, boundSql.getParameterMappings(), parameterObject);

        return executor.query(ms, parameterObject, RowBounds.DEFAULT, (ResultHandler) invocation.getArgs()[3], null, pagingBoundSql);
    }

    private Long executeCountQuery(Executor executor, MappedStatement ms, Object parameter) throws Exception {
        String originalSql = ms.getBoundSql(parameter).getSql();
        String countSql = generateCountSql(originalSql);

        MappedStatement countMs = new MappedStatement.Builder(ms.getConfiguration(), ms.getId() + "_count", ms.getSqlSource(), ms.getSqlCommandType())
                .resultMaps(List.of(new org.apache.ibatis.mapping.ResultMap.Builder(ms.getConfiguration(), "countResult", Long.class, List.of()).build()))
                .build();

        List<Object> countResult = executor.query(countMs, parameter, RowBounds.DEFAULT, null);
        return (Long) countResult.get(0);
    }

    private PageDto.Request findPageRequest(Object parameterObject) {
        if (parameterObject instanceof PageDto.Request) {
            return (PageDto.Request) parameterObject;
        } else if (parameterObject instanceof Map) {
            return ((Map<?, ?>) parameterObject).values().stream()
                    .filter(PageDto.Request.class::isInstance)
                    .map(PageDto.Request.class::cast)
                    .findFirst().orElse(null);
        }
        return null;
    }

    private String generateCountSql(String originalSql) {
        String countSql = originalSql.replaceAll("(?i)order\\s+by[\\s\\S]+", "");
        int fromIndex = countSql.toLowerCase().indexOf("from");
        return "SELECT count(*) " + countSql.substring(fromIndex);
    }

    private String generatePagingSql(String originalSql, PageDto.Request pageRequest) {
        return originalSql + " LIMIT " + pageRequest.getSize() + " OFFSET " + pageRequest.getOffset();
    }
}