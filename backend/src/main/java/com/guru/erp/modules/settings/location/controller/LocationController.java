package com.guru.erp.modules.settings.location.controller;

import com.guru.erp.modules.settings.location.dto.LocationDtos.LocationCreateRequest;
import com.guru.erp.modules.settings.location.dto.LocationDtos.LocationResponse;
import com.guru.erp.modules.settings.location.dto.LocationDtos.LocationUpdateRequest;
import com.guru.erp.modules.settings.location.service.LocationService;
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
 * ENT-002 Location endpoints (thin controller — @PreAuthorize on every route,
 * all rules in {@link LocationService}). Clients address locations by ULID.
 *
 * <p>Permissions collapse the reference's finer-grained codes into the v2
 * convention: {@code settings.location.read} for reads,
 * {@code settings.location.write} for every mutation.
 */
@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('settings.location.read')")
    public PageResponse<LocationResponse> list(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(companyId, status, type, q, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('settings.location.read')")
    public LocationResponse get(@PathVariable String publicId) {
        return service.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('settings.location.write')")
    public LocationResponse create(@Valid @RequestBody LocationCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('settings.location.write')")
    public LocationResponse update(@PathVariable String publicId,
                                   @Valid @RequestBody LocationUpdateRequest request) {
        return service.update(publicId, request);
    }

    @PostMapping("/{publicId}/deactivate")
    @PreAuthorize("hasAuthority('settings.location.write')")
    public LocationResponse deactivate(@PathVariable String publicId) {
        return service.deactivate(publicId);
    }

    @PostMapping("/{publicId}/activate")
    @PreAuthorize("hasAuthority('settings.location.write')")
    public LocationResponse activate(@PathVariable String publicId) {
        return service.activate(publicId);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('settings.location.write')")
    public void delete(@PathVariable String publicId) {
        service.delete(publicId);
    }
}
