package com.springboot.erp.modules.procurement.landed.repository;

import com.springboot.erp.modules.procurement.landed.domain.LandedCostGrnLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for the landed cost ↔ GRN bridge {@link LandedCostGrnLink}. */
public interface LandedCostGrnLinkRepository extends JpaRepository<LandedCostGrnLink, Long> {

    List<LandedCostGrnLink> findByLandedCostId(String landedCostId);

    List<LandedCostGrnLink> findByGrnId(String grnId);

    void deleteByLandedCostId(String landedCostId);
}
