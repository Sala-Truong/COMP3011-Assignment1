package com.sala.cassette.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;

import com.sala.cassette.model.Recording;

@RestController
@RequestMapping("/api/recordings")
public class RecordingController {
    private List<Recording> recordings = new ArrayList<>();
    private long nextId = 3;

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
    @GetMapping("/{id}")
    public Recording getRecordingById(@PathVariable Long id) {

        for (Recording recording : recordings) {
            if (recording.getId().equals(id)) {
                return recording;
            }
        }

        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Recording not found"
        );
    }
    @PostMapping
    public Recording addRecording(@RequestBody Recording recording) {
        recording.setId(nextId++);
        recordings.add(recording);
        return recording;
    }
    @DeleteMapping("/{id}")
    public void deleteRecording(@PathVariable Long id) {

        boolean removed = recordings.removeIf(
            recording -> recording.getId().equals(id)
        );

        if (!removed) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Recording not found"
            );
        }
    }
    @PutMapping("/{id}")
    public Recording updateRecording(
            @PathVariable Long id,
            @RequestBody Recording updatedRecording) {

        for (Recording recording : recordings) {

            if (recording.getId().equals(id)) {

                if (updatedRecording.getName() != null) {
                    recording.setName(updatedRecording.getName());
                }

                if (updatedRecording.getTranscript() != null) {
                    recording.setTranscript(updatedRecording.getTranscript());
                }

                return recording;
            }
        }

        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Recording not found"
        );
    }
}