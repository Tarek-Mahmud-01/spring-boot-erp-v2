package com.springboot.erp.modules.pos.registers.service;

import com.springboot.erp.modules.access.domain.User;
import com.springboot.erp.modules.access.repository.UserRepository;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-193 manager step-up — verifies a manager's live credentials for an
 * over-threshold till-close variance (mirrors the reference
 * {@code authorize_manager_override}: US-034 FR-178 no-receipt-refund step-up
 * uses the same shape). This is a synchronous authorization *read* (verify now,
 * in the request), not a cross-module side-effect, so it is exempt from the
 * outbox seam — it reads {@code access.User} directly rather than publish/
 * consume, the same way any login flow must hit the live credential store.
 *
 * <p>The approver is always the VERIFIED manager resolved here, never the
 * initiating cashier's own session — the caller must record the returned
 * public id, not {@code actor.id}, so a cashier whose own session happens to
 * hold the required permission cannot self-approve a variance they caused.
 */
@Service
public class ManagerStepUpAuthorizer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ManagerStepUpAuthorizer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Returns the verified manager's ULID public id, or throws FORBIDDEN. */
    @Transactional(readOnly = true)
    public String authorize(String username, String password, String requiredPermission) {
        User user = userRepository.findByUsernameWithAuthorities(username).orElse(null);
        if (user == null || !user.isActive() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new DomainException(ErrorCode.FORBIDDEN, "Manager authorization failed");
        }
        if (!user.permissionCodes().contains(requiredPermission)) {
            throw new DomainException(ErrorCode.FORBIDDEN,
                "Manager does not hold the required permission: " + requiredPermission);
        }
        return user.getPublicId();
    }
}
