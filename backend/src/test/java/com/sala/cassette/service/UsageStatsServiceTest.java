package com.sala.cassette.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class UsageStatsServiceTest {

    // The service should start empty before any request is recorded.
    @Test
    void shouldStartWithZeroUsage() {
        UsageStatsService service = new UsageStatsService();
        assertEquals(0, service.getInputTokens());
        assertEquals(0, service.getOutputTokens());
    }

    // Each update should add to the running totals rather than replace them.
    @Test
    void shouldAccumulateUsage() {
        UsageStatsService service = new UsageStatsService();

        service.addUsage(10, 5);
        service.addUsage(20, 8);

        assertEquals(30, service.getInputTokens());
        assertEquals(13, service.getOutputTokens());
    }

    // This simulates many threads updating the same counters at once.
    // Releasing them together checks whether the totals stay correct under real concurrency.
    @Test
    void shouldHandleConcurrentUpdates() throws Exception {
        UsageStatsService service = new UsageStatsService();
        int tasks = 200;
        int updatesPerTask = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(tasks);

        for (int i = 0; i < tasks; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < updatesPerTask; j++) {
                        service.addUsage(1, 2);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        // Start all tasks together so the contention is realistic.
        startGate.countDown();

        boolean completed = doneGate.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertTrue(completed, "Concurrent updates did not finish in time");

        // If the counter is thread-safe, these totals should match exactly.
        assertEquals(200_000, service.getInputTokens());
        assertEquals(400_000, service.getOutputTokens());
    }
}