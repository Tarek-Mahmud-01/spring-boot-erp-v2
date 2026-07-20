package com.springboot.erp.modules.pos.registers.service;

import com.springboot.erp.modules.pos.registers.repository.PosTillSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-25.9 / AC-005.1-7 — "has this register ever been used?" guard for hard
 * delete. The reference wires this via a cross-module probe registry (future
 * POS checkout module registers a transaction-history check); here the only
 * same-module history is till sessions, so the guard checks that directly.
 * When the sales/checkout sub-slice is ported it should extend this guard
 * (or register an additional probe) so a register with completed sales is
 * likewise protected.
 */
@Service
public class RegisterHistoryGuard {

    private final PosTillSessionRepository sessionRepository;

    public RegisterHistoryGuard(PosTillSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public boolean hasHistory(Long registerId) {
        return sessionRepository.existsByRegisterId(registerId);
    }
}
