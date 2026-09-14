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

    public OpenAiTranscriptionService(
            @Value("${openai.api.key}") String apiKey) {

        this.apiKey = apiKey;

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

        return response.text();
    }
    private record TranscriptionResponse(String text) {}
}