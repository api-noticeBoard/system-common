package com.portfolio.common.system.paging;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.List;
import java.util.Map;

@Slf4j
@Intercepts({@Signature(type = Executor.class, method = "query"
        , args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class PagingInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 파라미터에서 PageDto.Request 객체를 찾습니다.
        Object parameterObject = invocation.getArgs()[1];
        PageDto.Request pageRequest = findPageRequest(parameterObject);

        // 페이징 객체가 없으면 원래 로직을 그대로 실행합니다.
        if (pageRequest == null) {
            return invocation.proceed();
        }

        log.debug("MyBatis Paging Interceptor started for page {}, size {}.", pageRequest.getPage(), pageRequest.getSize());

        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Executor executor = (Executor) invocation.getTarget();
        BoundSql boundSql = ms.getBoundSql(parameterObject);

        // 2. 전체 카운트 쿼리를 실행합니다.
        Long totalCount = executeCountQuery(executor, ms, parameterObject, boundSql);

        // 3. ✨ [핵심] 파라미터로 넘어온 pageRequest 객체에 totalCount를 직접 설정합니다.
        pageRequest.setTotalCount(totalCount);

        // 전체 카운트가 0이면, 목록 조회는 의미 없으므로 빈 리스트를 반환합니다.
        if (totalCount == 0) {
            return List.of();
        }

        // 4. 페이징 쿼리를 생성하고 실행합니다.
        String originalSql = boundSql.getSql();
        String pagingSql = generatePagingSql(originalSql, pageRequest);
        BoundSql pagingBoundSql = new BoundSql(ms.getConfiguration(), pagingSql, boundSql.getParameterMappings(), parameterObject);

        return executor.query(ms, parameterObject, RowBounds.DEFAULT, (ResultHandler) invocation.getArgs()[3], null, pagingBoundSql);
    }

    private Long executeCountQuery(Executor executor, MappedStatement ms, Object parameter, BoundSql boundSql) throws Exception {
        String countSql = generateCountSql(boundSql.getSql());
        BoundSql countBoundSql = new BoundSql(ms.getConfiguration(), countSql, boundSql.getParameterMappings(), parameter);

        MappedStatement countMs = new MappedStatement.Builder(ms.getConfiguration(), ms.getId() + "_count", ms.getSqlSource(), ms.getSqlCommandType())
                .resultMaps(List.of(new ResultMap.Builder(ms.getConfiguration(), "countResult", Long.class, List.of()).build()))
                .build();

        List<Object> countResult = executor.query(countMs, parameter, RowBounds.DEFAULT, null, null, countBoundSql);
        return (Long) countResult.get(0);
    }

    private PageDto.Request findPageRequest(Object parameterObject) {
        if (parameterObject instanceof PageDto.Request) {
            return (PageDto.Request) parameterObject;
        } else if (parameterObject instanceof Map) {
            return ((Map<?, ?>) parameterObject).values().stream()
                    .filter(PageDto.Request.class::isInstance).map(PageDto.Request.class::cast)
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