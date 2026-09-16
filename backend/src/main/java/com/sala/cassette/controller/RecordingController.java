package com.sala.cassette.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.sala.cassette.model.Recording;
import com.sala.cassette.repository.RecordingRepository;
import com.sala.cassette.service.OpenAiTranscriptionService;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/recordings")
@CrossOrigin(origins = {
    "http://127.0.0.1:5500",
    "http://localhost:5500"
})
public class RecordingController {

    private final RecordingRepository recordingRepository;
    private final JsonMapper objectMapper;
    private final OpenAiTranscriptionService transcriptionService;
    private final Path uploadDirectory = Paths.get("uploads").toAbsolutePath().normalize();

    private static final Logger logger = LoggerFactory.getLogger(RecordingController.class);

    public RecordingController(
        JsonMapper objectMapper,
        RecordingRepository recordingRepository,
        OpenAiTranscriptionService transcriptionService) {

        this.objectMapper = objectMapper;
        this.recordingRepository = recordingRepository;
        this.transcriptionService = transcriptionService;

        // The uploads folder must exist before any audio file is saved.
        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create uploads folder", e);
        }
    }

    // This returns the newest recordings first so the UI shows the most recent items first.
    @GetMapping
    public List<Recording> getRecordings() {
        return recordingRepository.findAllByOrderByIdDesc();
    }

    // Fetching one recording lets the client load the full metadata for that item.
    @GetMapping("/{id}")
    public Recording getRecordingById(@PathVariable Long id) {
        return findRecording(id);
    }

    // This endpoint serves the stored audio file back to the client for playback or download.
    @GetMapping("/{id}/audio")
    public ResponseEntity<Resource> getRecordingAudio(@PathVariable Long id) {
        Recording recording = findRecording(id);

        if (recording.getAudioFilename() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audio not found");
        }

        Path filePath = uploadDirectory.resolve(recording.getAudioFilename());

        if (!Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audio file not found");
        }

        Resource resource = new FileSystemResource(filePath);
        String contentType;

        try {
            contentType = Files.probeContentType(filePath);
        } catch (IOException e) {
            contentType = null;
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource);
    }

    // Only the metadata fields provided by the client are updated, which keeps the patch simple and safe.
    @PutMapping("/{id}")
    public Recording updateRecording(@PathVariable Long id, @RequestBody Recording updatedRecording) {
        Recording recording = findRecording(id);

        if (updatedRecording.getTitle() != null) {
            recording.setTitle(updatedRecording.getTitle());
        }

        if (updatedRecording.getTranscript() != null) {
            recording.setTranscript(updatedRecording.getTranscript());
        }

        if (updatedRecording.getDate() != null) {
            recording.setDate(updatedRecording.getDate());
        }

        if (updatedRecording.getDuration() != null) {
            recording.setDuration(updatedRecording.getDuration());
        }

        if (updatedRecording.getIcon() != null) {
            recording.setIcon(updatedRecording.getIcon());
        }

        if (updatedRecording.getTags() != null) {
            recording.setTags(updatedRecording.getTags());
        }

        if (updatedRecording.getAccent() != null) {
            recording.setAccent(updatedRecording.getAccent());
        }

        return recordingRepository.save(recording);
    }

    // Deleting a recording also removes its saved audio file to keep the storage clean.
    @DeleteMapping("/{id}")
    public void deleteRecording(@PathVariable Long id) {
        Recording recording = findRecording(id);

        if (recording.getAudioFilename() != null) {
            Path filePath = uploadDirectory.resolve(recording.getAudioFilename());

            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                logger.warn("Could not delete audio file: {}", filePath, e);
            }
        }

        recordingRepository.delete(recording);
    }

    // This helper keeps the code DRY by centralising the common "not found" logic.
    private Recording findRecording(Long id) {
        return recordingRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording not found"));
    }

    // This is the main upload flow: validate the file, transcribe it, store it, then save the metadata.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Recording addRecording(
        @RequestParam("audio") MultipartFile audio,
        @RequestParam("title") String title,
        @RequestParam("date") String date,
        @RequestParam("duration") String duration,
        @RequestParam("transcript") String transcript,
        @RequestParam(value = "icon", defaultValue = "●") String icon,
        @RequestParam(value = "accent", defaultValue = "#b85d43") String accent,
        @RequestParam(value = "tags", defaultValue = "[]") String tagsJson) {

        if (audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audio file is empty");
        }

        String cloudTranscript;

        try {
            logger.info("Received audio recording for transcription");
            cloudTranscript = transcriptionService.transcribe(audio);
            logger.info("Audio transcription completed successfully");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429) {
                logger.warn("Transcription service rate limit reached");
                throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Transcription service is temporarily unavailable",
                    e);
            }

            logger.error("Transcription service returned HTTP error {}", e.getStatusCode().value());
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Transcription service returned an error",
                e);
        } catch (IOException e) {
            logger.error("Transcription service failed", e);
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Transcription service failed",
                e);
        }

        String originalFilename = audio.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "recording.webm";
        }

        originalFilename = Paths.get(originalFilename).getFileName().toString();
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;
        Path filePath = uploadDirectory.resolve(storedFilename);

        try {
            Files.copy(audio.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save audio file");
        }

        List<String> tags;
        try {
            tags = objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tags");
        }

        // We let the database create the ID, then we use that generated value to build the file URL.
        Recording recording = new Recording(
            title,
            date,
            duration,
            icon,
            tags,
            accent,
            cloudTranscript,
            null,
            storedFilename);

        recording = recordingRepository.save(recording);

        String audioUrl = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/api/recordings/{id}/audio")
            .buildAndExpand(recording.getId())
            .toUriString();

        recording.setAudioUrl(audioUrl);

        return recordingRepository.save(recording);
    }
}