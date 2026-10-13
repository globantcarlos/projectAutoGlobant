package tests;

import com.microsoft.playwright.APIResponse;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import services.JsonPlaceholderService;

@Epic("JSONPlaceholder API")
@Owner("Carlos Andres Lenis Fandino")
public class JsonPlaceholderTests {

    private JsonPlaceholderService jsonPlaceholderService;

    @BeforeClass
    public void setUp() {
        jsonPlaceholderService = new JsonPlaceholderService();
    }

    @Test(description = "GET /posts/1 - valida status 200 y contenido del post")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Consulta de posts")
    @Description("Obtiene el post con id 1 y valida que el status sea 200 y el cuerpo contenga el id correcto")
    public void getPostShouldReturn200AndValidBody() {
        APIResponse response = jsonPlaceholderService.getPostById(1);

        Assert.assertEquals(response.status(), 200, "El status code debe ser 200");
        Assert.assertEquals(response.headers().get("content-type"), "application/json; charset=utf-8");

        String responseBody = response.text();
        Assert.assertTrue(responseBody.contains("\"id\": 1"), "El cuerpo debe contener id 1");
        Assert.assertTrue(responseBody.contains("\"userId\": 1"), "El cuerpo debe contener userId 1");
    }

    @Test(description = "POST /posts - valida status 201 y cuerpo de la respuesta")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Creación de posts")
    @Description("Crea un post nuevo y valida que el status sea 201 y la respuesta refleje el payload enviado")
    public void createPostShouldReturn201AndEchoBody() {
        String expectedTitle = "QA Automation Test";
        String expectedBody = "Creado desde Playwright API testing";

        APIResponse response = jsonPlaceholderService.createPost(expectedTitle, expectedBody, 1);

        Assert.assertEquals(response.status(), 201, "El status code debe ser 201");

        String responseBody = response.text();
        Assert.assertTrue(responseBody.contains("\"title\": \"" + expectedTitle + "\""),
                "El cuerpo debe contener el título enviado");
        Assert.assertTrue(responseBody.contains("\"body\": \"" + expectedBody + "\""),
                "El cuerpo debe contener el body enviado");
        Assert.assertTrue(responseBody.contains("\"id\": 101"), "JSONPlaceholder asigna id 101 al nuevo post");
    }

    @AfterClass
    public void tearDown() {
        if (jsonPlaceholderService != null) {
            jsonPlaceholderService.close();
        }
    }
}
