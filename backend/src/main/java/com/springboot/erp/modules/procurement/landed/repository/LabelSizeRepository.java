package com.springboot.erp.modules.procurement.landed.repository;

import com.springboot.erp.modules.procurement.landed.domain.LabelSize;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for the seeded {@link LabelSize} lookup. */
public interface LabelSizeRepository extends JpaRepository<LabelSize, Long> {

    Optional<LabelSize> findByPublicId(String publicId);

    List<LabelSize> findByActiveTrueOrderBySortOrderAscIdAsc();
}
