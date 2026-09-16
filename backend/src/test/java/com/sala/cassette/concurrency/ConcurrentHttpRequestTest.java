package com.sala.cassette.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrentHttpRequestTest {

    @LocalServerPort
    private int port;

    // This test pushes the application with a large burst of simultaneous requests.
    // It checks that the service remains responsive and returns a successful response for each one.
    @Test
    void shouldHandleMoreThan200ConcurrentHttpRequests() throws Exception {
        int requestCount = 250;

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
        long start = System.nanoTime();

        for (int i = 0; i < requestCount; i++) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/global/stats"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

            CompletableFuture<HttpResponse<String>> future = client.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString());

            futures.add(future);
        }

        // Wait until every request completes before checking the results.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
            .get(15, TimeUnit.SECONDS);

        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        for (CompletableFuture<HttpResponse<String>> future : futures) {
            HttpResponse<String> response = future.get();
            assertEquals(200, response.statusCode());
        }

        assertEquals(requestCount, futures.size());

        assertTrue(
            elapsedMillis < 15_000,
            "250 concurrent requests took too long: " + elapsedMillis + " ms");

        System.out.println(
            requestCount + " concurrent HTTP requests completed in " + elapsedMillis + " ms");
    }
}