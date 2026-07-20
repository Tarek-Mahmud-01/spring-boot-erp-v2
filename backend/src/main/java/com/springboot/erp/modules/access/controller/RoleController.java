package com.springboot.erp.modules.access.controller;

import com.springboot.erp.modules.access.dto.RoleDtos.RoleRow;
import com.springboot.erp.modules.access.service.RoleQueryService;
import com.springboot.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Roles endpoints (ARCHITECTURE.md §2 — thin controller, @PreAuthorize on every
 * endpoint). Server-driven pagination via Spring's Pageable (?page&size&sort).
 */
@RestController
@RequestMapping("/api/access/roles")
public class RoleController {

    private final RoleQueryService roleQueryService;

    public RoleController(RoleQueryService roleQueryService) {
        this.roleQueryService = roleQueryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('access.role.read')")
    public PageResponse<RoleRow> list(@PageableDefault(size = 20) Pageable pageable) {
        return roleQueryService.list(pageable);
    }
}
