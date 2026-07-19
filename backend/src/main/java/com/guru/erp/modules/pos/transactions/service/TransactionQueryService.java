package com.guru.erp.modules.pos.transactions.service;

import com.guru.erp.modules.pos.transactions.domain.PosTransaction;
import com.guru.erp.modules.pos.transactions.domain.PosTransactionStatus;
import com.guru.erp.modules.pos.transactions.dto.TransactionDtos.PosTransactionResponse;
import com.guru.erp.modules.pos.transactions.mapper.TransactionMapper;
import com.guru.erp.modules.pos.transactions.repository.PosTransactionRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side use-cases for the core POS sale: server-paged/filtered list and single-transaction get. */
@Service
public class TransactionQueryService {

    private final PosTransactionRepository repository;
    private final TransactionMapper mapper;

    public TransactionQueryService(PosTransactionRepository repository, TransactionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<PosTransactionResponse> list(String registerId, String locationId, String status,
                                                      String search, Pageable pageable) {
        String reg = blankToNull(registerId);
        String loc = blankToNull(locationId);
        String q = blankToNull(search);
        PosTransactionStatus st = status == null || status.isBlank()
            ? null : PosTransactionStatus.valueOf(status.trim().toUpperCase());
        return PageResponse.of(repository.search(reg, loc, st, q, pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public PosTransactionResponse get(String publicId) {
        return toResponse(load(publicId));
    }

    private PosTransaction load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PosTransaction", publicId));
    }

    private PosTransactionResponse toResponse(PosTransaction txn) {
        long paid = txn.getTenders().stream().filter(t -> !t.isReversed()).mapToLong(t -> t.getAmountAmount()).sum();
        boolean ageRequired = txn.getLines().stream().anyMatch(l -> l.isRestricted18() || l.isRestricted21());
        return mapper.toResponse(txn, txn.getTotalAmount() - paid, ageRequired);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
