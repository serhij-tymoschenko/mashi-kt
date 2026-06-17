# ==========================================
# Stage 1: Build the application
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy gradle files for caching layers
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./

# Ensure gradlew has executable permissions
RUN chmod +x ./gradlew

# Copy the rest of the application source code
COPY src ./src
# Copy your keys.properties if needed during build (ensure it's ignored in .dockerignore if sensitive)
COPY keys.properties* ./

# Build the fat JAR / distribution (Ktor uses installDist or build)
RUN ./gradlew installDist --no-daemon

# ==========================================
# Stage 2: Create the runtime image
# ==========================================
# Playwright requires a full Ubuntu/Debian base to run browsers,
# and their official image comes with all necessary dependencies.
FROM mcr.microsoft.com/playwright:v1.49.0-noble AS runtime

# Install OpenJDK 21 and OpenCV-required native libs (glibc/libstdc++)
RUN apt-get update && apt-get install -y \
    openjdk-21-jre-headless \
    libopencv-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built application from the build stage
# Note: Adjust 'build/install/app' if your root project folder name is different
COPY --from=build /app/build/install/* ./

# Expose Ktor default port
EXPOSE 3000

# Environment variable to ensure Playwright knows where to find its browsers
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# Run the Ktor application using the generated startup script
CMD ["./bin/app"]