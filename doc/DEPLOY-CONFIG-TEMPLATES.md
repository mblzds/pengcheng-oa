# MasterLife 部署配置模板

本文档提供所有部署相关的配置模板。

---

## 一、环境变量模板 (.env)

位置：`pengcheng-admin/.env`

```bash
# ============================================================
# MasterLife 环境变量配置
# ============================================================

# -------------------- 数据库配置 --------------------
DB_HOST=mysql
DB_PORT=3306
DB_NAME=pengcheng-system
DB_USERNAME=pengcheng_app
DB_PASSWORD=你的数据库密码
DB_ROOT_PASSWORD=你的 root 密码

# -------------------- Redis 配置 --------------------
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=你的 Redis 密码
REDIS_DB=10

# -------------------- PostgreSQL 配置 --------------------
PG_HOST=postgres
PG_PORT=5432
PG_DB=pengcheng_vector
PG_USERNAME=postgres
PG_PASSWORD=你的 PG 密码

# -------------------- MinIO 配置 --------------------
MINIO_ENDPOINT=minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# -------------------- AI 配置 --------------------
# 阿里云 DashScope（通义千问）
DASHSCOPE_API_KEY=sk-你的密钥

# 智谱 AI（GLM）
ZHIPU_API_KEY=你的智谱密钥

# -------------------- SSL 配置 --------------------
SSL_KEYSTORE_PASSWORD=你的 SSL 密码

# -------------------- 微信小程序配置 --------------------
WECHAT_MINIPROGRAM_APPID=wx_你的小程序 AppID
WECHAT_MINIPROGRAM_SECRET=你的小程序 AppSecret

# -------------------- 微信支付配置 --------------------
WECHAT_PAY_MCHID=你的商户号
WECHAT_PAY_APIV3_KEY=你的 APIv3 密钥
WECHAT_PAY_CERT_SERIAL_NO=你的证书序列号
```

---

## 二、Nginx 配置模板

位置：`pengcheng-admin/nginx.conf`

```nginx
# MasterLife Nginx 配置

upstream app_backend {
    server app:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    
    # 强制 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com www.your-domain.com;
    
    # SSL 证书配置
    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    
    # 日志
    access_log /var/log/nginx/access.log;
    error_log /var/log/nginx/error.log;
    
    # 静态文件（前端）
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
        
        # Gzip 压缩
        gzip on;
        gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
        gzip_min_length 1000;
    }
    
    # API 接口代理
    location /api {
        proxy_pass http://app_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # WebSocket 直接代理
    location /ws {
        proxy_pass http://app_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        
        # 长连接超时
        proxy_connect_timeout 60s;
        proxy_send_timeout 3600s;
        proxy_read_timeout 3600s;
    }
    
    # 文件上传大小限制
    client_max_body_size 100M;
}
```

---

## 三、小程序配置模板

### 3.1 manifest.json

位置：`pengcheng-admin/pengcheng-uniapp/manifest.json`

```json
{
    "name": "MasterLife",
    "appid": "__UNI__.xxxxxxxx",
    "description": "MasterLife - 高效沟通，智慧办公",
    "versionName": "1.0.0",
    "versionCode": "100",
    "transformPx": false,
    "mp-weixin": {
        "appid": "wx_xxxxxxxxxxxxxxxx",
        "setting": {
            "urlCheck": false,
            "es6": true,
            "postcss": true,
            "minified": true
        },
        "usingComponents": true,
        "permission": {
            "scope.userLocation": {
                "desc": "你的位置信息将用于考勤打卡"
            }
        },
        "requiredPrivateInfos": ["chooseLocation", "getLocation"]
    },
    "app-plus": {
        "usingComponents": true,
        "nvueStyleCompiler": "uni-app",
        "compilerVersion": 3,
        "splashscreen": {
            "alwaysShowBeforeRender": true,
            "waiting": true,
            "autoclose": true,
            "delay": 0
        },
        "distribute": {
            "android": {
                "permissions": [
                    "<uses-permission android:name=\"android.permission.INTERNET\"/>",
                    "<uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\"/>",
                    "<uses-permission android:name=\"android.permission.ACCESS_WIFI_STATE\"/>",
                    "<uses-permission android:name=\"android.permission.CAMERA\"/>",
                    "<uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\"/>",
                    "<uses-permission android:name=\"android.permission.ACCESS_COARSE_LOCATION\"/>"
                ],
                "package": "com.pengcheng.masterlife",
                "minSdkVersion": 21,
                "targetSdkVersion": 30
            },
            "ios": {
                "bundleid": "com.pengcheng.masterlife",
                "appstore": true
            }
        }
    }
}
```

### 3.2 utils/config.js

位置：`pengcheng-admin/pengcheng-uniapp/utils/config.js`

```javascript
/**
 * 运行环境配置
 */

const API_BASE_URL_KEY = 'api_base_url'

// 生产环境配置
const DEFAULT_API_BASE_URL = 'https://your-domain.com/api'
const DEFAULT_WS_URL = 'wss://your-domain.com/ws'

// 开发环境配置（本地调试使用）
// const DEFAULT_API_BASE_URL = 'http://localhost:8080/api'
// const DEFAULT_WS_URL = 'ws://localhost:8080/ws'

const normalizeBaseUrl = (value) => {
    if (!value || typeof value !== 'string') return ''
    const trimmed = value.trim()
    if (!trimmed) return ''
    return trimmed.replace(/\/+$/, '')
}

export const getApiBaseUrl = () => {
    const custom = normalizeBaseUrl(uni.getStorageSync(API_BASE_URL_KEY))
    return custom || DEFAULT_API_BASE_URL
}

export const setApiBaseUrl = (value) => {
    const normalized = normalizeBaseUrl(value)
    if (!normalized) return false
    uni.setStorageSync(API_BASE_URL_KEY, normalized)
    return true
}

export const resetApiBaseUrl = () => {
    uni.removeStorageSync(API_BASE_URL_KEY)
}

export const getWsUrl = () => {
    return DEFAULT_WS_URL
}

export const joinBaseUrl = (path = '') => {
    if (!path) return getApiBaseUrl()
    if (/^https?:\/\//.test(path)) return path
    const prefix = path.startsWith('/') ? '' : '/'
    return `${getApiBaseUrl()}${prefix}${path}`
}
```

---

## 四、系统配置模板（后台管理）

### 4.1 微信小程序配置

登录后台管理，进入 **系统配置 → 微信小程序**:

```json
{
  "enabled": true,
  "appId": "wx_xxxxxxxxxxxxxxxx",
  "appSecret": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
}
```

### 4.2 微信支付配置

登录后台管理，进入 **系统配置 → 支付配置 → 微信支付**:

```json
{
  "enabled": true,
  "mchId": "1234567890",
  "appId": "wx_xxxxxxxxxxxxxxxx",
  "apiV3Key": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "privateKey": "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC...\n-----END PRIVATE KEY-----",
  "certSerialNo": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "notifyUrl": "https://your-domain.com/api/pay/wechat/notify"
}
```

### 4.3 文件存储配置

登录后台管理，进入 **系统配置 → 存储配置**:

**本地存储**:
```json
{
  "provider": "local",
  "localPath": "/app/uploads",
  "maxSize": 100,
  "domain": "https://your-domain.com"
}
```

**MinIO 存储**:
```json
{
  "provider": "minio",
  "minioEndpoint": "http://minio:9000",
  "minioAccessKey": "minioadmin",
  "minioSecretKey": "minioadmin",
  "minioBucket": "masterlife",
  "domain": "https://your-domain.com"
}
```

---

## 五、SSL 证书配置

### 5.1 申请免费 SSL 证书（Let's Encrypt）

```bash
# 安装 Certbot
sudo apt-get install certbot python3-certbot-nginx

# 申请证书
sudo certbot --nginx -d your-domain.com -d www.your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

### 5.2 转换证书格式（用于 Java）

```bash
# PEM 转 PKCS12
openssl pkcs12 -export \
    -in fullchain.pem \
    -inkey privkey.pem \
    -out keystore.p12 \
    -name pengcheng-admin \
    -CAfile fullchain.pem \
    -caname root

# 设置密码：changeit
```

---

## 六、数据库初始化

### 6.1 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS `pengcheng-system` 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `pengcheng_vector` 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 6.2 导入数据

```bash
# 方式一：使用 Docker
docker-compose exec mysql mysql -u root -p pengcheng-system < sql/pengcheng-system.sql

# 方式二：本地导入
mysql -u root -p pengcheng-system < sql/pengcheng-system.sql
```

---

## 七、防火墙配置

### 7.1 Ubuntu/Debian

```bash
# 开放必要端口
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS

# 启用防火墙
sudo ufw enable

# 查看状态
sudo ufw status
```

### 7.2 CentOS/RHEL

```bash
# 开放端口
sudo firewall-cmd --permanent --add-port=22/tcp
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp

# 重载配置
sudo firewall-cmd --reload

# 查看状态
sudo firewall-cmd --list-all
```

---

## 八、系统服务配置（Systemd）

### 8.1 创建服务文件

位置：`/etc/systemd/system/masterlife.service`

```ini
[Unit]
Description=MasterLife Application
Requires=docker.service
After=docker.service

[Service]
Restart=always
WorkingDirectory=/path/to/pengcheng-admin
ExecStart=/usr/bin/docker-compose up -d
ExecStop=/usr/bin/docker-compose down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
```

### 8.2 启用服务

```bash
# 重载 systemd
sudo systemctl daemon-reload

# 启用服务
sudo systemctl enable masterlife

# 启动服务
sudo systemctl start masterlife

# 查看状态
sudo systemctl status masterlife

# 查看日志
sudo journalctl -u masterlife -f
```

---

## 九、备份配置

### 9.1 数据库备份脚本

位置：`pengcheng-admin/scripts/backup-db.sh`

```bash
#!/bin/bash

DATE=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="/backups/database"
MYSQL_CONTAINER="masterlife-mysql"

mkdir -p ${BACKUP_DIR}

# 备份 MySQL
docker exec ${MYSQL_CONTAINER} mysqldump \
    -u root -p${DB_ROOT_PASSWORD} \
    pengcheng-system > ${BACKUP_DIR}/pengcheng-system-${DATE}.sql

# 压缩备份
cd ${BACKUP_DIR}
tar -czf pengcheng-system-${DATE}.tar.gz pengcheng-system-${DATE}.sql
rm pengcheng-system-${DATE}.sql

# 删除 7 天前的备份
find ${BACKUP_DIR} -name "*.tar.gz" -mtime +7 -delete

echo "备份完成：${BACKUP_DIR}/pengcheng-system-${DATE}.tar.gz"
```

### 9.2 定时备份（Cron）

```bash
# 编辑 crontab
crontab -e

# 添加每天凌晨 2 点备份
0 2 * * * /path/to/pengcheng-admin/scripts/backup-db.sh
```

---

## 十、监控告警配置

### 10.1 健康检查端点

```bash
# 应用健康检查
curl http://localhost:8080/actuator/health

# MySQL 健康检查
docker exec masterlife-mysql mysqladmin ping -h localhost

# Redis 健康检查
docker exec masterlife-redis redis-cli ping
```

### 10.2 监控脚本

位置：`pengcheng-admin/scripts/health-check.sh`

```bash
#!/bin/bash

# 检查应用
if ! curl -s http://localhost:8080/actuator/health | grep -q UP; then
    echo "应用异常！"
    # 发送告警通知
fi

# 检查 MySQL
if ! docker exec masterlife-mysql mysqladmin ping -h localhost --silent; then
    echo "MySQL 异常！"
fi

# 检查 Redis
if ! docker exec masterlife-redis redis-cli ping | grep -q PONG; then
    echo "Redis 异常！"
fi
```

---

## 相关文档

- [云端部署指南](./DEPLOYMENT.md)
- [小程序部署指南](./MINIPROGRAM-DEPLOY.md)
- [微信登录配置](./WECHAT-LOGIN-PAY-CONFIG.md)
- [运维手册](./USER-MANUAL.md)

---

**文档版本**: V1.0  
**最后更新**: 2026-03-12
