package com.springboot.erp.modules.product.catalog.dto;

import com.springboot.erp.modules.product.catalog.domain.LifecycleState;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Lifecycle transition request/response for the product state machine (FR-070/074). */
public final class LifecycleDtos {

    private LifecycleDtos() {
    }

    public record LifecycleTransitionRequest(
        @NotNull LifecycleState toState,
        @Size(max = 500) String reason
    ) {
    }
}
