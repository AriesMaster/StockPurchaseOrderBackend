FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system stockflow && useradd --system --gid stockflow stockflow
COPY --from=build /app/target/*.jar /app/app.jar
USER stockflow
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=65.0","-jar","/app/app.jar"]
