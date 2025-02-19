# JeKa Spring Boot-Angular Application

This repository demonstrates how to build a Spring Boot + Angular application using JeKa. The build process includes:

- Compilation and testing of both Java and Angular code
- SonarQube analysis for Java and Angular code (including code coverage)
- End-to-end testing (using Selenide) on the application deployed either locally or in Docker

Additionally, this demo shows [how to create Docker native images](#Docker-Image).

The application is a simple web app for managing a list of users, based on [this tutorial](https://www.baeldung.com/spring-boot-angular-web).

![screenshot.png](./screenshot.png)

## Overview

```
jeka -p                  : Runs the application (build it first -behind-the-scene- if jar and exec absent)
jeka pack                : Builds the application as bootable far (including tests)
jeka native: compile     : Builds the application as native executable
jeka checkQuality        : Runs Sonarqube analysis and gates for Java ans JS
jeka e2eTest             : Deploys application on localhost or Docker and run end-to-end tests
jeka docker: build       : Creates Docker image of the application
jeka docker: buildNative : Create Docker image running the native executable of the application
```

We need to create a `Build` class to configure the specific concerns:

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
jeka pack
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

The sonarqube JS setting is registered in `Build#postInit(ProjectKBean)` method.

It can be launched separately using:
```shell
jeka build: sonarJs
```

## End-to-End Testing

The application is tested using [Selenide](https://selenide.org/). 
Selenide helps simulate user actions in the browser for complete end-to-end testing.

The test classes for end-to-end (E2E) testing are located in the *e2e* package inside the *test* directory. 
These tests are run on deployed instances of the application:

Deploy the application on localhost and test:
```shell
jeka e2eTest
```

Deploy the application on Docker and test:
```shell
jeka e2eTest build: e2eTestOnDocker=true
```

The end-to-end tests are registered in `Build#postInit(ProjectKBean)` method.

### Testing on local host

Make sure the application is already build (`jeka project: pack`).

Deploy-test-undeploy:
```shell
jeka build: e2e
```

### Testing with Docker

> [!NOTE]
> This requires to have a Docker client running. This can be *DockerDesktop* running on your laptop.

Make sure that the docker image is already built: `jeka pack docker: build` or `jeka pack docker: buildNative`.

This constructs a Docker image of the application, that you can execute with:
```shell
docker run --rm -p  8080:8080 demo-project-springboot-angular:latest
```

Deploy-test-undeploy:
```shell
jeka build: e2eDocker
```

## Create Docker images

With Jeka, you can easily create Docker JVM or native images, regardless of whether you're running on Windows, Linux, or macOS.

To create a Docker image, run: 
```shell
jeka pack docker: build
```
or
```shell
jeka pack docker: buildNative
```

The docker image is customized using the `Build#postInit(DockerBuild)` method.

See [documentation](https://jeka-dev.github.io/jeka/reference/kbeans-docker/) for customizing Docker images.



