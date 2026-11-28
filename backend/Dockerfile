# Imagen del backend para Render (C-03, design D7).
# Los tests corren en CI; acá solo se empaqueta.

FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
# Primero el wrapper y la configuración, para cachear la descarga de dependencias.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /src/build/libs/app.jar ./app.jar
USER app
# JVM para 512 MB de RAM: heap acotado por porcentaje, GC serial, pilas chicas y JIT liviano.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1" \
    SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
