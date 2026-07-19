package com.guru.erp.modules.pos.registers.repository;

import com.guru.erp.modules.pos.registers.domain.PosTillSession;
import com.guru.erp.modules.pos.registers.domain.TillSessionStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link PosTillSession}. */
public interface PosTillSessionRepository extends JpaRepository<PosTillSession, Long> {

    Optional<PosTillSession> findByPublicId(String publicId);

    Optional<PosTillSession> findByRegisterIdAndStatus(Long registerId, TillSessionStatus status);

    boolean existsByRegisterId(Long registerId);

    @Query("""
        select s from PosTillSession s
        where (:registerId is null or s.register.publicId = :registerId)
          and (:status is null or s.status = :status)
        order by s.openedAt desc
        """)
    Page<PosTillSession> search(@Param("registerId") String registerId,
                                @Param("status") TillSessionStatus status, Pageable pageable);
}
