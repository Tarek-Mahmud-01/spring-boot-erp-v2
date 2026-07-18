package com.guru.erp.modules.settings.location.domain;

import java.util.Optional;

/** ENT-002 price display mode — whether displayed prices include or exclude tax. */
public enum PriceDisplayMode {
    INCLUSIVE,
    EXCLUSIVE;

    public String value() {
        return name();
    }

    public static Optional<PriceDisplayMode> from(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        for (PriceDisplayMode m : values()) {
            if (m.name().equals(raw)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }
}
