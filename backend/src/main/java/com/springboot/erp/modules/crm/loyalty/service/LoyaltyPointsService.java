package com.springboot.erp.modules.crm.loyalty.service;

import com.springboot.erp.modules.crm.loyalty.domain.LoyaltyAccount;
import com.springboot.erp.modules.crm.loyalty.domain.LoyaltyConfig;
import com.springboot.erp.modules.crm.loyalty.domain.LoyaltyLedger;
import com.springboot.erp.modules.crm.loyalty.domain.LoyaltyMovementType;
import com.springboot.erp.modules.crm.loyalty.dto.LoyaltyConfigDtos.LoyaltyTier;
import com.springboot.erp.modules.crm.loyalty.dto.LoyaltyLedgerDtos.LedgerEntryResponse;
import com.springboot.erp.modules.crm.loyalty.dto.LoyaltyLedgerDtos.LoyaltyBalanceResponse;
import com.springboot.erp.modules.crm.loyalty.dto.LoyaltyLedgerDtos.LoyaltyEarnRequest;
import com.springboot.erp.modules.crm.loyalty.dto.LoyaltyLedgerDtos.LoyaltyRedeemRequest;
import com.springboot.erp.modules.crm.loyalty.dto.LoyaltyLedgerDtos.LoyaltyRedeemResponse;
import com.springboot.erp.modules.crm.loyalty.dto.LoyaltyLedgerDtos.LoyaltyReverseRequest;
import com.springboot.erp.modules.crm.loyalty.mapper.LoyaltyMapper;
import com.springboot.erp.modules.crm.loyalty.repository.LoyaltyAccountRepository;
import com.springboot.erp.modules.crm.loyalty.repository.LoyaltyConfigRepository;
import com.springboot.erp.modules.crm.loyalty.repository.LoyaltyLedgerRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.outbox.OutboxPublisher;
import com.springboot.erp.platform.security.CurrentUser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loyalty points mechanics — earn / redeem / reverse / expire (reference
 * {@code earn_points} / {@code redeem_points} / {@code reverse_points} /
 * {@code expire_points}, AC-039-1..4 / FR-205-209). {@link LoyaltyAccount} is
 * created lazily on first activity; every movement writes one immutable
 * {@link LoyaltyLedger} row (append-only, same pattern as {@code StockLedger}).
 */
@Service
public class LoyaltyPointsService {

    static final String AUDIT_ENTITY_ACCOUNT = "loyalty_account";
    private static final long MINOR_UNITS_PER_MAJOR = 100L;
    private static final int DAYS_PER_MONTH = 30;

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyConfigRepository configRepository;
    private final LoyaltyLedgerRepository ledgerRepository;
    private final LoyaltyMapper mapper;
    private final AuditService auditService;
    private final OutboxPublisher outboxPublisher;
    private final CurrentUser currentUser;
    private final Clock clock = Clock.systemUTC();

    public LoyaltyPointsService(LoyaltyAccountRepository accountRepository,
                                LoyaltyConfigRepository configRepository,
                                LoyaltyLedgerRepository ledgerRepository,
                                LoyaltyMapper mapper, AuditService auditService,
                                OutboxPublisher outboxPublisher, CurrentUser currentUser) {
        this.accountRepository = accountRepository;
        this.configRepository = configRepository;
        this.ledgerRepository = ledgerRepository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.outboxPublisher = outboxPublisher;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public LoyaltyBalanceResponse getBalance(String customerId) {
        LoyaltyAccount account = accountRepository.findByCustomerId(customerId).orElse(null);
        return balanceResponse(customerId, account);
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> listLedger(String customerId, Pageable pageable) {
        return ledgerRepository.findByCustomerIdOrderByOccurredAtDescIdDesc(customerId, pageable)
            .map(mapper::toResponse);
    }

    /**
     * AC-039-1 — credit points for an eligible sale and bump rolling spend.
     * {@code companyId} identifies which company's {@link LoyaltyConfig} governs
     * this customer (the caller — CRM customer sub-slice or a POS sale — already
     * knows it; this slice does not hard-call the customer sub-slice to resolve it).
     */
    @Transactional
    public LoyaltyBalanceResponse earn(String customerId, String companyId, LoyaltyEarnRequest req) {
        LoyaltyConfig cfg = requireConfig(companyId);
        LoyaltyAccount account = getOrCreateAccount(customerId, companyId, cfg.getCurrency());

        double multiplier = tierMultiplier(cfg, account);
        long points = pointsForSpend(cfg, req.amount(), multiplier,
            req.categoryIds() != null ? req.categoryIds() : List.of());

        account.setRolling12mSpendAmount(account.getRolling12mSpendAmount() + req.amount());
        account.setLifetimeSpendAmount(account.getLifetimeSpendAmount() + req.amount());
        if (req.currency() != null) {
            account.setSpendCurrency(req.currency().toUpperCase());
        }

        Instant expiresAt = cfg.getExpiryMonths() != null
            ? Instant.now(clock).plus(Duration.ofDays((long) DAYS_PER_MONTH * cfg.getExpiryMonths()))
            : null;
        if (points > 0) {
            LoyaltyLedger row = new LoyaltyLedger();
            row.setCustomerId(customerId);
            row.setType(LoyaltyMovementType.EARN);
            row.setPointsSigned(points);
            row.setRemaining(points);
            row.setOccurredAt(Instant.now(clock));
            row.setExpiresAt(expiresAt);
            row.setSourceTransactionId(req.sourceTransactionId());
            row.setDescription("Earned on sale " + (req.sourceTransactionId() != null ? req.sourceTransactionId() : ""));
            row.setActorUserId(actorId());
            ledgerRepository.save(row);
            account.setPointsBalance(account.getPointsBalance() + points);
        }
        recomputeTier(cfg, account);
        LoyaltyAccount saved = accountRepository.save(account);

        auditPoints(saved, "EARN", points);
        publishPointsMoved(saved, "EARN", points);
        return balanceResponse(customerId, saved);
    }

    /** AC-039-2 — spend points for a bill discount (FIFO consumption). */
    @Transactional
    public LoyaltyRedeemResponse redeem(String customerId, LoyaltyRedeemRequest req) {
        LoyaltyAccount account = accountRepository.findByCustomerId(customerId)
            .orElseThrow(() -> DomainException.notFound("LoyaltyAccount", customerId));
        LoyaltyConfig cfg = requireConfig(account.getCompanyId());

        int minBalance = toInt(cfg.getRedeemRule().get("minBalanceForRedemption"), 0);
        if (account.getPointsBalance() < minBalance) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Balance %d is below the minimum %d required to redeem".formatted(account.getPointsBalance(), minBalance));
        }
        if (req.points() > account.getPointsBalance()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Requested %d points exceeds balance %d".formatted(req.points(), account.getPointsBalance()));
        }

        int perUnit = Math.max(1, toInt(cfg.getRedeemRule().get("pointsPerCurrencyUnit"), 100));
        long discountMajor = req.points() / perUnit;
        long discountMinor = discountMajor * MINOR_UNITS_PER_MAJOR;

        consumeLots(customerId, req.points(), null);
        account.setPointsBalance(account.getPointsBalance() - req.points());

        LoyaltyLedger row = new LoyaltyLedger();
        row.setCustomerId(customerId);
        row.setType(LoyaltyMovementType.REDEEM);
        row.setPointsSigned(-req.points());
        row.setRemaining(0);
        row.setOccurredAt(Instant.now(clock));
        row.setSourceTransactionId(req.sourceTransactionId());
        row.setDescription("Redeemed " + req.points() + " points");
        row.setActorUserId(actorId());
        ledgerRepository.save(row);

        LoyaltyAccount saved = accountRepository.save(account);
        auditPoints(saved, "REDEEM", -req.points());
        publishPointsMoved(saved, "REDEEM", -req.points());
        return new LoyaltyRedeemResponse(customerId, req.points(), discountMinor, saved.getSpendCurrency(),
            balanceResponse(customerId, saved));
    }

    /** AC-039-3 / FR-208 — reverse points earned on a (refunded) sale. */
    @Transactional
    public LoyaltyBalanceResponse reverse(String customerId, LoyaltyReverseRequest req) {
        LoyaltyAccount account = accountRepository.findByCustomerId(customerId).orElse(null);
        if (account == null) {
            return balanceResponse(customerId, null);
        }

        List<LoyaltyLedger> earned = ledgerRepository.findByCustomerIdAndTypeAndSourceTransactionId(
            customerId, LoyaltyMovementType.EARN, req.sourceTransactionId());
        long totalEarned = earned.stream().mapToLong(LoyaltyLedger::getPointsSigned).sum();
        if (totalEarned <= 0) {
            return balanceResponse(customerId, account);
        }

        // Claw back at most the current balance (points already redeemed/expired
        // cannot be reversed twice).
        long toReverse = Math.min(totalEarned, account.getPointsBalance());
        if (toReverse > 0) {
            consumeLots(customerId, toReverse, req.sourceTransactionId());
            account.setPointsBalance(account.getPointsBalance() - toReverse);
        }
        // Mark any remaining lots from this txn as consumed so they can't be reused.
        for (LoyaltyLedger lot : earned) {
            lot.setRemaining(0);
            ledgerRepository.save(lot);
        }

        LoyaltyLedger row = new LoyaltyLedger();
        row.setCustomerId(customerId);
        row.setType(LoyaltyMovementType.REVERSE);
        row.setPointsSigned(-toReverse);
        row.setRemaining(0);
        row.setOccurredAt(Instant.now(clock));
        row.setSourceTransactionId(req.sourceTransactionId());
        row.setDescription("Reversed earn on refund of " + req.sourceTransactionId());
        row.setActorUserId(actorId());
        ledgerRepository.save(row);

        LoyaltyAccount saved = accountRepository.save(account);
        auditPoints(saved, "REVERSE", -toReverse);
        publishPointsMoved(saved, "REVERSE", -toReverse);
        return balanceResponse(customerId, saved);
    }

    /**
     * AC-039-4 — remove points past their expiry as an EXPIRE movement. Stand-in
     * for the nightly job; callable per customer for tests / manual runs.
     */
    @Transactional
    public LoyaltyBalanceResponse expire(String customerId, Instant asOf) {
        LoyaltyAccount account = accountRepository.findByCustomerId(customerId).orElse(null);
        if (account == null) {
            return balanceResponse(customerId, null);
        }
        Instant now = asOf != null ? asOf : Instant.now(clock);

        List<LoyaltyLedger> stale = ledgerRepository.findExpiredLots(customerId, now);
        long expiredTotal = 0;
        for (LoyaltyLedger lot : stale) {
            expiredTotal += lot.getRemaining();
            lot.setRemaining(0);
            ledgerRepository.save(lot);
        }
        if (expiredTotal > 0) {
            account.setPointsBalance(Math.max(0, account.getPointsBalance() - expiredTotal));
            LoyaltyLedger row = new LoyaltyLedger();
            row.setCustomerId(customerId);
            row.setType(LoyaltyMovementType.EXPIRE);
            row.setPointsSigned(-expiredTotal);
            row.setRemaining(0);
            row.setOccurredAt(now);
            row.setDescription("Expired " + expiredTotal + " points");
            row.setActorUserId(actorId());
            ledgerRepository.save(row);

            LoyaltyAccount saved = accountRepository.save(account);
            auditPoints(saved, "EXPIRE", -expiredTotal);
            publishPointsMoved(saved, "EXPIRE", -expiredTotal);
            return balanceResponse(customerId, saved);
        }
        return balanceResponse(customerId, account);
    }

    // --- internals -----------------------------------------------------------

    private LoyaltyAccount getOrCreateAccount(String customerId, String companyId, String fallbackCurrency) {
        return accountRepository.findByCustomerId(customerId).orElseGet(() -> {
            LoyaltyAccount account = new LoyaltyAccount();
            account.setCustomerId(customerId);
            account.setCompanyId(companyId);
            account.setPointsBalance(0);
            account.setLifetimeSpendAmount(0);
            account.setRolling12mSpendAmount(0);
            account.setSpendCurrency(fallbackCurrency);
            return accountRepository.save(account);
        });
    }

    /** FR-206 — pick the highest tier whose threshold the rolling spend meets. */
    private void recomputeTier(LoyaltyConfig cfg, LoyaltyAccount account) {
        if (cfg.getTiers().isEmpty()) {
            account.setTierId(null);
            return;
        }
        String bestId = null;
        long bestThreshold = -1;
        for (Map<String, Object> t : cfg.getTiers()) {
            long threshold = toLong(t.get("minSpendAmount"), 0);
            if (account.getRolling12mSpendAmount() >= threshold && threshold >= bestThreshold) {
                bestThreshold = threshold;
                bestId = (String) t.get("id");
            }
        }
        account.setTierId(bestId);
        account.setTierRecalculatedAt(Instant.now(clock));
    }

    private double tierMultiplier(LoyaltyConfig cfg, LoyaltyAccount account) {
        if (account.getTierId() == null) {
            return 1.0;
        }
        for (Map<String, Object> t : cfg.getTiers()) {
            if (account.getTierId().equals(t.get("id"))) {
                return toDouble(t.get("earnMultiplier"), 1.0);
            }
        }
        return 1.0;
    }

    /** FR-205 — points = floor(eligible_major / currency_unit_per_point * multiplier). */
    private long pointsForSpend(LoyaltyConfig cfg, long amountMinor, double multiplier, List<String> categoryIds) {
        Map<String, Object> earn = cfg.getEarnRule();
        @SuppressWarnings("unchecked")
        List<String> eligibleCategories = earn.get("eligibleCategoryIds") instanceof List<?> l
            ? (List<String>) l : List.of();
        if (!eligibleCategories.isEmpty() && !categoryIds.isEmpty()
            && Set.copyOf(eligibleCategories).stream().noneMatch(categoryIds::contains)) {
            return 0;
        }
        double perPoint = Math.max(1.0, toDouble(earn.get("currencyUnitPerPoint"), 1.0));
        double major = amountMinor / (double) MINOR_UNITS_PER_MAJOR;
        return (long) ((major / perPoint) * multiplier);
    }

    /** Reduce {@code remaining} on EARN lots FIFO (oldest expiry first). */
    private void consumeLots(String customerId, long points, String onlyTransactionId) {
        long remainingToConsume = points;
        for (LoyaltyLedger lot : ledgerRepository.findEarnLotsWithRemaining(customerId, onlyTransactionId)) {
            if (remainingToConsume <= 0) {
                break;
            }
            long take = Math.min(lot.getRemaining(), remainingToConsume);
            lot.setRemaining(lot.getRemaining() - take);
            remainingToConsume -= take;
            ledgerRepository.save(lot);
        }
    }

    private LoyaltyBalanceResponse balanceResponse(String customerId, LoyaltyAccount account) {
        long total = account != null ? account.getPointsBalance() : 0;
        List<LoyaltyLedger> nextLots = account != null
            ? ledgerRepository.findNextExpiringLots(customerId) : List.of();
        LoyaltyLedger nextLot = nextLots.isEmpty() ? null : nextLots.get(0);

        LoyaltyTier tier = null;
        if (account != null && account.getTierId() != null) {
            LoyaltyConfig cfg = configRepository.findByCompanyId(account.getCompanyId()).orElse(null);
            if (cfg != null) {
                for (Map<String, Object> t : cfg.getTiers()) {
                    if (account.getTierId().equals(t.get("id"))) {
                        tier = mapper.toTier(t);
                        break;
                    }
                }
            }
        }
        return new LoyaltyBalanceResponse(
            customerId, total,
            nextLot != null ? nextLot.getExpiresAt() : null,
            nextLot != null ? nextLot.getRemaining() : 0,
            tier);
    }

    private LoyaltyConfig requireConfig(String companyId) {
        return configRepository.findByCompanyId(companyId)
            .orElseThrow(() -> DomainException.notFound("LoyaltyConfig", companyId));
    }

    private void auditPoints(LoyaltyAccount account, String movement, long points) {
        auditService.record(AUDIT_ENTITY_ACCOUNT, account.getPublicId(), AuditAction.UPDATE,
            null, Map.of("movement", movement, "points", points, "balance", account.getPointsBalance()));
    }

    private void publishPointsMoved(LoyaltyAccount account, String movement, long points) {
        outboxPublisher.publish("loyalty_account", account.getPublicId(), "loyalty.points_moved",
            Map.of("customerId", account.getCustomerId(), "movement", movement, "points", points,
                "balance", account.getPointsBalance()));
    }

    private String actorId() {
        return currentUser.optional().map(p -> p.userPublicId()).orElse(null);
    }

    private static double toDouble(Object v, double fallback) {
        return v instanceof Number n ? n.doubleValue() : fallback;
    }

    private static int toInt(Object v, int fallback) {
        return v instanceof Number n ? n.intValue() : fallback;
    }

    private static long toLong(Object v, long fallback) {
        return v instanceof Number n ? n.longValue() : fallback;
    }
}
