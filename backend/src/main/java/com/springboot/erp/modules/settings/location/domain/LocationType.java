package com.springboot.erp.modules.settings.location.domain;

import java.util.Optional;

/**
 * ENT-002 location type (AC-002-3). Originally {@code STORE / WAREHOUSE / OFFICE};
 * widened in the reference (2026-06) to the full eight retail-standard values the
 * UI offers. A type outside this set is rejected with a validation error.
 */
public enum LocationType {
    STORE,
    WAREHOUSE,
    OFFICE,
    HEAD_OFFICE,
    DISTRIBUTION_CENTER,
    FACTORY,
    OUTLET,
    SHOWROOM;

    /** The wire value equals the enum name (the reference uses upper-case values). */
    public String value() {
        return name();
    }

    /** Parse a wire value, empty if it is not a recognised type. */
    public static Optional<LocationType> from(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        for (LocationType t : values()) {
            if (t.name().equals(raw)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
}
