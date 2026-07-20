package com.springboot.erp.modules.product.catalog.controller;

import com.springboot.erp.modules.product.catalog.dto.ImageDtos.ImageCreateRequest;
import com.springboot.erp.modules.product.catalog.dto.ImageDtos.ImageResponse;
import com.springboot.erp.modules.product.catalog.service.ImageService;
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

/** ENT-011c ProductImage endpoints (FR-046). */
@RestController
@RequestMapping("/api/products")
public class ImageController {

    private final ImageService service;

    public ImageController(ImageService service) {
        this.service = service;
    }

    @GetMapping("/{productId}/images")
    @PreAuthorize("hasAuthority('product.image.read')")
    public List<ImageResponse> list(@PathVariable String productId) {
        return service.list(productId);
    }

    @PostMapping("/{productId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product.image.write')")
    public ImageResponse add(@PathVariable String productId,
                             @Valid @RequestBody ImageCreateRequest request) {
        return service.add(productId, request);
    }

    @DeleteMapping("/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('product.image.write')")
    public void delete(@PathVariable String imageId) {
        service.delete(imageId);
    }
}
