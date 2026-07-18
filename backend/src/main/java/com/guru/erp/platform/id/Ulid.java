package com.guru.erp.platform.id;

import com.github.f4b6a3.ulid.UlidCreator;

/**
 * Application-side ULID generation. Every entity carries a {@code char(26)}
 * public id (ARCHITECTURE.md §2 invariants) that is stable, sortable by
 * creation time, and safe to expose in URLs — unlike the internal bigint FK.
 */
public final class Ulid {

    private Ulid() {
    }

    /** A new monotonic ULID as a 26-char Crockford base32 string. */
    public static String next() {
        return UlidCreator.getMonotonicUlid().toString();
    }
}
