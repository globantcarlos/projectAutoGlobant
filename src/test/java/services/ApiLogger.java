package services;

import io.qameta.allure.Allure;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utilidad para loggear peticiones/respuestas de API.
 * - Consola: visible en el panel de ejecución de IntelliJ / terminal.
 * - Allure: adjunta el curl y la respuesta al resultado de cada test.
 */
public final class ApiLogger {

    private ApiLogger() {
    }

    /** Construye el equivalente curl de la petición ejecutada. */
    public static String buildCurl(String method, String url, String payload) {
        StringBuilder curl = new StringBuilder("curl -i");
        if (payload != null) {
            curl.append(" -X ").append(method.toUpperCase())
                .append(" -H 'Content-Type: application/json'")
                .append(" -d '").append(payload.replace("'", "'\\''")).append("'");
        }
        curl.append(" '").append(url).append("'");
        return curl.toString();
    }

    /**
     * Loggea la petición (como curl) y la respuesta en consola,
     * y las adjunta al reporte de Allure.
     */
    public static void logRequestAndResponse(String method, String url,
                                             String requestBody,
                                             int statusCode,
                                             String responseBody) {
        String curl = buildCurl(method, url, requestBody);
        String log = String.format(
                "==================== REQUEST ====================%n"
                        + "%s%n"
                        + "==================== RESPONSE ===================%n"
                        + "Status: %d%n"
                        + "Body: %s%n"
                        + "=================================================",
                curl, statusCode, responseBody
        );

        // Consola (panel Run de IntelliJ)
        System.out.println(log);

        // Adjuntos en Allure
        Allure.addAttachment("Request (curl)", "text/plain",
                new ByteArrayInputStream(curl.getBytes(StandardCharsets.UTF_8)), ".txt");
        Allure.addAttachment("Response", "application/json",
                new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8)), ".json");
    }
}
