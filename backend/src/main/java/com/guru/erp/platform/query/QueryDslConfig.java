package com.guru.erp.platform.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared {@link JPAQueryFactory} bean for QueryDSL joined/aggregation reads across the codebase
 * (ARCHITECTURE.md — "QueryDSL for join-fetch/projection, no N+1"). Any slice that needs a
 * cross-entity aggregation query (e.g. reporting) injects {@link JPAQueryFactory} directly rather
 * than hand-rolling {@code EntityManager} JPQL.
 */
@Configuration
public class QueryDslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
