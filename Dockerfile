# ============================================================
# MasterLife V3.0 多阶段构建 Dockerfile
# ============================================================
#
# 构建产出两个镜像：
#   - app (默认 target): Spring Boot 后端，JAR 自带前端静态资源
#   - nginx (target=nginx): 反向代理 + 前端静态资源，独立的 nginx 镜像
#
# 用法：
#   docker compose build app          # 构建后端
#   docker compose build nginx        # 构建 nginx
#   docker compose build              # 同时构建两个，共用 frontend-builder 缓存
# ============================================================

# ============================================================
# Stage 1: 前端构建
# ============================================================
FROM node:20-alpine AS frontend-builder
WORKDIR /build

# 利用 vite.config.ts 里 outDir = ../pengcheng-starter/src/main/resources/static
# 先建好目录结构，让 vite 能写出去
RUN mkdir -p /build/pengcheng-ui /build/pengcheng-starter/src/main/resources/static

WORKDIR /build/pengcheng-ui

# 先 COPY 锁文件，让 npm ci 这层尽量命中缓存
COPY pengcheng-ui/package.json pengcheng-ui/package-lock.json ./
# 国内构建用 npmmirror 加速；如果你换私有源，改下面 --registry 即可
RUN npm ci --registry=https://registry.npmmirror.com

COPY pengcheng-ui/ ./
RUN npm run build

# ============================================================
# Stage 2: 后端构建
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS backend-builder
WORKDIR /build

# Maven 镜像走阿里云（国内拉 Central 太慢）
COPY .mvn/settings.xml /root/.m2/settings.xml

COPY pom.xml .
COPY pengcheng-common/pom.xml pengcheng-common/
COPY pengcheng-infra/ pengcheng-infra/
COPY pengcheng-core/ pengcheng-core/
COPY pengcheng-api/ pengcheng-api/
COPY pengcheng-job/pom.xml pengcheng-job/
COPY pengcheng-starter/pom.xml pengcheng-starter/

RUN mvn dependency:go-offline -B 2>/dev/null || true

# 拷源码 + 把前端构建产物嵌入 starter resources/static
COPY . .
COPY --from=frontend-builder /build/pengcheng-starter/src/main/resources/static \
                             /build/pengcheng-starter/src/main/resources/static

RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 3 (默认): 后端运行时
# ============================================================
FROM eclipse-temurin:17-jre-jammy AS app
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
  && rm -rf /var/lib/apt/lists/*

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=backend-builder /build/pengcheng-starter/target/*.jar app.jar

RUN mkdir -p /app/logs /app/uploads && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE="prod"

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -fsS http://localhost:8080/api/v3/api-docs >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]

# ============================================================
# Stage 4: nginx 运行时（内置前端产物）
# ============================================================
FROM nginx:alpine AS nginx

# 移除默认配置，由 docker-compose 挂的 nginx.conf 替代
RUN rm -f /etc/nginx/conf.d/default.conf

# 前端静态资源直接 COPY 进镜像，不再依赖宿主机挂目录
COPY --from=frontend-builder /build/pengcheng-starter/src/main/resources/static \
                             /usr/share/nginx/html

EXPOSE 80 443
