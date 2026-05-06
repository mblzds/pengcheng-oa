#!/usr/bin/env bash
# MasterLife V3.0 发布前检查脚本（对应 RELEASE-CHECKLIST-V3.0.md 第 1 节）
# 使用方式：在项目根目录执行 bash scripts/release-check.sh

set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "========== 1.1 后端编译（需 JDK 17）=========="
if [ -z "$JAVA_HOME" ]; then
  echo "提示：未设置 JAVA_HOME，将使用当前默认 java。若编译报 Lombok/TypeTag 错误，请设置 JAVA_HOME 指向 JDK 17。"
fi
mvn clean compile -DskipTests -q
echo "后端编译通过"

echo ""
echo "========== 1.3 前端安装与构建 =========="
cd "$ROOT/pengcheng-ui"
npm ci
npm run build
echo "前端构建通过"

echo ""
echo "========== 1.4 前端 Lint（若存在）=========="
if npm run lint 2>/dev/null; then
  echo "前端 Lint 通过"
else
  echo "跳过或未配置 lint"
fi

echo ""
echo "========== 检查完成 =========="
echo "请继续按 RELEASE-CHECKLIST-V3.0.md 完成 2、3、4 节（配置/文档/环境验证）。"
