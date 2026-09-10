FROM docker.m.daocloud.io/library/eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/jarvis-service-1.0.0.jar app.jar
EXPOSE 9001
ENTRYPOINT ["java", "-jar", "app.jar"]