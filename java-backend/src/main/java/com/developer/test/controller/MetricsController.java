package com.developer.test.controller;

import com.developer.test.service.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class MetricsController {

    private final MetricsService metrics;

    public MetricsController(MetricsService metrics) {
        this.metrics = metrics;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(metrics.snapshot());
    }
}
