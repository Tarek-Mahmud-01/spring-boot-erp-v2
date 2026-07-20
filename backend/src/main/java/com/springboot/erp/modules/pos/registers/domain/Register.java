package com.springboot.erp.modules.pos.registers.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-060 Register — a POS lane (terminal) belonging to a Location (US-005.1 /
 * FR-25.1 / FR-25.6 / FR-25.9).
 *
 * <p>Domain columns only; base columns from {@link BaseEntity}. The owning
 * {@code Location} is a cross-slice reference (settings.location) held as a
 * loose ULID {@code char(26)} column — no hard cross-slice FK, per the
 * vertical-slice rule. {@link RegisterPeripheral} IS same-slice, so it keeps a
 * real FK + cascade.
 *
 * <p>Constraints reproduced in V50: unique {@code (location_id, code)}, status
 * check, operating-mode check.
 */
@Entity
@Table(
    name = "registers",
    uniqueConstraints = @UniqueConstraint(name = "uq_registers_location_code", columnNames = {"location_id", "code"}))
public class Register extends BaseEntity {

    /** ULID public id of the Location (cross-slice, resolved app-side). Must be a STORE location. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String locationId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "operating_mode", nullable = false, length = 20)
    private RegisterOperatingMode operatingMode = RegisterOperatingMode.FULL_SERVICE;

    @Convert(converter = RegisterStatusConverter.class)
    @Column(name = "status", nullable = false, length = 16)
    private RegisterStatus status = RegisterStatus.ACTIVE;

    @OneToMany(mappedBy = "register", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegisterPeripheral> peripherals = new ArrayList<>();

    public Register() {
    }

    public void addPeripheral(RegisterPeripheral peripheral) {
        peripheral.setRegister(this);
        peripherals.add(peripheral);
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public RegisterOperatingMode getOperatingMode() {
        return operatingMode;
    }

    public void setOperatingMode(RegisterOperatingMode operatingMode) {
        this.operatingMode = operatingMode;
    }

    public RegisterStatus getStatus() {
        return status;
    }

    public void setStatus(RegisterStatus status) {
        this.status = status;
    }

    public List<RegisterPeripheral> getPeripherals() {
        return peripherals;
    }
}
