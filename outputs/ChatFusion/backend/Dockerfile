# Build Stage using official Gradle 8.9 with JDK 21
FROM gradle:8.9-jdk21-alpine AS build
WORKDIR /app

# Copy backend files
COPY . .

# Grant execute permissions to gradlew and build distribution
RUN chmod +x ./gradlew && ./gradlew installDist --no-daemon

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built distribution from build stage
COPY --from=build /app/build/install/backend /app

ENV PORT=8080
EXPOSE 8080

CMD ["/app/bin/backend"]
