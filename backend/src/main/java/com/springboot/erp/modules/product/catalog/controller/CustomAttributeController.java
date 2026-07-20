package com.springboot.erp.modules.product.catalog.controller;

import com.springboot.erp.modules.product.catalog.dto.CustomAttributeDtos.CustomAttributeResponse;
import com.springboot.erp.modules.product.catalog.dto.CustomAttributeDtos.CustomAttributeUpsertRequest;
import com.springboot.erp.modules.product.catalog.service.CustomAttributeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ENT-012 CustomAttributeValue endpoints. Upsert-by-key semantics. */
@RestController
@RequestMapping("/api/products")
public class CustomAttributeController {

    private final CustomAttributeService service;

    public CustomAttributeController(CustomAttributeService service) {
        this.service = service;
    }

    @GetMapping("/{productId}/custom-attributes")
    @PreAuthorize("hasAuthority('product.customattribute.read')")
    public List<CustomAttributeResponse> list(@PathVariable String productId) {
        return service.list(productId);
    }

    @PostMapping("/{productId}/custom-attributes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product.customattribute.write')")
    public CustomAttributeResponse upsert(@PathVariable String productId,
                                          @Valid @RequestBody CustomAttributeUpsertRequest request) {
        return service.upsert(productId, request);
    }

    @DeleteMapping("/custom-attributes/{attributeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('product.customattribute.write')")
    public void delete(@PathVariable String attributeId) {
        service.delete(attributeId);
    }
}
