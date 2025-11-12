############################
# Build stage
############################
FROM maven:3.9.9-eclipse-temurin-22 AS builder

WORKDIR /build

# Pre-fetch dependencies for faster incremental builds
COPY pom.xml ./
RUN mvn -q -B -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -q -B -DskipTests clean package

############################
# Runtime stage
############################
FROM eclipse-temurin:22-jre-alpine

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /build/target/kopi-0.0.1-SNAPSHOT.jar /app/app.jar

# Default environment (override at runtime if needed)
ENV JAVA_OPTS=""

EXPOSE 8080

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS ${SPRING_PROFILES_ACTIVE:+-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}} -jar /app/app.jar"]


