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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final long WINDOW_MS = 60_000L;

    private final boolean enabled;
    private final int limit;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentHashMap<String, Window> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${ratelimit.enabled:true}") boolean enabled,
            @Value("${ratelimit.requests-per-minute:120}") int limit) {
        this.enabled = enabled;
        this.limit = limit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !enabled || path.startsWith("/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String key = clientIp(request);
        long now = System.currentTimeMillis();

        Window window = buckets.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= WINDOW_MS) {
                return new Window(now);
            }
            return existing;
        });
        int count = window.count.incrementAndGet();

        if (count > limit) {
            long retryAfter = Math.max(1, (WINDOW_MS - (now - window.windowStart)) / 1000);
            log.warn("Rate limit exceeded for ip={} count={}/{}", key, count, limit);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(),
                    new ErrorResponse("Rate limit exceeded. Try again in "
                            + retryAfter + " seconds."));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger();

        Window(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
