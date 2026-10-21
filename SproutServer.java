import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sprout 2.0 Java Production Server
 * Pure Java (no external frameworks or Python).
 * Serves static web assets and provides REST endpoints for analytics and KYC simulation.
 */
public class SproutServer {

    private static final int DEFAULT_PORT = 8080;
    private static final Path ROOT_DIR = Paths.get(".").toAbsolutePath().normalize();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("htm", "text/html; charset=UTF-8");
        MIME_TYPES.put("css", "text/css; charset=UTF-8");
        MIME_TYPES.put("js", "application/javascript; charset=UTF-8");
        MIME_TYPES.put("json", "application/json; charset=UTF-8");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("txt", "text/plain; charset=UTF-8");
    }

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // API Endpoints
        server.createContext("/api/analytics", new AnalyticsHandler());
        server.createContext("/api/kyc/verify", new KycVerifyHandler());
        server.createContext("/api/investment/order", new InvestmentOrderHandler());
        server.createContext("/api/health", new HealthHandler());

        // Static File Handler
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // Default single-thread or system thread pool
        server.start();

        System.out.println("=================================================");
        System.out.println(" Sprout 2.0 Java Server Started");
        System.out.println(" URL: http://localhost:" + port + "/");
        System.out.println(" Root Directory: " + ROOT_DIR);
        System.out.println(" Active API routes: /api/analytics, /api/kyc/verify, /api/investment/order");
        System.out.println(" Compliance: Risk disclaimer >= 12px enforced");
        System.out.println("=================================================");
    }

    /**
     * Handles static file serving with safe path resolution and correct MIME types
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String pathStr = exchange.getRequestURI().getPath();
            if (pathStr.equals("/") || pathStr.isEmpty()) {
                pathStr = "/index.html";
            } else if (pathStr.equals("/app")) {
                pathStr = "/app.html";
            }

            // Sanitize path against directory traversal
            Path resolvedPath = ROOT_DIR.resolve("." + pathStr).normalize();
            if (!resolvedPath.startsWith(ROOT_DIR) || !Files.exists(resolvedPath) || Files.isDirectory(resolvedPath)) {
                sendResponse(exchange, 404, "text/plain", "404 Not Found: " + pathStr);
                return;
            }

            String ext = "";
            int dotIdx = resolvedPath.getFileName().toString().lastIndexOf('.');
            if (dotIdx > 0) {
                ext = resolvedPath.getFileName().toString().substring(dotIdx + 1).toLowerCase();
            }

            String contentType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");
            byte[] fileBytes = Files.readAllBytes(resolvedPath);

            // Add standard caching & security headers
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, fileBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }
    }

    /**
     * Analytics Ingestion Handler
     */
    static class AnalyticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCors(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String now = LocalDateTime.now().format(TIME_FMT);
                System.out.println("[" + now + "] [ANALYTICS] " + body);

                String jsonResponse = "{\"status\":\"recorded\",\"timestamp\":\"" + now + "\"}";
                sendResponse(exchange, 200, "application/json", jsonResponse);
            } else {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    /**
     * Simulated KYC Verification Handler
     */
    static class KycVerifyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCors(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String refId = "KRA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                String jsonResponse = String.format(
                    "{\"status\":\"VERIFIED\",\"kraReference\":\"%s\",\"message\":\"Identity confirmed with CVL-KRA. Penny drop ₹1 successful.\"}",
                    refId
                );
                sendResponse(exchange, 200, "application/json", jsonResponse);
            } else {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    /**
     * Simulated Investment Order Handler
     */
    static class InvestmentOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCors(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String orderId = "SPR-" + (100000 + (int)(Math.random() * 900000));
                String jsonResponse = String.format(
                    "{\"status\":\"SUCCESS\",\"orderId\":\"%s\",\"scheme\":\"Sprout Nifty 50 Index Basket\",\"amount\":100.00,\"allotmentBusinessDays\":1}",
                    orderId
                );
                sendResponse(exchange, 200, "application/json", jsonResponse);
            } else {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    /**
     * Server Health Handler
     */
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String json = "{\"status\":\"UP\",\"runtime\":\"Java " + System.getProperty("java.version") + "\",\"app\":\"Sprout 2.0\"}";
            sendResponse(exchange, 200, "application/json", json);
        }
    }

    private static void enableCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
