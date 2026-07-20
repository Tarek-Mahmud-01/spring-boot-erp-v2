package com.springboot.erp.platform.error;

import com.springboot.erp.platform.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates every exception into RFC-7807 {@code application/problem+json}
 * (ARCHITECTURE.md §2). Each problem carries the stable {@code code}, the
 * request id (for correlation with logs), and any structured properties.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_PREFIX = "https://errors.guru-erp.local/";

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException ex, HttpServletRequest req) {
        return build(ex.errorCode(), ex.getMessage(), ex.properties(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(Map.of(
                "field", fe.getField(),
                "message", fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage()));
        }
        return build(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.defaultDetail(),
            Map.of("errors", fieldErrors), req);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        return build(ErrorCode.OPTIMISTIC_LOCK, ErrorCode.OPTIMISTIC_LOCK.defaultDetail(), Map.of(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultDetail(), Map.of(), req);
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return build(ErrorCode.UNAUTHENTICATED, ErrorCode.UNAUTHENTICATED.defaultDetail(), Map.of(), req);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest req) {
        // Log the full stack for internal errors; never leak internals to the client.
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL, ErrorCode.INTERNAL.defaultDetail(), Map.of(), req);
    }

    private ProblemDetail build(ErrorCode code, String detail, Map<String, Object> props, HttpServletRequest req) {
        HttpStatus status = code.status();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setType(URI.create(TYPE_PREFIX + code.code()));
        pd.setProperty("code", code.code());
        pd.setProperty("requestId", req.getAttribute(RequestIdFilter.REQUEST_ID_ATTR));
        props.forEach(pd::setProperty);
        return pd;
    }
}
