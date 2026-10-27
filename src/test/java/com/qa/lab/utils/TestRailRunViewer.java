package com.qa.lab.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * Script utilitario para consultar los test cases de un Test Run en TestRail.
 * Exporta la información a un archivo JSON en docs/testrail-run-{id}.json
 *
 * USO desde IntelliJ: Run con argumento del runId (ej: 10671)
 * USO por Maven: mvn compile test-compile exec:java -Dexec.mainClass=com.qa.lab.utils.TestRailRunViewer -Dexec.args=10671
 */
public class TestRailRunViewer {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) throws IOException, InterruptedException {
        int runId;

        if (args.length > 0) {
            runId = Integer.parseInt(args[0]);
        } else {
            System.out.print("Ingresa el Run ID de TestRail: ");
            Scanner scanner = new Scanner(System.in);
            runId = scanner.nextInt();
            scanner.close();
        }

        Properties props = new Properties();
        try (InputStream is = TestRailRunViewer.class.getClassLoader()
                .getResourceAsStream("properties/testrail.properties")) {
            if (is == null) {
                System.err.println("ERROR: testrail.properties no encontrado");
                System.exit(1);
            }
            props.load(is);
        }

        String baseUrl = props.getProperty("testrail.url");
        String username = props.getProperty("testrail.username");
        String apiKey = props.getProperty("testrail.apikey");
        String authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + apiKey).getBytes());

        // Obtener info del run
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  TESTRAIL RUN VIEWER - Run ID: " + runId);
        System.out.println("═══════════════════════════════════════════════════════════════");

        JsonNode runInfo = apiGet(baseUrl + "/index.php?/api/v2/get_run/" + runId, authHeader);
        if (runInfo != null) {
            System.out.println("  Nombre: " + runInfo.path("name").asText());
            System.out.println("  Proyecto ID: " + runInfo.path("project_id").asInt());
            System.out.println("  Completado: " + (runInfo.path("is_completed").asBoolean() ? "SI" : "NO"));
            System.out.println("  URL: " + baseUrl + "/index.php?/runs/view/" + runId);
        }

        // Obtener tests del run
        System.out.println("\n───────────────────────────────────────────────────────────────");
        System.out.println("  TEST CASES EN EL RUN");
        System.out.println("───────────────────────────────────────────────────────────────\n");

        JsonNode testsResponse = apiGet(baseUrl + "/index.php?/api/v2/get_tests/" + runId, authHeader);
        JsonNode tests = testsResponse != null && testsResponse.has("tests")
                ? testsResponse.get("tests") : testsResponse;

        if (tests == null || !tests.isArray()) {
            System.err.println("ERROR: No se pudieron obtener los tests del run");
            System.exit(1);
        }

        List<TestInfo> allTests = new ArrayList<>();
        for (JsonNode test : tests) {
            TestInfo info = new TestInfo();
            info.testId = test.get("id").asInt();
            info.caseId = test.get("case_id").asInt();
            info.title = test.path("title").asText("Sin título");
            info.statusId = test.path("status_id").asInt(0);
            allTests.add(info);

            String statusIcon = getStatusIcon(info.statusId);
            System.out.printf("  %s C%-8d T%-10d %s%n",
                    statusIcon, info.caseId, info.testId, info.title);
        }

        System.out.println("\n───────────────────────────────────────────────────────────────");
        System.out.println("  TOTAL: " + allTests.size() + " test cases");
        System.out.println("═══════════════════════════════════════════════════════════════");

        // Obtener detalle de cada case y exportar a JSON
        System.out.println("\n  Obteniendo detalle de cada case...");
        List<Map<String, Object>> exportData = new ArrayList<>();

        for (TestInfo info : allTests) {
            Map<String, Object> caseDetail = getCaseDetail(info.caseId, baseUrl, authHeader);
            if (caseDetail != null) {
                caseDetail.put("testId", info.testId);
                caseDetail.put("statusId", info.statusId);
                exportData.add(caseDetail);
                System.out.println("    ✓ C" + info.caseId + " - " + info.title);
            }
        }

        // Exportar a JSON
        String outputPath = "docs/testrail-run-" + runId + ".json";
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new java.io.File(outputPath), exportData);

        System.out.println("  Total cases: " + exportData.size());
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    private static Map<String, Object> getCaseDetail(int caseId, String baseUrl, String authHeader)
            throws IOException, InterruptedException {
        JsonNode caseNode = apiGet(baseUrl + "/index.php?/api/v2/get_case/" + caseId, authHeader);
        if (caseNode == null) return null;

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("caseId", caseId);
        detail.put("title", caseNode.path("title").asText("Sin título"));
        detail.put("refs", caseNode.path("refs").asText(""));
        detail.put("preconditions", caseNode.path("custom_preconds").asText(""));
        detail.put("steps", caseNode.path("custom_steps").asText(""));
        detail.put("expected", caseNode.path("custom_expected").asText(""));

        JsonNode stepsSeparated = caseNode.path("custom_steps_separated");
        if (stepsSeparated.isArray() && stepsSeparated.size() > 0) {
            List<Map<String, String>> stepsList = new ArrayList<>();
            for (JsonNode step : stepsSeparated) {
                Map<String, String> stepMap = new LinkedHashMap<>();
                stepMap.put("content", step.path("content").asText(""));
                stepMap.put("expected", step.path("expected").asText(""));
                stepsList.add(stepMap);
            }
            detail.put("stepsSeparated", stepsList);
        }

        return detail;
    }

    private static JsonNode apiGet(String url, String authHeader) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readTree(response.body());
        } else {
            System.err.println("ERROR API: HTTP " + response.statusCode() + " - " + response.body());
            return null;
        }
    }

    private static String getStatusIcon(int statusId) {
        return switch (statusId) {
            case 1 -> "✅";
            case 2 -> "⛔";
            case 3 -> "⏭️";
            case 4 -> "🔄";
            case 5 -> "❌";
            default -> "⬜";
        };
    }

    private static class TestInfo {
        int testId;
        int caseId;
        String title;
        int statusId;
    }
}
