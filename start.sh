#!/bin/bash
# ============================================================
# MasterLife V3.0 快速启动脚本
# ============================================================

set -e

echo "=========================================="
echo "  MasterLife V3.0 快速启动"
echo "=========================================="
echo ""

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "❌ 错误：Docker 未安装"
    echo "请先安装 Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# 检查 Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "❌ 错误：Docker Compose 未安装"
    echo "请先安装 Docker Compose"
    exit 1
fi

# 检查 .env 文件
if [ ! -f .env ]; then
    echo "⚠️  未找到 .env 文件，正在创建..."
    cp .env.example .env
    echo ""
    echo "⚠️  请编辑 .env 文件配置以下必要参数："
    echo "   - DB_PASSWORD (数据库密码)"
    echo "   - REDIS_PASSWORD (Redis 密码)"
    echo "   - DASHSCOPE_API_KEY (通义千问 API 密钥，可选)"
    echo ""
    read -p "按回车键继续..."
fi

echo "✅ 环境检查通过"
echo ""

# 启动服务
echo "🚀 正在启动服务..."
docker-compose up -d

echo ""
echo "⏳ 等待服务启动..."
sleep 30

echo ""
echo "=========================================="
echo "  服务启动完成！"
echo "=========================================="
echo ""
echo "📌 访问地址："
echo "   前端：http://localhost"
echo "   API 文档：http://localhost:8080/api/doc.html"
echo "   MinIO 控制台：http://localhost:9001"
echo ""
echo "📌 默认账号："
echo "   管理员：admin / admin123"
echo ""
echo "📌 查看日志："
echo "   docker-compose logs -f app"
echo ""
echo "📌 停止服务："
echo "   docker-compose down"
echo ""
