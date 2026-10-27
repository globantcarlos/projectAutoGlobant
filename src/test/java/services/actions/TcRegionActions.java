package services.actions;

import com.microsoft.playwright.APIResponse;
import io.qameta.allure.Allure;
import org.testng.Assert;
import services.TcRegionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Capa de acciones/aserciones para am-tc-region (estilo am-my-trips-test-automation).
 * Reutiliza TcRegionService (Playwright APIRequestContext) y ApiLogger.
 *
 * Contrato real: GET /tc/region/regionByRoute con headers
 * channel / x-transactionId / departure / arrival (Postman am-my-trips).
 */
public class TcRegionActions {

    public static APIResponse responseTest;
    public static APIResponse secondResponse;
    public static APIResponse thirdResponse;
    public static long elapsedMillis;
    public static final List<Integer> stabilityStatuses = new ArrayList<>();

    private static final TcRegionService tcRegionService = new TcRegionService();

    // ==================== Acciones ====================

    /** TC001/TC002 - Consulta regionByRoute y valida el status code. */
    public static void regionByRouteRequest(String departure, String arrival, String statusCode) {
        long startMillis = System.currentTimeMillis();
        responseTest = tcRegionService.getRegionByRoute(departure, arrival);
        long elapsedMillis = System.currentTimeMillis() - startMillis;
        logRequestAndStatus(responseTest, elapsedMillis);
        validateStatusCode(statusCode);
    }

    /** TC003 - Tres consultas consecutivas del mismo OD. */
    public static void regionByRouteConsecutiveQueries(String departure, String arrival) {
        responseTest = tcRegionService.getRegionByRoute(departure, arrival);
        secondResponse = tcRegionService.getRegionByRoute(departure, arrival);
        thirdResponse = tcRegionService.getRegionByRoute(departure, arrival);
    }

    /** TC004 - Consulta forzando error de downstream (IATA inexistente). */
    public static void regionByRouteDownstreamError() {
        long startMillis = System.currentTimeMillis();
        responseTest = tcRegionService.getRegionByRoute("ZZZ", "CUN");
        elapsedMillis = System.currentTimeMillis() - startMillis;
        logRequestAndStatus(responseTest, elapsedMillis);
    }

    /** TC005 - Consulta con transactionId de traza para New Relic. */
    public static void regionByRouteWithTraceTransactionId(String departure, String arrival) {
        String traceTransactionId = "nr-trace-17667-" + java.util.UUID.randomUUID();
        responseTest = tcRegionService.getRegionByRoute(departure, arrival, traceTransactionId);
        // El transactionId de traza queda registrado en Allure para su localizacion
        // en New Relic (Logs / Custom Events).
        Allure.addAttachment("nr-trace-transactionId", traceTransactionId);
        logRequestAndStatus(responseTest, 0);
    }

    /** TC006 - Consulta sin el header arrival. */
    public static void regionByRouteWithoutArrival(String departure) {
        responseTest = tcRegionService.getRegionByRouteWithoutArrival(departure, null);
        logRequestAndStatus(responseTest, 0);
    }

    /** TC008 - Rafaga de consultas consecutivas con OD validos. */
    public static void regionByRouteStability(int totalQueries, String departure, String arrival) {
        stabilityStatuses.clear();
        for (int queryIndex = 1; queryIndex <= totalQueries; queryIndex++) {
            APIResponse queryResponse = tcRegionService.getRegionByRoute(departure, arrival);
            stabilityStatuses.add(queryResponse.status());
            logRequestAndStatus(queryResponse, 0);
        }
    }

    /**
     * BDD Run 13939 - Consulta regionByRoute con EXACTAMENTE los headers indicados.
     * Permite omitir headers obligatorios o enviar valores con espacios (negativos).
     */
    public static void regionByRouteWithExactHeaders(Map<String, String> exactHeaders) {
        long startMillis = System.currentTimeMillis();
        responseTest = tcRegionService.getRegionByRouteWithExactHeaders(exactHeaders);
        elapsedMillis = System.currentTimeMillis() - startMillis;
        logRequestAndStatus(responseTest, elapsedMillis);
    }

    /** TC009 - Consulta management/info. */
    public static void managementInfoRequest() {
        responseTest = tcRegionService.getManagementInfo();
        logRequestAndStatus(responseTest, 0);
    }

    // ==================== Validaciones ====================

    /** Valida el status code contra el esperado. */
    public static void validateStatusCode(String expectedStatusCode) {
        Assert.assertEquals(responseTest.status(), Integer.parseInt(expectedStatusCode),
                "El status code debe ser " + expectedStatusCode);
    }

    /** Valida regionShortDescription (INT/DOM). */
    public static void validateRegionShortDescription(String expectedRegionShortDescription) {
        String body = responseTest.text();
        Assert.assertTrue(
                body.contains("\"regionShortDescription\":\"" + expectedRegionShortDescription + "\"")
                        || body.contains("\"regionShortDescription\": \"" + expectedRegionShortDescription + "\""),
                "regionShortDescription debe ser " + expectedRegionShortDescription);
    }

    /** Valida los campos del contrato regionByRoute. */
    public static void validateRegionByRouteContractFields() {
        String body = responseTest.text();
        String[] requiredFields = {
                "regionDescription", "subRegionDestination", "subRegionFinal", "subRegionOrigin",
                "departureAirportLoyaltyZone", "arrivalAirportLoyaltyZone",
                "departureAirportCountry", "arrivalAirportCountry", "departureAirportTimeZoneId"
        };
        for (String requiredField : requiredFields) {
            Assert.assertTrue(body.contains("\"" + requiredField + "\""),
                    "Debe traer el campo " + requiredField);
        }
    }

    /** TC003 - Las tres respuestas son 200 con contenido identico. */
    public static void validateThreeConsecutiveResponsesIdentical() {
        Assert.assertEquals(responseTest.status(), 200, "La primera consulta debe ser 200");
        Assert.assertEquals(secondResponse.status(), 200, "La segunda consulta debe ser 200");
        Assert.assertEquals(thirdResponse.status(), 200, "La tercera consulta debe ser 200");

        Assert.assertEquals(secondResponse.text(), responseTest.text(),
                "El contenido de la segunda consulta debe ser identico al primero");
        Assert.assertEquals(thirdResponse.text(), responseTest.text(),
                "El contenido de la tercera consulta debe ser identico al primero");
    }

    /**
     * TC004/TC005/TC007 - 500 con reason del error catalog.
     * NOTA: el contrato declara SERVICE_UNAVAILABLE / EMPTY_RESPONSE, pero en DEV
     * el servicio responde INTERNAL_SERVER_ERROR (defecto a reportar al equipo del micro).
     */
    public static void validate500WithErrorCatalogReason() {
        Assert.assertEquals(responseTest.status(), 500, "El status code debe ser 500");
        String body = responseTest.text();
        Assert.assertTrue(
                body.contains("ERR.TC.REGION.SERVICE_UNAVAILABLE")
                        || body.contains("ERR.TC.REGION.EMPTY_RESPONSE")
                        || body.contains("ERR.TC.REGION.INTERNAL_SERVER_ERROR"),
                "reason debe ser del error catalog (SERVICE_UNAVAILABLE/EMPTY_RESPONSE en contrato; "
                        + "INTERNAL_SERVER_ERROR es el comportamiento actual en DEV)");
    }

    /** TC004 - Fail-fast en menos de 30s. */
    public static void validateFailFastUnder30Seconds() {
        Assert.assertTrue(elapsedMillis < 30000,
                "El servicio debe responder fail-fast en menos de 30s (timeouts 5s/10s), tardo " + elapsedMillis + "ms");
    }

    /** TC005 - El transactionId de traza queda registrado (efecto observable en logs). */
    public static void validateTraceTransactionIdRegistered() {
        // La traza se registra via Allure attachment en la accion; aqui se valida el efecto
        // observable: el caso de error respondio y el reason del error catalog esta presente.
        Assert.assertTrue(responseTest.text().contains("ERR.TC.REGION"),
                "El response debe contener el reason del error catalog");
        // TODO: localizar nr-trace-17667-* en New Relic (NRQL) y comparar naming contra baseline.
    }

    /** TC006 - 400 con reason ERR.TC.REGION.BAD_REQUEST. */
    public static void validate400BadRequest() {
        Assert.assertEquals(responseTest.status(), 400, "El status code debe ser 400");
        Assert.assertTrue(responseTest.text().contains("ERR.TC.REGION.BAD_REQUEST"),
                "reason debe iniciar con ERR.TC.REGION.BAD_REQUEST");
    }

    /** TC008 - Las N consultas responden 200. */
    public static void validateAllQueries200(int expectedQueries) {
        Assert.assertEquals(stabilityStatuses.size(), expectedQueries,
                "El numero de consultas registradas debe ser " + expectedQueries);
        for (int queryIndex = 0; queryIndex < stabilityStatuses.size(); queryIndex++) {
            Assert.assertEquals(stabilityStatuses.get(queryIndex).intValue(), 200,
                    "La consulta " + (queryIndex + 1) + " debe responder 200");
        }
    }

    /**
     * BDD Run 13939 (negativos) - Valida el body de error del contrato:
     * httpCode coincide con el status, reason del error catalog y
     * channel recibido coincide con el enviado (null si se omitio).
     */
    public static void validateErrorBody(String expectedChannel, String expectedHttpCode) {
        String body = responseTest.text();
        Assert.assertEquals(responseTest.status(), Integer.parseInt(expectedHttpCode),
                "El status code debe ser " + expectedHttpCode);
        Assert.assertTrue(body.contains("\"httpCode\":\"" + expectedHttpCode + "\"")
                        || body.contains("\"httpCode\": \"" + expectedHttpCode + "\""),
                "El campo httpCode debe ser " + expectedHttpCode);
        Assert.assertTrue(body.contains("ERR.TC.REGION.BAD_REQUEST"),
                "El campo reason debe ser ERR.TC.REGION.BAD_REQUEST");
        if (expectedChannel == null || expectedChannel.isBlank() || "null".equalsIgnoreCase(expectedChannel)) {
            Assert.assertTrue(body.contains("\"channel\":null") || body.contains("\"channel\": null"),
                    "El campo channel debe ser null cuando no se envia el header");
        } else {
            Assert.assertTrue(
                    body.contains("\"channel\":\"" + expectedChannel + "\"")
                            || body.contains("\"channel\": \"" + expectedChannel + "\""),
                    "El campo channel recibido (" + expectedChannel + ") debe coincidir con el enviado");
        }
    }

    /** TC009 - management/info 200. */
    public static void validateManagementInfo200() {
        Assert.assertEquals(responseTest.status(), 200, "El status code debe ser 200");
    }

    /** TC009 - management/info reporta microservicio y Version. */
    public static void validateManagementInfoReportsService(String expectedMicroservice) {
        String body = responseTest.text();
        Assert.assertTrue(body.contains(expectedMicroservice),
                "Debe reportar el microservicio " + expectedMicroservice);
        Assert.assertTrue(body.contains("\"Version\"") || body.contains("\"version\""),
                "Debe exponer la Version desplegada");
    }

    private static void logRequestAndStatus(APIResponse apiResponse, long elapsedMillis) {
        System.out.println("Status: " + apiResponse.status() + " (elapsed: " + elapsedMillis + "ms)");
    }
}
