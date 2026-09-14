package com.sala.cassette.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sala.cassette.model.Recording;

@RestController
@RequestMapping("/api/recordings")
public class RecordingController {

    private List<Recording> recordings = new ArrayList<>();

    public RecordingController() {
        recordings.add(
            new Recording(
                1L,
                "Lecture",
                "This is my first lecture transcript."
            )
        );

        recordings.add(
            new Recording(
                2L,
                "Meeting",
                "This is my meeting transcript."
            )
        );
    }

    @GetMapping
    public List<Recording> getRecordings() {
        return recordings;
    }
}