package com.guru.erp.modules.pos.registers.domain;

/**
 * ENT-060 Register lifecycle (reference {@code app.pos.constants.RegisterStatus}).
 *
 * <p>Persisted lower-case via {@link RegisterStatusConverter} to satisfy the
 * {@code ck_registers_status} check constraint and match the reference model
 * (which stores {@code RegisterStatus.value}, e.g. {@code "active"}).
 */
public enum RegisterStatus {

    ACTIVE("active"),
    INACTIVE("inactive");

    private final String wire;

    RegisterStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static RegisterStatus fromWire(String wire) {
        for (RegisterStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown register status: " + wire);
    }
}
