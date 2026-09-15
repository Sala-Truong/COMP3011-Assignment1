package com.sala.cassette.controller;

import java.time.Duration;
import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final Instant serverStart = Instant.now();

    @GetMapping("/api/health")
    public String health() {
        return "Cassette backend is running!";
    }

    @GetMapping("/api/v1/admin/uptime")
    public UptimeResponse getUptime() {

        Instant now = Instant.now();

        double uptimeSeconds =
                Duration.between(serverStart, now).toNanos()
                / 1_000_000_000.0;

        return new UptimeResponse(
                serverStart.toString(),
                now.toString(),
                uptimeSeconds
        );
    }

    public record UptimeResponse(
            String utcServerStart,
            String utcNow,
            double serverUptimeSeconds
    ) {
    }
}