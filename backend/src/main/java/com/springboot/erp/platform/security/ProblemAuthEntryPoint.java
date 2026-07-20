package com.springboot.erp.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Emits RFC-7807 problem+json for authentication (401) and access-denied (403)
 * failures raised inside the security filter chain — before controllers, so the
 * {@code @RestControllerAdvice} can't see them. Keeps the error shape uniform.
 */
@Component
public class ProblemAuthEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;

    public ProblemAuthEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
        throws IOException {
        write(response, request, ErrorCode.UNAUTHENTICATED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       org.springframework.security.access.AccessDeniedException ex) throws IOException {
        write(response, request, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletResponse response, HttpServletRequest request, ErrorCode code) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.status(), code.defaultDetail());
        pd.setTitle(code.status().getReasonPhrase());
        pd.setType(URI.create("https://errors.guru-erp.local/" + code.code()));
        pd.setProperty("code", code.code());
        pd.setProperty("requestId", request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR));
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getWriter(), pd);
    }
}
