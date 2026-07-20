package com.springboot.erp.modules.inventory.counts.domain;

/**
 * ENT-045 StockOpening lifecycle (reference app.inventory.constants.StockOpeningStatus).
 *
 * <p>Persisted via {@link StockOpeningStatusConverter} as the wire value
 * ("Draft" / "Posted") to satisfy {@code ck_stock_opening_status}.
 *
 * <ul>
 *   <li>{@code DRAFT} — operator captured the row; editable / deletable.</li>
 *   <li>{@code POSTED} — ledger + journal writes done; row is immutable.</li>
 * </ul>
 */
public enum StockOpeningStatus {
    DRAFT("Draft"),
    POSTED("Posted");

    private final String wire;

    StockOpeningStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static StockOpeningStatus fromWire(String value) {
        for (StockOpeningStatus s : values()) {
            if (s.wire.equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown stock opening status: " + value);
    }
}
