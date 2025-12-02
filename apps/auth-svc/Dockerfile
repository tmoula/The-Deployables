FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle gradle
COPY src src
RUN ./gradlew build -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]