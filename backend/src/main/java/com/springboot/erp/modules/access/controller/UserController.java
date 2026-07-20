package com.springboot.erp.modules.access.controller;

import com.springboot.erp.modules.access.dto.UserDtos.UserRow;
import com.springboot.erp.modules.access.service.UserQueryService;
import com.springboot.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Users endpoints (ARCHITECTURE.md §2 — thin controller, @PreAuthorize on every
 * endpoint). Server-driven pagination via Spring's Pageable (?page&size&sort).
 */
@RestController
@RequestMapping("/api/access/users")
public class UserController {

    private final UserQueryService userQueryService;

    public UserController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('access.user.read')")
    public PageResponse<UserRow> list(@PageableDefault(size = 20) Pageable pageable) {
        return userQueryService.list(pageable);
    }
}
