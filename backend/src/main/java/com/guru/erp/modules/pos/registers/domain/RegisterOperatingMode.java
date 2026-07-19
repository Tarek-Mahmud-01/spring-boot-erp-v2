package com.guru.erp.modules.pos.registers.domain;

/**
 * ENT-060.operating_mode (reference {@code app.pos.constants.RegisterOperatingMode}).
 * Persisted UPPERCASE via {@code @Enumerated(STRING)} — matches the reference wire
 * values verbatim, so no converter is needed.
 */
public enum RegisterOperatingMode {
    FULL_SERVICE,
    EXPRESS,
    MANAGER_ONLY
}
