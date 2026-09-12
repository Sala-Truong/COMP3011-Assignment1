import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) throws IOException {

        int port = 8080;

        HttpServer server = HttpServer.create(
                new InetSocketAddress(port),
                0);

        server.createContext("/api/health", Main::handleHealth);

        server.start();

        System.out.println("Backend running on http://localhost:" + port);
    }

    private static void handleHealth(HttpExchange exchange)
            throws IOException {

        String response = """
                {
                    "status": "ok",
                    "message": "Cassette backend is running"
                }
                """;

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json");

        exchange.sendResponseHeaders(
                200,
                response.getBytes().length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response.getBytes());
        }
    }
}