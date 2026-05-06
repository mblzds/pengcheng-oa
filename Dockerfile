# ============================================================
# MasterLife V3.0 多阶段构建 Dockerfile
# ============================================================

# Stage 1: 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml .
COPY pengcheng-common/pom.xml pengcheng-common/
COPY pengcheng-infra/ pengcheng-infra/
COPY pengcheng-core/ pengcheng-core/
COPY pengcheng-api/ pengcheng-api/
COPY pengcheng-job/pom.xml pengcheng-job/
COPY pengcheng-starter/pom.xml pengcheng-starter/

RUN mvn dependency:go-offline -B 2>/dev/null || true

COPY . .
RUN mvn clean package -DskipTests -B

# Stage 2: 运行阶段
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
  && rm -rf /var/lib/apt/lists/*

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=builder /build/pengcheng-starter/target/*.jar app.jar

RUN mkdir -p /app/logs /app/uploads && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE="prod"

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -fsS http://localhost:8080/api/v3/api-docs >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]
