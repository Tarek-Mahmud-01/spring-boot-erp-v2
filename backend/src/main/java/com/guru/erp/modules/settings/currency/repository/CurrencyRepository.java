package com.guru.erp.modules.settings.currency.repository;

import com.guru.erp.modules.settings.currency.domain.Currency;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Currency}. The {@code @SQLRestriction} on
 * {@link com.guru.erp.platform.entity.BaseEntity} already excludes soft-deleted
 * rows from every query below.
 */
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    Optional<Currency> findByPublicId(String publicId);

    boolean existsByCode(String code);

    Optional<Currency> findByIsDefaultTrue();

    /**
     * Non-deleted currencies ordered by code, optionally filtered by a
     * case-insensitive partial match across code/name/short_name/country and by
     * active flag. {@code active} null = both; the {@code q} null-guard is
     * handled with a coalesced LIKE pattern.
     */
    @Query("""
        select c from Currency c
        where (:q is null
               or lower(c.code) like lower(concat('%', :q, '%'))
               or lower(c.name) like lower(concat('%', :q, '%'))
               or lower(c.shortName) like lower(concat('%', :q, '%'))
               or lower(c.country) like lower(concat('%', :q, '%')))
          and (:active is null or c.isActive = :active)
        order by c.code
        """)
    Page<Currency> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);
}
