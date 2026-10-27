package runner;

import java.util.stream.Stream;

/**
 * Runner Cucumber CLI (mismo patron que am-my-trips-test-automation).
 * Ejecutar: mvn exec:java -Dexec.mainClass=runner.TestRunner -Dexec.classpathScope=test
 * O con tags: mvn exec:java -Dexec.mainClass=runner.TestRunner -Dexec.classpathScope=test -Dexec.args="--tags @Regression"
 *
 * El reporte Allure en HTML se genera y abre automaticamente al terminar,
 * desde el hook @AfterAll de Cucumber (steps.AllureReportHook), sin importar
 * si la suite se lanza desde IntelliJ (feature o runner) o Maven.
 */
public class TestRunner {
    private static final String[] defaultOptions = {
            "--glue", "steps",
            "--plugin", "pretty", // Para salida en consola
            "--plugin", "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
            "--plugin", "summary", // Para salida en consola
            "--plugin", "json:target/cucumber-reports/Cucumber.json",
            "--plugin", "html:target/cucumber-reports/Cucumber.html",
            "--plugin", "junit:target/cucumber-reports/Cucumber.xml",
            "--tags", "@tcRegion",
            "classpath:features"
    };

    public static void main(String[] args) {
        Stream<String> cucumberOptions = Stream.concat(Stream.of(defaultOptions), Stream.of(args));
        byte exitCode = io.cucumber.core.cli.Main.run(
                cucumberOptions.toArray(String[]::new), Thread.currentThread().getContextClassLoader());

        // Siempre se fuerza la salida: los hilos no-daemon de Playwright
        // mantendrian la JVM viva indefinidamente. El reporte ya fue
        // generado y abierto por el hook @AfterAll de Cucumber.
        System.exit(exitCode);
    }
}
