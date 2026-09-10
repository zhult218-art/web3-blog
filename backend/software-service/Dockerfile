FROM docker.m.daocloud.io/library/eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/software-service-1.0.0.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]