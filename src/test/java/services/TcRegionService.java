package services;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service Wrapper (equivalente del Page Object Model para APIs).
 * Encapsula: base URL, endpoints, headers y payloads del microservicio am-tc-region.
 * Cada llamada loggea el curl y la respuesta (consola + Allure).
 *
 * Contrato real (Postman am-my-trips / run TestRail 13927 - SSM-17667):
 * - GET /tc/region/regionByRoute con headers: channel, x-transactionId, departure, arrival
 * - GET /tc/region/management/info
 */
public class TcRegionService {

    /** Base URL del gateway en DEV (patron: amx-c-mtpsbk-de.amlab7.com). */
    private static final String BASE_URL = "https://amx-c-mtpsbk-de.amlab7.com";

    private static final String REGION_BY_ROUTE_PATH = "/tc/region/regionByRoute";
    private static final String MANAGEMENT_INFO_PATH = "/tc/region/management/info";

    private final APIRequestContext requestContext;
    private final APIRequestContext bareRequestContext;

    public TcRegionService() {
        Playwright playwright = Playwright.create();

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("channel", "web");

        this.requestContext = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(BASE_URL)
                        .setExtraHTTPHeaders(headers)
        );

        // Contexto sin headers por defecto (BDD Run 13939): permite omitir
        // headers obligatorios como channel en los escenarios negativos.
        Map<String, String> bareHeaders = new HashMap<>();
        bareHeaders.put("Accept", "application/json");
        this.bareRequestContext = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(BASE_URL)
                        .setExtraHTTPHeaders(bareHeaders)
        );
    }

    /**
     * GET /tc/region/regionByRoute con headers obligatorios.
     * Genera un x-transactionId unico (uuid) salvo que se envie uno de traza.
     */
    public APIResponse getRegionByRoute(String departure, String arrival, String transactionId) {
        String txn = (transactionId == null || transactionId.isBlank())
                ? UUID.randomUUID().toString() : transactionId;

        RequestOptions options = RequestOptions.create()
                .setHeader("x-transactionId", txn)
                .setHeader("departure", departure)
                .setHeader("arrival", arrival);

        APIResponse response = requestContext.get(REGION_BY_ROUTE_PATH, options);
        String url = BASE_URL + REGION_BY_ROUTE_PATH
                + " [headers: departure=" + departure + ", arrival=" + arrival + "]";
        ApiLogger.logRequestAndResponse("GET", url, null,
                response.status(), response.text());
        return response;
    }

    /** GET /tc/region/regionByRoute con x-transactionId autogenerado. */
    public APIResponse getRegionByRoute(String departure, String arrival) {
        return getRegionByRoute(departure, arrival, null);
    }

    /** GET /tc/region/regionByRoute sin el header arrival (para casos negativos). */
    public APIResponse getRegionByRouteWithoutArrival(String departure, String transactionId) {
        String txn = (transactionId == null || transactionId.isBlank())
                ? UUID.randomUUID().toString() : transactionId;

        // El header arrival se omite a proposito (caso negativo TC006).
        APIResponse response = requestContext.get(REGION_BY_ROUTE_PATH,
                RequestOptions.create().setHeader("x-transactionId", txn)
                        .setHeader("departure", departure));
        ApiLogger.logRequestAndResponse("GET", BASE_URL + REGION_BY_ROUTE_PATH + " [sin arrival]", null,
                response.status(), response.text());
        return response;
    }

    /**
     * GET /tc/region/regionByRoute con EXACTAMENTE los headers indicados
     * (BDD Run 13939 - SSM-11165). Permite omitir headers obligatorios o enviar
     * valores con espacios entre caracteres para los escenarios negativos.
     */
    public APIResponse getRegionByRouteWithExactHeaders(Map<String, String> exactHeaders) {
        RequestOptions options = RequestOptions.create();
        for (Map.Entry<String, String> headerEntry : exactHeaders.entrySet()) {
            options.setHeader(headerEntry.getKey(), headerEntry.getValue());
        }
        APIResponse response = bareRequestContext.get(REGION_BY_ROUTE_PATH, options);
        String url = BASE_URL + REGION_BY_ROUTE_PATH + " [headers exactos: " + exactHeaders + "]";
        ApiLogger.logRequestAndResponse("GET", url, null,
                response.status(), response.text());
        return response;
    }

    /** GET /tc/region/management/info */
    public APIResponse getManagementInfo() {
        APIResponse response = requestContext.get(MANAGEMENT_INFO_PATH);
        ApiLogger.logRequestAndResponse("GET", BASE_URL + MANAGEMENT_INFO_PATH, null,
                response.status(), response.text());
        return response;
    }

    public void close() {
        if (requestContext != null) {
            requestContext.dispose();
        }
    }
}
