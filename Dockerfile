FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml mvnw ./
COPY .mvn/ .mvn/

RUN ./mvnw dependency:go-offline --batch-mode

COPY src/ src/
RUN ./mvnw package -DskipTests --batch-mode

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]