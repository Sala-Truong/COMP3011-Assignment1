package com.sala.cassette.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sala.cassette.service.UsageStatsService;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    // MockMvc lets us test the HTTP layer without starting the full application.
    @Autowired
    private MockMvc mockMvc;

    // This service is stubbed so the controller can be tested in isolation.
    @MockitoBean
    private UsageStatsService usageStatsService;

    // The endpoint should return the current token totals in the expected JSON format.
    @Test
    void shouldReturnGlobalStats() throws Exception {
        when(usageStatsService.getInputTokens())
            .thenReturn(25L);

        when(usageStatsService.getOutputTokens())
            .thenReturn(10L);

        mockMvc.perform(get("/api/v1/global/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inputTokens").value(25))
            .andExpect(jsonPath("$.outputTokens").value(10));
    }
}