package com.developer.test.config;

import com.developer.test.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("access");

    private final MetricsService metrics;

    public RequestLoggingFilter(MetricsService metrics) {
        this.metrics = metrics;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();
            String method = request.getMethod();
            String path = request.getRequestURI();
            String query = request.getQueryString();
            String fullPath = query == null ? path : path + "?" + query;
            String ip = clientIp(request);

            metrics.record(method, path, status, duration);

            if (status >= 500) {
                log.error("{} {} -> {} ({} ms) ip={}", method, fullPath, status, duration, ip);
            } else if (status >= 400) {
                log.warn("{} {} -> {} ({} ms) ip={}", method, fullPath, status, duration, ip);
            } else {
                log.info("{} {} -> {} ({} ms) ip={}", method, fullPath, status, duration, ip);
            }
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
