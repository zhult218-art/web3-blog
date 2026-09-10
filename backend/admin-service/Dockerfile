FROM docker.m.daocloud.io/library/eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/admin-service-1.0.0.jar app.jar
EXPOSE 9002
ENTRYPOINT ["java", "-jar", "app.jar"]