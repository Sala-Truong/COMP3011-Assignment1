package com.sala.cassette.service;

import java.io.IOException;

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

    public OpenAiTranscriptionService(
            @Value("${openai.api.key}") String apiKey,
            UsageStatsService usageStatsService) {

        this.apiKey = apiKey;
        this.usageStatsService = usageStatsService;

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader(
                        "Authorization",
                        "Bearer " + apiKey
                )
                .build();
    }

    public String transcribe(MultipartFile audio)
            throws IOException {

        MultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();

        body.add("model", "gpt-4o-mini-transcribe");
        body.add("file", audio.getResource());

        TranscriptionResponse response = restClient.post()
                .uri("/v1/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(TranscriptionResponse.class);

        if (response == null || response.text() == null) {
            throw new IOException("OpenAI returned no transcription");
        }

        if (response.usage() != null) {
            usageStatsService.addUsage(
                    response.usage().input_tokens(),
                    response.usage().output_tokens()
            );
        }

        return response.text();
    }

    private record TranscriptionResponse(
            String text,
            Usage usage
    ) {}

    private record Usage(
            long input_tokens,
            long output_tokens,
            long total_tokens
    ) {}
}