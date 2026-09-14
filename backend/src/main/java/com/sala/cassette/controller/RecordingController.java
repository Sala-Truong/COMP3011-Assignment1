package com.sala.cassette.controller;

import com.sala.cassette.model.Recording;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recordings")

public class RecordingController {

    @GetMapping
    public Recording getRecording() {

        return new Recording(
            1L,
            "My First Cassette",
            "This is a test transcript."
        );
    }
}