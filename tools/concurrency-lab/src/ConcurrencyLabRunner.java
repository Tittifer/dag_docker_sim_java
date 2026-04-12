import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConcurrencyLabRunner {
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("\"device_id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

    private final Map<String, String> args;
    private final HttpClient httpClient;

    private ConcurrencyLabRunner(Map<String, String> args) {
        this.args = args;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public static void main(String[] rawArgs) throws Exception {
        Map<String, String> args = parseArgs(rawArgs);
        String mode = args.getOrDefault("--mode", "full");
        ConcurrencyLabRunner runner = new ConcurrencyLabRunner(args);

        if ("register".equals(mode)) {
            List<RegisterResult> registerResults = runner.runRegisterScenario();
            runner.writeRegisterCsv(registerResults, runner.requireArg("--register-output"));
            runner.printRegisterSummary(registerResults);
            return;
        }
        if ("telemetry".equals(mode)) {
            List<RegisterResult> registerResults = runner.loadRegisterCsv(runner.requireArg("--session-file"));
            List<TelemetryResult> telemetryResults = runner.runTelemetryScenario(registerResults);
            runner.writeTelemetryCsv(telemetryResults, runner.requireArg("--telemetry-output"));
            runner.printTelemetrySummary(telemetryResults);
            return;
        }
        if ("full".equals(mode)) {
            List<RegisterResult> registerResults = runner.runRegisterScenario();
            runner.writeRegisterCsv(registerResults, runner.requireArg("--register-output"));
            runner.printRegisterSummary(registerResults);

            List<TelemetryResult> telemetryResults = runner.runTelemetryScenario(registerResults);
            runner.writeTelemetryCsv(telemetryResults, runner.requireArg("--telemetry-output"));
            runner.printTelemetrySummary(telemetryResults);
            return;
        }

        throw new IllegalArgumentException("Unsupported mode: " + mode);
    }

    private List<RegisterResult> runRegisterScenario() {
        String[] terminals = requireArg("--terminals").split(",");
        int deviceCount = Integer.parseInt(requireArg("--device-count"));
        int concurrency = Integer.parseInt(requireArg("--register-concurrency"));
        String baseUrl = trimTrailingSlash(requireArg("--base-url"));
        String devicePrefix = args.getOrDefault("--device-prefix", "sim-device");
        boolean useBootstrapIdentity = Boolean.parseBoolean(args.getOrDefault("--use-bootstrap-identity", "false"));
        boolean autoConfirm = Boolean.parseBoolean(args.getOrDefault("--auto-confirm", "true"));

        List<RegisterPlan> plans = new ArrayList<RegisterPlan>();
        for (int index = 1; index <= deviceCount; index++) {
            String terminalId = terminals[(index - 1) % terminals.length].trim();
            plans.add(new RegisterPlan(index, terminalId, String.format(Locale.ROOT, "%s-%04d", devicePrefix, index)));
        }

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        try {
            List<CompletableFuture<RegisterResult>> futures = new ArrayList<CompletableFuture<RegisterResult>>();
            for (RegisterPlan plan : plans) {
                futures.add(CompletableFuture.supplyAsync(
                    () -> registerOne(baseUrl, plan, useBootstrapIdentity, autoConfirm),
                    executor
                ));
            }
            return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        } finally {
            executor.shutdown();
        }
    }

    private List<TelemetryResult> runTelemetryScenario(List<RegisterResult> registerResults) {
        String baseUrl = trimTrailingSlash(requireArg("--base-url"));
        int messagesPerDevice = Integer.parseInt(requireArg("--messages-per-device"));
        int concurrency = Integer.parseInt(requireArg("--telemetry-concurrency"));
        AtomicInteger sequenceCounter = new AtomicInteger(1);

        List<RegisterResult> successfulDevices = registerResults.stream()
            .filter(RegisterResult::isSuccess)
            .filter(result -> result.deviceId != null && !result.deviceId.isEmpty())
            .collect(Collectors.toList());

        List<TelemetryPlan> plans = new ArrayList<TelemetryPlan>();
        for (RegisterResult device : successfulDevices) {
            for (int sequence = 1; sequence <= messagesPerDevice; sequence++) {
                plans.add(new TelemetryPlan(
                    device.terminalId,
                    device.deviceId,
                    device.deviceName,
                    sequenceCounter.getAndIncrement()
                ));
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        try {
            List<CompletableFuture<TelemetryResult>> futures = new ArrayList<CompletableFuture<TelemetryResult>>();
            for (TelemetryPlan plan : plans) {
                futures.add(CompletableFuture.supplyAsync(() -> submitTelemetry(baseUrl, plan), executor));
            }
            return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        } finally {
            executor.shutdown();
        }
    }

    private RegisterResult registerOne(String baseUrl, RegisterPlan plan, boolean useBootstrapIdentity, boolean autoConfirm) {
        String body = "{"
            + "\"deviceName\":\"" + escapeJson(plan.deviceName) + "\","
            + "\"useBootstrapIdentity\":" + useBootstrapIdentity + ","
            + "\"autoConfirm\":" + autoConfirm
            + "}";

        String url = baseUrl + "/api/fusions/" + plan.terminalId + "/devices/register";
        Instant startedAt = Instant.now();
        try {
            String responseBody = postJson(url, body);
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            return new RegisterResult(
                plan.index,
                true,
                plan.terminalId,
                plan.deviceName,
                extractValue(responseBody, DEVICE_ID_PATTERN),
                durationMs,
                extractValue(responseBody, MESSAGE_PATTERN)
            );
        } catch (RuntimeException exception) {
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            return new RegisterResult(
                plan.index,
                false,
                plan.terminalId,
                plan.deviceName,
                "",
                durationMs,
                exception.getMessage()
            );
        }
    }

    private TelemetryResult submitTelemetry(String baseUrl, TelemetryPlan plan) {
        double capturedAt = System.currentTimeMillis() / 1000.0;
        double voltage = 220.0 + (plan.sequence % 15);
        double current = 10.0 + ((plan.sequence * 3) % 8);
        double temperature = 25.0 + ((plan.sequence * 2) % 12);
        double power = 2.0 + ((plan.sequence % 20) / 10.0);

        String body = "{"
            + "\"dataPayload\":{"
            + "\"sequence\":" + plan.sequence + ","
            + "\"device_name\":\"" + escapeJson(plan.deviceName) + "\","
            + "\"captured_at\":" + String.format(Locale.ROOT, "%.3f", capturedAt) + ","
            + "\"metrics\":{"
            + "\"voltage_v\":" + String.format(Locale.ROOT, "%.2f", voltage) + ","
            + "\"current_a\":" + String.format(Locale.ROOT, "%.2f", current) + ","
            + "\"temperature_c\":" + String.format(Locale.ROOT, "%.2f", temperature) + ","
            + "\"active_power_kw\":" + String.format(Locale.ROOT, "%.3f", power)
            + "}"
            + "}"
            + "}";

        String url = baseUrl + "/api/fusions/" + plan.terminalId + "/devices/" + plan.deviceId + "/telemetry";
        Instant startedAt = Instant.now();
        try {
            String responseBody = postJson(url, body);
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            return new TelemetryResult(
                true,
                plan.terminalId,
                plan.deviceId,
                plan.deviceName,
                plan.sequence,
                durationMs,
                extractValue(responseBody, MESSAGE_PATTERN)
            );
        } catch (RuntimeException exception) {
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            return new TelemetryResult(
                false,
                plan.terminalId,
                plan.deviceId,
                plan.deviceName,
                plan.sequence,
                durationMs,
                exception.getMessage()
            );
        }
    }

    private String postJson(String url, String body) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("HTTP " + response.statusCode() + " -> " + response.body());
            }
            return response.body();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Request interrupted", exception);
        } catch (CompletionException exception) {
            throw new IllegalStateException("Request failed", exception);
        }
    }

    private List<RegisterResult> loadRegisterCsv(String path) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
            List<RegisterResult> results = new ArrayList<RegisterResult>();
            for (int index = 1; index < lines.size(); index++) {
                String line = lines.get(index);
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] columns = parseCsvLine(line);
                results.add(new RegisterResult(
                    Integer.parseInt(columns[0]),
                    Boolean.parseBoolean(columns[1]),
                    columns[2],
                    columns[3],
                    columns[4],
                    Long.parseLong(columns[5]),
                    columns[6]
                ));
            }
            return results;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void writeRegisterCsv(List<RegisterResult> results, String path) {
        List<String> lines = new ArrayList<String>();
        lines.add("index,success,terminal_id,device_name,device_id,duration_ms,message");
        for (RegisterResult result : results) {
            lines.add(csv(
                Integer.toString(result.index),
                Boolean.toString(result.success),
                result.terminalId,
                result.deviceName,
                result.deviceId,
                Long.toString(result.durationMs),
                result.message
            ));
        }
        writeLines(path, lines);
    }

    private void writeTelemetryCsv(List<TelemetryResult> results, String path) {
        List<String> lines = new ArrayList<String>();
        lines.add("success,terminal_id,device_id,device_name,sequence,duration_ms,message");
        for (TelemetryResult result : results) {
            lines.add(csv(
                Boolean.toString(result.success),
                result.terminalId,
                result.deviceId,
                result.deviceName,
                Integer.toString(result.sequence),
                Long.toString(result.durationMs),
                result.message
            ));
        }
        writeLines(path, lines);
    }

    private void writeLines(String path, List<String> lines) {
        Path outputPath = Paths.get(path);
        try {
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }
            Files.write(outputPath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void printRegisterSummary(List<RegisterResult> results) {
        long successCount = results.stream().filter(RegisterResult::isSuccess).count();
        long averageDuration = results.isEmpty()
            ? 0
            : Math.round(results.stream().mapToLong(result -> result.durationMs).average().orElse(0));
        System.out.println("Register total: " + results.size());
        System.out.println("Register success: " + successCount);
        System.out.println("Register failed: " + (results.size() - successCount));
        System.out.println("Register avg ms: " + averageDuration);
        System.out.println("Register output: " + requireArg("--register-output"));
    }

    private void printTelemetrySummary(List<TelemetryResult> results) {
        long successCount = results.stream().filter(TelemetryResult::isSuccess).count();
        long averageDuration = results.isEmpty()
            ? 0
            : Math.round(results.stream().mapToLong(result -> result.durationMs).average().orElse(0));
        System.out.println("Telemetry total: " + results.size());
        System.out.println("Telemetry success: " + successCount);
        System.out.println("Telemetry failed: " + (results.size() - successCount));
        System.out.println("Telemetry avg ms: " + averageDuration);
        System.out.println("Telemetry output: " + requireArg("--telemetry-output"));
    }

    private String requireArg(String key) {
        String value = args.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing argument: " + key);
        }
        return value;
    }

    private static String[] parseCsvLine(String line) {
        List<String> columns = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);
            if (currentChar == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
                continue;
            }
            if (currentChar == ',' && !quoted) {
                columns.add(current.toString());
                current = new StringBuilder();
                continue;
            }
            current.append(currentChar);
        }
        columns.add(current.toString());
        return columns.toArray(new String[0]);
    }

    private static Map<String, String> parseArgs(String[] rawArgs) {
        Map<String, String> parsed = new LinkedHashMap<String, String>();
        for (int index = 0; index < rawArgs.length; index += 2) {
            if (index + 1 >= rawArgs.length) {
                throw new IllegalArgumentException("Expected value after argument: " + rawArgs[index]);
            }
            parsed.put(rawArgs[index], rawArgs[index + 1]);
        }
        return parsed;
    }

    private static String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String extractValue(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static String csv(String... columns) {
        return Arrays.stream(columns).map(ConcurrencyLabRunner::escapeCsv).collect(Collectors.joining(","));
    }

    private static String escapeCsv(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.contains(",") || safeValue.contains("\"") || safeValue.contains("\n")) {
            return "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }
        return safeValue;
    }

    private static String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    private static final class RegisterPlan {
        private final int index;
        private final String terminalId;
        private final String deviceName;

        private RegisterPlan(int index, String terminalId, String deviceName) {
            this.index = index;
            this.terminalId = terminalId;
            this.deviceName = deviceName;
        }
    }

    private static final class TelemetryPlan {
        private final String terminalId;
        private final String deviceId;
        private final String deviceName;
        private final int sequence;

        private TelemetryPlan(String terminalId, String deviceId, String deviceName, int sequence) {
            this.terminalId = terminalId;
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.sequence = sequence;
        }
    }

    private static final class RegisterResult {
        private final int index;
        private final boolean success;
        private final String terminalId;
        private final String deviceName;
        private final String deviceId;
        private final long durationMs;
        private final String message;

        private RegisterResult(
            int index,
            boolean success,
            String terminalId,
            String deviceName,
            String deviceId,
            long durationMs,
            String message
        ) {
            this.index = index;
            this.success = success;
            this.terminalId = terminalId;
            this.deviceName = deviceName;
            this.deviceId = deviceId;
            this.durationMs = durationMs;
            this.message = message == null ? "" : message.replace('\r', ' ').replace('\n', ' ');
        }

        private boolean isSuccess() {
            return success;
        }
    }

    private static final class TelemetryResult {
        private final boolean success;
        private final String terminalId;
        private final String deviceId;
        private final String deviceName;
        private final int sequence;
        private final long durationMs;
        private final String message;

        private TelemetryResult(
            boolean success,
            String terminalId,
            String deviceId,
            String deviceName,
            int sequence,
            long durationMs,
            String message
        ) {
            this.success = success;
            this.terminalId = terminalId;
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.sequence = sequence;
            this.durationMs = durationMs;
            this.message = message == null ? "" : message.replace('\r', ' ').replace('\n', ' ');
        }

        private boolean isSuccess() {
            return success;
        }
    }
}
