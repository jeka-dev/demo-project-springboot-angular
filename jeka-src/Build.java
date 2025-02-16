import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkApplicationTester;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.tooling.docker.JkDocker;
import dev.jeka.core.api.utils.JkUtilsNet;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInject;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;
import dev.jeka.plugins.sonarqube.JkSonarqube;
import dev.jeka.plugins.sonarqube.SonarqubeKBean;

import java.nio.file.Files;

class Build extends KBean {

    static final String E2E_TEST_PATTERN = "^e2e\\..*";

    @JkInject
    private ProjectKBean projectKBean;

    @JkDoc("Execute a Sonarqube scan on the NodeJs project")
    public void sonarJs() {
        JkSonarqube javaSonarqube = load(SonarqubeKBean.class).getSonarqube();
        JkSonarqube jsSonarqube = javaSonarqube.copyWithoutProperties();
        String projectId = projectKBean.project.getBaseDir().toAbsolutePath().getFileName() + "-js";
        jsSonarqube
                .setProperty(JkSonarqube.PROJECT_KEY, projectId)
                .setProperty(JkSonarqube.PROJECT_NAME, projectId)
                .setProperty(JkSonarqube.HOST_URL, getRunbase().getProperties().get("sonar.host.url"))
                .setProperty(JkSonarqube.TOKEN, getRunbase().getProperties().get("sonar.token"))
                .setProperty(JkSonarqube.PROJECT_BASE_DIR, getBaseDir().resolve("app-js").toString())
                .setProperty(JkSonarqube.SOURCES, "src")
                .setProperty(JkSonarqube.EXCLUSIONS, "dist/**/*, node_modules/**/*")
                .setProperty(JkSonarqube.SOURCE_ENCODING, "UTF-8")
                .setProperty(JkSonarqube.TEST, "src")
                .setProperty(JkSonarqube.TEST_INCLUSIONS, "**/*.spec.ts")
                .setProperty("sonar.typescript.lcov.reportPaths", "coverage/client/lcov.info");
        jsSonarqube.run();
    }

    @JkDoc("Execute E2E test on application deployed locally.")
    public void e2e() {
        new HostAppTester().run();
    }

    @JkDoc("Execute E2E test on application deployed in Docker.")
    public void e2eDocker() {
        new DockerTester().run();
    }

    @JkPostInit
    private void postInit(ProjectKBean projectKBean) {
        projectKBean.project.testing.testSelection.addExcludePatterns(E2E_TEST_PATTERN);
    }

    @JkPostInit
    private void customizeDockerImage(DockerKBean dockerKBean) {
        dockerKBean.customizeJvmImage(dockerBuild ->
        dockerBuild
                .addAgent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.32.0", "")
                .setBaseImage("eclipse-temurin:21.0.1_12-jre-jammy"));
    }

    private void execSelenideTests(String baseUrl) {
        JkProject project = projectKBean.project;
        if (!Files.exists(project.testing.compilation.layout.resolveClassDir())) {
            project.testing.compilation.run();
        }
        JkTestSelection selection = project.testing.createDefaultTestSelection()
                .addIncludePatterns(E2E_TEST_PATTERN);
        JkTestProcessor testProcessor = project.testing.createDefaultTestProcessor().setForkingProcess(true);
        testProcessor.getForkingProcess()
                .setLogWithJekaDecorator(true)
                .setLogCommand(true)
                .addJavaOptions("-Dselenide.reportsFolder=jeka-output/test-report/selenide")
                .addJavaOptions("-Dselenide.downloadsFolder=jeka-output/test-report/selenide-download")
                .addJavaOptions("-Dselenide.headless=true")
                .addJavaOptions("-Dselenide.baseUrl=" + baseUrl);
        testProcessor.launch(project.testing.getTestClasspath(), selection).assertSuccess();
    }

    // Deploy application on Host, test it and undeploy
    private class HostAppTester extends JkApplicationTester {

        int port;

        String baseUrl;

        @Override
        public void startApp() {
            startTimeout = 30*1000;
            port = findFreePort();
            baseUrl = "http://localhost:" + port;
            projectKBean.project.prepareRunJar(JkProject.RuntimeDeps.EXCLUDE)
                    .addJavaOptions("-Dserver.port=" + port)
                    .addJavaOptions("-Dmanagement.endpoint.shutdown.enabled=true")
                    .setInheritIO(true)
                    .execAsync();
        }

        @Override
        public boolean isApplicationReady() {
            return JkUtilsNet.isStatusOk(baseUrl + "/actuator/health", JkLog.isDebug());
        }

        @Override
        public void executeTests() {
            execSelenideTests(baseUrl);
        }

        @Override
        public void stopGracefully() {
            String shutdownUrl = baseUrl + "/actuator/shutdown";
            JkLog.info("Invoke %s", shutdownUrl);
            JkUtilsNet.sendHttpRequest(shutdownUrl, "POST", null).asserOk();
        }
    }

    // Deploy application on Docker, test it and undeploy
    class DockerTester extends JkApplicationTester {

        int port;

        String baseUrl;

        String containerName;

        @Override
        protected void startApp() {
            port = findFreePort();
            baseUrl = "http://localhost:" + port;
            containerName = projectKBean.project.getBaseDir().toAbsolutePath().getFileName().toString() + "-" + port;
            JkDocker.of().addParams("run", "-d", "-p", String.format("%s:8080", port), "--name",
                    containerName, load(DockerKBean.class).jvmImageName)
                    .setInheritIO(false)
                    .setLogWithJekaDecorator(true)
                    .exec();
        }

        @Override
        protected boolean isApplicationReady() {
            return JkUtilsNet.isAvailableAndOk(baseUrl, JkLog.isDebug());
        }

        @Override
        protected void executeTests() {
            execSelenideTests(baseUrl);
        }

        @Override
        protected void stopGracefully() {
            JkDocker.of().addParams("rm", "-f", containerName)
                    .setInheritIO(false).setLogWithJekaDecorator(true)
                    .exec();
        }

    }

}