FROM docker.m.daocloud.io/library/eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/ai-proxy-service-1.0.0.jar app.jar
EXPOSE 8093
ENTRYPOINT ["java", "-jar", "app.jar"]
