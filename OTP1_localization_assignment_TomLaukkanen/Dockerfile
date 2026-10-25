# Build stage: compile and package the app with Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Download the dependencies in their own layer so code changes do not download them again
RUN mvn -B dependency:go-offline
COPY src ./src
# The tests run in the CI pipeline before the image is built
RUN mvn -B package -DskipTests

# Run stage: only a JRE and the jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/temperature-converter.jar app.jar

# UTF-8 locale so Finnish, Swedish and Japanese text prints correctly in the container
ENV LANG=C.UTF-8

# Interactive console app: run with "docker run -it"
ENTRYPOINT ["java", "-jar", "app.jar"]
