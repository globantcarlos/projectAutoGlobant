package steps;

import io.cucumber.java.AfterAll;
import services.AllureReportGenerator;

/**
 * Hook global de Cucumber: al terminar TODOS los escenarios genera el
 * reporte Allure en HTML y lo abre en el navegador.
 *
 * Se ejecuta sin importar como se lance la suite:
 * - IntelliJ: clic derecho sobre el feature -> Run
 * - IntelliJ: Run sobre runner.TestRunner
 * - Maven: mvn exec:java -Dexec.mainClass=runner.TestRunner -Dexec.classpathScope=test
 */
public class AllureReportHook {

    @AfterAll
    public static void afterAllScenarios() {
        AllureReportGenerator.generateAndOpen();
    }
}
