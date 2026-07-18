package com.guru.erp.modules.settings.location.domain;

/**
 * Postal address stored as a JSONB document on {@link Location}. Mirrors the
 * reference {@code Address} schema: {@code street} (required), {@code city}
 * (required), {@code region} (optional), {@code postcode} (optional) and a
 * 2-letter ISO {@code country} (required).
 *
 * <p>Persisted verbatim as JSON; field-level format validation lives on the
 * request DTO ({@code AddressRequest}).
 */
public record Address(
    String street,
    String city,
    String region,
    String postcode,
    String country
) {
}
