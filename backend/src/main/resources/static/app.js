const recordButton = document.getElementById("recordButton");
const stopButton = document.getElementById("stopButton");
const resetButton = document.getElementById("resetButton");
const saveButton = document.getElementById("saveButton");
const playbackButton = document.querySelector(".playback-button");

const cassette = document.querySelector(".cassette");

const timer = document.getElementById("timer");
const statusText = document.getElementById("statusText");
const statusDot = document.getElementById("statusDot");

const transcriptionText = document.getElementById("transcriptionText");
const transcriptionTitle = document.getElementById("transcriptionTitle");
const transcriptStatus = document.getElementById("transcriptStatus");
const transcriptStats = document.getElementById("transcriptStats");

const waveformDuration = document.getElementById("waveformDuration");
const waveformBars = document.getElementById("waveformBars");

const archiveList = document.getElementById("archiveList");
const archiveCount = document.getElementById("archiveCount");
const recordingNameInput = document.getElementById("recordingName");

const API_URL = "/api/recordings";
const MAX_RECORDING_SECONDS = 60;

let seconds = 0;
let timerInterval = null;
let selectedRecordingId = null;
let currentRecordingName = "Untitled Recording";
let pendingRecording = null;
let recordings = [];

let mediaRecorder = null;
let mediaStream = null;
let audioChunks = [];
let currentAudio = null;
let recordedBlob = null;

let speechRecognition = null;
let finalTranscript = "";
let interimTranscript = "";


/* =========================================================
   HELPERS
========================================================= */

function escapeHTML(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}


function getTapeName() {
    if (!recordingNameInput) {
        return "Untitled Recording";
    }

    const typedName = recordingNameInput.value.trim();

    if (typedName) {
        return typedName;
    }

    recordingNameInput.value = "Untitled Recording";
    return "Untitled Recording";
}


function getCurrentDate() {
    return new Date().toLocaleDateString("en-AU", {
        day: "2-digit",
        month: "short",
        year: "numeric"
    });
}


function formatDuration(totalSeconds) {
    const minutes = Math.floor(totalSeconds / 60)
        .toString()
        .padStart(2, "0");

    const remainingSeconds = (totalSeconds % 60)
        .toString()
        .padStart(2, "0");

    return `${minutes}:${remainingSeconds}`;
}


function buildWaveform() {
    if (!waveformBars) return;

    const heights = [
        22, 48, 30, 68, 42, 78, 54, 34,
        62, 88, 46, 70, 26, 58, 40, 76,
        52, 30, 66, 44, 82, 50, 36, 72,
        46, 62, 28, 56, 86, 52, 34, 74,
        42, 68, 48, 30, 60, 80, 38, 70,
        54, 32, 64, 48, 84, 44, 58, 36,
        72, 50, 30, 66, 46, 78, 40, 60,
        34, 70, 52, 28, 64, 44, 82, 38
    ];

    waveformBars.innerHTML = heights
        .map(
            (height, index) =>
                `<span style="--h:${height}%; animation-delay:${(index % 11) * -0.06}s"></span>`
        )
        .join("");
}


function setTranscriptState(label, mode = "ready") {
    if (!transcriptStatus) return;

    transcriptStatus.className = `status-badge ${mode}`;
    transcriptStatus.innerHTML = `<i></i> ${label}`;
}


function updateTimer() {
    const formatted = formatDuration(seconds);

    if (timer) {
        timer.textContent = formatted;
    }

    if (waveformDuration) {
        waveformDuration.textContent = formatted;
    }
}


function updateStats(text, durationText) {
    if (!transcriptStats) return;

    const words = text.trim()
        ? text.trim().split(/\s+/).length
        : 0;

    transcriptStats.textContent = `${words} words · ${durationText}`;
}


function stopCurrentAudio() {
    if (!currentAudio) return;

    currentAudio.pause();
    currentAudio.currentTime = 0;
    currentAudio = null;

    if (playbackButton) {
        playbackButton.textContent = "▶";
        playbackButton.setAttribute("aria-label", "Play selected recording");
    }
}


function stopMicrophoneStream() {
    if (!mediaStream) return;

    mediaStream.getTracks().forEach(track => track.stop());
    mediaStream = null;
}


function updateLiveTranscript() {
    const text = `${finalTranscript}${interimTranscript}`.trim();

    transcriptionText.classList.toggle("placeholder", !text);
    transcriptionText.textContent = text || "Listening… start speaking and your words will appear here.";
    updateStats(text, `${formatDuration(seconds)} · recording in progress`);
}


function setupSpeechRecognition() {
    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;

    if (!Recognition) {
        speechRecognition = null;
        return;
    }

    speechRecognition = new Recognition();
    speechRecognition.lang = "en-US";
    speechRecognition.continuous = true;
    speechRecognition.interimResults = true;

    speechRecognition.onresult = (event) => {
        interimTranscript = "";

        for (let i = event.resultIndex; i < event.results.length; i++) {
            const text = event.results[i][0].transcript;

            if (event.results[i].isFinal) {
                finalTranscript += `${text.trim()} `;
            } else {
                interimTranscript += text;
            }
        }

        updateLiveTranscript();
    };

    speechRecognition.onerror = (event) => {
        if (event.error === "no-speech" || event.error === "aborted") {
            return;
        }

        console.warn("Speech recognition error:", event.error);
    };

    speechRecognition.onend = () => {
        // Chrome can stop speech recognition by itself while MediaRecorder
        // is still running. Restart it so the 60-second demo keeps listening.
        if (mediaRecorder && mediaRecorder.state === "recording") {
            try {
                speechRecognition.start();
            } catch (error) {
                console.warn("Could not restart speech recognition:", error);
            }
        }
    };
}


function normaliseRecording(recording) {
    return {
        id: recording.id,
        title: recording.title || "Untitled Recording",
        date: recording.date || getCurrentDate(),
        duration: recording.duration || "00:00",
        icon: recording.icon || "●",
        tags: Array.isArray(recording.tags) && recording.tags.length
            ? recording.tags
            : ["Voice Memo"],
        accent: recording.accent || "#b85d43",
        transcript: recording.transcript || "No transcript was captured for this recording.",
        audioUrl: recording.audioUrl || null
    };
}


/* =========================================================
   API
========================================================= */

async function loadRecordings() {
    try {
        const response = await fetch(API_URL);

        if (!response.ok) {
            throw new Error(`Server returned ${response.status}`);
        }

        const data = await response.json();
        recordings = Array.isArray(data)
            ? data.map(normaliseRecording)
            : [];

        renderArchive();
    } catch (error) {
        console.error("Could not load recordings:", error);
        recordings = [];
        renderArchive();

        statusText.textContent = "SERVER OFFLINE";
        setTranscriptState("BACKEND UNAVAILABLE", "recording");
    }
}


async function saveRecordingToServer(recording) {
    const formData = new FormData();

    formData.append("audio", recording.audioBlob, `recording-${Date.now()}.webm`);
    formData.append("title", recording.title);
    formData.append("date", recording.date);
    formData.append("duration", recording.duration);
    formData.append("transcript", recording.transcript);
    formData.append("icon", recording.icon);
    formData.append("accent", recording.accent);
    formData.append("tags", JSON.stringify(recording.tags));

    const response = await fetch(API_URL, {
        method: "POST",
        body: formData
    });

    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || `Server returned ${response.status}`);
    }

    return normaliseRecording(await response.json());
}


/* =========================================================
   RECORD
========================================================= */

recordButton.addEventListener("click", async () => {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        statusText.textContent = "MIC NOT SUPPORTED";
        setTranscriptState("MICROPHONE UNAVAILABLE", "recording");
        transcriptionText.classList.remove("placeholder");
        transcriptionText.textContent =
            "This browser does not support microphone recording. Please open the project in a recent version of Chrome.";
        return;
    }

    try {
        stopCurrentAudio();
        clearInterval(timerInterval);

        seconds = 0;
        selectedRecordingId = null;
        pendingRecording = null;
        audioChunks = [];
        finalTranscript = "";
        interimTranscript = "";
        updateTimer();

        currentRecordingName = getTapeName();

        if (recordingNameInput) {
            recordingNameInput.value = currentRecordingName;
            recordingNameInput.disabled = false;
        }

        mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
        mediaRecorder = new MediaRecorder(mediaStream);

        mediaRecorder.addEventListener("dataavailable", (event) => {
            if (event.data && event.data.size > 0) {
                audioChunks.push(event.data);
            }
        });

        mediaRecorder.addEventListener("stop", () => {
            const mimeType = mediaRecorder.mimeType || "audio/webm";
            const audioBlob = new Blob(audioChunks, { type: mimeType });
            const duration = formatDuration(seconds);
            const transcript = `${finalTranscript}${interimTranscript}`.trim();
            const fallbackTranscript = transcript ||
                "Audio recorded successfully. No speech transcript was captured by this browser.";

            transcriptionText.classList.remove("placeholder");
            transcriptionText.textContent = fallbackTranscript;

            if (transcriptionTitle) {
                transcriptionTitle.textContent = currentRecordingName;
            }

            updateStats(fallbackTranscript, `${duration} · unsaved`);

            pendingRecording = {
                title: currentRecordingName,
                date: getCurrentDate(),
                duration,
                icon: "●",
                tags: ["New", "Voice Memo"],
                accent: "#b85d43",
                transcript: fallbackTranscript,
                audioBlob,
                localAudioUrl: URL.createObjectURL(audioBlob)
            };

            statusText.textContent = "READY TO SAVE";
            setTranscriptState("STOPPED · UNSAVED", "ready");
            recordButton.disabled = true;
            stopButton.disabled = true;
            saveButton.disabled = false;
            resetButton.disabled = false;
            stopMicrophoneStream();
        });

        cassette.classList.add("recording");
        statusText.textContent = "RECORDING";
        statusDot.classList.add("recording");
        setTranscriptState("TRANSCRIBING", "recording");

        if (transcriptionTitle) {
            transcriptionTitle.textContent = currentRecordingName;
        }

        transcriptionText.classList.add("placeholder");
        transcriptionText.textContent = "Listening… start speaking and your words will appear here.";

        if (transcriptStats) {
            transcriptStats.textContent = "0 words · recording in progress";
        }

        recordButton.disabled = true;
        stopButton.disabled = false;
        saveButton.disabled = true;
        resetButton.disabled = true;

        mediaRecorder.start(250);

        if (speechRecognition) {
            try {
                speechRecognition.start();
            } catch (error) {
                console.warn("Speech recognition could not start:", error);
            }
        }

        timerInterval = setInterval(() => {
            seconds++;
            updateTimer();

            if (seconds >= MAX_RECORDING_SECONDS) {
                stopButton.click();
            }
        }, 1000);
    } catch (error) {
        console.error("Could not start recording:", error);
        stopMicrophoneStream();

        cassette.classList.remove("recording");
        statusDot.classList.remove("recording");
        statusText.textContent = "MIC ACCESS NEEDED";
        setTranscriptState("MICROPHONE BLOCKED", "recording");

        transcriptionText.classList.remove("placeholder");
        transcriptionText.textContent =
            "Microphone access was not available. Allow microphone permission in your browser and press REC again.";

        recordButton.disabled = false;
        stopButton.disabled = true;
        saveButton.disabled = true;
        resetButton.disabled = false;
    }
});


/* =========================================================
   STOP
   Stops the tape, but does not save it yet.
========================================================= */

stopButton.addEventListener("click", () => {
    if (!mediaRecorder || mediaRecorder.state !== "recording") {
        return;
    }

    clearInterval(timerInterval);
    timerInterval = null;

    cassette.classList.remove("recording");
    statusDot.classList.remove("recording");
    statusText.textContent = "PROCESSING";
    setTranscriptState("FINISHING TRANSCRIPT", "ready");

    if (recordingNameInput) {
        const finalName = recordingNameInput.value.trim();
        currentRecordingName = finalName || "Untitled Recording";
        recordingNameInput.value = currentRecordingName;
    }

    if (speechRecognition) {
        try {
            speechRecognition.stop();
        } catch (error) {
            console.warn("Speech recognition stop error:", error);
        }
    }

    recordButton.disabled = true;
    stopButton.disabled = true;
    saveButton.disabled = true;

    mediaRecorder.stop();
});


/* =========================================================
   SAVE
   Persists the stopped tape through the backend.
========================================================= */

saveButton.addEventListener("click", async () => {
    if (!pendingRecording) return;

    if (recordingNameInput) {
        const finalName = recordingNameInput.value.trim();
        pendingRecording.title = finalName || "Untitled Recording";
        recordingNameInput.value = pendingRecording.title;
    }

    saveButton.disabled = true;
    statusText.textContent = "SAVING TAPE";
    setTranscriptState("UPLOADING", "recording");

    try {
        const savedRecording = await saveRecordingToServer(pendingRecording);

        selectedRecordingId = savedRecording.id;
        recordings.unshift(savedRecording);

        if (transcriptionTitle) {
            transcriptionTitle.textContent = savedRecording.title;
        }

        statusText.textContent = "TAPE SAVED";
        setTranscriptState("SAVED TO ARCHIVE", "ready");
        updateStats(savedRecording.transcript, `${savedRecording.duration} · saved`);

        if (pendingRecording.localAudioUrl) {
            URL.revokeObjectURL(pendingRecording.localAudioUrl);
        }

        pendingRecording = null;
        recordButton.disabled = true;
        stopButton.disabled = true;
        saveButton.disabled = true;
        renderArchive();
    } catch (error) {
        console.error("Could not save recording:", error);
        statusText.textContent = "SAVE FAILED";
        setTranscriptState("SERVER ERROR · TRY AGAIN", "recording");
        saveButton.disabled = false;
    }
});


/* =========================================================
   NEW TAPE
========================================================= */

resetButton.addEventListener("click", () => {
    clearInterval(timerInterval);
    timerInterval = null;

    stopCurrentAudio();

    if (speechRecognition) {
        try {
            speechRecognition.abort();
        } catch (error) {
            console.warn(error);
        }
    }

    if (mediaRecorder && mediaRecorder.state === "recording") {
        mediaRecorder.stop();
    }

    stopMicrophoneStream();

    if (pendingRecording && pendingRecording.localAudioUrl) {
        URL.revokeObjectURL(pendingRecording.localAudioUrl);
    }

    seconds = 0;
    selectedRecordingId = null;
    pendingRecording = null;
    currentRecordingName = "Untitled Recording";
    finalTranscript = "";
    interimTranscript = "";

    updateTimer();

    cassette.classList.remove("recording");
    statusText.textContent = "READY TO RECORD";
    statusDot.classList.remove("recording");
    setTranscriptState("STANDBY", "ready");

    recordButton.disabled = false;
    stopButton.disabled = true;
    saveButton.disabled = true;
    resetButton.disabled = false;

    if (recordingNameInput) {
        recordingNameInput.value = "Untitled Recording";
        recordingNameInput.disabled = false;
        recordingNameInput.focus();
        recordingNameInput.select();
    }

    if (transcriptionTitle) {
        transcriptionTitle.textContent = "Transcription";
    }

    transcriptionText.classList.add("placeholder");
    transcriptionText.textContent =
        "Your recorded words will appear here. Give the tape a name, then press REC.";

    if (transcriptStats) {
        transcriptStats.textContent = "0 words · waiting for audio";
    }

    renderArchive();
});

async function deleteRecording(id) {
	const response = await fetch(
	    `${API_URL}/${id}`,
	    {
	        method: "DELETE"
	    }
	);

    if (!response.ok) {
        throw new Error("Failed to delete recording");
    }
}
/* =========================================================
   ARCHIVE
========================================================= */
async function updateRecordingTitle(id, newTitle) {
    const response = await fetch(
        `${API_URL}/${id}`,
        {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                title: newTitle
            })
        }
    );

    if (!response.ok) {
        throw new Error(`Failed to update recording: ${response.status}`);
    }

    return await response.json();
}
recordingNameInput.addEventListener("change", async () => {
    if (!selectedRecordingId) return;

    const newTitle = recordingNameInput.value.trim();

    if (!newTitle) return;

    const updatedRecording = await updateRecordingTitle(
        selectedRecordingId,
        newTitle
    );

    const index = recordings.findIndex(
        recording => String(recording.id) === String(selectedRecordingId)
    );

    if (index !== -1) {
        recordings[index] = updatedRecording;
    }

    if (transcriptionTitle) {
        transcriptionTitle.textContent = updatedRecording.title;
    }

    renderArchive();
});
function renderArchive() {
    if (!archiveList) return;

    archiveList.innerHTML = "";

    if (archiveCount) {
        archiveCount.textContent = String(recordings.length).padStart(2, "0");
    }

    if (recordings.length === 0) {
        const emptyState = document.createElement("p");
        emptyState.className = "archive-empty";
        emptyState.textContent = "No tapes saved yet. Record your first voice memo.";
        archiveList.appendChild(emptyState);
        return;
    }

    recordings.forEach((recording) => {
        const caseItem = document.createElement("article");

        caseItem.className = "cassette-case";
        caseItem.style.setProperty("--case-accent", recording.accent);
        caseItem.tabIndex = 0;

        if (String(recording.id) === String(selectedRecordingId)) {
            caseItem.classList.add("active");
        }

        caseItem.innerHTML = `
            <div class="case-top">
                <span class="case-side">SIDE A</span>
                <span class="case-date">${escapeHTML(recording.date)}</span>
            </div>

            <div class="case-main">
                <div class="case-icon" aria-hidden="true">${escapeHTML(recording.icon)}</div>

                <div class="case-copy">
                    <div class="case-title">${escapeHTML(recording.title)}</div>
                    <div class="case-tags">
                        ${recording.tags
                .map(tag => `<span class="case-tag">${escapeHTML(tag)}</span>`)
                .join("")}
                    </div>
                </div>

                <span class="case-bookmark" aria-hidden="true">⌑</span>
            </div>

            <div class="case-window">
                <span class="case-reel"></span>

                <div class="case-progress">
                    <span class="case-play-dot">▶</span>
                    <span class="case-progress-line"></span>
                    <span class="case-duration">${escapeHTML(recording.duration)}</span>
                </div>

                <span class="case-reel"></span>
            </div>

            <div class="case-bottom">
    <span class="case-meta">Recorded voice memo · SIDE A</span>

    <div class="case-actions">
        <button class="case-delete" type="button">DELETE</button>
        <button class="case-play" type="button">PLAY</button>
    </div>
</div>
        `;

        const openRecording = () => {
            selectRecording(recording.id);
        };

        caseItem.addEventListener("click", openRecording);

        caseItem.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                openRecording();
            }
        });

        const casePlayButton = caseItem.querySelector(".case-play");
        casePlayButton.addEventListener("click", (event) => {
            event.stopPropagation();
            selectRecording(recording.id);
            playRecording(recording);
        });

        const caseDeleteButton = caseItem.querySelector(".case-delete");

        caseDeleteButton.addEventListener("click", async (event) => {
            event.stopPropagation();
            const confirmed = confirm(
                `Delete "${recording.title}"?`
            );

            if (!confirmed) return;
            await deleteRecording(recording.id);

            recordings = recordings.filter(
                item => String(item.id) !== String(recording.id)
            );

            if (String(selectedRecordingId) === String(recording.id)) {
                selectedRecordingId = null;

                if (recordingNameInput) {
                    recordingNameInput.value = "";
                }

                if (transcriptionTitle) {
                    transcriptionTitle.textContent = "TRANSCRIPT";
                }

                transcriptionText.textContent = "No recording selected.";
                transcriptionText.classList.add("placeholder");

                if (waveformDuration) {
                    waveformDuration.textContent = "00:00";
                }

                if (timer) {
                    timer.textContent = "00:00";
                }

                statusText.textContent = "READY TO RECORD";

                recordButton.disabled = false;
                stopButton.disabled = true;
                saveButton.disabled = true;
            }

            renderArchive();
        });
        archiveList.appendChild(caseItem);
    });
}


/* =========================================================
   SELECT + PLAY SAVED RECORDING
========================================================= */

function selectRecording(id) {
    const recording = recordings.find(item => String(item.id) === String(id));

    if (!recording) return;

    stopCurrentAudio();
    selectedRecordingId = recording.id;
    clearInterval(timerInterval);

    cassette.classList.remove("recording");
    statusDot.classList.remove("recording");
    statusText.textContent = "PLAYBACK READY";
    setTranscriptState("ARCHIVED TAPE", "ready");

    if (recordingNameInput) {
        recordingNameInput.value = recording.title;
    }

    if (transcriptionTitle) {
        transcriptionTitle.textContent = recording.title;
    }

    transcriptionText.classList.remove("placeholder");
    transcriptionText.textContent = recording.transcript;

    if (waveformDuration) {
        waveformDuration.textContent = recording.duration;
    }

    if (timer) {
        timer.textContent = recording.duration;
    }

    updateStats(recording.transcript, `${recording.duration} tape`);

    recordButton.disabled = true;
    stopButton.disabled = true;
    saveButton.disabled = true;

    renderArchive();
}


function playRecording(recording) {
    if (!recording || !recording.audioUrl) {
        statusText.textContent = "AUDIO UNAVAILABLE";
        return;
    }

    stopCurrentAudio();

    currentAudio = new Audio(recording.audioUrl);
    currentAudio.play()
        .then(() => {
            statusText.textContent = "PLAYING TAPE";

            if (playbackButton) {
                playbackButton.textContent = "■";
                playbackButton.setAttribute("aria-label", "Stop selected recording");
            }
        })
        .catch((error) => {
            console.error("Playback failed:", error);
            statusText.textContent = "PLAYBACK FAILED";
            currentAudio = null;
        });

    currentAudio.addEventListener("ended", () => {
        statusText.textContent = "PLAYBACK READY";
        currentAudio = null;

        if (playbackButton) {
            playbackButton.textContent = "▶";
            playbackButton.setAttribute("aria-label", "Play selected recording");
        }
    });
}


if (playbackButton) {
    playbackButton.addEventListener("click", () => {
        if (currentAudio) {
            stopCurrentAudio();
            statusText.textContent = "PLAYBACK READY";
            return;
        }

        if (pendingRecording && pendingRecording.localAudioUrl) {
            const localRecording = {
                ...pendingRecording,
                audioUrl: pendingRecording.localAudioUrl
            };
            playRecording(localRecording);
            return;
        }

        const selected = recordings.find(
            recording => String(recording.id) === String(selectedRecordingId)
        );

        if (selected) {
            playRecording(selected);
        } else {
            statusText.textContent = "SELECT A TAPE";
        }
    });
}

/* =========================================================
   INITIALISE
========================================================= */
document.addEventListener("DOMContentLoaded", async () => {
    await loadRecordings();
});

saveButton.disabled = true;

setupSpeechRecognition();
buildWaveform();
updateTimer();