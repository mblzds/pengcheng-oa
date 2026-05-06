#!/bin/bash

# ============================================================
# MasterLife 小程序自动化部署脚本
# 功能：编译、上传、备份小程序代码
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置变量
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
UNIPROJECT_DIR="${PROJECT_ROOT}/pengcheng-uniapp"
DIST_DIR="${UNIPROJECT_DIR}/unpackage/dist/mp-weixin"
BACKUP_DIR="${PROJECT_ROOT}/backups/mp-weixin"
LOG_FILE="${PROJECT_ROOT}/logs/deploy-$(date +%Y%m%d-%H%M%S).log"

# 日志函数
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${timestamp} [${level}] ${message}" | tee -a "${LOG_FILE}"
}

info() { log "${BLUE}INFO${NC}" "$@"; }
success() { log "${GREEN}SUCCESS${NC}" "$@"; }
warn() { log "${YELLOW}WARN${NC}" "$@"; }
error() { log "${RED}ERROR${NC}" "$@"; }

# 检查依赖
check_dependencies() {
    info "检查依赖..."
    
    if ! command -v node &> /dev/null; then
        error "Node.js 未安装，请先安装 Node.js"
        exit 1
    fi
    
    if ! command -v npm &> /dev/null; then
        error "npm 未安装，请先安装 npm"
        exit 1
    fi
    
    if ! command -v hx &> /dev/null; then
        warn "HBuilderX CLI 未安装，将使用微信开发者工具 CLI"
    fi
    
    success "依赖检查通过"
}

# 创建必要目录
create_directories() {
    info "创建必要目录..."
    mkdir -p "${BACKUP_DIR}"
    mkdir -p "${PROJECT_ROOT}/logs"
    success "目录创建完成"
}

# 编译小程序
build_miniprogram() {
    info "开始编译小程序..."
    
    cd "${UNIPROJECT_DIR}"
    
    # 检查 package.json 是否存在
    if [ ! -f "package.json" ]; then
        error "package.json 不存在"
        exit 1
    fi
    
    # 安装依赖
    info "安装依赖..."
    npm install --production
    
    # 使用 HBuilderX CLI 编译（如果已安装）
    if command -v hx &> /dev/null; then
        info "使用 HBuilderX CLI 编译..."
        hx compile --mp-weixin
    else
        # 使用 uni-app 命令行工具
        info "使用 uni-app CLI 编译..."
        if [ -f "node_modules/.bin/uni" ]; then
            ./node_modules/.bin/uni build -p mp-weixin
        else
            # 尝试使用 npm scripts
            if npm run | grep -q "build:mp-weixin"; then
                npm run build:mp-weixin
            else
                warn "未找到编译命令，请手动使用 HBuilderX 编译"
                info "编译输出目录：${DIST_DIR}"
                return 0
            fi
        fi
    fi
    
    # 检查编译结果
    if [ -d "${DIST_DIR}" ]; then
        success "小程序编译完成"
        ls -la "${DIST_DIR}"
    else
        error "编译失败，输出目录不存在"
        exit 1
    fi
}

# 备份旧版本
backup_version() {
    local version=$1
    local backup_path="${BACKUP_DIR}/v${version}-$(date +%Y%m%d-%H%M%S)"
    
    info "备份当前版本到：${backup_path}"
    
    if [ -d "${DIST_DIR}" ]; then
        cp -r "${DIST_DIR}" "${backup_path}"
        success "版本备份完成"
    else
        warn "无旧版本可备份"
    fi
}

# 上传到微信后台（需要微信开发者工具 CLI）
upload_to_weixin() {
    local appid=$1
    local version=$2
    local desc=$3
    
    info "准备上传到微信小程序后台..."
    
    # 检查微信开发者工具 CLI
    if ! command -v wechat-devtools &> /dev/null; then
        warn "微信开发者工具 CLI 未安装"
        info "请手动上传："
        info "1. 打开微信开发者工具"
        info "2. 导入项目：${DIST_DIR}"
        info "3. 点击'上传'按钮"
        info "4. 版本号：${version}"
        info "5. 版本描述：${desc}"
        return 0
    fi
    
    # 使用 CLI 上传
    info "使用微信开发者工具 CLI 上传..."
    wechat-devtools upload \
        --upload-path "${DIST_DIR}" \
        --appid "${appid}" \
        --version "${version}" \
        --desc "${desc}"
    
    success "上传完成"
}

# 设置体验版
set_experience_version() {
    local version=$1
    
    info "设置版本 ${version} 为体验版..."
    info "请手动操作："
    info "1. 登录 https://mp.weixin.qq.com"
    info "2. 进入'版本管理'"
    info "3. 找到版本 ${version}"
    info "4. 点击'选为体验版'"
}

# 生成部署报告
generate_report() {
    local version=$1
    local report_file="${PROJECT_ROOT}/logs/deploy-report-${version}.md"
    
    cat > "${report_file}" << EOF
# 小程序部署报告

## 基本信息

- **版本号**: ${version}
- **部署时间**: $(date '+%Y-%m-%d %H:%M:%S')
- **部署人员**: $(whoami)

## 编译信息

- **编译输出**: ${DIST_DIR}
- **编译大小**: $(du -sh "${DIST_DIR}" 2>/dev/null | cut -f1)

## 文件清单

$(find "${DIST_DIR}" -type f | head -20)

## 下一步操作

1. [ ] 验证小程序功能
2. [ ] 设置体验版
3. [ ] 添加体验成员
4. [ ] 提交审核

## 备注

${2:-无}

EOF
    
    success "部署报告已生成：${report_file}"
}

# 主函数
main() {
    echo "============================================"
    echo "  MasterLife 小程序自动化部署脚本"
    echo "============================================"
    echo ""
    
    # 解析参数
    local action="build"
    local appid=""
    local version="1.0.0"
    local desc="自动部署"
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            build)
                action="build"
                shift
                ;;
            upload)
                action="upload"
                shift
                ;;
            help)
                echo "用法：$0 [action] [options]"
                echo ""
                echo "Actions:"
                echo "  build     编译小程序（默认）"
                echo "  upload    编译并上传"
                echo "  help      显示帮助"
                echo ""
                echo "Options:"
                echo "  --appid     小程序 AppID"
                echo "  --version   版本号"
                echo "  --desc      版本描述"
                exit 0
                ;;
            --appid)
                appid="$2"
                shift 2
                ;;
            --version)
                version="$2"
                shift 2
                ;;
            --desc)
                desc="$2"
                shift 2
                ;;
            *)
                error "未知参数：$1"
                exit 1
                ;;
        esac
    done
    
    # 执行操作
    create_directories
    check_dependencies
    
    case $action in
        build)
            backup_version "${version}"
            build_miniprogram
            generate_report "${version}" "${desc}"
            ;;
        upload)
            if [ -z "${appid}" ]; then
                error "上传需要指定 --appid"
                exit 1
            fi
            backup_version "${version}"
            build_miniprogram
            upload_to_weixin "${appid}" "${version}" "${desc}"
            generate_report "${version}" "${desc}"
            ;;
    esac
    
    echo ""
    echo "============================================"
    success "部署完成！"
    echo "============================================"
}

# 执行主函数
main "$@"
