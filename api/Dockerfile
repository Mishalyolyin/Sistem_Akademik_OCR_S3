# Dua tahap: tahap pertama membangun jar, tahap kedua hanya menjalankannya.
# Image akhir jadi jauh lebih kecil karena tidak membawa Maven dan kode sumber.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Dependensi disalin lebih dulu supaya lapisan ini bisa dipakai ulang
# selama pom.xml tidak berubah.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tidak berjalan sebagai root.
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar app.jar
RUN mkdir -p /app/storage && chown -R app:app /app
USER app

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
