package com.guru.erp.modules.inventory.stock.service;

import com.guru.erp.modules.inventory.stock.domain.InventoryValuationConfig;
import com.guru.erp.modules.inventory.stock.dto.ValuationConfigDtos.ValuationConfigResponse;
import com.guru.erp.modules.inventory.stock.dto.ValuationConfigDtos.ValuationConfigSetRequest;
import com.guru.erp.modules.inventory.stock.mapper.StockMapper;
import com.guru.erp.modules.inventory.stock.repository.InventoryValuationConfigRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read + write use-cases for {@link InventoryValuationConfig} (reference
 * {@code valuation_config} view + {@code get/set_valuation_config}). One config
 * per company: PUT creates it on first call, then updates it. Once locked (first
 * stock movement — AC-021-3) the method can no longer change.
 */
@Service
public class ValuationConfigService {

    private static final String AUDIT_ENTITY = "inventory_valuation_config";

    private final InventoryValuationConfigRepository repository;
    private final StockMapper mapper;
    private final AuditService auditService;

    public ValuationConfigService(InventoryValuationConfigRepository repository, StockMapper mapper,
                                  AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ValuationConfigResponse get(String companyId) {
        return mapper.toResponse(load(companyId));
    }

    @Transactional
    public ValuationConfigResponse set(ValuationConfigSetRequest req) {
        InventoryValuationConfig cfg = repository.findByCompanyId(req.companyId()).orElse(null);
        if (cfg == null) {
            cfg = new InventoryValuationConfig();
            cfg.setCompanyId(req.companyId());
            cfg.setMethod(req.method());
            cfg.setLocked(false);
            InventoryValuationConfig saved = repository.save(cfg);
            ValuationConfigResponse response = mapper.toResponse(saved);
            auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, response);
            return response;
        }

        checkVersion(cfg, req.version());
        // AC-021-3: the method is immutable once the first movement has locked it.
        if (cfg.isLocked()) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Valuation method is locked after the first stock movement.");
        }
        ValuationConfigResponse before = mapper.toResponse(cfg);
        cfg.setMethod(req.method());
        InventoryValuationConfig saved = repository.save(cfg);
        ValuationConfigResponse response = mapper.toResponse(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, response);
        return response;
    }

    private InventoryValuationConfig load(String companyId) {
        return repository.findByCompanyId(companyId)
            .orElseThrow(() -> DomainException.notFound("InventoryValuationConfig", companyId));
    }

    private void checkVersion(InventoryValuationConfig cfg, Long requestVersion) {
        if (requestVersion != null && requestVersion != cfg.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }
}
