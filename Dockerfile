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
FROM mcr.microsoft.com/playwright:v1.49.0-noble AS runtime

# Install OpenJDK 21 and OpenCV-required native libs
RUN apt-get update && apt-get install -y \
    openjdk-21-jre-headless \
    libopencv-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 1. Copy the built distribution contents directly into /app
# This flattens the project-name subfolder so bin/ and lib/ are in the root
COPY --from=build /app/build/install/* /app/

# Environment variable for Playwright browsers
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

EXPOSE 3000

# 2. Dynamically execute the only file in the bin directory,
# preventing hardcoding issues if your project name changes.
CMD ["sh", "-c", "./bin/* 2>&1"]