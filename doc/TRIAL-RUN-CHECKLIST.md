# 试运行前检查清单

**更新日期**: 2026-03-02  
**结论**: 代码与配置已就绪；**需在本地使用 JDK 17 完成编译**后，再启动应用。MySQL、Redis 需提前就绪。

---

## 一、开发与测试情况概览

### 1.1 项目结构

| 项目 | 状态 |
|------|------|
| 启动模块 | `pengcheng-starter`，主类 `com.pengcheng.PengchengApplication` |
| 配置 | `application.yml` 激活 `dev`；`application-dev.yml` 使用环境变量 `${DB_HOST}` 等，支持 `.env` 或系统变量 |
| 模块 | common / infra / core / api / job / starter，依赖关系完整 |

### 1.2 单元测试与集成测试

| 类型 | 数量/范围 | 说明 |
|------|-----------|------|
| 单元测试 | 25+ 测试类，90+ 用例 | 覆盖 AI 编排、实验、权限、路由、工具、业务服务（Todo/Doc/SceneTemplate/Heartbeat/Channel/Calendar/GlobalSearch/Automation/SalesVisit/PmProject/PmTask/AiLlm/Chat/MCP 等） |
| 测试分布 | pengcheng-ai、pengcheng-admin-api | 见 `doc/TEST-AND-REFERENCE.md`、`TASKS-V3.0.md` 4.1 |
| 已移除的测试 | `AgentscopeRuntimeRouteAgentClientTest`、`CompositeRouteAgentClientTest` | 随 AgentScope Runtime 路由移除而删除，避免编译失败 |

### 1.3 已修复的启动问题

| 问题 | 处理 |
|------|------|
| **KpiTemplateServiceImpl Bean 冲突** | 两处实现（`pengcheng-system` 与 `pengcheng-hr`）同名为 `kpiTemplateServiceImpl`，已分别改为 `@Service("systemKpiTemplateService")` 与 `@Service("performanceKpiTemplateService")`，避免冲突。 |
| **CompositeRouteAgentClientTest** | 已随 AgentScope Runtime 移除删除，避免编译/测试报错。 |

### 1.4 编译与 JDK 兼容性

| 项目 | 说明 |
|------|------|
| 当前现象 | 在 **JDK 21 或 24** 下执行 `mvn compile` 可能报 `com.sun.tools.javac.code.TypeTag :: UNKNOWN`（Lombok 与高版本 JDK 不兼容） |
| 建议 | **使用 JDK 17** 进行编译与运行（与 `java.version` 一致） |
| 操作 | 设置 `JAVA_HOME` 指向 JDK 17，再执行 Maven 与启动命令 |

---

## 二、试运行前置条件

### 2.1 必需服务

| 服务 | 用途 | 默认连接 |
|------|------|----------|
| **MySQL** | 业务库、Flyway 迁移 | `localhost:3306`，库名 `pengcheng-system`，账号由 `DB_USERNAME`/`DB_PASSWORD` 指定 |
| **Redis** | 会话、缓存、AI 记忆、Skill 禁用列表等 | `localhost:6379`，database 10 |

### 2.2 可选服务（不影响启动）

| 服务 | 用途 |
|------|------|
| PostgreSQL (PGVector) | RAG 向量库；未配置时使用内存 fallback |
| MinIO | 文件存储；可按需配置 |
| kkFileView | 文件预览；不配则预览能力受限 |
| DashScope API Key | AI 对话/路由；不配时部分 AI 能力降级或不可用 |

### 2.3 环境变量 / 配置

- 复制 `.env.example` 为 `.env`（或直接导出环境变量），至少配置：
  - `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`
  - `REDIS_HOST`、`REDIS_PORT`（有密码时 `REDIS_PASSWORD`）
- 开发环境若使用默认值，可只改 `DB_PASSWORD`、`REDIS_PASSWORD` 等敏感项。
- `.env` 需由启动方式加载（如 IDE 或 `env $(cat .env | xargs)` 等），Spring Boot 不会自动读 `.env`，需通过系统环境变量或 `application-dev.yml` 占位符传入。

---

## 三、试运行步骤（本地 JDK 17）

### 3.1 编译（必须使用 JDK 17）

```bash
cd /Users/pengchengkeji/Desktop/260226fangchanxiaoshou/pengcheng-admin
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS 指定 JDK 17；Windows 请设为 JDK 17 安装目录
mvn clean compile -DskipTests
```

若本机仅有 JDK 21/24，会报 Lombok `TypeTag :: UNKNOWN`，需安装 JDK 17 或升级 Lombok 后再编译。

### 3.2 运行测试（可选）

```bash
mvn test
# 或仅跑指定模块
mvn test -pl pengcheng-core/pengcheng-ai
```

### 3.3 启动应用

```bash
# 方式一：Maven 启动（需先 compile 或 package）
mvn spring-boot:run -pl pengcheng-starter -DskipTests

# 方式二：打包后启动
mvn package -DskipTests -pl pengcheng-starter -am
java -jar pengcheng-starter/target/pengcheng-starter-1.0.0.jar
```

启动成功后，后端默认端口 **8080**；前端需单独启动（如 `npm run dev`），并配置代理到 `http://localhost:8080`。

### 3.4 健康与登录

- 健康检查：`curl -s http://localhost:8080/actuator/health`（若已启用 actuator）
- 登录：根据 `doc/USER-MANUAL.md` 或系统初始化脚本中的默认账号（如 `admin` / `admin123`）访问后台

---

## 四、试运行结论与建议

| 项目 | 结论 |
|------|------|
| 代码与配置 | 已就绪；AgentScope Runtime 已移除，路由 100% Spring AI；`.env.example` 已去掉 AgentScope 相关项；已删除对已移除类的测试 |
| 编译 | 需在 **JDK 17** 下执行，否则可能因 Lombok 与高版本 JDK 不兼容导致编译失败 |
| 测试 | 90+ 用例，覆盖核心 AI 与业务；建议在试运行前跑一遍 `mvn test` |
| 启动 | 依赖 MySQL、Redis；配置好数据源与 Redis 后，按上述命令即可启动 |
| 建议 | 1）本机安装并选用 JDK 17；2）确保 MySQL/Redis 已启动并建库；3）先 `mvn clean compile -DskipTests` 再 `mvn spring-boot:run -pl pengcheng-starter -DskipTests` |

---

## 五、常见问题

1. **TypeTag / Unsafe 编译错误**  
   使用 JDK 17 编译；或升级 Lombok 至 1.18.36+（根 POM 已为 1.18.36）。

2. **Flyway 基线或迁移失败**  
   确认数据库已创建、账号有建表/迁移权限；参考 `doc/PRODUCTION-DB-USER.md`、`doc/DEPLOYMENT.md`。

3. **Redis 连接失败**  
   检查 `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`（若有）；Redis 需先启动。

4. **前端无法访问接口**  
   确认前端开发服务器代理到 `http://localhost:8080`，或配置后端 CORS。

---

## 六、后台与商业交付相关修复（2026-03）

### 6.1 已修复问题

| 问题 | 处理 |
|------|------|
| **工作台「我的任务」报错**：`Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; For input string: "my-tasks"` | 将 `PmProjectController` 中 `GET /project/my-tasks` 映射移到 `GET /project/{id}` 之前，避免 "my-tasks" 被当作路径变量 id 解析。 |
| **左侧主菜单与「菜单管理」不一致、重复/缺项** | **彻底修复**：左侧主菜单改为**仅使用后端返回的菜单**（与「系统管理 → 菜单管理」完全一致），仅固定前置「首页」；移除所有前端硬编码的协作办公、房产业务、智能助手、系统管理等静态菜单，避免重复与不一致。面包屑改为优先从后端菜单树动态解析。 |
| **工作台欢迎语与「我的待办」不一致** | 待办数、会议数初始值改为 0，仅由接口返回数据更新，避免默认显示「3 项待办、2 个会议」而下方为空。 |
| **过时/测试菜单** | 不再依赖过滤；菜单全部来自后端，由菜单管理统一维护。 |

### 6.2 移动端（Uniapp）与菜单一致性

| 项目 | 说明 |
|------|------|
| **TabBar** | 工作台、消息、通讯录、我的（4 个主入口，`pages.json` 固定配置） |
| **已配置页面** | 工作台、消息/聊天、群聊、个人资料、客户报备/列表/详情、考勤打卡/月报/扫码签到、请假/调休/报销/垫佣/预付佣申请、申请记录、审批中心、AI 助手等（见 `pengcheng-uniapp/pages.json`） |
| **与后台菜单关系** | 移动端当前为**固定** tabBar + 固定工作台快捷入口、固定「我的」页功能列表，未按后台「菜单管理」或角色做显隐。详见 `pengcheng-uniapp/doc/MENU-AND-BACKEND.md`；后续可接后端「应用端菜单」接口实现与后台一致的显隐。 |
| **建议** | 与 PC 后台共用同一后端；上线前需在菜单/权限与接口联调、真机测试。 |

### 6.3 商业交付前建议自检

- [ ] 数据库：生产库建库、迁移执行、默认管理员账号与初始角色/菜单
- [ ] 菜单：在「系统管理 → 菜单管理」中确认无重复、无测试菜单；隐藏或删除「测试菜单」
- [ ] 工作台：登录后无报错；待办/会议/数据概览与真实接口一致（或友好空状态）
- [ ] 移动端：打包并真机验证 TabBar、登录、工作台、客户/考勤/审批等核心流程
- [ ] 环境变量：生产环境配置 `.env` 或等价配置（DB、Redis、AI 密钥等），勿提交明文密钥
