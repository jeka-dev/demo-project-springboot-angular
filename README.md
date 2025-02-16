# JeKa Springboot-Angular application

This repo show-cases how to build a Springboot+Angular Application with JeKa. The build involves :

- Compilation + testing of the Java an Angular code
- Sonarqube analysis of Java and Angular code (with Java code coverage)
- End-to-end testing (using selenide) on application deployed on host or Docker

Additionally this demo showcases [how to create Docker native images](#Docker-Image).

The application is a simple web app, managing a list of users, copied from [this tutorial](https://www.baeldung.com/spring-boot-angular-web).

![screenshot.png](./screenshot.png)


## Run the application

```shell
jeka -p
```
Creates a bootable JAR if not found and starts the app. Access it at [http://localhost:8080](http://localhost:8080).

If a native executable is built, this command runs that version instead.

> [!TIP]
> If you want to start the application without cloning Git repository by yourself, just execute :
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
jeka pack native: compile
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
jeka ::packQuality
```
Command *::packQuality* is defined in [jeka.properties](jeka.properties)

The Sonarqube analysis + coverage for Java code is provided out-of-the-box, thanks to *Jacoco* and *Sonarqube* Kbean, 
that are activated in the command line.

For Angular part, a specific method `sonarJs` has been implemented.

## End-to-end testing

Here, the application is tested end-to-end using [selenide](https://https://selenide.org/).
This allows to test the application by simulating user actions on the browser.

The test classes for e2e tests are located in *e2e* package from *test* dir.

The tests are executed on deployed applications. This build includes 2 scenarios :

- Testing the application deployed on local host
- Testing the application deployed as docker container

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

See [documentation](https://jeka-dev.github.io/jeka/reference/kbeans-docker/) for customizing Docker images.



