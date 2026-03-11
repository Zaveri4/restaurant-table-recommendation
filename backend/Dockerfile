FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY gradle gradle
COPY gradlew gradlew.bat settings.gradle build.gradle /app/
RUN chmod +x /app/gradlew

COPY src src
RUN ./gradlew bootJar -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
