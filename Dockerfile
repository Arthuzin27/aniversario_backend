# --- Etapa 1: build com o Maven Wrapper -------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# O projeto é desenvolvido no Windows: remove um eventual CRLF do mvnw antes de executá-lo no Linux
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src ./src
# Os testes usam Testcontainers (precisam de Docker); rode-os fora do build: ./mvnw test
RUN ./mvnw clean package -DskipTests -B

# --- Etapa 2: imagem final somente com a JRE --------------------------------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && addgroup --system spring && adduser --system --ingroup spring spring
COPY --from=build /app/target/evelyn.jar app.jar
RUN chown spring:spring app.jar
USER spring
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
