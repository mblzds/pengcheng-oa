@echo off
chcp 65001 >nul

echo ==========================================
echo   MasterLife APP 打包环境配置 (Windows)
echo ==========================================
echo.

REM 检查 Node.js
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] Node.js 未安装
    echo 请先安装 Node.js: https://nodejs.org/
    pause
    exit /b 1
)

for /f "tokens=*" %%i in ('node -v') do set NODE_VERSION=%%i
echo [成功] Node.js 版本：%NODE_VERSION%

REM 检查 npm
where npm >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] npm 未安装
    pause
    exit /b 1
)

for /f "tokens=*" %%i in ('npm -v') do set NPM_VERSION=%%i
echo [成功] npm 版本：%NPM_VERSION%
echo.

REM 进入项目目录
cd /d "%~dp0"

REM 安装依赖
echo --- 安装项目依赖 ---
call npm install

if %errorlevel% neq 0 (
    echo [错误] 依赖安装失败
    pause
    exit /b 1
)

echo.
echo [成功] 依赖安装完成
echo.

REM 检查项目文件
echo --- 检查项目结构 ---

set FILES=manifest.json pages.json App.vue main.js package.json
for %%f in (%FILES%) do (
    if exist "%%f" (
        echo [成功] %%f 存在
    ) else (
        echo [错误] %%f 不存在
        pause
        exit /b 1
    )
)

echo.
echo --- 检查目录结构 ---

set DIRS=pages static utils
for %%d in (%DIRS%) do (
    if exist "%%d" (
        echo [成功] %%d 目录存在
    ) else (
        echo [错误] %%d 目录不存在
        pause
        exit /b 1
    )
)

echo.
echo ==========================================
echo   环境配置完成！
echo ==========================================
echo.
echo 下一步操作：
echo 1. 使用 HBuilderX 打开本项目
echo 2. 登录 DCloud 账号（工具 → 登录）
echo 3. 配置 manifest.json 中的证书和包名
echo 4. 菜单：发行 → 原生 App-云打包
echo 5. 等待打包完成后下载安装包
echo.
echo 详细文档：pengcheng-uniapp/APP-BUILD-GUIDE.md
echo.
pause
