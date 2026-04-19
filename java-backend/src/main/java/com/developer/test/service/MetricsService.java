package com.developer.test.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {

    private final Instant startedAt = Instant.now();
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong totalDurationMs = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicLong> byRoute = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, AtomicLong> byStatus = new ConcurrentHashMap<>();

    public void record(String method, String path, int status, long durationMs) {
        totalRequests.incrementAndGet();
        totalDurationMs.addAndGet(durationMs);
        byRoute.computeIfAbsent(method + " " + path, k -> new AtomicLong()).incrementAndGet();
        byStatus.computeIfAbsent(status, k -> new AtomicLong()).incrementAndGet();
    }

    public Map<String, Object> snapshot() {
        long total = totalRequests.get();
        long totalMs = totalDurationMs.get();

        Map<String, Long> routes = new TreeMap<>();
        byRoute.forEach((k, v) -> routes.put(k, v.get()));

        Map<String, Long> statuses = new TreeMap<>();
        byStatus.forEach((k, v) -> statuses.put(String.valueOf(k), v.get()));

        Map<String, Object> out = new TreeMap<>();
        out.put("startedAt", startedAt.toString());
        out.put("uptimeSeconds", (System.currentTimeMillis() - startedAt.toEpochMilli()) / 1000);
        out.put("totalRequests", total);
        out.put("averageDurationMs", total == 0 ? 0 : (double) totalMs / total);
        out.put("requestsByRoute", routes);
        out.put("requestsByStatus", statuses);
        return out;
    }
}
