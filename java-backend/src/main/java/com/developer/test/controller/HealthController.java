package com.developer.test.controller;

import com.developer.test.dto.HealthResponse;
import com.developer.test.service.DataStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping
@CrossOrigin(origins = "*")
public class HealthController {

    private final DataStore dataStore;
    private final long startedAt = System.currentTimeMillis();

    public HealthController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("ok", "Java backend is running"));
    }

    @GetMapping("/health/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Runtime runtime = Runtime.getRuntime();
        long uptimeMs = System.currentTimeMillis() - startedAt;

        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("usedMb", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        memory.put("freeMb", runtime.freeMemory() / (1024 * 1024));
        memory.put("totalMb", runtime.totalMemory() / (1024 * 1024));
        memory.put("maxMb", runtime.maxMemory() / (1024 * 1024));

        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("version", System.getProperty("java.version"));
        jvm.put("vendor", System.getProperty("java.vendor"));
        jvm.put("processUptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", dataStore.getUserCount());
        data.put("tasks", dataStore.getTaskCount());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("message", "Java backend is running");
        body.put("timestamp", Instant.now().toString());
        body.put("uptimeMs", uptimeMs);
        body.put("memory", memory);
        body.put("jvm", jvm);
        body.put("data", data);
        return ResponseEntity.ok(body);
    }
}
