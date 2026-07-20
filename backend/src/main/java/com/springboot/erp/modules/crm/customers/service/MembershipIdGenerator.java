package com.springboot.erp.modules.crm.customers.service;

import com.springboot.erp.modules.crm.customers.repository.CustomerRepository;
import com.springboot.erp.platform.id.Ulid;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** FR-210 — short, scannable, unique-per-tenant membership id used by POS lookup. */
@Component
public class MembershipIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CustomerRepository customerRepository;

    public MembershipIdGenerator(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public String next() {
        for (int i = 0; i < 20; i++) {
            StringBuilder sb = new StringBuilder("M");
            for (int d = 0; d < 9; d++) {
                sb.append(RANDOM.nextInt(10));
            }
            String candidate = sb.toString();
            if (!customerRepository.existsByMembershipId(candidate)) {
                return candidate;
            }
        }
        // Astronomically unlikely; fall back to a ULID slice.
        return ("M" + Ulid.next()).substring(0, 20);
    }
}
