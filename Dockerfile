# Build stage: compiles the app. Tests are run by CI, not here.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sh ./mvnw -B -q dependency:go-offline
COPY src src
RUN sh ./mvnw -B -q package -DskipTests

# Run stage: just a Java runtime and the jar, as a non-root user.
FROM eclipse-temurin:21-jre
RUN useradd --system --create-home app
USER app
# The app writes journalApp.log into its working directory.
WORKDIR /home/app
COPY --from=build /build/target/journalApp-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
