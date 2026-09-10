FROM docker.m.daocloud.io/library/eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/quant-service-1.0.0.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]