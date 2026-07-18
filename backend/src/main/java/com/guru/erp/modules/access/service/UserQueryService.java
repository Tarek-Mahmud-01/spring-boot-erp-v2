package com.guru.erp.modules.access.service;

import com.guru.erp.modules.access.dto.UserDtos.UserRow;
import com.guru.erp.modules.access.mapper.UserMapper;
import com.guru.erp.modules.access.repository.UserRepository;
import com.guru.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side use-cases for users (ARCHITECTURE.md §3.2 — server-driven lists).
 * Returns a {@link PageResponse} of joined DTOs; no N+1 (roles are fetched via
 * an entity graph in the repository).
 */
@Service
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserQueryService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserRow> list(Pageable pageable) {
        return PageResponse.of(userRepository.findAllWithRoles(pageable), userMapper::toRow);
    }
}
