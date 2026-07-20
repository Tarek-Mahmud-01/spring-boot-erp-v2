package com.springboot.erp.modules.product.promotions.dto;

import com.springboot.erp.modules.product.promotions.domain.LifecycleState;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request/response DTOs for the product lifecycle ledger (US-015 / FR-070..074).
 */
public final class LifecycleDtos {

    private LifecycleDtos() {
    }

    /**
     * Record one lifecycle transition for a product. {@code fromState} is the
     * product's current state supplied by the caller (the Product row itself
     * lives on the catalog slice — app-layer resolution, no hard FK). The
     * service validates the move against the FR-070 state machine.
     */
    public record LifecycleTransitionRequest(
        @NotNull @Size(min = 26, max = 26) String productId,
        @NotNull LifecycleState fromState,
        @NotNull LifecycleState toState,
        @Size(max = 500) String reason
    ) {
    }

    public record LifecycleTransitionResponse(
        String id,
        String productId,
        String fromState,
        String toState,
        String reason,
        Instant changedAt,
        String changedBy
    ) {
    }
}
