FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV PORT=8080
RUN useradd --system --create-home --uid 10001 appuser && mkdir -p /app/uploads && chown -R appuser:appuser /app
COPY --from=build /workspace/target/edubridge-bd-0.0.1-SNAPSHOT.jar /app/app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
