#!/usr/bin/env bash
# dev-restart.sh — 本地 java 替代 docker app，快速验证后端改动
#
# 思路：DB/Redis/PG 仍跑在 docker（已对外暴露 3307/6379/5432），
# app 改成本地 java -jar，避免 docker rebuild 那 5 分钟。
#
# 用法：
#   ./dev-restart.sh             # 默认：增量 mvn 构建 + 前台启动（Ctrl+C 退出）
#   ./dev-restart.sh --full      # 全量 mvn clean package
#   ./dev-restart.sh --no-build  # 跳过 mvn，仅重启已有 jar
#   ./dev-restart.sh --bg        # 后台启动，日志写 dev-app.log
#   ./dev-restart.sh --stop      # 停掉本地 dev app（不影响 docker db）
#   ./dev-restart.sh -h          # 本帮助

set -euo pipefail
cd "$(dirname "$0")"

PIDFILE=".dev-app.pid"
LOGFILE="dev-app.log"
JAR_GLOB="pengcheng-starter/target/pengcheng-starter-*.jar"
NGINX_CONF="nginx.conf"
NGINX_CONF_BAK=".nginx.conf.original"

# nginx upstream 切换：dev 模式 → host.docker.internal:8080；docker 模式 → app:8080
# 通过备份 / 恢复原 nginx.conf 实现，避免 git diff 噪音
# 原地截断写入（保 inode）——macOS Docker bind mount 对 rename-based 替换不友好
write_in_place() {
  # $1=源内容文件  $2=目标（被截断后写入，保留 inode）
  cat "$1" > "$2"
}

flip_nginx_to_host() {
  if [[ ! -f "$NGINX_CONF" ]]; then return 0; fi
  if grep -q "server app:8080;" "$NGINX_CONF"; then
    cp "$NGINX_CONF" "$NGINX_CONF_BAK"
    local tmp
    tmp=$(mktemp)
    sed 's|server app:8080;|server host.docker.internal:8080;|' "$NGINX_CONF" > "$tmp"
    write_in_place "$tmp" "$NGINX_CONF"
    rm -f "$tmp"
    nginx_apply "host.docker.internal:8080 (本地 java)"
  fi
}

flip_nginx_to_docker() {
  if [[ -f "$NGINX_CONF_BAK" ]]; then
    write_in_place "$NGINX_CONF_BAK" "$NGINX_CONF"
    rm -f "$NGINX_CONF_BAK"
    nginx_apply "app:8080 (docker)"
  fi
}

# nginx reload；reload 失败时 fallback 到 restart（macOS bind mount inode 失效场景）
nginx_apply() {
  local label="$1"
  if ! docker ps --filter name=masterlife-nginx --filter status=running -q | grep -q .; then
    echo "[nginx] 容器未运行，跳过 reload（生效需要启动 nginx）"
    return 0
  fi
  if docker exec masterlife-nginx nginx -s reload >/dev/null 2>&1; then
    echo "[nginx] upstream → $label (reload)"
  else
    echo "[nginx] reload 失败，fallback restart 容器"
    docker restart masterlife-nginx >/dev/null 2>&1
    echo "[nginx] upstream → $label (restart)"
  fi
}

BUILD="incremental"
FOREGROUND=true
ACTION="start"

case "${1:-}" in
  --full)     BUILD="full" ;;
  --no-build) BUILD="skip" ;;
  --bg)       FOREGROUND=false ;;
  --stop)     ACTION="stop" ;;
  -h|--help)  sed -n '2,15p' "$0" | sed 's/^# \?//'; exit 0 ;;
  "") ;;
  *) echo "未知参数: $1（用 -h 看帮助）"; exit 1 ;;
esac

# 1. 杀旧 dev app（按 pid 文件 + 兜底按 8080 占用）
stop_local_app() {
  if [[ -f "$PIDFILE" ]]; then
    local oldpid
    oldpid=$(cat "$PIDFILE")
    if kill -0 "$oldpid" 2>/dev/null; then
      echo "[stop] killing dev app PID=$oldpid"
      kill "$oldpid" 2>/dev/null || true
      sleep 2
      kill -9 "$oldpid" 2>/dev/null || true
    fi
    rm -f "$PIDFILE"
  fi
  # 兜底：清掉占 8080 的本地 java（不会动 docker，docker 占的是另一个 PID 命名空间）
  local pids_on_8080
  pids_on_8080=$(lsof -ti tcp:8080 2>/dev/null || true)
  if [[ -n "$pids_on_8080" ]]; then
    for p in $pids_on_8080; do
      if ps -p "$p" -o comm= 2>/dev/null | grep -q java; then
        echo "[stop] killing stray local java on :8080 (PID=$p)"
        kill -9 "$p" 2>/dev/null || true
      fi
    done
  fi
}

stop_local_app

if [[ "$ACTION" == "stop" ]]; then
  flip_nginx_to_docker
  echo "[stop] dev app stopped"
  echo "       想恢复 docker 模式：docker compose start app"
  exit 0
fi

# 2. 撞端口检查：docker app 还跑着会占 8080
if docker ps --filter name=masterlife-app --filter status=running -q 2>/dev/null | grep -q .; then
  echo "[err] docker masterlife-app 仍在运行，会占用 8080 端口。"
  echo "      请先执行：docker compose stop app"
  exit 1
fi

# 3. 构建
case "$BUILD" in
  full)
    echo "[build] mvn clean package -DskipTests"
    mvn clean package -DskipTests -q
    ;;
  incremental)
    echo "[build] mvn -pl pengcheng-starter -am package -DskipTests"
    mvn -pl pengcheng-starter -am package -DskipTests -q
    ;;
  skip)
    echo "[build] 跳过构建"
    ;;
esac

# 4. 选最新 jar
JAR_FILE=$(ls -t $JAR_GLOB 2>/dev/null | head -1)
if [[ -z "$JAR_FILE" || ! -f "$JAR_FILE" ]]; then
  echo "[err] 找不到 jar：$JAR_GLOB（先去掉 --no-build 跑一次构建）"
  exit 1
fi

# 5. 加载 .env（拿到 docker 配的密码），然后覆盖几个本地差异：
#    - DB_PORT: docker 容器内是 3306，但 host 映射是 3307
#    - REDIS_DB: dev profile 用 0（.env 给的是 prod 的 10）
#    - DB_USERNAME/PASSWORD: 用 root + 容器 root 密码（dev 简单点；也可改成 pengcheng_app）
if [[ -f ".env" ]]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi

export DB_HOST=localhost
export DB_PORT=3307
export DB_NAME="${DB_NAME:-pengcheng-system}"
export DB_USERNAME=root
export DB_PASSWORD="${DB_ROOT_PASSWORD:-your_mysql_root_password}"
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD="${REDIS_PASSWORD:-}"
export REDIS_DB=0
export SPRING_PROFILES_ACTIVE=dev

# 选 Java 17（项目要求；本机如有 JDK 25 会与 Spring Boot 3.3.5 冲突）
JAVA_HOME_17="${JAVA_HOME_17:-/opt/homebrew/opt/openjdk@17}"
if [[ -x "$JAVA_HOME_17/bin/java" ]]; then
  export JAVA_HOME="$JAVA_HOME_17"
  export PATH="$JAVA_HOME/bin:$PATH"
elif ! command -v java >/dev/null 2>&1; then
  echo "[err] 找不到 java，且 $JAVA_HOME_17 不存在。装一下：brew install openjdk@17"
  exit 1
fi

echo "[run] $JAR_FILE"
echo "      java=$(java -version 2>&1 | head -1)"
echo "      profile=dev  db=$DB_HOST:$DB_PORT  redis db=$REDIS_DB"

flip_nginx_to_host

if $FOREGROUND; then
  exec java -jar "$JAR_FILE"
else
  nohup java -jar "$JAR_FILE" > "$LOGFILE" 2>&1 &
  echo $! > "$PIDFILE"
  sleep 1
  echo "[bg] PID=$(cat "$PIDFILE")  log: tail -f $LOGFILE"
fi
