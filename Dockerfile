# ============================================================
# MasterLife V3.0 Dockerfile（仅后端编译；前端在开发者本地 build 后 commit 入仓）
# ============================================================
#
# 设计：阿里云 ECS 配置较低，把前端 build 留在 dev 上，仓库直接带前端产物
# （pengcheng-starter/src/main/resources/static/）。容器构建只跑 maven，
# 拿到已经被 commit 的 static 文件一起打进 JAR + nginx 卷挂使用。
#
# 发布流程：
#   dev:    cd pengcheng-ui && npm run build
#           git add pengcheng-starter/src/main/resources/static/ && git commit && git push
#   aliyun: git pull && docker compose build app && docker compose up -d
# ============================================================

# Stage 1: 构建阶段（仅后端）
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Maven 镜像走阿里云仓库，国内构建避免 Central 慢
COPY .mvn/settings.xml /root/.m2/settings.xml

COPY pom.xml .
COPY pengcheng-common/pom.xml pengcheng-common/
COPY pengcheng-infra/ pengcheng-infra/
COPY pengcheng-core/ pengcheng-core/
COPY pengcheng-api/ pengcheng-api/
COPY pengcheng-job/pom.xml pengcheng-job/
COPY pengcheng-starter/pom.xml pengcheng-starter/

RUN mvn dependency:go-offline -B 2>/dev/null || true

# 这一步把 pengcheng-starter/src/main/resources/static 一起拿进来（已 commit 的前端产物）
COPY . .
RUN mvn clean package -DskipTests -B

# Stage 2: 运行阶段
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    tzdata \
  && rm -rf /var/lib/apt/lists/* \
  && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
  && echo "Asia/Shanghai" > /etc/timezone

ENV TZ=Asia/Shanghai

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
