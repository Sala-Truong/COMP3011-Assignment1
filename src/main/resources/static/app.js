const recordButton = document.getElementById("recordButton");
const stopButton = document.getElementById("stopButton");
const resetButton = document.getElementById("resetButton");
const saveButton = document.getElementById("saveButton");

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

/*
    Optional cassette-name input.

    If your index.html contains:

    <input
        id="recordingName"
        class="recording-name"
        type="text"
        value="Untitled Recording"
        maxlength="40"
    >

    the user can rename the tape directly on the cassette.

    If the input is not present, this JavaScript falls back to a
    browser prompt when REC is pressed.
*/
const recordingNameInput = document.getElementById("recordingName");


let seconds = 0;
let timerInterval = null;
let selectedRecordingId = null;
let currentRecordingName = "Untitled Recording";
let pendingRecording = null;


const recordings = [
    {
        id: 1,
        title: "Project Ideas",
        date: "22 Aug 2026",
        duration: "00:48",
        icon: "✦",
        tags: ["Ideation", "Brainstorm"],
        accent: "#b67635",
        transcript:
            "A few possible project directions include a retro cassette interface, a tape archive panel, and a warm analogue visual theme. The core idea is to make the speech-to-text tool feel nostalgic but still practical."
    },
    {
        id: 2,
        title: "Lecture Summary",
        date: "21 Aug 2026",
        duration: "00:55",
        icon: "▤",
        tags: ["Lecture", "Summary"],
        accent: "#8b7148",
        transcript:
            "Today’s lecture focused on the main concepts of cloud systems, including scalability, concurrency, and distributed communication. The key takeaway is that performance and coordination matter as much as correctness."
    },
    {
        id: 3,
        title: "Meeting Notes",
        date: "20 Aug 2026",
        duration: "00:42",
        icon: "✎",
        tags: ["Meeting", "Notes"],
        accent: "#a17645",
        transcript:
            "The team agreed that the user interface should be built first, followed by microphone recording, then transcription, and finally storage of previous recordings. The archive panel should remain simple and visually consistent."
    },
    {
        id: 4,
        title: "Daily Journal",
        date: "19 Aug 2026",
        duration: "00:36",
        icon: "◫",
        tags: ["Journal", "Personal"],
        accent: "#8d6d45",
        transcript:
            "Today I worked on the design direction for the assignment and decided to use a cassette tape concept. I want the final app to look aesthetic, vintage, and memorable."
    },
    {
        id: 5,
        title: "Interview Prep",
        date: "18 Aug 2026",
        duration: "00:51",
        icon: "●",
        tags: ["Interview", "Preparation"],
        accent: "#a65a3d",
        transcript:
            "Important interview points include problem solving, communication, and being able to explain technical choices clearly. Good structure and calm delivery can make a strong impression."
    }
];


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
    if (recordingNameInput) {
        const typedName = recordingNameInput.value.trim();

        if (typedName) {
            return typedName;
        }

        recordingNameInput.value = "Untitled Recording";
        return "Untitled Recording";
    }

    const enteredName = window.prompt(
        "Name this tape:",
        `Recording ${recordings.length + 1}`
    );

    return enteredName && enteredName.trim()
        ? enteredName.trim()
        : "Untitled Recording";
}


function getCurrentDate() {
    return new Date().toLocaleDateString("en-AU", {
        day: "2-digit",
        month: "short",
        year: "numeric"
    });
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
    const minutes = Math.floor(seconds / 60)
        .toString()
        .padStart(2, "0");

    const remainingSeconds = (seconds % 60)
        .toString()
        .padStart(2, "0");

    const formatted = `${minutes}:${remainingSeconds}`;

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

    transcriptStats.textContent =
        `${words} words · ${durationText}`;
}


/* =========================================================
   RECORD
========================================================= */

recordButton.addEventListener("click", () => {
    clearInterval(timerInterval);

    seconds = 0;
    selectedRecordingId = null;
    pendingRecording = null;
    updateTimer();

    currentRecordingName = getTapeName();

    if (recordingNameInput) {
        recordingNameInput.value = currentRecordingName;
        recordingNameInput.disabled = false;
    }

    cassette.classList.add("recording");

    statusText.textContent = "RECORDING";
    statusDot.classList.add("recording");

    setTranscriptState("TRANSCRIBING", "recording");

    if (transcriptionTitle) {
        transcriptionTitle.textContent = currentRecordingName;
    }

    transcriptionText.classList.add("placeholder");
    transcriptionText.textContent =
        "Listening… your live transcription will appear here once the speech-to-text service is connected.";

    if (transcriptStats) {
        transcriptStats.textContent =
            "0 words · recording in progress";
    }

    recordButton.disabled = true;
    stopButton.disabled = false;
    saveButton.disabled = true;

    timerInterval = setInterval(() => {
        seconds++;
        updateTimer();
    }, 1000);
});


/* =========================================================
   STOP
   Stops the tape, but does not save it yet.
========================================================= */

stopButton.addEventListener("click", () => {
    clearInterval(timerInterval);
    timerInterval = null;

    cassette.classList.remove("recording");

    statusText.textContent = "READY TO SAVE";
    statusDot.classList.remove("recording");

    setTranscriptState("STOPPED · UNSAVED", "ready");

    recordButton.disabled = true;
    stopButton.disabled = true;
    saveButton.disabled = false;

    if (recordingNameInput) {
        const finalName = recordingNameInput.value.trim();

        currentRecordingName =
            finalName || "Untitled Recording";

        recordingNameInput.value = currentRecordingName;
    }

    const duration =
        timer && timer.textContent
            ? timer.textContent
            : "00:00";

    const stoppedText =
        "Recording stopped. Once the real speech-to-text API is connected, the spoken words from this new recording will appear here.";

    transcriptionText.classList.remove("placeholder");
    transcriptionText.textContent = stoppedText;

    if (transcriptionTitle) {
        transcriptionTitle.textContent =
            currentRecordingName;
    }

    updateStats(
        stoppedText,
        `${duration} · unsaved`
    );

    pendingRecording = {
        id: Date.now(),
        title: currentRecordingName,
        date: getCurrentDate(),
        duration: duration,
        icon: "●",
        tags: ["New", "Voice Memo"],
        accent: "#b85d43",
        transcript: stoppedText
    };
});


/* =========================================================
   SAVE
   Adds the stopped tape to Saved Recordings.
========================================================= */

saveButton.addEventListener("click", () => {
    if (!pendingRecording) return;

    if (recordingNameInput) {
        const finalName = recordingNameInput.value.trim();

        pendingRecording.title =
            finalName || "Untitled Recording";

        recordingNameInput.value =
            pendingRecording.title;
    }

    recordings.unshift(pendingRecording);

    selectedRecordingId = pendingRecording.id;

    if (transcriptionTitle) {
        transcriptionTitle.textContent =
            pendingRecording.title;
    }

    statusText.textContent = "TAPE SAVED";
    setTranscriptState("SAVED TO ARCHIVE", "ready");

    updateStats(
        pendingRecording.transcript,
        `${pendingRecording.duration} · saved`
    );

    pendingRecording = null;

    recordButton.disabled = true;
    stopButton.disabled = true;
    saveButton.disabled = true;

    renderArchive();
});


/* =========================================================
   NEW TAPE
   Clears the current tape and prepares a fresh recording.
========================================================= */

resetButton.addEventListener("click", () => {
    clearInterval(timerInterval);
    timerInterval = null;

    seconds = 0;
    selectedRecordingId = null;
    pendingRecording = null;
    currentRecordingName = "Untitled Recording";

    updateTimer();

    cassette.classList.remove("recording");

    statusText.textContent = "READY TO RECORD";
    statusDot.classList.remove("recording");

    setTranscriptState("STANDBY", "ready");

    recordButton.disabled = false;
    stopButton.disabled = true;
    saveButton.disabled = true;

    if (recordingNameInput) {
        recordingNameInput.value =
            "Untitled Recording";

        recordingNameInput.disabled = false;
        recordingNameInput.focus();
        recordingNameInput.select();
    }

    if (transcriptionTitle) {
        transcriptionTitle.textContent =
            "Transcription";
    }

    transcriptionText.classList.add("placeholder");
    transcriptionText.textContent =
        "Your recorded words will appear here. Give the tape a name, then press REC.";

    if (transcriptStats) {
        transcriptStats.textContent =
            "0 words · waiting for audio";
    }

    renderArchive();
});


/* =========================================================
   ARCHIVE
========================================================= */

function renderArchive() {
    if (!archiveList) return;

    archiveList.innerHTML = "";

    if (archiveCount) {
        archiveCount.textContent =
            String(recordings.length).padStart(2, "0");
    }

    recordings.forEach((recording) => {
        const caseItem =
            document.createElement("article");

        caseItem.className = "cassette-case";
        caseItem.style.setProperty(
            "--case-accent",
            recording.accent
        );

        caseItem.tabIndex = 0;

        if (recording.id === selectedRecordingId) {
            caseItem.classList.add("active");
        }

        caseItem.innerHTML = `
            <div class="case-top">
                <span class="case-side">SIDE A</span>
                <span class="case-date">
                    ${escapeHTML(recording.date)}
                </span>
            </div>

            <div class="case-main">

                <div
                    class="case-icon"
                    aria-hidden="true"
                >
                    ${escapeHTML(recording.icon)}
                </div>

                <div class="case-copy">

                    <div class="case-title">
                        ${escapeHTML(recording.title)}
                    </div>

                    <div class="case-tags">
                        ${recording.tags
                            .map(
                                tag =>
                                    `<span class="case-tag">${escapeHTML(tag)}</span>`
                            )
                            .join("")}
                    </div>

                </div>

                <span
                    class="case-bookmark"
                    aria-hidden="true"
                >
                    ⌑
                </span>

            </div>

            <div class="case-window">

                <span class="case-reel"></span>

                <div class="case-progress">

                    <span class="case-play-dot">
                        ▶
                    </span>

                    <span
                        class="case-progress-line"
                    ></span>

                    <span class="case-duration">
                        ${escapeHTML(recording.duration)}
                    </span>

                </div>

                <span class="case-reel"></span>

            </div>

            <div class="case-bottom">

                <span class="case-meta">
                    Recorded voice memo · SIDE A
                </span>

                <button
                    class="case-play"
                    type="button"
                >
                    PLAY
                </button>

            </div>
        `;

        const openRecording = () => {
            selectRecording(recording.id);
        };

        caseItem.addEventListener(
            "click",
            openRecording
        );

        caseItem.addEventListener(
            "keydown",
            (event) => {
                if (
                    event.key === "Enter" ||
                    event.key === " "
                ) {
                    event.preventDefault();
                    openRecording();
                }
            }
        );

        archiveList.appendChild(caseItem);
    });
}


/* =========================================================
   SELECT SAVED RECORDING
========================================================= */

function selectRecording(id) {
    const recording = recordings.find(
        item => item.id === id
    );

    if (!recording) return;

    selectedRecordingId = id;

    clearInterval(timerInterval);

    cassette.classList.remove("recording");
    statusDot.classList.remove("recording");

    statusText.textContent = "PLAYBACK READY";

    setTranscriptState(
        "ARCHIVED TAPE",
        "ready"
    );

    if (recordingNameInput) {
        recordingNameInput.value =
            recording.title;
    }

    if (transcriptionTitle) {
        transcriptionTitle.textContent =
            recording.title;
    }

    transcriptionText.classList.remove(
        "placeholder"
    );

    transcriptionText.textContent =
        recording.transcript;

    if (waveformDuration) {
        waveformDuration.textContent =
            recording.duration;
    }

    updateStats(
        recording.transcript,
        `${recording.duration} tape`
    );

    recordButton.disabled = true;
    stopButton.disabled = true;
    saveButton.disabled = true;

    renderArchive();
}


/* =========================================================
   INITIALISE
========================================================= */

saveButton.disabled = true;
buildWaveform();
updateTimer();
renderArchive();