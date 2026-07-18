package com.guru.erp.modules.procurement.landed.repository;

import com.guru.erp.modules.procurement.landed.domain.LandedCostPoLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for the landed cost ↔ PurchaseOrder bridge {@link LandedCostPoLink}. */
public interface LandedCostPoLinkRepository extends JpaRepository<LandedCostPoLink, Long> {

    List<LandedCostPoLink> findByLandedCostId(String landedCostId);

    List<LandedCostPoLink> findByPoId(String poId);

    void deleteByLandedCostId(String landedCostId);
}
