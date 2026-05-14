#!/usr/bin/env bash
# deploy.sh —— 本地 mvn 增量编译 + scp jar + ssh 重启服务器 app 容器
#
# 用法：
#   ./deploy.sh                # 增量编译（推荐，30 秒内）
#   ./deploy.sh --full         # 全量 mvn clean package（依赖升级或解析报错时用）
#   ./deploy.sh --skip-build   # 跳过编译，直接 scp 已有 jar
#   ./deploy.sh --no-restart   # 只传 jar，不 ssh 重启（自己上去看）
#
# 前置（一次性）：
#   1. ssh 免密：在本机执行 ssh-copy-id <你的服务器>
#   2. 服务器上 git clone 仓库，目录默认是 ~/pengcheng-oa
#   3. .env 配置：
#        DEPLOY_SSH_HOST=root@1.2.3.4              # 或 user@host
#        DEPLOY_REMOTE_DIR=~/pengcheng-oa          # 服务器仓库路径
#   4. 服务器首次启动前需要 git pull（拿到新的 Dockerfile / docker-compose.yml）

set -euo pipefail
cd "$(dirname "$0")"

# 读取 .env
if [[ -f ".env" ]]; then
    set -a; . ./.env; set +a
fi

: "${DEPLOY_SSH_HOST:?需要在 .env 设置 DEPLOY_SSH_HOST，例如 root@1.2.3.4}"
: "${DEPLOY_REMOTE_DIR:=~/pengcheng-oa}"

BUILD_MODE="incremental"
DO_SCP=true
DO_RESTART=true

while [[ $# -gt 0 ]]; do
    case "$1" in
        --full)        BUILD_MODE="full" ;;
        --skip-build)  BUILD_MODE="skip" ;;
        --no-restart)  DO_RESTART=false ;;
        --no-scp)      DO_SCP=false ;;
        -h|--help)     sed -n '2,17p' "$0" | sed 's/^# \?//'; exit 0 ;;
        *)             echo "未知参数: $1（-h 看帮助）"; exit 1 ;;
    esac
    shift
done

# 1. 编译
case "$BUILD_MODE" in
    full)
        echo "[build] mvn clean package -DskipTests（全量）"
        mvn clean package -DskipTests -q
        ;;
    incremental)
        echo "[build] mvn package -DskipTests（增量，未改的模块跳过）"
        mvn package -DskipTests -q
        ;;
    skip)
        echo "[build] 跳过编译"
        ;;
esac

# 2. 定位 jar
JAR_FILE=$(ls -t pengcheng-starter/target/pengcheng-starter-*.jar 2>/dev/null | head -1)
if [[ -z "$JAR_FILE" || ! -f "$JAR_FILE" ]]; then
    echo "[err] 找不到 jar，需要先跑一次完整编译：./deploy.sh --full"
    exit 1
fi

JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
echo "[jar] $JAR_FILE ($JAR_SIZE)"

# 3. scp 推到服务器
if $DO_SCP; then
    REMOTE_JAR="${DEPLOY_REMOTE_DIR}/release/app.jar"
    echo "[scp] → $DEPLOY_SSH_HOST:$REMOTE_JAR"
    ssh "$DEPLOY_SSH_HOST" "mkdir -p ${DEPLOY_REMOTE_DIR}/release"
    # -C 启用压缩，jar 是 zip 压缩比有限，但加几个百分比也好
    scp -C "$JAR_FILE" "$DEPLOY_SSH_HOST:$REMOTE_JAR"
fi

# 4. ssh 重启 app 容器
if $DO_RESTART; then
    echo "[restart] docker compose restart app"
    ssh "$DEPLOY_SSH_HOST" "cd ${DEPLOY_REMOTE_DIR} && docker compose restart app"

    echo ""
    echo "[log] 查看启动日志："
    echo "  ssh $DEPLOY_SSH_HOST 'cd ${DEPLOY_REMOTE_DIR} && docker compose logs -f app | head -50'"
fi

echo ""
echo "✅ 部署完成"
