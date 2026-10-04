# Build stage: compile and package the app with Maven. Inside this Linux container
# Maven picks the Linux versions of the JavaFX jars.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Download the dependencies in their own layer so code changes do not download them again
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# Run stage: a JRE plus the GTK and X11 libraries JavaFX needs to open a window
FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends libgtk-3-0t64 libxtst6 libxxf86vm1 fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/temperature-converter.jar app.jar

# The window is drawn by the X server (Xming) running on the Windows host...
ENV DISPLAY=host.docker.internal:0.0
# ...and the conversions are saved to the MariaDB running on the host.
# DB_USER and DB_PASSWORD can be passed with "docker run -e" as well.
ENV DB_URL=jdbc:mariadb://host.docker.internal:3306/temperature_converter
# Finnish time, so conversions saved from the container get the same clock as the
# ones saved on Windows (override with "docker run -e TZ=...")
ENV TZ=Europe/Helsinki

# Software rendering, because Xming offers no OpenGL that JavaFX can use
ENTRYPOINT ["java", "-Dprism.order=sw", "-jar", "app.jar"]
