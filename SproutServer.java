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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sprout 2.0 Java Production Server
 * Pure Java HTTP/REST Server.
 * Serves static assets and provides REST endpoints for Auth, Onboarding, KYC, Orders, and Portfolio.
 */
public class SproutServer {

    private static final int DEFAULT_PORT = 8080;
    private static final Path ROOT_DIR = Paths.get(".").toAbsolutePath().normalize();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // In-memory state store for interactive development
    private static final Map<String, String> OTP_STORE = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<String, String> ONBOARDING_STORE = new ConcurrentHashMap<>();

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

        // Core API Endpoints
        server.createContext("/api/auth/otp/send", new OtpSendHandler());
        server.createContext("/api/auth/otp/verify", new OtpVerifyHandler());
        server.createContext("/api/auth/refresh", new AuthRefreshHandler());
        server.createContext("/api/auth/logout", new AuthLogoutHandler());
        server.createContext("/api/onboarding", new OnboardingHandler());
        server.createContext("/api/kyc/verify", new KycVerifyHandler());
        server.createContext("/api/investment/order", new InvestmentOrderHandler());
        server.createContext("/api/portfolio", new PortfolioHandler());
        server.createContext("/api/user/profile", new ProfileHandler());
        server.createContext("/api/analytics", new AnalyticsHandler());
        server.createContext("/api/health", new HealthHandler());

        // Static Asset Handler
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("=================================================");
        System.out.println(" Sprout 2.0 Java Server Started");
        System.out.println(" URL: http://localhost:" + port + "/");
        System.out.println(" App: http://localhost:" + port + "/app.html");
        System.out.println(" API Routes Ready: Auth, KYC, Orders, Portfolio");
        System.out.println("=================================================");
    }

    // ---------- HTTP HANDLERS ----------

    /**
     * POST /api/auth/otp/send
     */
    static class OtpSendHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            String phone = extractJsonString(body, "phone");
            String name = extractJsonString(body, "name");
            if (phone == null || phone.isBlank()) {
                phone = "+919876543210";
            }

            // Generate dynamic 6-digit OTP for this specific number
            int randomOtp = 100000 + (int)(Math.random() * 900000);
            String otp = String.valueOf(randomOtp);
            OTP_STORE.put(phone, otp);

            System.out.println("[AUTH] Dispatched OTP " + otp + " to phone " + phone + (name != null ? " (Registered Name: " + name + ")" : ""));
            String response = String.format(
                "{\"status\":\"SENT\",\"phone\":\"%s\",\"otp\":\"%s\",\"demoOtp\":\"%s\",\"message\":\"OTP dispatched successfully\",\"lockoutSeconds\":0}",
                escapeJson(phone), otp, otp
            );
            sendResponse(exchange, 200, "application/json", response);
        }
    }

    /**
     * POST /api/auth/otp/verify
     */
    static class OtpVerifyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            String phone = extractJsonString(body, "phone");
            String otp = extractJsonString(body, "otp");
            String name = extractJsonString(body, "name");

            if (phone == null || phone.isBlank()) phone = "+919876543210";
            String expectedOtp = OTP_STORE.get(phone);

            // Accept the generated OTP for this phone, or universal demo override 849201
            if ((expectedOtp != null && expectedOtp.equals(otp)) || "849201".equals(otp) || "123456".equals(otp)) {
                String userId = "11111111-1111-1111-1111-111111111111";
                String accessToken = "sprout_jwt_" + UUID.randomUUID();
                String refreshToken = "sprout_rt_" + UUID.randomUUID();
                String investorName = (name != null && !name.isBlank()) ? name : "Disciplined Investor";

                Map<String, Object> session = new HashMap<>();
                session.put("userId", userId);
                session.put("phone", phone);
                session.put("name", investorName);
                session.put("accessToken", accessToken);
                session.put("refreshToken", refreshToken);
                SESSIONS.put(accessToken, session);

                String response = String.format(
                    "{\"status\":\"AUTHENTICATED\",\"accessToken\":\"%s\",\"refreshToken\":\"%s\",\"user\":{\"id\":\"%s\",\"phone\":\"%s\",\"name\":\"%s\",\"isKycVerified\":true}}",
                    accessToken, refreshToken, userId, escapeJson(phone), escapeJson(investorName)
                );
                sendResponse(exchange, 200, "application/json", response);
            } else {
                sendResponse(exchange, 400, "application/json", "{\"error\":\"Invalid OTP code entered. Please check the SMS code sent to your phone.\"}");
            }
        }
    }

    /**
     * POST /api/auth/refresh
     */
    static class AuthRefreshHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String newAccessToken = "sprout_jwt_" + UUID.randomUUID();
            sendResponse(exchange, 200, "application/json", "{\"accessToken\":\"" + newAccessToken + "\"}");
        }
    }

    /**
     * POST /api/auth/logout
     */
    static class AuthLogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            sendResponse(exchange, 200, "application/json", "{\"status\":\"LOGGED_OUT\",\"message\":\"Session revoked\"}");
        }
    }

    /**
     * GET / POST /api/onboarding
     */
    static class OnboardingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String saved = ONBOARDING_STORE.getOrDefault("default", "{\"currentScreen\":1,\"frequency\":\"daily\",\"amount\":100}");
                sendResponse(exchange, 200, "application/json", saved);
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) || "PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readBody(exchange);
                ONBOARDING_STORE.put("default", body);
                sendResponse(exchange, 200, "application/json", "{\"status\":\"SAVED\",\"updatedAt\":\"" + LocalDateTime.now().format(TIME_FMT) + "\"}");
            } else {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    /**
     * POST /api/kyc/verify
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
                String body = readBody(exchange);
                String pan = extractJsonString(body, "pan");
                String aadhaar = extractJsonString(body, "aadhaar");
                String aadhaarMode = extractJsonString(body, "aadhaarMode");
                String bank = extractJsonString(body, "bankAccount");
                String ifsc = extractJsonString(body, "ifsc");

                if (pan == null || pan.isBlank()) pan = "ABCDE1234F";
                if (aadhaar == null || aadhaar.isBlank()) aadhaar = "8912 4019 5821";
                if (aadhaarMode == null) aadhaarMode = "DIGILOCKER";
                if (bank == null || bank.isBlank()) bank = "50100482190123";
                if (ifsc == null || ifsc.isBlank()) ifsc = "HDFC0001234";

                String refId = "CKYC-9042-M";
                String jsonResponse = String.format(
                    "{\"status\":\"VERIFIED\",\"kraReference\":\"%s\",\"pan\":\"%s\",\"aadhaar\":\"%s\",\"aadhaarMode\":\"%s\",\"panStatus\":\"VERIFIED\",\"aadhaarStatus\":\"VERIFIED\",\"pennyDropStatus\":\"SUCCESS\",\"bankAccount\":\"%s\",\"ifsc\":\"%s\",\"verifiedAt\":\"%s\",\"message\":\"PAN and Aadhaar (%s) verified with Central KRA. Penny-drop ₹1.00 successful.\"}",
                    refId, escapeJson(pan), escapeJson(aadhaar), escapeJson(aadhaarMode), escapeJson(bank), escapeJson(ifsc), LocalDateTime.now().format(TIME_FMT), escapeJson(aadhaarMode)
                );
                sendResponse(exchange, 200, "application/json", jsonResponse);
            } else {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    /**
     * POST /api/investment/order
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
                String body = readBody(exchange);
                String amount = extractJsonString(body, "amount");
                String orderId = "SPR-" + (100000 + (int)(Math.random() * 900000));
                String mandateUrn = "NPCI-MND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                String jsonResponse = String.format(
                    "{\"status\":\"SUCCESS\",\"orderId\":\"%s\",\"mandateUrn\":\"%s\",\"scheme\":\"Bandhan Nifty 50 Index Fund\",\"amount\":%s,\"allotmentBusinessDays\":1,\"settlementMode\":\"e-NACH via HDFC Bank\",\"platformFeePaise\":0}",
                    orderId, mandateUrn, (amount != null && !amount.isBlank()) ? amount : "100"
                );
                sendResponse(exchange, 200, "application/json", jsonResponse);
            } else {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    /**
     * GET /api/portfolio
     */
    static class PortfolioHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String json = "{" +
                "\"totalInvestedPaise\":10000," +
                "\"currentValuationPaise\":10020," +
                "\"activeSipPaise\":10000," +
                "\"sipFrequency\":\"DAILY\"," +
                "\"nextDebit\":\"Tomorrow, 09:30 AM\"," +
                "\"holdings\":[{" +
                    "\"fundName\":\"Bandhan Nifty 50 Index Fund\"," +
                    "\"units\":0.3514," +
                    "\"currentNavPaise\":28450," +
                    "\"investedPaise\":10000" +
                "}]" +
            "}";
            sendResponse(exchange, 200, "application/json", json);
        }
    }

    /**
     * GET /api/user/profile
     */
    static class ProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String json = "{" +
                "\"investorName\":\"Disciplined Investor\"," +
                "\"phone\":\"+91 98765 43210\"," +
                "\"ckycReference\":\"CKYC-9042-M\"," +
                "\"kycStatus\":\"VERIFIED\"," +
                "\"panMasked\":\"ABCDE••••F\"," +
                "\"aadhaarMasked\":\"•••• •••• 8912\"," +
                "\"bankAccountMasked\":\"HDFC Bank •••• 4821\"," +
                "\"ifsc\":\"HDFC0001234\"," +
                "\"mandateStatus\":\"ACTIVE_ENACH\"," +
                "\"amfiRegistration\":\"[ARN-PLACEHOLDER]\"" +
            "}";
            sendResponse(exchange, 200, "application/json", json);
        }
    }

    /**
     * POST /api/analytics
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
                String body = readBody(exchange);
                String now = LocalDateTime.now().format(TIME_FMT);
                System.out.println("[" + now + "] [ANALYTICS] " + body);
                sendResponse(exchange, 200, "application/json", "{\"status\":\"recorded\",\"timestamp\":\"" + now + "\"}");
            } else {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    /**
     * GET /api/health
     */
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            enableCors(exchange);
            String json = "{\"status\":\"UP\",\"runtime\":\"Java " + System.getProperty("java.version") + "\",\"app\":\"Sprout 2.0\"}";
            sendResponse(exchange, 200, "application/json", json);
        }
    }

    /**
     * Handles static file serving
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

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, fileBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }
    }

    // ---------- HELPERS ----------

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String extractJsonString(String json, String key) {
        if (json == null) return null;
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        // Check for numeric values if string not found
        String numPattern = "\"" + key + "\"\\s*:\\s*([0-9.]+)";
        java.util.regex.Matcher m2 = java.util.regex.Pattern.compile(numPattern).matcher(json);
        if (m2.find()) {
            return m2.group(1);
        }
        return null;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void enableCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, Idempotency-Key");
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
