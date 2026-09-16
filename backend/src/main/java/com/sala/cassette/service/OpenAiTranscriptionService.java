package com.sala.cassette.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OpenAiTranscriptionService {

    private final String apiKey;
    private final RestClient restClient;
    private final UsageStatsService usageStatsService;
    private static final Logger logger = LoggerFactory.getLogger(OpenAiTranscriptionService.class);

    public OpenAiTranscriptionService(
        @Value("${openai.api.key}") String apiKey,
        UsageStatsService usageStatsService) {

        this.apiKey = apiKey;
        this.usageStatsService = usageStatsService;

        // This client is configured once and reused for every transcription request.
        this.restClient = RestClient.builder()
            .baseUrl("https://api.openai.com")
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
    }

    // This method sends the uploaded audio to OpenAI and returns the transcript.
    public String transcribe(MultipartFile audio) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("model", "gpt-4o-mini-transcribe");
        body.add("file", audio.getResource());

        logger.debug("Sending audio to OpenAI transcription service");

        TranscriptionResponse response = restClient.post()
            .uri("/v1/audio/transcriptions")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(TranscriptionResponse.class);

        if (response == null || response.text() == null) {
            throw new IOException("OpenAI returned no transcription");
        }

        logger.debug("OpenAI transcription response received");

        // After a successful call, we record how many input and output tokens were used.
        if (response.usage() != null) {
            usageStatsService.addUsage(
                response.usage().input_tokens(),
                response.usage().output_tokens());

            logger.debug(
                "Transcription token usage: input={}, output={}",
                response.usage().input_tokens(),
                response.usage().output_tokens());
        }

        return response.text();
    }

    private record TranscriptionResponse(
        String text,
        Usage usage) {
    }

    private record Usage(
        long input_tokens,
        long output_tokens,
        long total_tokens) {
    }
}