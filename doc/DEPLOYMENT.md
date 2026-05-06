# 鹏程地产系统 — 部署运维手册

## 1. 系统要求

| 组件 | 最低要求 | 推荐配置 |
|------|----------|----------|
| CPU | 2 核 | 4 核+ |
| 内存 | 4 GB | 8 GB+ |
| 磁盘 | 40 GB | 100 GB SSD |
| OS | CentOS 7+ / Ubuntu 20.04+ | Ubuntu 22.04 LTS |
| Docker | 20.10+ | 24.0+ |
| Docker Compose | 2.0+ | 2.20+ |

## 2. 快速部署（Docker Compose）

### 2.1 准备环境变量

```bash
cp .env.example .env
vim .env
```

必须配置的关键变量：

```env
# 数据库
DB_PASSWORD=<强密码>
DB_ROOT_PASSWORD=<root强密码>

# Redis
REDIS_PASSWORD=<Redis密码>

# AI 服务
DASHSCOPE_API_KEY=<阿里云DashScope API Key>
ZHIPU_API_KEY=<智谱AI API Key>

# JWT
SA_TOKEN_SECRET=<随机字符串>

# 可选：OnlyOffice 在线编辑
ONLYOFFICE_JWT_SECRET=<与 Document Server 一致>

# 文件存储
UPLOAD_PATH=/data/uploads
```

### 2.2 启动服务

```bash
# 首次启动（构建镜像 + 初始化数据库）
docker compose up -d --build

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f app
```

### 2.3 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Nginx | 80/443 | 反向代理入口 |
| App | 8080 | Spring Boot 应用 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存/会话 |
| PostgreSQL | 5432 | PGVector 向量库 |
| kkFileView | 8012 | 文件预览 |
| OnlyOffice | 8443 | 可选，docx/xlsx/pptx 在线编辑 |

### 2.4 可选：OnlyOffice 在线编辑

启用 OnlyOffice 后，可在系统内对 docx/xlsx/pptx 进行在线编辑。需在 `.env` 中配置：

```env
ONLYOFFICE_JWT_SECRET=与 Document Server 一致的 JWT 密钥
```

应用配置（如 `application-prod.yml`）中可设置：

```yaml
onlyoffice:
  server-url: https://你的域名/onlyoffice/
  jwt-secret: ${ONLYOFFICE_JWT_SECRET}
```

Nginx 需将 `/onlyoffice/` 反向代理到 OnlyOffice 容器 443 端口；WOPI 回调 `/wopi/` 代理到应用 8080。详见项目根目录 `nginx.conf`。

### 2.5 生产环境数据库账号

生产环境应使用专用数据库账号（非 root），按最小权限授权。详见 [PRODUCTION-DB-USER.md](./PRODUCTION-DB-USER.md)。

### 2.6 初始账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 超级管理员 |

> 首次登录后请立即修改密码。

## 3. 手动部署

### 3.1 后端

```bash
cd pengcheng-admin

# 编译
mvn clean package -DskipTests -P prod

# 启动
java -jar pengcheng-starter/target/pengcheng-admin.jar \
  --spring.profiles.active=prod \
  -Xms512m -Xmx2g \
  -XX:+UseG1GC
```

### 3.2 前端

```bash
cd pengcheng-admin/pengcheng-ui

# 安装依赖
npm ci

# 构建
npm run build

# 产物位于 dist/ 目录，部署到 Nginx static 目录
cp -r dist/* /usr/share/nginx/html/
```

### 3.3 Nginx 配置要点

```nginx
# API 反向代理
location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}

# WebSocket
location /ws/ {
    proxy_pass http://127.0.0.1:8080/ws/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}

# 前端 SPA
location / {
    try_files $uri $uri/ /index.html;
}
```

## 4. 数据库运维

### 4.1 备份

```bash
# MySQL 全量备份
mysqldump -u root -p pengcheng_admin > backup_$(date +%Y%m%d).sql

# 定时备份 (crontab)
0 2 * * * mysqldump -u root -p${DB_PASSWORD} pengcheng_admin | gzip > /backup/mysql_$(date +\%Y\%m\%d).sql.gz
```

### 4.2 Flyway 迁移

数据库迁移由 Flyway 自动管理。启动时自动执行：

```
db/migration/V10__chat_enhance.sql
db/migration/V11__smart_table.sql
db/migration/V12__document_space.sql
...
db/migration/V19__daily_report_and_quality.sql
...
db/migration/V26__smart_table_menu_under_file.sql
db/migration/V27__contacts_menu_under_message.sql
db/migration/V28__role_menu_new_menus.sql
```

**菜单/导航变更**：新增或调整菜单时须同步更新迁移与全量 SQL，详见 `doc/MENU-CHANGE-PROCEDURE.md`。

### 4.3 性能优化

```sql
-- 检查慢查询
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;

-- 查看索引使用情况
SHOW INDEX FROM realty_customer;
```

## 5. 监控与告警

### 5.1 健康检查

```bash
# 应用健康检查
curl http://localhost:8080/actuator/health

# Redis 连通性
redis-cli -a ${REDIS_PASSWORD} ping

# MySQL 连通性
mysqladmin -u root -p ping
```

### 5.2 日志管理

```bash
# 应用日志
tail -f logs/pengcheng-admin.log

# 按级别过滤
grep "ERROR" logs/pengcheng-admin.log

# Docker 日志
docker compose logs --tail=100 app
```

## 6. 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 启动失败 "Connection refused" | MySQL 未就绪 | 等待 MySQL 健康检查通过后重启 app |
| 文件上传失败 | UPLOAD_PATH 权限 | `chmod 755 /data/uploads` |
| WebSocket 连接失败 | Nginx 未配置 upgrade | 添加 WebSocket proxy 配置 |
| AI 功能不可用 | API Key 未配置 | 检查 .env 中 DASHSCOPE_API_KEY |
| 文件预览失败 | kkFileView 未启动 | `docker compose restart kkfileview` |

## 7. 版本升级

```bash
# 拉取最新代码
git pull origin main

# 重新构建
docker compose up -d --build app

# Flyway 自动执行新迁移脚本
# 检查迁移状态
docker compose exec app java -jar app.jar flyway info
```
