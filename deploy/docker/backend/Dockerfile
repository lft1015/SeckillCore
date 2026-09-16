FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
RUN mvn -f backend/pom.xml -pl seckill-launcher -am clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /workspace/backend/seckill-launcher/target/*-exec.jar /app/app.jar

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
