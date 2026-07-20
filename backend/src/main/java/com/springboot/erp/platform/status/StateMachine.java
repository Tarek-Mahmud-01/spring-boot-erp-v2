package com.springboot.erp.platform.status;

import com.springboot.erp.platform.error.DomainException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Generic, reusable status state machine (ARCHITECTURE.md §2 — status machine).
 * Each domain concept (Invoice, PurchaseOrder, …) declares its allowed
 * transitions once with a status enum; {@link #transition} enforces them,
 * throwing an {@code ILLEGAL_STATE_TRANSITION} DomainException on a bad move.
 *
 * @param <S> the concept's status enum
 */
public final class StateMachine<S extends Enum<S>> {

    private final Map<S, Set<S>> allowed;

    private StateMachine(Map<S, Set<S>> allowed) {
        this.allowed = allowed;
    }

    public static <S extends Enum<S>> Builder<S> builder(Class<S> type) {
        return new Builder<>(type);
    }

    public boolean canTransition(S from, S to) {
        return allowed.getOrDefault(from, Set.of()).contains(to);
    }

    /** Validate a move; returns {@code to} if legal, else throws. */
    public S transition(S from, S to) {
        if (!canTransition(from, to)) {
            throw DomainException.illegalTransition(from.name(), to.name());
        }
        return to;
    }

    public static final class Builder<S extends Enum<S>> {
        private final Map<S, Set<S>> allowed;

        private Builder(Class<S> type) {
            this.allowed = new EnumMap<>(type);
        }

        /** Declare {@code from} may move to any of {@code targets}. */
        @SafeVarargs
        public final Builder<S> allow(S from, S... targets) {
            allowed.put(from, Set.of(targets));
            return this;
        }

        public StateMachine<S> build() {
            return new StateMachine<>(Map.copyOf(allowed));
        }
    }
}
