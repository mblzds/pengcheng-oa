#!/bin/bash

# ============================================================
# MasterLife 云端一键部署脚本
# 功能：Docker Compose 部署、环境配置、数据库初始化
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 配置变量
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
ADMIN_DIR="${PROJECT_ROOT}/pengcheng-admin"
ENV_FILE="${ADMIN_DIR}/.env"
DOCKER_COMPOSE_FILE="${ADMIN_DIR}/docker-compose.yml"

# 日志函数
info() { echo -e "${BLUE}[INFO]${NC} $*"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; }

# 检查 Docker
check_docker() {
    info "检查 Docker 环境..."
    
    if ! command -v docker &> /dev/null; then
        error "Docker 未安装"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        error "Docker Compose 未安装"
        exit 1
    fi
    
    success "Docker 环境检查通过"
}

# 创建.env 文件
create_env_file() {
    info "创建环境配置文件..."
    
    if [ -f "${ENV_FILE}" ]; then
        warn ".env 文件已存在，跳过创建"
        return 0
    fi
    
    cat > "${ENV_FILE}" << 'EOF'
# ============================================================
# MasterLife 环境变量配置
# ============================================================

# -------------------- 数据库配置 --------------------
DB_HOST=mysql
DB_PORT=3306
DB_NAME=pengcheng-system
DB_USERNAME=pengcheng_app
DB_PASSWORD=MasterLife@2026
DB_ROOT_PASSWORD=MasterLife@Root2026

# -------------------- Redis 配置 --------------------
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=MasterLife@Redis2026
REDIS_DB=10

# -------------------- PostgreSQL 配置 --------------------
PG_HOST=postgres
PG_PORT=5432
PG_DB=pengcheng_vector
PG_USERNAME=postgres
PG_PASSWORD=MasterLife@PG2026

# -------------------- MinIO 配置 --------------------
MINIO_ENDPOINT=minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# -------------------- AI 配置 --------------------
DASHSCOPE_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
ZHIPU_API_KEY=xxxxxxxxxxxxxxxxxxxxxxxx

# -------------------- SSL 配置 --------------------
SSL_KEYSTORE_PASSWORD=changeit

# -------------------- 微信小程序配置 --------------------
WECHAT_MINIPROGRAM_APPID=wx_xxxxxxxxxxxxxxxx
WECHAT_MINIPROGRAM_SECRET=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# -------------------- 微信支付配置 --------------------
WECHAT_PAY_MCHID=1234567890
WECHAT_PAY_APIV3_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
WECHAT_PAY_CERT_SERIAL_NO=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

EOF
    
    success "环境配置文件已创建：${ENV_FILE}"
    warn "请修改默认密码和 API 密钥！"
}

# 初始化目录
init_directories() {
    info "初始化目录..."
    
    cd "${ADMIN_DIR}"
    
    # 创建日志和数据目录
    mkdir -p logs
    mkdir -p uploads
    mkdir -p ssl
    
    success "目录初始化完成"
}

# 下载 SSL 证书（如果有）
setup_ssl() {
    info "配置 SSL 证书..."
    
    if [ -f "${ADMIN_DIR}/ssl/keystore.p12" ]; then
        success "SSL 证书已存在"
        return 0
    fi
    
    warn "SSL 证书不存在，将使用 HTTP 模式"
    info "如需 HTTPS，请将证书文件放到 ssl/keystore.p12"
}

# 启动服务
start_services() {
    info "启动 Docker 服务..."
    
    cd "${ADMIN_DIR}"
    
    # 停止旧服务
    docker-compose down || true
    
    # 启动所有服务
    docker-compose up -d
    
    success "服务启动完成"
}

# 等待服务就绪
wait_for_services() {
    info "等待服务就绪..."
    
    # 等待 MySQL
    info "等待 MySQL 启动..."
    for i in {1..30}; do
        if docker-compose exec -T mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
            success "MySQL 已就绪"
            break
        fi
        sleep 2
    done
    
    # 等待 Redis
    info "等待 Redis 启动..."
    for i in {1..10}; do
        if docker-compose exec -T redis redis-cli ping | grep -q PONG; then
            success "Redis 已就绪"
            break
        fi
        sleep 1
    done
    
    # 等待应用
    info "等待应用启动..."
    for i in {1..60}; do
        if curl -s http://localhost:8080/actuator/health | grep -q UP; then
            success "应用已就绪"
            break
        fi
        sleep 2
    done
}

# 查看服务状态
show_status() {
    info "服务状态:"
    echo ""
    docker-compose ps
    echo ""
}

# 显示访问信息
show_access_info() {
    echo "============================================"
    success "部署完成！"
    echo "============================================"
    echo ""
    echo "服务访问地址:"
    echo "  - Web 管理后台：http://localhost:80"
    echo "  - API 接口：http://localhost:8080/api"
    echo "  - MinIO 控制台：http://localhost:9001"
    echo "  - kkFileView: http://localhost:8012"
    echo ""
    echo "数据库连接信息:"
    echo "  - MySQL: localhost:3306"
    echo "  - Redis: localhost:6379"
    echo "  - PostgreSQL: localhost:5432"
    echo ""
    echo "默认账号:"
    echo "  - 账号：admin"
    echo "  - 密码：admin123"
    echo ""
    echo "日志查看:"
    echo "  docker-compose logs -f app"
    echo ""
    echo "============================================"
}

# 主函数
main() {
    echo "============================================"
    echo "  MasterLife 云端一键部署脚本"
    echo "============================================"
    echo ""
    
    # 解析参数
    local action="deploy"
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            deploy)
                action="deploy"
                shift
                ;;
            start)
                action="start"
                shift
                ;;
            stop)
                action="stop"
                shift
                ;;
            restart)
                action="restart"
                shift
                ;;
            status)
                action="status"
                shift
                ;;
            logs)
                action="logs"
                shift
                ;;
            help)
                echo "用法：$0 [command]"
                echo ""
                echo "Commands:"
                echo "  deploy    一键部署（默认）"
                echo "  start     启动服务"
                echo "  stop      停止服务"
                echo "  restart   重启服务"
                echo "  status    查看状态"
                echo "  logs      查看日志"
                echo "  help      显示帮助"
                exit 0
                ;;
            *)
                error "未知命令：$1"
                exit 1
                ;;
        esac
    done
    
    case $action in
        deploy)
            check_docker
            create_env_file
            init_directories
            setup_ssl
            start_services
            wait_for_services
            show_status
            show_access_info
            ;;
        start)
            cd "${ADMIN_DIR}" && docker-compose up -d
            success "服务已启动"
            ;;
        stop)
            cd "${ADMIN_DIR}" && docker-compose down
            success "服务已停止"
            ;;
        restart)
            cd "${ADMIN_DIR}" && docker-compose restart
            success "服务已重启"
            ;;
        status)
            cd "${ADMIN_DIR}" && docker-compose ps
            ;;
        logs)
            cd "${ADMIN_DIR}" && docker-compose logs -f app
            ;;
    esac
}

# 执行主函数
main "$@"
