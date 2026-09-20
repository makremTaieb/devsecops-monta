FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /build/target/*.jar app.jar
RUN chown spring:spring app.jar

USER spring

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO /dev/null http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java","-Xms256m","-Xmx512m","-jar","app.jar"]