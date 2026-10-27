package services;

import java.io.File;

/**
 * Genera el reporte Allure en HTML (target/allure-report) a partir de
 * allure-results y lo abre en el navegador.
 *
 * Se invoca desde el hook @AfterAll de Cucumber (steps.AllureReportHook),
 * por lo que funciona sin importar como se ejecute la suite:
 * IntelliJ (feature o runner), Maven exec:java, etc.
 */
public final class AllureReportGenerator {

    private static boolean alreadyGenerated = false;

    private AllureReportGenerator() {
    }

    /** Genera el reporte y lo abre en el navegador (idempotente por JVM). */
    public static synchronized void generateAndOpen() {
        if (alreadyGenerated) {
            return;
        }
        alreadyGenerated = true;

        File resultsDir = new File("allure-results");
        String[] resultFiles = resultsDir.isDirectory() ? resultsDir.list() : null;
        if (resultFiles == null || resultFiles.length == 0) {
            System.out.println("[Allure] No hay resultados en allure-results ("
                    + resultsDir.getAbsolutePath() + "); no se genera reporte.");
            return;
        }

        String allureCommand = resolveAllureCommand();
        if (allureCommand == null) {
            System.out.println("[Allure] CLI de Allure no encontrado. Genera el reporte manualmente con:");
            System.out.println("  allure generate allure-results -o target/allure-report --clean");
            return;
        }

        writeEnvironmentProperties(resultsDir);
        writeCategoriesFile(resultsDir);
        rewriteFeatureLabelsFromTags(resultsDir);

        try {
            ProcessBuilder generateProcess = new ProcessBuilder(
                    allureCommand, "generate", "allure-results",
                    "-o", "target/allure-report", "--clean",
                    "--report-name", "am-tc-region - Casos funcionales - DEV - Run 13927 (SSM-17667) - Carlos Andres Lenis Fandino")
                    .inheritIO();
            int generateExitCode = generateProcess.start().waitFor();
            if (generateExitCode != 0) {
                System.out.println("[Allure] La generacion del reporte termino con codigo " + generateExitCode);
                return;
            }

            System.out.println("[Allure] Reporte generado en target/allure-report/index.html");
            serveAndOpenReport(new File("target/allure-report"));
        } catch (Exception reportException) {
            System.out.println("[Allure] No se pudo generar/abrir el reporte automaticamente: "
                    + reportException.getMessage());
        }
    }

    /**
     * Escribe environment.properties en allure-results: aparece en el widget
     * "Environment" del dashboard del reporte (autor y ambiente de ejecucion).
     */
    private static void writeEnvironmentProperties(File resultsDir) {
        File environmentFile = new File(resultsDir, "environment.properties");
        try (java.io.PrintWriter writer = new java.io.PrintWriter(environmentFile, "UTF-8")) {
            writer.println("Autor=Carlos Andres Lenis Fandino");
            writer.println("Ambiente=DEV (amx-c-mtpsbk-de.amlab7.com)");
            writer.println("Proyecto=am-tc-region");
            writer.println("TestRail\\ Run=13927 (SSM-17667)");
            System.out.println("[Allure] environment.properties escrito (autor, ambiente, proyecto).");
        } catch (Exception environmentException) {
            System.out.println("[Allure] No se pudo escribir environment.properties: "
                    + environmentException.getMessage());
        }
    }

    /**
     * Escribe categories.json en allure-results: agrupa los casos en el tab
     * "Categories" del reporte, identificando los casos funcionales.
     */
    private static void writeCategoriesFile(File resultsDir) {
        File categoriesFile = new File(resultsDir, "categories.json");
        String categoriesJson = "[\n" +
                "  {\n" +
                "    \"name\": \"Casos funcionales\",\n" +
                "    \"matchedStatuses\": [\"passed\"]\n" +
                "  },\n" +
                "  {\n" +
                "    \"name\": \"Defectos del contrato (a reportar)\",\n" +
                "    \"matchedStatuses\": [\"failed\", \"broken\"]\n" +
                "  }\n" +
                "]\n";
        try (java.io.PrintWriter writer = new java.io.PrintWriter(categoriesFile, "UTF-8")) {
            writer.print(categoriesJson);
            System.out.println("[Allure] categories.json escrito (casos funcionales).");
        } catch (Exception categoriesException) {
            System.out.println("[Allure] No se pudo escribir categories.json: "
                    + categoriesException.getMessage());
        }
    }

    /**
     * Post-procesa los *-result.json de allure-results: reescribe el label
     * "feature" (Behaviors) de cada test segun su tag "feature:<Grupo>".
     * Asi el dashboard separa en graficos diferentes: Funcionales,
     * BDD_Positivos y BDD_Negativos.
     */
    private static void rewriteFeatureLabelsFromTags(File resultsDir) {
        File[] resultFiles = resultsDir.listFiles((dir, name) -> name.endsWith("-result.json"));
        if (resultFiles == null) {
            return;
        }
        int rewrittenCount = 0;
        for (File resultFile : resultFiles) {
            try {
                String json = new String(java.nio.file.Files.readAllBytes(resultFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8);
                java.util.regex.Matcher tagMatcher = java.util.regex.Pattern
                        .compile("\"name\":\\s*\"tag\",\\s*\"value\":\\s*\"feature:([^\"]+)\"")
                        .matcher(json);
                if (!tagMatcher.find()) {
                    continue;
                }
                String behaviorName = tagMatcher.group(1).replace("_", " ");
                String rewritten = json.replaceFirst(
                        "(\"name\":\\s*\"feature\",\\s*\"value\":\\s*\")[^\"]*(\")",
                        "$1" + java.util.regex.Matcher.quoteReplacement(behaviorName) + "$2");
                java.nio.file.Files.write(resultFile.toPath(), rewritten.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                rewrittenCount++;
            } catch (Exception rewriteException) {
                System.out.println("[Allure] No se pudo reescribir el label feature de "
                        + resultFile.getName() + ": " + rewriteException.getMessage());
            }
        }
        System.out.println("[Allure] Labels feature reescritos en " + rewrittenCount + " resultados (Behaviors).");
    }

    /** Resuelve el ejecutable de Allure: ~/bin/allure o PATH. */
    private static String resolveAllureCommand() {
        File allureInHomeBin = new File(System.getProperty("user.home"), "bin/allure");
        if (allureInHomeBin.canExecute()) {
            return allureInHomeBin.getAbsolutePath();
        }
        String pathEnvironment = System.getenv("PATH");
        if (pathEnvironment == null) {
            return null;
        }
        for (String pathEntry : pathEnvironment.split(File.pathSeparator)) {
            File allureExecutable = new File(pathEntry, "allure");
            if (allureExecutable.canExecute()) {
                return allureExecutable.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * Sirve el reporte por HTTP local (python3 -m http.server) y lo abre en el navegador.
     * Abrir index.html directamente con file:// hace que el navegador bloquee los
     * datos del dashboard (fetch/XHR sobre file://) y el reporte quede en blanco
     * mostrando "cargando".
     */
    private static void serveAndOpenReport(File reportDir) {
        int serverPort = 4777;
        try {
            ProcessBuilder serverProcess = new ProcessBuilder(
                    "python3", "-m", "http.server", String.valueOf(serverPort),
                    "--directory", reportDir.getAbsolutePath())
                    .inheritIO();
            serverProcess.start();

            // Pequena espera para que el servidor este listo antes de abrir el navegador
            Thread.sleep(1500);

            String reportUrl = "http://localhost:" + serverPort + "/index.html";
            openInBrowser(reportUrl);
            System.out.println("[Allure] Servidor local activo en " + reportUrl
                    + " (detenlo con Ctrl+C al cerrar la suite, o dejalo abierto para revisar el reporte).");
        } catch (Exception serveException) {
            System.out.println("[Allure] No se pudo levantar el servidor local. Abre el reporte manualmente con:");
            System.out.println("  allure open " + reportDir.getAbsolutePath());
        }
    }

    /** Abre una URL o archivo en el navegador segun el sistema operativo. */
    private static void openInBrowser(String reportUrl) {
        try {
            String operatingSystem = System.getProperty("os.name").toLowerCase();
            String[] openCommand;
            if (operatingSystem.contains("mac")) {
                openCommand = new String[]{"open", reportUrl};
            } else if (operatingSystem.contains("win")) {
                openCommand = new String[]{"cmd", "/c", "start", "", reportUrl};
            } else {
                openCommand = new String[]{"xdg-open", reportUrl};
            }
            new ProcessBuilder(openCommand).start();
            System.out.println("[Allure] Reporte abierto en el navegador.");
        } catch (Exception openException) {
            System.out.println("[Allure] Abre el reporte manualmente: " + reportUrl);
        }
    }
}
