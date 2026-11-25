FROM gradle:9.3.1-jdk21-alpine AS build

WORKDIR /workspace

COPY gradle gradle
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY gradle.properties settings.gradle.kts build.gradle.kts ./
COPY server/build.gradle.kts server/build.gradle.kts
COPY server/src server/src

RUN rm -f gradle/gradle-daemon-jvm.properties \
 && sed -i '/include(":app")/d' settings.gradle.kts \
 && ./gradlew :server:installDist --no-daemon --no-configuration-cache

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /workspace/server/build/install/server ./

EXPOSE 8081

ENTRYPOINT ["./bin/server"]
