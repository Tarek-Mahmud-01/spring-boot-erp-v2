package com.springboot.erp.modules.pos.registers.repository;

import com.springboot.erp.modules.pos.registers.domain.Register;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Register}. Soft-deleted rows are excluded automatically by the
 * {@code @SQLRestriction} on {@code BaseEntity}.
 */
public interface RegisterRepository extends JpaRepository<Register, Long> {

    Optional<Register> findByPublicId(String publicId);

    boolean existsByLocationIdAndCode(String locationId, String code);

    boolean existsByLocationIdAndCodeAndPublicIdNot(String locationId, String code, String publicId);

    @Query("""
        select r from Register r
        where (:locationId is null or r.locationId = :locationId)
        order by r.code asc
        """)
    Page<Register> search(@Param("locationId") String locationId, Pageable pageable);
}
