package com.springboot.erp.platform.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security (ARCHITECTURE.md §2). No sessions, no CSRF (token in
 * header, not cookie). {@code @EnableMethodSecurity} turns on {@code @PreAuthorize}
 * so every mutating endpoint gates on a permission code. All errors surface as
 * RFC-7807 via {@link ProblemAuthEntryPoint}.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, AuthCookieProperties.class})
public class SecurityConfig {

    /** Endpoints reachable without authentication. */
    private static final String[] PUBLIC = {
        "/api/auth/login",
        "/api/auth/refresh",
        "/actuator/health",
        "/actuator/health/**",
    };

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtAuthenticationFilter jwtFilter,
                                    CsrfCookieFilter csrfCookieFilter,
                                    ProblemAuthEntryPoint problemHandler) throws Exception {
        http
            // Spring's built-in CSRF is disabled; we run our own double-submit
            // cookie check (CsrfCookieFilter) tailored to the cookie-token flow.
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(PUBLIC).permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(problemHandler)
                .accessDeniedHandler(problemHandler))
            // Hardening response headers: block sniffing/clickjacking; HSTS for TLS.
            .headers(headers -> headers
                .contentTypeOptions(cto -> {})
                .frameOptions(fo -> fo.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)))
            // CSRF check must run after the JWT filter (it needs to know whether
            // the request is cookie-authenticated) but before the controller.
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(csrfCookieFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
