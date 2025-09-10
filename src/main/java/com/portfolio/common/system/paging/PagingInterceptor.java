package com.portfolio.common.system.paging;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
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
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
// @Intercepts: 이 클래스가 어떤 메서드를 가로챌지(intercept) 정의합니다.
// @Signature: 가로챌 메서드의 시그니처(타입, 이름, 파라미터)를 명시합니다.
//  - type=Executor.class: Executor의 메서드를 감시하겠습니다.
//  - method="query": 그 중에서도 'query'(조회) 작업을 하는 메서드만 가로채겠습니다.
//  - args={...}: 'query' 메서드의 다양한 파라미터 형태들을 모두 감시 대상에 포함합니다.
@Intercepts({@Signature(type = Executor.class, method = "query"
        , args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class PagingInterceptor implements Interceptor {

    // DB 종류를 저장할 변수
    private String databaseType;

    /**
     * 인터셉터가 생성될 때 mybatis-config.xml 이나 Spring 설정에서 넘겨준
     * 프로퍼티(DB dialect 등)를 설정하는 메서드입니다.
     * 여기서는 DB 종류를 파악하기 위해 사용합니다.
     */
    @Override
    public void setProperties(Properties properties) {
        // 이 부분은 필수는 아니지만, 명시적으로 DB 타입을 설정하고 싶을 때 사용 가능
        // 예: <plugin interceptor="..."><property name="databaseType" value="oracle"/></plugin>
    }

    /**
     * 실제로 모든 요청을 가로채서 로직을 수행하는 핵심 메서드입니다.
     * @param invocation 가로챈 원본 메서드(Executor.query)의 모든 정보(메서드 자체, 파라미터 등)를 담고 있는 객체.
     * @return 조작된 쿼리의 실행 결과 또는 원본 쿼리의 실행 결과.
     * @throws Throwable
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        // invocation가 이 메서드로 들어옵니다.
        // invocation.getArgs()[1]은 'query' 메서드의 두 번째 파라미터, 즉 파라미터 객체(parameterObject)입니다.
        Object parameterObject = invocation.getArgs()[1];
        // 파라미터에서 직접 찾는 대신 PagingContext에서 페이징 정보를 가져옵니다.
        PageDto.Request pageRequest = findPageRequest(parameterObject);

        // 페이징 객체가 없으면 원래 로직을 그대로 실행합니다.
        if (pageRequest == null) {
            return invocation.proceed();
        }
        log.debug("MyBatis Paging Interceptor started.");

        // --- 1. 원본 쿼리 정보 가져오기 ---
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];  // 실행할 쿼리 정보 (SELECT, INSERT 등)
        Executor executor = (Executor) invocation.getTarget();           // 실제 쿼리를 실행할 주체 (택배 기사)
        BoundSql boundSql = ms.getBoundSql(parameterObject);             // 파라미터가 적용된 SQL과 정보

        // --- DB 종류 감지 (한 번만 실행) ---
        if (databaseType == null) {
            this.databaseType = getDatabaseType(ms);
        }

        // 2. 전체 카운트 쿼리를 실행합니다.
        Long totalCount = executeCountQuery(executor, ms, parameterObject, boundSql);

        log.info(">>>> Paging Interceptor: Total Count = {}", totalCount);

        // ✨ [핵심] 파라미터로 넘어온 pageRequest 객체에 totalCount를 설정.
        pageRequest.setTotalCount(totalCount);

        // 전체 카운트가 0이면, 목록 조회는 의미 없으므로 빈 리스트를 반환합니다.
        if (totalCount == 0) {
            return List.of();
        }

        // 3. 페이징 쿼리를 생성하고 실행(원본 SQL을 가져와서 '페이징' 추가)
        String originalSql = boundSql.getSql();
        // DB 타입에 맞는 페이징 SQL 생성
        String pagingSql = generatePagingSql(originalSql, pageRequest, databaseType); // LIMIT, OFFSET 추가
        // 변경된 쿼리로 작성
        BoundSql pagingBoundSql = new BoundSql(ms.getConfiguration(), pagingSql, boundSql.getParameterMappings(), parameterObject);

        CacheKey cacheKey = executor.createCacheKey(ms, parameterObject, RowBounds.DEFAULT, pagingBoundSql);
        return executor.query(ms, parameterObject, RowBounds.DEFAULT, (ResultHandler) invocation.getArgs()[3], cacheKey, pagingBoundSql);
    }

    private Long executeCountQuery(Executor executor, MappedStatement ms, Object parameter, BoundSql boundSql) throws Exception {
        String countSql = generateCountSql(boundSql.getSql(), databaseType);

        // ✨ [디버깅 로그] 생성된 COUNT 쿼리 자체를 로그로 출력합니다.
        log.debug(">>>> Paging Interceptor: Generated Count SQL = {}", countSql);

        // ✨ [핵심 수정] count 쿼리를 위한 BoundSql을 원본과 동일한 파라미터로 다시 생성합니다.
        // 이렇게 하면 MyBatis가 동적 쿼리(<if>)를 평가할 때 필요한 파라미터(keyword)를 정확히 찾을 수 있습니다.
        BoundSql countBoundSql = new BoundSql(ms.getConfiguration(), countSql, boundSql.getParameterMappings(), parameter);

        MappedStatement countMs = new MappedStatement.Builder(ms.getConfiguration(), ms.getId() + "_count", ms.getSqlSource(), ms.getSqlCommandType())
                .resultMaps(List.of(new ResultMap.Builder(ms.getConfiguration(), "countResult", Long.class, List.of()).build()))
                .build();

        // ✨ 생성된 countBoundSql을 사용하여 쿼리를 실행합니다.
        // 만약 원본 boundSql을 기준으로 만들어진 기존 캐시 키를 그대로 사용한다면,
        // MyBatis는 1페이지를 조회하는 쿼리와 2페이지를 조회하는 쿼리를 같은 쿼리로 오인하여 반환
        CacheKey countCacheKey = executor.createCacheKey(countMs, parameter, RowBounds.DEFAULT, countBoundSql);
        List<Object> countResult = executor.query(countMs, parameter, RowBounds.DEFAULT, null, countCacheKey, countBoundSql);

        // 결과가 비어있으면 0 반환
        if (countResult == null || countResult.isEmpty()){
            return 0L;
        }
        return (Long) countResult.get(0);
    }

    /**
     * MyBatis 매퍼 메서드로 전달된 파라미터 객체(parameterObject) 안에서
     * 우리가 페이징 기준으로 삼기로 약속한 `PageDto.Request` 타입의 객체를 찾아내는 헬퍼 메서드입니다.
     *
     * MyBatis는 매퍼 메서드의 파라미터 개수에 따라 전달하는 객체의 형태가 달라지기 때문에,
     * 여러 경우의 수를 모두 처리해야 합니다.
     *
     * @param parameterObject MyBatis가 매퍼 메서드에 전달한 파라미터 객체.
     *                        (타입은 단일 객체일 수도, Map일 수도 있습니다.)
     * @return 파라미터 안에서 발견된 `PageDto.Request` 객체. 찾지 못하면 null을 반환합니다.
     */
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

    // totalCount를 계산하는 퀴리조작 메서드
    private String generateCountSql(String originalSql, String dbType) {
        String countSql = originalSql.replaceAll("(?i)order\\s+by[\\s\\S]+", "");
        if ("oracle".equals(dbType)) {
            return "SELECT COUNT(*) FROM (" + countSql + ") count_table";   // oracle문법
        } else{
        return "SELECT COUNT(*) FROM (" + countSql + ") AS count_table"; // h2문법
        }
    }

    // 페이징 정렬 쿼리 조작 메서드
    private String generatePagingSql(String originalSql, PageDto.Request pageRequest, String dbType) {

        StringBuilder sqlBuilder = new StringBuilder(originalSql);

        // 현재 정렬관련 변수가 채워졌는지 확인
        if (StringUtils.hasText(pageRequest.getSortBy())) {
            // 기존 쿼리에 order by 있는지 확인
            if (originalSql.toLowerCase().contains("order by")) {
                log.warn("Original SQL already contains an ORDER BY clause. The sort parameter from PageRequest will be ignored.");
            }else{
                sqlBuilder.append(" ORDER BY ")
                        .append(pageRequest.getSortBy())                // 정렬 컬럼
                        .append(" ")
                        .append(pageRequest.getSortDirection().name()); // 정렬 방향
            }
        }

        // --- 페이징(Limit/Offset) 처리 로직 ---
        if ("oracle".equals(dbType)) {
            // Oracle 12c+ 표준 페이징 문법
            // 파라미터 이름은 BoundSql에 추가할 이름과 맞춰줍니다.
            sqlBuilder.append(" OFFSET ").append(pageRequest.getOffset());
            sqlBuilder.append(" ROWS FETCH NEXT ").append(pageRequest.getSize());
            sqlBuilder.append(" ROWS ONLY");

        } else {
            // H2, MySQL, PostgreSQL 등에서 사용하는 표준 페이징 문법
            sqlBuilder.append(" LIMIT ").append(pageRequest.getSize());
            sqlBuilder.append(" OFFSET ").append(pageRequest.getOffset());
        }

        return sqlBuilder.toString();
    }

    /**
     * MappedStatement로부터 DataSource를 얻어와 DB 종류를 파악하는 헬퍼 메서드
     */
    private String getDatabaseType(MappedStatement mappedStatement) {
        DataSource dataSource = mappedStatement.getConfiguration().getEnvironment().getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            if (databaseProductName.toLowerCase().contains("oracle")) {
                return "oracle";
            } else if (databaseProductName.toLowerCase().contains("h2")) {
                return "h2";
            } else if (databaseProductName.toLowerCase().contains("mysql")) {
                return "mysql";
            }
            // 기타 다른 DB 추가 가능
        } catch (SQLException e) {
            log.error("Could not detect database type", e);
        }
        // 기본값 또는 감지 실패 시
        return "default";
    }
}