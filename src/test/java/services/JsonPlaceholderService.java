package services;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Service Wrapper (equivalente del Page Object Model para APIs).
 * Encapsula: base URL, endpoints, headers y payloads de JSONPlaceholder.
 * Cada llamada loggea el curl y la respuesta (consola + Allure).
 */
public class JsonPlaceholderService {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    private final APIRequestContext requestContext;

    public JsonPlaceholderService() {
        Playwright playwright = Playwright.create();

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        this.requestContext = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(BASE_URL)
                        .setExtraHTTPHeaders(headers)
        );
    }

    /** GET /posts/{id} */
    public APIResponse getPostById(int postId) {
        String url = BASE_URL + "/posts/" + postId;
        APIResponse response = requestContext.get("/posts/" + postId);
        ApiLogger.logRequestAndResponse("GET", url, null,
                response.status(), response.text());
        return response;
    }

    /** POST /posts con payload de ejemplo */
    public APIResponse createPost(String title, String body, int userId) {
        String payload = String.format(
                "{\"title\": \"%s\", \"body\": \"%s\", \"userId\": %d}",
                title, body, userId
        );
        APIResponse response = requestContext.post("/posts",
                RequestOptions.create().setData(payload));
        ApiLogger.logRequestAndResponse("POST", BASE_URL + "/posts",
                payload, response.status(), response.text());
        return response;
    }

    public void close() {
        if (requestContext != null) {
            requestContext.dispose();
        }
    }
}
