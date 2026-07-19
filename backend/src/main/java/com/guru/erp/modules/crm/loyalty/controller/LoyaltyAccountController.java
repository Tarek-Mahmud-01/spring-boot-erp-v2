package com.guru.erp.modules.crm.loyalty.controller;

import com.guru.erp.modules.crm.loyalty.dto.LoyaltyLedgerDtos.LedgerEntryResponse;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyLedgerDtos.LoyaltyBalanceResponse;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyLedgerDtos.LoyaltyEarnRequest;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyLedgerDtos.LoyaltyRedeemRequest;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyLedgerDtos.LoyaltyRedeemResponse;
import com.guru.erp.modules.crm.loyalty.dto.LoyaltyLedgerDtos.LoyaltyReverseRequest;
import com.guru.erp.modules.crm.loyalty.service.LoyaltyPointsService;
import com.guru.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ENT-070 LoyaltyAccount + ENT-071 LoyaltyLedger endpoints (US-039 / AC-039-1..4)
 * — thin controller, {@code @PreAuthorize} per method. Business rules live in
 * {@link LoyaltyPointsService}.
 */
@RestController
@RequestMapping("/api/crm/loyalty/customers/{customerId}")
public class LoyaltyAccountController {

    private final LoyaltyPointsService service;

    public LoyaltyAccountController(LoyaltyPointsService service) {
        this.service = service;
    }

    @GetMapping("/balance")
    @PreAuthorize("hasAuthority('crm.loyaltyaccount.read')")
    public LoyaltyBalanceResponse getBalance(@PathVariable String customerId) {
        return service.getBalance(customerId);
    }

    @GetMapping("/ledger")
    @PreAuthorize("hasAuthority('crm.loyaltyaccount.read')")
    public PageResponse<LedgerEntryResponse> listLedger(@PathVariable String customerId,
                                                        @PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(service.listLedger(customerId, pageable));
    }

    /** AC-039-1 — credit points for an eligible sale (also accepts the owning company). */
    @PostMapping("/earn")
    @PreAuthorize("hasAuthority('crm.loyaltyaccount.write')")
    public LoyaltyBalanceResponse earn(@PathVariable String customerId,
                                       @RequestParam String companyId,
                                       @Valid @RequestBody LoyaltyEarnRequest request) {
        return service.earn(customerId, companyId, request);
    }

    /** AC-039-2 — spend points for a bill discount (FIFO consumption). */
    @PostMapping("/redeem")
    @PreAuthorize("hasAuthority('crm.loyaltyaccount.write')")
    public LoyaltyRedeemResponse redeem(@PathVariable String customerId,
                                        @Valid @RequestBody LoyaltyRedeemRequest request) {
        return service.redeem(customerId, request);
    }

    /** AC-039-3 / FR-208 — reverse points earned on a (now refunded) sale. */
    @PostMapping("/reverse")
    @PreAuthorize("hasAuthority('crm.loyaltyaccount.write')")
    public LoyaltyBalanceResponse reverse(@PathVariable String customerId,
                                         @Valid @RequestBody LoyaltyReverseRequest request) {
        return service.reverse(customerId, request);
    }

    /** AC-039-4 — sweep expired point lots. Stand-in for the nightly job. */
    @PostMapping("/expire")
    @PreAuthorize("hasAuthority('crm.loyaltyaccount.write')")
    public LoyaltyBalanceResponse expire(@PathVariable String customerId,
                                        @RequestParam(required = false) Instant asOf) {
        return service.expire(customerId, asOf);
    }
}
