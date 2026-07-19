package com.guru.erp.modules.finance.gl.controller;

import com.guru.erp.modules.finance.gl.dto.VoucherTypeDtos.VoucherTypeCreateRequest;
import com.guru.erp.modules.finance.gl.dto.VoucherTypeDtos.VoucherTypeResponse;
import com.guru.erp.modules.finance.gl.dto.VoucherTypeDtos.VoucherTypeUpdateRequest;
import com.guru.erp.modules.finance.gl.service.VoucherTypeService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** VoucherType catalogue CRUD (reference {@code VoucherTypeListView}/{@code DetailView}). */
@RestController
@RequestMapping("/api/finance/gl/voucher-types")
public class VoucherTypeController {

    private final VoucherTypeService service;

    public VoucherTypeController(VoucherTypeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance.voucher_type.read')")
    public PageResponse<VoucherTypeResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('finance.voucher_type.read')")
    public VoucherTypeResponse get(@PathVariable String publicId) {
        return service.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('finance.voucher_type.write')")
    public VoucherTypeResponse create(@Valid @RequestBody VoucherTypeCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('finance.voucher_type.write')")
    public VoucherTypeResponse update(@PathVariable String publicId,
                                      @Valid @RequestBody VoucherTypeUpdateRequest request) {
        return service.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('finance.voucher_type.write')")
    public void delete(@PathVariable String publicId) {
        service.delete(publicId);
    }
}
