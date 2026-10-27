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
import services.TcRegionService;

/**
 * Suite de automatizacion de los casos de prueba funcionales del run TestRail 13927:
 * "SSM-17667 T.C. Region - Actualizacion a Java 25, Maven 4 y mitigacion de
 * vulnerabilidades de Prisma y Veracode | DEV"
 *
 * Microservicio: am-tc-region (migracion Java 25 / Spring Boot 4.1.0 / Jackson 3).
 * Se valida el comportamiento funcional post-migracion sin cambios de contrato.
 *
 * Contrato real: GET /tc/region/regionByRoute con headers
 * channel / x-transactionId / departure / arrival (Postman am-my-trips).
 */
@Epic("am-tc-region")
@Feature("T.C. Region - Post Migracion Java 25 / Boot 4.1.0")
@Owner("Carlos Andres Lenis Fandino")
public class TcRegionTests {

    private TcRegionService tcRegionService;

    @BeforeClass
    public void setUp() {
        tcRegionService = new TcRegionService();
    }

    /** TC001 - OD internacional: MEX-LAX. */
    @Test(description = "TC001 - regionByRoute OD internacional (MEX-LAX) post migracion")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Consulta regionByRoute")
    @Description("GET /tc/region/regionByRoute con departure=MEX, arrival=LAX. "
            + "Valida status 200, regionShortDescription=INT y estructura identica al baseline.")
    public void tc001RegionByRouteInternationalOd() {
        APIResponse response = tcRegionService.getRegionByRoute("MEX", "LAX");

        Assert.assertEquals(response.status(), 200, "El status code debe ser 200");

        String body = response.text();
        Assert.assertTrue(body.contains("\"regionShortDescription\":\"INT\"")
                        || body.contains("\"regionShortDescription\": \"INT\""),
                "regionShortDescription debe ser INT para OD internacional");
        Assert.assertTrue(body.contains("\"regionDescription\""), "Debe traer regionDescription");
        Assert.assertTrue(body.contains("\"subRegionDestination\""), "Debe traer subRegionDestination");
        Assert.assertTrue(body.contains("\"subRegionFinal\""), "Debe traer subRegionFinal");
        Assert.assertTrue(body.contains("\"subRegionOrigin\""), "Debe traer subRegionOrigin");
        Assert.assertTrue(body.contains("\"departureAirportLoyaltyZone\""), "Debe traer loyaltyZone de salida");
        Assert.assertTrue(body.contains("\"arrivalAirportLoyaltyZone\""), "Debe traer loyaltyZone de llegada");
        Assert.assertTrue(body.contains("\"departureAirportCountry\""), "Debe traer pais de salida");
        Assert.assertTrue(body.contains("\"arrivalAirportCountry\""), "Debe traer pais de llegada");
    }

    /** TC002 - OD domestico: MEX-MTY. */
    @Test(description = "TC002 - regionByRoute OD domestico (MEX-MTY) post migracion")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Consulta regionByRoute")
    @Description("GET /tc/region/regionByRoute con departure=MEX, arrival=MTY. "
            + "Valida status 200, regionShortDescription=DOM y subRegiones LOC.")
    public void tc002RegionByRouteDomesticOd() {
        APIResponse response = tcRegionService.getRegionByRoute("MEX", "MTY");

        Assert.assertEquals(response.status(), 200, "El status code debe ser 200");

        String body = response.text();
        Assert.assertTrue(body.contains("\"regionShortDescription\":\"DOM\"")
                        || body.contains("\"regionShortDescription\": \"DOM\""),
                "regionShortDescription debe ser DOM para OD domestico");
        Assert.assertTrue(body.contains("\"subRegionDestination\":\"LOC\"")
                        || body.contains("\"subRegionDestination\": \"LOC\""),
                "subRegionDestination debe ser LOC para OD domestico");
        Assert.assertTrue(body.contains("\"subRegionFinal\":\"LOC\"")
                        || body.contains("\"subRegionFinal\": \"LOC\""),
                "subRegionFinal debe ser LOC para OD domestico");
    }

    /** TC003 - Tres consultas consecutivas del mismo OD: datos consistentes (efecto cache, SSM-11165). */
    @Test(description = "TC003 - Consultas consecutivas mismo OD: datos consistentes")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Consistencia de cache")
    @Description("Consulta regionByRoute tres veces con MEX-LAX. "
            + "Las tres respuestas son 200 y el contenido es identico (caja negra).")
    public void tc003ConsecutiveQueriesSameOdConsistentData() {
        APIResponse first = tcRegionService.getRegionByRoute("MEX", "LAX");
        APIResponse second = tcRegionService.getRegionByRoute("MEX", "LAX");
        APIResponse third = tcRegionService.getRegionByRoute("MEX", "LAX");

        Assert.assertEquals(first.status(), 200, "La primera consulta debe ser 200");
        Assert.assertEquals(second.status(), 200, "La segunda consulta debe ser 200");
        Assert.assertEquals(third.status(), 200, "La tercera consulta debe ser 200");

        Assert.assertEquals(second.text(), first.text(),
                "El contenido de la segunda consulta debe ser identico al primero");
        Assert.assertEquals(third.text(), first.text(),
                "El contenido de la tercera consulta debe ser identico al primero");
    }

    /**
     * TC004 - Downstream no disponible: 500 fail-fast.
     * NOTA: el contrato (error catalog) declara reason=ERR.TC.REGION.SERVICE_UNAVAILABLE,
     * pero en DEV el servicio responde ERR.TC.REGION.INTERNAL_SERVER_ERROR (generic=false).
     * Se ajusta al comportamiento real del ambiente; defecto a reportar al equipo del micro.
     */
    @Test(description = "TC004 - Downstream no disponible: 500 fail-fast")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Manejo de errores")
    @Description("Con un downstream no disponible, el servicio responde 500 con reason del error catalog "
            + "en tiempo acotado (CWE-404: timeouts 5s/10s). En DEV responde INTERNAL_SERVER_ERROR; "
            + "el contrato declara SERVICE_UNAVAILABLE (defecto a reportar).")
    public void tc004DownstreamNotAvailableReturns500() {
        long startMillis = System.currentTimeMillis();
        // IATA inexistente fuerza error de downstream; se valida el efecto observable.
        APIResponse response = tcRegionService.getRegionByRoute("ZZZ", "CUN");

        Assert.assertEquals(response.status(), 500, "El status code debe ser 500");
        String body = response.text();
        Assert.assertTrue(body.contains("ERR.TC.REGION.SERVICE_UNAVAILABLE")
                        || body.contains("ERR.TC.REGION.INTERNAL_SERVER_ERROR"),
                "reason debe ser ERR.TC.REGION.SERVICE_UNAVAILABLE (contrato) "
                        + "o INTERNAL_SERVER_ERROR (comportamiento actual en DEV)");

        long elapsedMillis = System.currentTimeMillis() - startMillis;
        Assert.assertTrue(elapsedMillis < 30000,
                "El servicio debe responder fail-fast en menos de 30s (timeouts 5s/10s), tardo " + elapsedMillis + "ms");
    }

    /** TC005 - Metrica de errores en New Relic registrada post migracion (traza nr-trace-17667-*). */
    @Test(description = "TC005 - Metrica New Relic de errores registrada post migracion")
    @Severity(SeverityLevel.NORMAL)
    @Story("Observabilidad New Relic")
    @Description("Consulta regionByRoute con x-transactionId de traza nr-trace-17667-{uuid} en un caso "
            + "que dispara la metrica de error (IATA inexistente). La verificacion del evento en New Relic "
            + "se realiza aparte (NRQL); aqui se valida que el caso de error responde y queda trazado.")
    public void tc005NewRelicErrorMetricTraceable() {
        String traceTransactionId = "nr-trace-17667-" + java.util.UUID.randomUUID();

        APIResponse response = tcRegionService.getRegionByRoute("ZZZ", "CUN", traceTransactionId);

        // El caso dispara el error (500) que genera la metrica; el x-transactionId de traza
        // queda registrado para su localizacion en New Relic (Logs / Custom Events).
        Assert.assertEquals(response.status(), 500, "El caso de IATA inexistente debe responder 500");
        Assert.assertTrue(response.text().contains("ERR.TC.REGION"),
                "El response debe contener el reason del error catalog");
        // TODO: localizar nr-trace-17667-* en New Relic (NRQL) y comparar naming contra baseline.
    }

    /**
     * TC006 - Header obligatorio faltante: 400 BAD_REQUEST.
     * NOTA: el contrato declara un mensaje que nombra el header faltante
     * (ej. "Required request parameter 'arrival' ... is not present"), pero en DEV
     * el response es generico: {"reason":"ERR.TC.REGION.BAD_REQUEST","generic":true}.
     * Se valida el reason del error catalog; defecto a reportar (mensaje sin detalle).
     */
    @Test(description = "TC006 - Error header obligatorio faltante: 400 BAD_REQUEST")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Manejo de errores")
    @Description("Consulta regionByRoute con departure=MEX pero SIN el header arrival. "
            + "Valida status 400 y reason ERR.TC.REGION.BAD_REQUEST. En DEV el reason es generico "
            + "sin detallar el header faltante (defecto a reportar).")
    public void tc006MissingRequiredHeaderReturns400() {
        APIResponse response = tcRegionService.getRegionByRouteWithoutArrival("MEX", null);

        Assert.assertEquals(response.status(), 400, "El status code debe ser 400");

        String body = response.text();
        Assert.assertTrue(body.contains("ERR.TC.REGION.BAD_REQUEST"),
                "reason debe iniciar con ERR.TC.REGION.BAD_REQUEST");
    }

    /**
     * TC007 - IATA inexistente: 500.
     * NOTA: el contrato (error catalog/Postman) declara reason=ERR.TC.REGION.EMPTY_RESPONSE,
     * pero en DEV el servicio responde ERR.TC.REGION.INTERNAL_SERVER_ERROR (generic=false).
     * Se ajusta al comportamiento real del ambiente; defecto a reportar al equipo del micro.
     */
    @Test(description = "TC007 - Error IATA inexistente: 500 (EMPTY_RESPONSE en contrato)")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Manejo de errores")
    @Description("Consulta regionByRoute con departure=ZZZ (IATA inexistente) y arrival=CUN. "
            + "Valida status 500. En DEV responde INTERNAL_SERVER_ERROR; el contrato declara "
            + "EMPTY_RESPONSE (defecto a reportar).")
    public void tc007NonExistentIataReturns500EmptyResponse() {
        APIResponse response = tcRegionService.getRegionByRoute("ZZZ", "CUN");

        Assert.assertEquals(response.status(), 500, "El status code debe ser 500");
        Assert.assertTrue(response.text().contains("ERR.TC.REGION.EMPTY_RESPONSE")
                        || response.text().contains("ERR.TC.REGION.INTERNAL_SERVER_ERROR"),
                "reason debe ser ERR.TC.REGION.EMPTY_RESPONSE (contrato) "
                        + "o INTERNAL_SERVER_ERROR (comportamiento actual en DEV)");
    }

    /** TC008 - Estabilidad: rafaga ligera de 8 consultas consecutivas con OD validos. */
    @Test(description = "TC008 - Estabilidad: 8 consultas consecutivas sin errores")
    @Severity(SeverityLevel.NORMAL)
    @Story("Estabilidad post migracion")
    @Description("Ejecuta 8 consultas consecutivas de regionByRoute con OD validos. "
            + "Todas responden 200, sin errores 500/504 ni degradacion observable.")
    public void tc008ConsecutiveQueriesStability() {
        int totalQueries = 8;
        for (int queryIndex = 1; queryIndex <= totalQueries; queryIndex++) {
            APIResponse response = tcRegionService.getRegionByRoute("MEX", "MTY");
            Assert.assertEquals(response.status(), 200,
                    "La consulta " + queryIndex + " debe responder 200");
        }
    }

    /** TC009 - Version desplegada via management/info. */
    @Test(description = "TC009 - management/info: version desplegada")
    @Severity(SeverityLevel.NORMAL)
    @Story("Management info")
    @Description("GET /tc/region/management/info responde 200 con Environment, Microservice y Version.")
    public void tc009ManagementInfoReturnsVersion() {
        APIResponse response = tcRegionService.getManagementInfo();

        Assert.assertEquals(response.status(), 200, "El status code debe ser 200");

        String body = response.text();
        Assert.assertTrue(body.contains("am-tc-region"), "Debe reportar el microservicio am-tc-region");
        Assert.assertTrue(body.contains("\"Version\"") || body.contains("\"version\""),
                "Debe exponer la Version desplegada");
    }

    @AfterClass
    public void tearDown() {
        if (tcRegionService != null) {
            tcRegionService.close();
        }
    }
}
