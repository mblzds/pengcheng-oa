# 生产环境数据库账号创建说明

生产环境应使用**专用应用账号**连接 MySQL，禁止使用 root。按最小权限原则授权。

## 1. 创建应用账号

在 MySQL 中执行（请替换 `pengcheng_app`、`your_secure_password` 为实际值）：

```sql
-- 创建应用用户（与 application-prod.yml 中 spring.datasource.username 一致）
CREATE USER IF NOT EXISTS 'pengcheng_app'@'%' IDENTIFIED BY 'your_secure_password';

-- 仅授予业务库的 DML + DDL（Flyway 迁移需要）
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE VIEW, SHOW VIEW, REFERENCES
ON `pengcheng-system`.* TO 'pengcheng_app'@'%';

-- 不授予：FILE、SUPER、REPLICATION、GRANT OPTION、*.* 等
FLUSH PRIVILEGES;
```

## 2. 权限说明

| 权限 | 用途 |
|------|------|
| SELECT, INSERT, UPDATE, DELETE | 业务 CRUD |
| CREATE, DROP, INDEX, ALTER | Flyway 迁移脚本建表/改表 |
| CREATE VIEW, SHOW VIEW | 视图（若迁移脚本含视图） |
| REFERENCES | 外键（若使用） |

未授予：`ALL PRIVILEGES`、`*.*`、`FILE`、`SUPER`、`REPLICATION SLAVE` 等，降低风险。

## 3. 配置应用

在 `.env` 或 `application-prod.yml` 中配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:pengcheng-system}?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME}   # 填 pengcheng_app
    password: ${DB_PASSWORD}   # 填上面设置的密码
```

`.env` 示例：

```env
DB_USERNAME=pengcheng_app
DB_PASSWORD=your_secure_password
```

## 4. 验证

```bash
# 使用应用账号登录
mysql -u pengcheng_app -p -h <host> pengcheng-system

# 应能查表
SHOW TABLES;

# 应无权限访问其他库
USE mysql;
# 应报错：Access denied
```

完成以上步骤后，可将任务「生产环境创建专用数据库账号（非 root），设置最小权限」视为已完成。
