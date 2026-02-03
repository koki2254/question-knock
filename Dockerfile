# 1. ビルド環境 (Maven)
FROM maven:3.9-eclipse-temurin-17 AS build
COPY . .
RUN mvn clean package -Dmaven.test.skip=true

# 2. 実行環境 (JRE)
FROM eclipse-temurin:17-jre-jammy
COPY --from=build /target/*.jar app.jar
# Renderは環境変数 PORT で指定されたポートをリッスンする必要があるため
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]