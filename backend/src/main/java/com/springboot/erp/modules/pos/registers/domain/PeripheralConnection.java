package com.springboot.erp.modules.pos.registers.domain;

/**
 * ENT-060a.connection (reference {@code app.pos.constants.PeripheralConnection}).
 * Persisted UPPERCASE via {@code @Enumerated(STRING)} — matches the reference
 * wire values verbatim.
 */
public enum PeripheralConnection {
    USB,
    NETWORK,
    BLUETOOTH,
    HID,
    SERIAL
}
