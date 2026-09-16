# Cassette Speech-to-Text Web Application

A Java Spring Boot web application that records microphone audio in the browser,
uploads it to a REST backend, sends the audio to OpenAI's speech-to-text API,
and displays the returned transcription.

## Features

- Browser microphone recording
- Clear recording and stopped states
- Speech-to-text using gpt-4o-mini-transcribe
- REST API built with Spring Boot
- Token usage statistics
- Application uptime endpoint
- Graceful shutdown endpoint
- Saved recording metadata and audio playback
- Error handling for microphone, API, and transcription failures
- Regression and concurrency testing

## Architecture

Browser
    ↓
MediaRecorder
    ↓
POST /api/recordings
    ↓
RecordingController
    ↓
OpenAiTranscriptionService
    ↓
OpenAI Audio Transcription API
    ↓
Transcript + token usage
    ↓
UsageStatsService
    ↓
Response returned to browser

## Design Decisions

### Thread-safe usage statistics
Usage statistics are shared across requests. AtomicLong is used instead
of a normal long because token totals may be updated by multiple request
threads concurrently. Atomic operations prevent lost updates.

### STT service separation
OpenAI communication is implemented in OpenAiTranscriptionService rather
than directly in the controller. This separates HTTP request handling from
external cloud communication and allows the STT dependency to be mocked
during regression testing.

### Environment-based API credentials
The OpenAI API key is obtained through OPENAI_API_KEY at runtime. It is
never included in frontend code, persisted, or logged.

## Regression Test Rationale

RecordingControllerTest uses a mocked STT service so controller behaviour
can be tested deterministically without network access or API charges.

UsageStatsServiceTest deliberately performs concurrent updates to expose
potential race conditions.

ConcurrentHttpRequestTest sends 250 concurrent HTTP requests to verify
that the application remains responsive beyond the assignment requirement
of 200 simultaneous requests.

## Backend Structure

### RecordingController
Receives audio uploads, validates requests, invokes the transcription service,
stores recordings, and returns the resulting recording data.

### OpenAiTranscriptionService
Handles communication with OpenAI.

The API key is never stored in the frontend or source code. It is supplied
through application configuration/environment variables at runtime.

### UsageStatsService
Tracks input and output token counts.

AtomicLong is used because multiple requests may update statistics
concurrently. Atomic operations prevent lost updates caused by race conditions.

### RecordingRepository
Provides persistence using Spring Data JPA and H2.

## Configuration

The application requires an OpenAI API key.

TITAN provides:

OPENAI_API_KEY

The application reads the key dynamically at runtime.

The API key must never be:

- committed to Git
- included in JavaScript
- printed to logs
- stored in the database

## Running the Application

Build:

./mvnw clean package

Run:

java -jar target/Assignment1-0.0.1-SNAPSHOT.jar

Then visit:

http://localhost:8080/

## Testing

Run the complete regression suite:

./mvnw test

### Controller regression tests

StatsControllerTest verifies that the global statistics endpoint returns
the expected input and output token counts.

RecordingControllerTest verifies:

- successful audio upload
- STT transcription result
- empty audio rejection
- STT service failure handling
- OpenAI rate-limit handling

The STT service is mocked during these tests so regression tests do not
depend on the real OpenAI service.

### Race-condition testing

UsageStatsServiceTest performs many concurrent updates against the shared
token counters.

AtomicLong ensures updates remain correct when multiple threads modify the
statistics simultaneously.

### HTTP concurrency testing

ConcurrentHttpRequestTest launches the actual embedded Spring Boot server
and sends 250 concurrent HTTP requests.

The test verifies that:

- all requests complete
- all responses return HTTP 200
- the server does not crash
- requests complete within the test timeout

This provides regression evidence that the web application can handle
more than 200 simultaneous requests.

## Error Handling

The application handles several failure scenarios, including:

- empty audio uploads → HTTP 400
- STT service failure → HTTP 502
- OpenAI rate limiting → HTTP 503
- unavailable microphone access
- failed frontend REST requests

## Concurrency

Spring Boot handles incoming HTTP requests using multiple server threads.

Application controllers are designed to avoid unnecessary shared mutable
state.

The shared usage statistics use AtomicLong so token counters remain
thread-safe when requests overlap.

## Security

The OpenAI bearer token is handled only by the Java backend.

The browser never communicates directly with OpenAI and therefore never
receives the API key.

## Technologies

- Java
- Spring Boot
- Spring MVC
- Spring Data JPA
- H2
- Maven
- HTML
- CSS
- JavaScript
- OpenAI Audio Transcription API
- JUnit
- Mockito

## Development Assistance

Generative AI was used as a development aid for explaining Spring Boot
concepts, debugging errors, reviewing code, and assisting with regression
test design. All generated suggestions were reviewed, modified, tested,
and integrated by the author, who is responsible for the final implementation.