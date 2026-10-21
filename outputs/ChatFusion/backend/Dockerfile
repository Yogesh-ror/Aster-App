# Build Stage using official Gradle 8.9 with JDK 21
FROM gradle:8.9-jdk21-alpine AS build
WORKDIR /app

# Copy project files
COPY . .

# Ensure gradlew permissions and try local task then fallback root task
RUN chmod +x gradlew && \
    ( ./gradlew installDist --no-daemon || \
      ./gradlew outputs:ChatFusion:backend:installDist --no-daemon || \
      gradle installDist --no-daemon )

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built distribution from build stage
COPY --from=build /app/build/install/backend /app

ENV PORT=8080
EXPOSE 8080

CMD ["/app/bin/backend"]
