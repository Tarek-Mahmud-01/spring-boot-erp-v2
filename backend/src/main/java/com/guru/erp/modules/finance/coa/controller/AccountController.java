package com.guru.erp.modules.finance.coa.controller;

import com.guru.erp.modules.finance.coa.dto.AccountDtos.AccountCreateRequest;
import com.guru.erp.modules.finance.coa.dto.AccountDtos.AccountResponse;
import com.guru.erp.modules.finance.coa.dto.AccountDtos.AccountUpdateRequest;
import com.guru.erp.modules.finance.coa.dto.AccountDtos.CoaImportRequest;
import com.guru.erp.modules.finance.coa.dto.AccountDtos.CoaImportResponse;
import com.guru.erp.modules.finance.coa.dto.AccountDtos.MoveAccountRequest;
import com.guru.erp.modules.finance.coa.service.AccountService;
import com.guru.erp.modules.finance.coa.service.CoaImportService;
import com.guru.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chart-of-accounts CRUD + tree-move + CSV import (ARCHITECTURE.md §2 — thin
 * controller, {@code @PreAuthorize} on every method, reference
 * {@code views/chart_of_accounts.py} AccountListView / AccountDetailView /
 * AccountImportView). All business logic lives in the services; ULIDs only.
 */
@RestController
@RequestMapping("/api/finance/accounts")
public class AccountController {

    private final AccountService accountService;
    private final CoaImportService importService;

    public AccountController(AccountService accountService, CoaImportService importService) {
        this.accountService = accountService;
        this.importService = importService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance.coa.read')")
    public PageResponse<AccountResponse> list(@RequestParam String companyId,
                                              @RequestParam(required = false) String q,
                                              Pageable pageable) {
        return accountService.list(companyId, q, pageable);
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAuthority('finance.coa.read')")
    public AccountResponse get(@PathVariable String accountId) {
        return accountService.get(accountId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('finance.coa.write')")
    public AccountResponse create(@Valid @RequestBody AccountCreateRequest request) {
        return accountService.create(request);
    }

    @PatchMapping("/{accountId}")
    @PreAuthorize("hasAuthority('finance.coa.write')")
    public AccountResponse update(@PathVariable String accountId, @Valid @RequestBody AccountUpdateRequest request) {
        return accountService.update(accountId, request);
    }

    /** FR-223 — re-parent an account (and its subtree) via the nested-set move algorithm. */
    @PostMapping("/{accountId}/move")
    @PreAuthorize("hasAuthority('finance.coa.write')")
    public AccountResponse moveAccountInTree(@PathVariable String accountId, @Valid @RequestBody MoveAccountRequest request) {
        return accountService.moveAccount(accountId, request);
    }

    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('finance.coa.write')")
    public void delete(@PathVariable String accountId) {
        accountService.delete(accountId);
    }

    /** FR-227 — bulk CSV import (columns: code,name,type,parent_code,posting_type,currency). */
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('finance.coa.import')")
    public CoaImportResponse importCsv(@Valid @RequestBody CoaImportRequest request) {
        return importService.importCsv(request);
    }
}
