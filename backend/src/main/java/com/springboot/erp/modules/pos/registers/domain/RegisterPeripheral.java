package com.springboot.erp.modules.pos.registers.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-060a RegisterPeripheral — bound hardware (printer / scanner / drawer) per
 * Register (US-005.1 / FR-25.2..25.8). Same-slice child of {@link Register};
 * real FK + cascade delete.
 *
 * <p>{@code config} is a free-form JSON document (connection-specific settings:
 * {@code address}/{@code port} for NETWORK, {@code device_path} for USB,
 * {@code mac} for BLUETOOTH, {@code via_printer}/{@code printer_id} for a
 * drawer fired through a bound printer). Constraints reproduced in V50: unique
 * {@code (register_id, type)} — one bound peripheral of each type per register
 * (re-binding replaces it in place), type check, connection check.
 */
@Entity
@Table(
    name = "register_peripherals",
    uniqueConstraints = @UniqueConstraint(name = "uq_register_peripherals_type", columnNames = {"register_id", "type"}))
public class RegisterPeripheral extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "register_id", nullable = false)
    private Register register;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PeripheralType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection", nullable = false, length = 16)
    private PeripheralConnection connection;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> config = Map.of();

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public RegisterPeripheral() {
    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        this.register = register;
    }

    public PeripheralType getType() {
        return type;
    }

    public void setType(PeripheralType type) {
        this.type = type;
    }

    public PeripheralConnection getConnection() {
        return connection;
    }

    public void setConnection(PeripheralConnection connection) {
        this.connection = connection;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config == null ? Map.of() : config;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
