FROM docker.m.daocloud.io/library/eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/tool-service-1.0.0.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]