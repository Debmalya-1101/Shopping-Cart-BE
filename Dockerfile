# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build the application, skipping tests to speed up the process
RUN mvn clean package -DskipTests

# Stage 2: Run the application
# We use a lightweight JRE image since Playwright is disabled via the dev profile.
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Environment variables for Render's 512MB constraint
# -Xmx256m: Max Heap size
# -XX:MaxMetaspaceSize=128m: Cap class metadata
# -Xss512k: Thread stack size
ENV JAVA_OPTS="-Xmx256m -Xms256m -XX:MaxMetaspaceSize=128m -Xss512k"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
