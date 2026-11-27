# JeKa Spring Boot-Angular Application

This repository demonstrates how to build a Spring Boot + Angular application using JeKa. The build process includes:

- Compilation and testing of both Java and Angular code
- SonarQube analysis for Java and Angular code (including code coverage)
- End-to-end testing (using Selenide) on the application deployed either locally or in Docker

Additionally, this demo shows [how to create Docker native images](#Docker-Image).

The application is a simple web app for managing a list of users, based on [this tutorial](https://www.baeldung.com/spring-boot-angular-web).

![screenshot.png](./screenshot.png)

## Prerequisites

- Nothing to install to build and run: the `./jeka` script downloads JeKa, the JDK and Node.js.
  The commands below use `jeka`: use `./jeka` if JeKa is not installed.
- Google Chrome, for the Angular tests (headless) and the end-to-end tests.
- Docker, for the Docker images and the end-to-end tests on Docker.
- A SonarQube server, only for `jeka checkQuality`.

## Overview

Standard methods:
```
jeka -p                  : Runs the application (build it first -behind-the-scene- if jar and exec absent)
jeka test                : Compiles and runs tests
jeka pack                : Builds the application as bootable jar
jeka native: compile     : Builds the application as native executable
jeka checkQuality        : Runs Sonarqube analysis and gates for Java and JS
jeka e2eTest             : Deploys application on localhost or Docker and run end-to-end tests
jeka docker: build       : Creates Docker image of the application
jeka docker: buildNative : Create Docker image running the native executable of the application
```

The [Custom](jeka-src/Custom.java) KBean, activated with `@custom=on` in [jeka.properties](jeka.properties),
configures the specific concerns:

- Sonarqube setting for Javascript app
- End-to-end testing
- Docker image customization

## Run the Application without building
```shell
jeka -p
```
This command creates a bootable JAR (if it doesn't already exist) and starts the application. 
Access it at [http://localhost:8080](http://localhost:8080).

If a native executable has been built, this command runs the native version instead.

> [!TIP]  
> To run the application without manually cloning the Git repository, use:
>
> `jeka -r https://github.com/jeka-dev/demo-project-springboot-angular.git -p`


## Build application

Build application, including both Java and Angular testing:
```shell
jeka test pack
```
The bootable jar embeds the Angular application.

Build a native executable of the application:
```shell
jeka native: compile
```
Once built, you can execute it using `jeka -p`.

## Build application with sonar analysis + code coverage

> [!NOTE]
> You need a sonarqube server to execute this.
> 
> By default, the server is expected to be found at *http://localhost:9000*.
> 
> You can launch a Sonarqube instance using docker `docker run -p 9000:9000 -d sonarqube:latest`. Then :
>    - Go to http;//localhost:9000
>    - login with 'admin/admin'
>    - Go Administration > Security > User > Tokens > add ..
>    - Copy the token and add a property 'sonar.token=[TOKEN]' in *[USER HOME]/.jeka/global.properties* file


```shell
jeka checkQuality
```
The Sonarqube analysis + coverage for Java code is provided out-of-the-box, thanks to *Jacoco* and *Sonarqube* Kbean, 
that are activated in the command line.

The sonarqube JS setting is registered in the `Custom#postInit(ProjectKBean)` method.

It can be launched separately using:
```shell
jeka custom: sonarJs
```

## End-to-End Testing

The application is tested using [Selenide](https://selenide.org/). 
Selenide helps simulate user actions in the browser for complete end-to-end testing.

The test classes for end-to-end (E2E) testing are located in the [e2e](test/e2e) package inside the *test* directory. 
These tests are run on deployed instances of the application:

Deploy the application on localhost and test:
```shell
jeka e2eTest
```

Run end-to-end tests against the application deployed on Docker
```shell
jeka e2eTest custom: e2eTestOnDocker=true
```

The end-to-end tests are registered in the `Custom#postInit(ProjectKBean)` method.

## Create Docker images

With Jeka, you can easily create Docker JVM or native images, regardless of whether you're running on Windows, Linux, or macOS.

To create a Docker image, run: 
```shell
jeka docker: build
```
or
```shell
jeka docker: buildNative
```

The JVM Docker image is customized in the `Custom#postInit(DockerKBean)` method: base image and OpenTelemetry agent.

To see details about KBean initialization, run the commands using `--inspect` option.

See [documentation](https://jeka-dev.github.io/jeka/reference/kbeans-docker/) for customizing Docker images.

## How it works

- [jeka.properties](jeka.properties): JeKa and Java versions, the plugins (`springboot`, `nodeJs`, `jacoco`, `sonarqube`)
  and their settings. The `nodeJs` KBean builds the Angular app of [app-js](app-js) with `buildCmd`, copies it in the
  `static` resources of the jar, and runs its tests with `testCmd` after the Java tests.
- [jeka.project.deps](jeka.project.deps): the Java dependencies, with the Spring Boot BOM.
- [jeka-src/Custom.java](jeka-src/Custom.java): SonarQube for JS, end-to-end tests, Docker image customization.

## Links

- [Build projects](https://jeka-dev.github.io/jeka/tutorials/build-projects/)
- [Spring Boot plugin](https://github.com/jeka-dev/jeka/tree/master/plugins/plugins.springboot),
  [Node.js plugin](https://github.com/jeka-dev/jeka/tree/master/plugins/plugins.nodejs)
- [docker KBean](https://jeka-dev.github.io/jeka/reference/kbeans-docker/)
