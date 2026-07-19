package com.guru.erp.modules.pos.registers.repository;

import com.guru.erp.modules.pos.registers.domain.PosTillMovement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link PosTillMovement}. */
public interface PosTillMovementRepository extends JpaRepository<PosTillMovement, Long> {

    List<PosTillMovement> findBySessionIdOrderByCreatedAtAscIdAsc(Long sessionId);
}
