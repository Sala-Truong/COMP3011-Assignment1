package com.sala.cassette.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sala.cassette.service.UsageStatsService;

@RestController
public class StatsController {

    private final UsageStatsService usageStatsService;

    public StatsController(UsageStatsService usageStatsService) {
        this.usageStatsService = usageStatsService;
    }

    @GetMapping("/api/v1/global/stats")
    public GlobalStatsResponse getGlobalStats() {
        return new GlobalStatsResponse(
                usageStatsService.getInputTokens(),
                usageStatsService.getOutputTokens()
        );
    }

    public record GlobalStatsResponse(
            long inputTokens,
            long outputTokens
    ) {
    }
}