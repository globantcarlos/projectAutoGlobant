package steps;

import static services.actions.TcRegionActions.*;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Step definitions para am-tc-region (estilo am-my-trips-test-automation).
 * Delegan en TcRegionActions (capa de aserciones) que usa TcRegionService.
 */
public class am_tc_region_steps {

    @Given("el usuario consulta regionByRoute con departure {string} y arrival {string}")
    public void consultarRegionByRoute(String departure, String arrival) {
        regionByRouteRequest(departure, arrival, "200");
    }

    @Given("el usuario consulta nuevamente regionByRoute con departure {string} y arrival {string}")
    public void consultarRegionByRouteNuevamente(String departure, String arrival) {
        regionByRouteConsecutiveQueries(departure, arrival);
    }

    @Given("el usuario consulta regionByRoute con departure {string} y arrival {string} por tercera vez")
    public void consultarRegionByRouteTerceraVez(String departure, String arrival) {
        // Las tres consultas se ejecutan juntas en la accion de TC003; este paso es declarativo.
    }

    @Given("el usuario consulta regionByRoute con departure {string} y arrival {string} forzando error de downstream")
    public void consultarRegionByRouteDownstreamError(String departure, String arrival) {
        regionByRouteDownstreamError();
    }

    @Given("el usuario consulta regionByRoute con departure {string} arrival {string} y transactionId de traza nr-trace-17667")
    public void consultarRegionByRouteConTraza(String departure, String arrival) {
        regionByRouteWithTraceTransactionId(departure, arrival);
    }

    @Given("el usuario consulta regionByRoute con departure {string} sin el header arrival")
    public void consultarRegionByRouteSinArrival(String departure) {
        regionByRouteWithoutArrival(departure);
    }

    @Given("el usuario consulta regionByRoute con departure {string} y arrival {string} con IATA inexistente")
    public void consultarRegionByRouteIataInexistente(String departure, String arrival) {
        regionByRouteRequest(departure, arrival, "500");
    }

    @Given("el usuario ejecuta 8 consultas consecutivas de regionByRoute con departure {string} y arrival {string}")
    public void ejecutar8ConsultasConsecutivas(String departure, String arrival) {
        regionByRouteStability(8, departure, arrival);
    }

    // ==================== BDD Run 13939 (SSM-11165) ====================

    /**
     * Consulta regionByRoute enviando EXACTAMENTE los headers indicados.
     * Formato del parametro: "header=valor, header=valor" (vacío = sin headers).
     * "OMITIR:<header>" no aplica aqui: para omitir un header simplemente no se incluye.
     */
    @Given("el usuario consulta regionByRoute con los headers exactos {string}")
    public void consultarRegionByRouteConHeadersExactos(String exactHeadersSpec) {
        Map<String, String> exactHeaders = new LinkedHashMap<>();
        if (exactHeadersSpec != null && !exactHeadersSpec.isBlank()) {
            for (String headerPair : exactHeadersSpec.split(",")) {
                String[] headerParts = headerPair.split("=", 2);
                exactHeaders.put(headerParts[0].trim(), headerParts[1].trim());
            }
        }
        regionByRouteWithExactHeaders(exactHeaders);
    }

    @Given("el usuario consulta el management info de am-tc-region")
    public void consultarManagementInfo() {
        managementInfoRequest();
    }

    // ==================== Then ====================

    @Then("se valida que la respuesta region sea {string}")
    public void validarRespuestaRegion(String statusCode) {
        validateStatusCode(statusCode);
    }

    @And("se valida que la respuesta contenga regionShortDescription {string}")
    public void validarRegionShortDescription(String regionShortDescription) {
        validateRegionShortDescription(regionShortDescription);
    }

    @And("se valida que la respuesta contenga los campos del contrato regionByRoute")
    public void validarCamposContrato() {
        validateRegionByRouteContractFields();
    }

    @Then("se valida que las tres respuestas region sean 200 con contenido identico")
    public void validarTresRespuestasIdenticas() {
        validateThreeConsecutiveResponsesIdentical();
    }

    @Then("se valida que la respuesta region sea 500 con reason del error catalog")
    public void validar500ConReason() {
        validate500WithErrorCatalogReason();
    }

    @And("se valida que el servicio responda fail-fast en menos de 30 segundos")
    public void validarFailFast() {
        validateFailFastUnder30Seconds();
    }

    @And("se valida que el transactionId de traza quede registrado para New Relic")
    public void validarTrazaRegistrada() {
        validateTraceTransactionIdRegistered();
    }

    @Then("se valida que la respuesta region sea 400 con reason ERR.TC.REGION.BAD_REQUEST")
    public void validar400BadRequest() {
        validate400BadRequest();
    }

    @Then("se valida que las 8 consultas region respondan 200")
    public void validar8Consultas200() {
        validateAllQueries200(8);
    }

    @Then("se valida que la respuesta management info sea 200")
    public void validarManagementInfo200() {
        validateManagementInfo200();
    }

    @And("se valida que el management info reporte el microservicio {string} y su Version")
    public void validarManagementInfoServicio(String microservicio) {
        validateManagementInfoReportsService(microservicio);
    }

    // ==================== Then BDD Run 13939 (SSM-11165) ====================

    /** Valida el body de error del contrato BDD negativos: channel/httpCode/reason. */
    @Then("se valida que la respuesta de error tenga channel {string} httpCode {string} y reason del error catalog")
    public void validarBodyDeError(String channel, String httpCode) {
        validateErrorBody(channel, httpCode);
    }

    /** Valida el body exitoso del contrato regionByRoute (positivos BDD). */
    @And("se valida que la respuesta exitosa contenga los campos del contrato regionByRoute")
    public void validarBodyExitosoBdd() {
        validateRegionByRouteContractFields();
    }
}
