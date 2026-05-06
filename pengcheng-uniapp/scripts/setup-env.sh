#!/bin/bash

# MasterLife APP 打包环境快速配置脚本
# 用于自动安装依赖和检查环境

set -e

echo "=========================================="
echo "  MasterLife APP 打包环境配置"
echo "=========================================="
echo ""

# 检查 Node.js
if ! command -v node &> /dev/null; then
    echo "✗ Node.js 未安装"
    echo "请先安装 Node.js: https://nodejs.org/"
    exit 1
fi

echo "✓ Node.js 版本：$(node -v)"

# 检查 npm
if ! command -v npm &> /dev/null; then
    echo "✗ npm 未安装"
    exit 1
fi

echo "✓ npm 版本：$(npm -v)"
echo ""

# 进入项目目录
cd "$(dirname "$0")"

# 安装依赖
echo "--- 安装项目依赖 ---"
npm install

echo ""
echo "✓ 依赖安装完成"
echo ""

# 检查项目文件
echo "--- 检查项目结构 ---"

FILES=("manifest.json" "pages.json" "App.vue" "main.js" "package.json")
for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file 存在"
    else
        echo "✗ $file 不存在"
        exit 1
    fi
done

echo ""
echo "--- 检查目录结构 ---"

DIRS=("pages" "static" "utils")
for dir in "${DIRS[@]}"; do
    if [ -d "$dir" ]; then
        echo "✓ $dir 目录存在"
    else
        echo "✗ $dir 目录不存在"
        exit 1
    fi
done

echo ""
echo "=========================================="
echo "  环境配置完成！"
echo "=========================================="
echo ""
echo "下一步操作："
echo "1. 使用 HBuilderX 打开本项目"
echo "2. 登录 DCloud 账号（工具 → 登录）"
echo "3. 配置 manifest.json 中的证书和包名"
echo "4. 菜单：发行 → 原生 App-云打包"
echo "5. 等待打包完成后下载安装包"
echo ""
echo "详细文档：pengcheng-uniapp/APP-BUILD-GUIDE.md"
echo ""
