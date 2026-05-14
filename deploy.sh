#!/usr/bin/env bash
# deploy.sh —— 本地构建 docker 镜像并 push 到阿里云 ACR。
#
# 用法：
#   ./deploy.sh             # 默认 tag = 当前 git short-sha
#   ./deploy.sh v1.2.3      # 用指定 tag
#   ./deploy.sh --skip-build  # 只 push（已经 build 过）
#
# 前置：
#   1. 阿里云 ACR 已开通，且创建好命名空间 + 镜像仓库
#   2. 已 docker login 到 registry（详见 README 或本脚本底部提示）
#   3. .env 里配置：
#        ACR_REGISTRY=registry.cn-shenzhen.aliyuncs.com   # 你的 ACR region
#        ACR_NAMESPACE=masterlife                         # 你创建的命名空间
#        ACR_REPO=app                                     # 你创建的镜像仓库名
#
# 服务器端发布步骤（脚本 push 完成后，ssh 到阿里云 ECS 执行）：
#   cd ~/pengcheng-oa
#   git pull origin main         # 同步 docker-compose.yml 等配置
#   docker compose pull app      # 拉新镜像
#   docker compose up -d app     # 重启 app 容器
#   docker compose logs -f app | head -50  # 看启动日志

set -euo pipefail
cd "$(dirname "$0")"

# 读取 .env
if [[ -f ".env" ]]; then
    set -a; . ./.env; set +a
fi

: "${ACR_REGISTRY:?需要在 .env 设置 ACR_REGISTRY，例如 registry.cn-shenzhen.aliyuncs.com}"
: "${ACR_NAMESPACE:?需要在 .env 设置 ACR_NAMESPACE，例如 masterlife}"
: "${ACR_REPO:?需要在 .env 设置 ACR_REPO，例如 app}"

SKIP_BUILD=false
TAG=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-build) SKIP_BUILD=true; shift ;;
        -h|--help) sed -n '2,28p' "$0" | sed 's/^# \?//'; exit 0 ;;
        *) TAG="$1"; shift ;;
    esac
done

# 默认 tag = git short-sha；若工作区有未提交改动，附 -dirty 后缀
if [[ -z "$TAG" ]]; then
    TAG=$(git rev-parse --short HEAD)
    if ! git diff-index --quiet HEAD --; then
        TAG="${TAG}-dirty"
        echo "[warn] 工作区有未提交改动，tag 自动加 -dirty 后缀"
    fi
fi

IMAGE="${ACR_REGISTRY}/${ACR_NAMESPACE}/${ACR_REPO}:${TAG}"
LATEST="${ACR_REGISTRY}/${ACR_NAMESPACE}/${ACR_REPO}:latest"

echo "[deploy] image = $IMAGE"

# 1. 本地构建
if ! $SKIP_BUILD; then
    echo "[deploy] docker build (本地跑 mvn，服务器不用编译)"
    docker build -t "$IMAGE" -t "$LATEST" -f Dockerfile .
fi

# 2. push 到 ACR
echo "[deploy] docker push $IMAGE"
docker push "$IMAGE"

echo "[deploy] docker push $LATEST"
docker push "$LATEST"

echo ""
echo "============================================================"
echo "✅ 镜像已推送到 ACR：$IMAGE"
echo ""
echo "下一步：ssh 到生产服务器执行"
echo "  cd ~/pengcheng-oa"
echo "  git pull origin main"
echo "  echo 'APP_IMAGE=$LATEST' >> .env  # 仅首次需要"
echo "  docker compose pull app"
echo "  docker compose up -d app"
echo "============================================================"
echo ""
echo "[提示] 首次使用前需 docker login："
echo "  docker login --username=<你的阿里云账号> $ACR_REGISTRY"
echo "  （ACR 控制台 → 访问凭证 → 设置固定密码）"
