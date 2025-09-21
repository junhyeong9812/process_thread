FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy gradle files
COPY java-spring/gradle* ./
COPY java-spring/gradle ./gradle
RUN ./gradlew --version

# Copy source and build
COPY java-spring/src ./src
COPY java-spring/build.gradle ./
RUN ./gradlew build -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy jar from builder
COPY --from=builder /app/build/libs/*.jar app.jar

# JVM options for Virtual Threads
ENV JAVA_OPTS="--enable-preview -Xms256m -Xmx512m"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]