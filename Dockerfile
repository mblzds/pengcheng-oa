# ============================================================
# MasterLife V3.0 Dockerfile（仅运行时；jar 由本地 mvn 打包后 scp 上来挂载）
# ============================================================
#
# 设计：阿里云 ECS 跑 mvn 太慢，把 mvn build 完全转移到 dev 本地。
# 镜像只含 JRE + 时区 + 健康检查，jar 通过 docker-compose volume 挂载进来。
#
# 发布流程（详见 deploy.sh）：
#   dev:    cd pengcheng-ui && npm run build && cd ..
#           mvn clean package -DskipTests
#           ./deploy.sh                # 自动 scp + ssh restart
#   aliyun: 自动重启，无需手工操作
# ============================================================

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

RUN mkdir -p /app/logs /app/uploads && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE="prod"

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -fsS http://localhost:8080/api/v3/api-docs >/dev/null || exit 1

# jar 文件通过 volume 挂载到 /app/app.jar（详见 docker-compose.yml）
# 容器启动时 jar 必须已存在；首次部署需要先跑 ./deploy.sh 把 jar scp 上来
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]
