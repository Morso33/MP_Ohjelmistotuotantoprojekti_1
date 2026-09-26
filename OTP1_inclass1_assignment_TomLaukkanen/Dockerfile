# Build stage: compile and package the app with Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

# Run stage: only a JRE and the runnable jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/temperature-converter.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
