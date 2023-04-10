# Negentropy

Learn to buck up.

## Running the application

http://localhost:8080 for web application.
http://localhost:8080/h2 for DB console.

## Deploying to Production

To create a production build, call `mvnw clean package -Pproduction` (Windows),
or `./mvnw clean package -Pproduction` (Mac & Linux).
This will build a JAR file with all the dependencies and front-end resources,
ready to be deployed. The file can be found in the `target` folder after the build completes.

Once the JAR file is built, you can run it using
`java -jar target/negentropy-1.0-SNAPSHOT.jar`

## Deploying using Docker

To build the Dockerized version of the project, run

```
mvn clean package -Pproduction
docker build . -t negentropy:latest
```

Once the Docker image is correctly built, you can test it locally using

```
docker run -p 8080:8080 negentropy:latest
```
