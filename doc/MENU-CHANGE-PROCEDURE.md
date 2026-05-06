# 菜单与导航变更规范

**目的**：新增/调整导航菜单时，避免只改一处导致「已有库看不到」或「新装库不一致」，保证菜单变更被全面应用。

---

## 一、菜单数据来源（为何容易漏改）

| 来源 | 作用 | 漏改后果 |
|------|------|----------|
| **Flyway 迁移** `db/migration/Vxx__*.sql` | 已有数据库升级时执行 | 老环境看不到新菜单或归属错误 |
| **全量 SQL** `sql/pengcheng-system.sql` | 新装/恢复库的初始数据 | 新装后导航缺项或结构不一致 |
| **后台「菜单管理」** | 运行时修改，存库 | 仅影响当前库，不影响脚本与文档 |

变更菜单时**必须同时更新**前两项（迁移 + 全量 SQL），并视情况更新文档和角色权限。

---

## 二、标准流程（新增或调整菜单时必做）

### 1. 确定归属与路由

- 菜单名称、父级（如：文件管理、消息中心）、path、component、权限标识、图标、排序。
- 前端路由已在 `pengcheng-ui/src/router/index.ts` 的，path/component 需与菜单表一致。

### 2. 编写 Flyway 迁移

- 在 `pengcheng-starter/src/main/resources/db/migration/` 下新增 `Vxx__描述.sql`。
- **新增菜单**：`INSERT INTO sys_menu (...) SELECT 父级id, ... WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '...')`，父级用 `name` 查询避免写死 id。
- **调整归属**：`UPDATE sys_menu SET parent_id = (SELECT id FROM sys_menu WHERE name = '父级名' ...) WHERE name IN ('子菜单1','子菜单2')`。
- **非 admin 角色**：若希望「已有父级权限的角色」自动拥有新子菜单，在同一迁移或后续迁移中 `INSERT INTO sys_role_menu (role_id, menu_id)`，按「拥有父级菜单的角色」补充（参考 V28）。

### 3. 更新全量 SQL

- 在 `sql/pengcheng-system.sql` 的 `sys_menu` 插入区，在对应父级下增加一行 `INSERT INTO sys_menu VALUES (id, parent_id, name, ...)`。
- 使用未占用的 id（与现有 INSERT 不冲突）；父级 id 与全量 SQL 中一致（如文件管理 126，消息中心 134）。

### 4. 文档与发布清单

- **ARCHITECTURE.md**：路由/菜单结构说明若有列表，补充或修改对应项（如「智能表格归属：文件管理」）。
- **USER-MANUAL.md**：若入口变化（如「通讯录在消息中心下」），更新相应章节入口说明。
- **RELEASE-CHECKLIST-V3.0.md**：发布前勾选「菜单与导航同步」项（见下文）。

### 5. 可选：历史脚本说明

- `sql/update_menu_2.0.sql` 等为历史/可选脚本，表结构可能与当前库不一致；若仍在使用，在脚本顶部注释说明「与 Flyway 菜单迁移并存时，以 Flyway 为准，避免重复执行导致重复菜单」。

---

## 三、检查清单（发布前自检）

在完成菜单相关改动后，按下面逐项确认：

- [ ] **迁移**：已新增或修改 `db/migration/Vxx__*.sql`，且语法可在当前 MySQL 下执行。
- [ ] **全量 SQL**：已在 `sql/pengcheng-system.sql` 的 `sys_menu` 中增加/修改对应记录，id 不冲突。
- [ ] **角色权限**：若需非 admin 角色默认可见，已通过迁移补充 `sys_role_menu`（或说明需在「角色管理」中手动勾选）。
- [ ] **文档**：ARCHITECTURE / USER-MANUAL 中入口与归属描述已更新。
- [ ] **发布清单**：RELEASE-CHECKLIST 中「菜单与导航」已勾选。

---

## 四、当前菜单归属速查（便于后续改版对齐）

| 菜单名 | 父级（一级） | 说明 |
|--------|--------------|------|
| 文件列表、文件配置、**智能表格**、**表格模板管理** | 文件管理 | 智能表格已归入文件管理 |
| 系统通知、即时聊天、**通讯录** | 消息中心 | 通讯录已归入消息中心 |

后续新增或移动菜单时，按上述流程和检查清单执行即可减少「没有及时更新」的问题。
