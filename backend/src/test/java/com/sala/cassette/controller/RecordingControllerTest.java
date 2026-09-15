package com.sala.cassette.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import com.sala.cassette.model.Recording;
import com.sala.cassette.repository.RecordingRepository;
import com.sala.cassette.service.OpenAiTranscriptionService;

@WebMvcTest(RecordingController.class)
class RecordingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecordingRepository recordingRepository;

    @MockitoBean
    private OpenAiTranscriptionService transcriptionService;


    @Test
    void shouldUploadAudioAndReturnCloudTranscript()
            throws Exception {

        MockMultipartFile audio =
                new MockMultipartFile(
                        "audio",
                        "test.webm",
                        "audio/webm",
                        "fake audio data".getBytes()
                );

        when(transcriptionService.transcribe(
                any(MultipartFile.class)))
                .thenReturn(
                        "This is the cloud transcript."
                );

        AtomicReference<Path> createdFile =
                new AtomicReference<>();

        /*
         * Our repository is mocked, so H2 will not generate
         * an ID automatically. We simulate that behaviour here.
         */
        when(recordingRepository.save(
                any(Recording.class)))
                .thenAnswer(invocation -> {

                    Recording recording =
                            invocation.getArgument(0);

                    if (recording.getId() == null) {
                        recording.setId(1L);
                    }

                    if (recording.getAudioFilename()
                            != null) {

                        createdFile.set(
                                Paths.get("uploads")
                                        .toAbsolutePath()
                                        .normalize()
                                        .resolve(
                                                recording
                                                        .getAudioFilename()
                                        )
                        );
                    }

                    return recording;
                });

        try {

            mockMvc.perform(
                    multipart("/api/recordings")
                            .file(audio)
                            .param(
                                    "title",
                                    "Test Recording"
                            )
                            .param(
                                    "date",
                                    "15 Sep 2026"
                            )
                            .param(
                                    "duration",
                                    "00:05"
                            )
                            .param(
                                    "transcript",
                                    ""
                            )
                            .param(
                                    "icon",
                                    "●"
                            )
                            .param(
                                    "accent",
                                    "#b85d43"
                            )
                            .param(
                                    "tags",
                                    "[\"Voice Memo\"]"
                            )
            )
            .andExpect(status().isOk())
            .andExpect(
                    jsonPath("$.id")
                            .value(1)
            )
            .andExpect(
                    jsonPath("$.title")
                            .value("Test Recording")
            )
            .andExpect(
                    jsonPath("$.transcript")
                            .value(
                                    "This is the cloud transcript."
                            )
            )
            .andExpect(
                    jsonPath("$.audioUrl")
                            .value(
                                    endsWith(
                                            "/api/recordings/1/audio"
                                    )
                            )
            );

            verify(transcriptionService)
                    .transcribe(
                            any(MultipartFile.class)
                    );

        } finally {

            /*
             * The controller saves the uploaded audio
             * to disk, so remove the test file afterwards.
             */
            if (createdFile.get() != null) {
                Files.deleteIfExists(
                        createdFile.get()
                );
            }
        }
    }


    @Test
    void shouldRejectEmptyAudio()
            throws Exception {

        MockMultipartFile emptyAudio =
                new MockMultipartFile(
                        "audio",
                        "empty.webm",
                        "audio/webm",
                        new byte[0]
                );

        mockMvc.perform(
                multipart("/api/recordings")
                        .file(emptyAudio)
                        .param(
                                "title",
                                "Empty Recording"
                        )
                        .param(
                                "date",
                                "15 Sep 2026"
                        )
                        .param(
                                "duration",
                                "00:00"
                        )
                        .param(
                                "transcript",
                                ""
                        )
                        .param(
                                "tags",
                                "[]"
                        )
        )
        .andExpect(
                status().isBadRequest()
        );

        verifyNoInteractions(
                transcriptionService
        );
    }


    @Test
    void shouldReturnBadGatewayWhenTranscriptionFails()
            throws Exception {

        MockMultipartFile audio =
                new MockMultipartFile(
                        "audio",
                        "test.webm",
                        "audio/webm",
                        "fake audio data".getBytes()
                );

        when(transcriptionService.transcribe(
                any(MultipartFile.class)))
                .thenThrow(
                        new IOException(
                                "Fake STT failure"
                        )
                );

        mockMvc.perform(
                multipart("/api/recordings")
                        .file(audio)
                        .param(
                                "title",
                                "Test Recording"
                        )
                        .param(
                                "date",
                                "15 Sep 2026"
                        )
                        .param(
                                "duration",
                                "00:05"
                        )
                        .param(
                                "transcript",
                                ""
                        )
                        .param(
                                "tags",
                                "[]"
                        )
        )
        .andExpect(
                status().isBadGateway()
        );
    }
}