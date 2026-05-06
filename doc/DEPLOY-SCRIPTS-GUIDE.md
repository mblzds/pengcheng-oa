# MasterLife 部署脚本使用说明

本文档说明所有部署脚本的使用方法。

---

## 一、脚本清单

| 脚本 | 位置 | 用途 |
|------|------|------|
| `deploy-cloud.sh` | `pengcheng-admin/scripts/` | 云端一键部署 |
| `deploy-miniprogram.sh` | `pengcheng-admin/scripts/` | 小程序自动化部署 |
| `setup-env.sh` | `pengcheng-uniapp/scripts/` | APP 打包环境配置 |
| `check-env.js` | `pengcheng-uniapp/scripts/` | 环境检查工具 |

---

## 二、云端部署脚本

### 2.1 快速部署

```bash
cd pengcheng-admin/scripts
chmod +x deploy-cloud.sh
./deploy-cloud.sh
```

### 2.2 可用命令

```bash
# 一键部署（默认）
./deploy-cloud.sh deploy

# 启动服务
./deploy-cloud.sh start

# 停止服务
./deploy-cloud.sh stop

# 重启服务
./deploy-cloud.sh restart

# 查看状态
./deploy-cloud.sh status

# 查看日志
./deploy-cloud.sh logs

# 显示帮助
./deploy-cloud.sh help
```

### 2.3 部署流程

```
1. 检查 Docker 环境
2. 创建.env 配置文件
3. 初始化目录
4. 配置 SSL 证书
5. 启动 Docker 服务
6. 等待服务就绪
7. 显示访问信息
```

### 2.4 部署后操作

```bash
# 查看服务状态
docker-compose ps

# 查看应用日志
docker-compose logs -f app

# 重启应用
docker-compose restart app

# 进入容器
docker-compose exec app sh
```

---

## 三、小程序部署脚本

### 3.1 编译小程序

```bash
cd pengcheng-admin/scripts
chmod +x deploy-miniprogram.sh
./deploy-miniprogram.sh build
```

### 3.2 编译并上传

```bash
./deploy-miniprogram.sh upload \
    --appid wx_xxxxxxxxxxxxxxxx \
    --version 1.0.0 \
    --desc "v1.0.0 首次发布"
```

### 3.3 参数说明

| 参数 | 说明 | 必填 |
|------|------|------|
| `--appid` | 小程序 AppID | 上传时必填 |
| `--version` | 版本号 | 可选，默认 1.0.0 |
| `--desc` | 版本描述 | 可选 |

### 3.4 输出目录

```
pengcheng-admin/
├── unpackage/
│   └── dist/
│       └── mp-weixin/    # 编译输出
├── backups/
│   └── mp-weixin/        # 版本备份
└── logs/
    ├── deploy-*.log      # 部署日志
    └── deploy-report-*.md # 部署报告
```

---

## 四、APP 打包环境配置

### 4.1 macOS/Linux

```bash
cd pengcheng-admin/pengcheng-uniapp/scripts
chmod +x setup-env.sh
./setup-env.sh
```

### 4.2 Windows

双击运行或使用命令行：

```cmd
cd pengcheng-admin\pengcheng-uniapp\scripts
setup-env.bat
```

### 4.3 环境检查

```bash
cd pengcheng-admin/pengcheng-uniapp
node scripts/check-env.js
```

---

## 五、自动化部署（CI/CD）

### 5.1 GitHub Actions 配置

位置：`.github/workflows/deploy.yml`

```yaml
name: Deploy MasterLife

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Setup Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
    
    - name: Install dependencies
      run: |
        cd pengcheng-admin/pengcheng-uniapp
        npm install
    
    - name: Build Mini Program
      run: |
        cd pengcheng-admin/scripts
        chmod +x deploy-miniprogram.sh
        ./deploy-miniprogram.sh build
    
    - name: Deploy to Server
      uses: appleboy/ssh-action@master
      with:
        host: ${{ secrets.SERVER_HOST }}
        username: ${{ secrets.SERVER_USER }}
        key: ${{ secrets.SSH_PRIVATE_KEY }}
        script: |
          cd /path/to/pengcheng-admin
          ./scripts/deploy-cloud.sh deploy
```

### 5.2 GitLab CI 配置

位置：`.gitlab-ci.yml`

```yaml
stages:
  - build
  - deploy

build_miniapp:
  stage: build
  image: node:18
  script:
    - cd pengcheng-admin/pengcheng-uniapp
    - npm install
    - npm run build:mp-weixin
  artifacts:
    paths:
      - pengcheng-admin/pengcheng-uniapp/unpackage/dist/mp-weixin/

deploy_prod:
  stage: deploy
  image: alpine:latest
  script:
    - apk add --no-cache openssh-client
    - cd pengcheng-admin/scripts
    - chmod +x deploy-cloud.sh
    - ./deploy-cloud.sh deploy
  only:
    - main
```

---

## 六、手动部署流程

### 6.1 云端手动部署

```bash
# 1. 进入项目目录
cd pengcheng-admin

# 2. 创建.env 文件
cp .env.example .env
# 编辑.env 文件，修改密码和密钥

# 3. 启动服务
docker-compose up -d

# 4. 查看日志
docker-compose logs -f

# 5. 验证部署
curl http://localhost:8080/actuator/health
```

### 6.2 小程序手动部署

```bash
# 1. 使用 HBuilderX 打开项目
# 2. 菜单：发行 → 微信小程序
# 3. 等待编译完成
# 4. 打开微信开发者工具
# 5. 导入 unpackage/dist/mp-weixin/
# 6. 点击"上传"
```

---

## 七、部署验证

### 7.1 后端验证

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# API 测试
curl http://localhost:8080/api/auth/info

# WebSocket 测试
# 使用 wscat 或 Postman 测试 ws://localhost:8080/ws
```

### 7.2 前端验证

```bash
# 访问管理后台
open http://localhost:80

# 测试登录
# 账号：admin
# 密码：admin123
```

### 7.3 小程序验证

1. 微信开发者工具导入项目
2. 编译运行
3. 测试登录功能
4. 测试核心功能

---

## 八、故障排查

### 8.1 常见问题

**Q: Docker 服务启动失败**

```bash
# 查看 Docker 日志
docker-compose logs

# 检查端口占用
netstat -tulpn | grep 8080

# 重启 Docker
sudo systemctl restart docker
```

**Q: 小程序无法连接服务器**

```bash
# 检查服务器域名配置
# 登录小程序后台：开发 → 开发管理 → 开发设置

# 检查 SSL 证书
openssl s_client -connect your-domain.com:443

# 检查防火墙
sudo ufw status
```

**Q: 数据库连接失败**

```bash
# 检查 MySQL 容器
docker-compose ps mysql

# 查看 MySQL 日志
docker-compose logs mysql

# 进入 MySQL 容器
docker-compose exec mysql mysql -u root -p
```

### 8.2 日志位置

```bash
# 应用日志
docker-compose logs -f app

# Nginx 日志
docker-compose logs -f nginx

# MySQL 日志
docker-compose logs -f mysql

# Redis 日志
docker-compose logs -f redis

# 系统日志
journalctl -u docker -f
```

---

## 九、性能优化

### 9.1 Docker 优化

```yaml
# docker-compose.yml 中添加
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

### 9.2 Nginx 优化

```nginx
# 启用 Gzip 压缩
gzip on;
gzip_types text/plain text/css application/json application/javascript;
gzip_min_length 1000;

# 启用缓存
location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
    expires 30d;
    add_header Cache-Control "public, immutable";
}
```

### 9.3 数据库优化

```sql
-- 优化 MySQL 配置
SET GLOBAL max_connections = 500;
SET GLOBAL innodb_buffer_pool_size = 1G;
SET GLOBAL query_cache_size = 64M;
```

---

## 十、安全加固

### 10.1 修改默认密码

```bash
# 编辑.env 文件
vi pengcheng-admin/.env

# 修改以下密码：
# DB_PASSWORD
# DB_ROOT_PASSWORD
# REDIS_PASSWORD
# PG_PASSWORD
```

### 10.2 防火墙配置

```bash
# 只开放必要端口
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
```

### 10.3 SSL 配置

```bash
# 申请 Let's Encrypt 证书
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

---

## 相关文档

- [云端部署指南](./DEPLOYMENT.md)
- [小程序部署指南](./MINIPROGRAM-DEPLOY.md)
- [配置模板](./DEPLOY-CONFIG-TEMPLATES.md)
- [运维手册](./USER-MANUAL.md)

---

**文档版本**: V1.0  
**最后更新**: 2026-03-12
