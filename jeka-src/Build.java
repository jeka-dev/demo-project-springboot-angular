import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInject;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;
import dev.jeka.plugins.sonarqube.JkSonarqube;
import dev.jeka.plugins.sonarqube.SonarqubeKBean;
import dev.jeka.plugins.springboot.JkSpringbootAppTester;

import java.nio.file.Files;

class Build extends KBean {

    static final String E2E_TEST_PATTERN = "^e2e\\..*";

    @JkDoc("If true, end-to-end tests runs application in container.")
    public boolean e2eTestOnDocker = false;

    @JkInject
    private ProjectKBean projectKBean;

    @JkDoc("Execute a Sonarqube scan on the NodeJs project")
    public void sonarJs() {
        SonarqubeKBean sonarqubeKBean = load(SonarqubeKBean.class);
        JkSonarqube javaSonarqube = sonarqubeKBean.getSonarqube();
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
        if (sonarqubeKBean.gate) {
            jsSonarqube.checkQualityGate();
        }
    }

    @JkDoc("Execute E2E test on application deployed locally.")
    public void e2e() {
        JkSpringbootAppTester.of(projectKBean, this::execSelenideTests).run();
    }

    @JkDoc("Execute E2E test on application deployed in Docker.")
    public void e2eDocker() {
        load(DockerKBean.class).createJvmAppTester(this::execSelenideTests)
                .setShowAppLogs(true)
                .run();
    }

    @JkDoc("Execute E2E test on native application deployed in Docker.")
    public void e2eDockerNative() {
        load(DockerKBean.class).createNativeAppTester(this::execSelenideTests)
                .setShowAppLogs(true)
                .run();
    }

    @JkPostInit
    private void postInit(ProjectKBean projectKBean) {
        projectKBean.project.testing.testSelection.addExcludePatterns(E2E_TEST_PATTERN);
        projectKBean.project.addE2eTester("localhost-tester",
                this.e2eTestOnDocker ? this::e2eDocker : this::e2e);
        projectKBean.project.addQualityChecker("sonarqube-js", this::sonarJs);
    }

    @JkPostInit
    private void postInit(DockerKBean dockerKBean) {
        dockerKBean.customizeJvmImage(dockerBuild -> dockerBuild
                .addAgent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.32.0", "")
                .setBaseImage("eclipse-temurin:21-jre-jammy")
        );
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
                //.addJavaOptions("-Dorg.slf4j.simpleLogger.defaultLogLevel=ERROR")
                .addJavaOptions("-Dselenide.baseUrl=" + baseUrl);
        testProcessor.launch(project.testing.getTestClasspath(), selection).assertSuccess();
    }

}