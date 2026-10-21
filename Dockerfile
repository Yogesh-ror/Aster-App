# Build Stage using official Gradle 8.9 with JDK 21
FROM gradle:8.9-jdk21-alpine AS build
WORKDIR /app

# Copy project files
COPY . .

# Ensure gradlew permissions and try root task then fallback task
RUN chmod +x gradlew && \
    ( ./gradlew outputs:ChatFusion:backend:installDist --no-daemon || \
      ./gradlew installDist --no-daemon || \
      gradle installDist --no-daemon )

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built distribution from build stage (checking both possible build paths)
COPY --from=build /app/outputs/ChatFusion/backend/build/install/backend /app

ENV PORT=8080
EXPOSE 8080

CMD ["/app/bin/backend"]
