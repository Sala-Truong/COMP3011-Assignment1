package com.sala.cassette.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final Instant serverStart = Instant.now();
    private final ConfigurableApplicationContext applicationContext;
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);

    public HealthController(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    // This endpoint prevents duplicate shutdown requests by locking the shutdown flow with an atomic flag.
    @PostMapping("/api/v1/admin/shutdown")
    public ResponseEntity<ShutdownResponse> shutdown() {
        if (!shutdownInProgress.compareAndSet(false, true)) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ShutdownResponse("Graceful shutdown is already in progress."));
        }

        // The application is closed shortly after, giving the server a graceful shutdown window.
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            applicationContext.close();
        });

        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(new ShutdownResponse("Graceful shutdown requested."));
    }

    public record ShutdownResponse(String message) {
    }

    // A simple health check endpoint to confirm the backend is alive.
    @GetMapping("/api/health")
    public String health() {
        return "Cassette backend is running!";
    }

    // This calculates how long the server has been running since startup.
    @GetMapping("/api/v1/admin/uptime")
    public UptimeResponse getUptime() {
        Instant now = Instant.now();

        double uptimeSeconds = Duration.between(serverStart, now).toNanos() / 1_000_000_000.0;

        return new UptimeResponse(
            serverStart.toString(),
            now.toString(),
            uptimeSeconds);
    }

    public record UptimeResponse(
        String utcServerStart,
        String utcNow,
        double serverUptimeSeconds) {
    }
}