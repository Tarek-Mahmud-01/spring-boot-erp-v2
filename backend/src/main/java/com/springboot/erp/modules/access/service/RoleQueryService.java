package com.springboot.erp.modules.access.service;

import com.springboot.erp.modules.access.domain.Permission;
import com.springboot.erp.modules.access.domain.Role;
import com.springboot.erp.modules.access.dto.RoleDtos.RoleRow;
import com.springboot.erp.modules.access.repository.RoleRepository;
import com.springboot.erp.platform.web.PageResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side use-cases for roles (ARCHITECTURE.md §3.2 — server-driven lists).
 * Returns a {@link PageResponse} of joined DTOs; permissions are fetched via an
 * entity graph in the repository to avoid N+1.
 */
@Service
public class RoleQueryService {

    private final RoleRepository roleRepository;

    public RoleQueryService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<RoleRow> list(Pageable pageable) {
        return PageResponse.of(roleRepository.findAllWithPermissions(pageable), this::toRow);
    }

    private RoleRow toRow(Role role) {
        List<String> permissionCodes = role.getPermissions().stream()
            .map(Permission::getCode)
            .sorted()
            .toList();
        return new RoleRow(
            role.getPublicId(),
            role.getCode(),
            role.getName(),
            role.getDescription(),
            role.isSystem(),
            permissionCodes,
            role.getCreatedAt());
    }
}
