# ===== BUILD STAGE =====
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY . .
RUN chmod +x mvnw
RUN ./mvnw -DskipTests clean package

# ===== RUNTIME STAGE =====
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

# FLIGHT-SERVICE runs on 8081
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
