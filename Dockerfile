# ---------- BUILD STAGE ----------
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY pom.xml .

# Magic trick 1: Mount the Maven cache so dependencies survive between builds
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -e -B dependency:go-offline

COPY src ./src

# Magic trick 2: Mount the cache again so the package phase doesn't re-download missing plugins
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:25-jdk-jammy
WORKDIR /app

# create non-root user for security
RUN useradd -m appuser
USER appuser

# copy jar
COPY --from=builder /app/target/service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]