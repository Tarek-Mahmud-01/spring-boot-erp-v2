package com.guru.erp.modules.inventory.counts.controller;

import com.guru.erp.modules.inventory.counts.dto.BarcodeRuleDtos.BarcodeRuleCreateRequest;
import com.guru.erp.modules.inventory.counts.dto.BarcodeRuleDtos.BarcodeRuleResponse;
import com.guru.erp.modules.inventory.counts.dto.BarcodeRuleDtos.BarcodeRuleUpdateRequest;
import com.guru.erp.modules.inventory.counts.service.BarcodeRuleService;
import com.guru.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ENT-046 BarcodeNomenclatureRule endpoints (thin controller, @PreAuthorize on
 * every method). Straight list / get / create / update / delete; business rules
 * delegate to {@link BarcodeRuleService}.
 */
@RestController
@RequestMapping("/api/inventory/barcode-rules")
public class BarcodeRuleController {

    private final BarcodeRuleService service;

    public BarcodeRuleController(BarcodeRuleService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory.barcoderule.read')")
    public PageResponse<BarcodeRuleResponse> list(
            @RequestParam(required = false) String companyId,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(companyId, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('inventory.barcoderule.read')")
    public BarcodeRuleResponse get(@PathVariable String publicId) {
        return service.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory.barcoderule.write')")
    public BarcodeRuleResponse create(@Valid @RequestBody BarcodeRuleCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('inventory.barcoderule.write')")
    public BarcodeRuleResponse update(@PathVariable String publicId,
                                      @Valid @RequestBody BarcodeRuleUpdateRequest request) {
        return service.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('inventory.barcoderule.write')")
    public void delete(@PathVariable String publicId) {
        service.delete(publicId);
    }
}
