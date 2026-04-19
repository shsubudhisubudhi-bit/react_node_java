package com.developer.test.config;

import com.developer.test.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
@Order(20)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);

    private final boolean enabled;
    private final String headerName;
    private final Set<String> validKeys;
    private final ObjectMapper mapper = new ObjectMapper();

    public ApiKeyAuthFilter(
            @Value("${auth.enabled:false}") boolean enabled,
            @Value("${auth.header-name:X-API-Key}") String headerName,
            @Value("${auth.api-keys:}") String keysCsv) {
        this.enabled = enabled;
        this.headerName = headerName;
        Set<String> keys = new HashSet<>();
        if (keysCsv != null && !keysCsv.trim().isEmpty()) {
            for (String k : keysCsv.split(",")) {
                String trimmed = k.trim();
                if (!trimmed.isEmpty()) {
                    keys.add(trimmed);
                }
            }
        }
        this.validKeys = Collections.unmodifiableSet(keys);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getHeader(headerName);
        if (key == null || key.isEmpty() || !validKeys.contains(key)) {
            log.warn("Unauthorized request path={} ip={}", request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(),
                    new ErrorResponse("Unauthorized: missing or invalid API key"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
