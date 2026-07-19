package com.guru.erp.modules.pos.registers.repository;

import com.guru.erp.modules.pos.registers.domain.PeripheralType;
import com.guru.erp.modules.pos.registers.domain.RegisterPeripheral;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link RegisterPeripheral}. */
public interface RegisterPeripheralRepository extends JpaRepository<RegisterPeripheral, Long> {

    Optional<RegisterPeripheral> findByPublicId(String publicId);

    Optional<RegisterPeripheral> findByRegisterIdAndType(Long registerId, PeripheralType type);

    Optional<RegisterPeripheral> findByRegisterIdAndPublicId(Long registerId, String publicId);
}
