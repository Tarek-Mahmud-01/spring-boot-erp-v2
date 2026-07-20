package com.springboot.erp.modules.settings.location.domain;

/**
 * ENT-002 lifecycle status. New locations start {@link #ACTIVE}; the deactivate
 * action flips to {@link #INACTIVE}. The wire value is the lower-case form.
 */
public enum LocationStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    LocationStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
