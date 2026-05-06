#!/bin/bash
# ============================================================
# Redis 缓存清理脚本
# ============================================================
# 说明：项目从 mars -> pengcheng 重命名后，Redis 中旧的缓存 key
#       前缀为 "mars:"，需要清理以避免冲突。
#
# 使用方式：
#   chmod +x sql/redis-cleanup.sh
#   ./sql/redis-cleanup.sh [REDIS_HOST] [REDIS_PORT] [REDIS_PASSWORD] [REDIS_DB]
#
# 默认参数：localhost 6379 无密码 数据库10
# ============================================================

REDIS_HOST=${1:-localhost}
REDIS_PORT=${2:-6379}
REDIS_PASSWORD=${3:-}
REDIS_DB=${4:-10}

if [ -n "$REDIS_PASSWORD" ]; then
    AUTH_PARAM="-a $REDIS_PASSWORD"
else
    AUTH_PARAM=""
fi

echo "========================================="
echo "Redis 缓存清理工具"
echo "========================================="
echo "连接: $REDIS_HOST:$REDIS_PORT (DB: $REDIS_DB)"
echo ""

# 检查 redis-cli 是否可用
if ! command -v redis-cli &> /dev/null; then
    echo "[错误] redis-cli 未安装。请先安装 Redis CLI 工具："
    echo "  macOS: brew install redis"
    echo "  Ubuntu: sudo apt install redis-tools"
    echo ""
    echo "或者手动在 Redis CLI 中执行以下命令："
    echo "  SELECT $REDIS_DB"
    echo "  KEYS mars:*"
    echo "  DEL <以上返回的所有key>"
    echo ""
    echo "或直接清空整个数据库（开发环境推荐）："
    echo "  SELECT $REDIS_DB"
    echo "  FLUSHDB"
    exit 1
fi

echo "[1/3] 查找旧的 mars: 前缀缓存 key..."
OLD_KEYS=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT $AUTH_PARAM -n $REDIS_DB KEYS "mars:*" 2>/dev/null)

if [ -z "$OLD_KEYS" ]; then
    echo "  未发现 mars: 前缀的 key，无需清理。"
else
    KEY_COUNT=$(echo "$OLD_KEYS" | wc -l | tr -d ' ')
    echo "  发现 $KEY_COUNT 个旧 key："
    echo "$OLD_KEYS" | head -20
    [ "$KEY_COUNT" -gt 20 ] && echo "  ... 还有 $((KEY_COUNT - 20)) 个"
    echo ""

    echo "[2/3] 删除旧的 mars: 前缀 key..."
    echo "$OLD_KEYS" | xargs redis-cli -h $REDIS_HOST -p $REDIS_PORT $AUTH_PARAM -n $REDIS_DB DEL
    echo "  已删除 $KEY_COUNT 个 key。"
fi

echo ""
echo "[3/3] 清理 Sa-Token 会话缓存（用户需重新登录）..."
SA_KEYS=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT $AUTH_PARAM -n $REDIS_DB KEYS "satoken:*" 2>/dev/null)
if [ -n "$SA_KEYS" ]; then
    SA_COUNT=$(echo "$SA_KEYS" | wc -l | tr -d ' ')
    echo "$SA_KEYS" | xargs redis-cli -h $REDIS_HOST -p $REDIS_PORT $AUTH_PARAM -n $REDIS_DB DEL
    echo "  已清除 $SA_COUNT 个会话 key，用户需重新登录。"
else
    echo "  无会话缓存需要清理。"
fi

echo ""
echo "========================================="
echo "清理完成！"
echo "========================================="
