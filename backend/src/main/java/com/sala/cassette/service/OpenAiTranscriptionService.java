package com.sala.cassette.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenAiTranscriptionService {

    private final String apiKey;

    public OpenAiTranscriptionService(
            @Value("${openai.api.key}") String apiKey) {

        this.apiKey = apiKey;
    }
}