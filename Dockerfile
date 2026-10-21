FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy project files
COPY . .

# Grant execute permissions to gradlew
RUN chmod +x gradlew

# Build backend distribution package
RUN ./gradlew outputs:ChatFusion:backend:installDist --no-daemon

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built distribution from build stage
COPY --from=build /app/outputs/ChatFusion/backend/build/install/backend /app

ENV PORT=8080
EXPOSE 8080

CMD ["/app/bin/backend"]
