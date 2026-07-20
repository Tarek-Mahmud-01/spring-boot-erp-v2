package com.springboot.erp.platform.entity;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing so {@code @CreatedDate}/{@code @LastModifiedDate} and
 * {@code @CreatedBy}/{@code @LastModifiedBy} on {@link BaseEntity} are populated.
 * The actor comes from {@code SecurityContextAuditorAware}; timestamps are UTC.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
public class JpaAuditingConfig {

    /** All persisted timestamps are UTC instants (ARCHITECTURE.md §2). */
    @Bean
    DateTimeProvider utcDateTimeProvider() {
        return () -> Optional.of(Instant.now(Clock.systemUTC()));
    }
}
