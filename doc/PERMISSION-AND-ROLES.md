# 权限与角色体系说明

本文档说明 MasterLife OA 平台后台的角色、数据权限、审批流之间的关系，以及给具体岗位挂角色的规则。

> 适用范围：管理后台（admin）。小程序 / App 端权限边界另行处理。

---

## 一、两个独立轴

权限分两个**互不耦合**的维度：

| 维度 | 字段 | 控制什么 | 在哪里配置 |
|---|---|---|---|
| **数据可见范围** | `sys_role.data_scope` | "能看到几行数据" | 角色管理 → 编辑角色 → 数据范围 |
| **模块可见性** | `sys_role_menu` | "能进哪些页面 / 按哪些按钮" | 角色管理 → 编辑角色 → 菜单权限 |

举例：
- HR 和 财务 两个角色都有 `data_scope=1`（看全公司行）
- 但 HR 角色只绑了"人力相关"菜单（考勤 / 请假 / 档案）
- 财务角色只绑了"财务相关"菜单（报销 / 付款 / 工资 / 发票）
- 结果：HR 看的是全公司考勤；财务看的是全公司报销 —— 互不踏入对方领地

---

## 二、`data_scope` 五档语义

| 值 | 含义 | 展开规则 |
|---|---|---|
| `1` | 全部 | 不限，所有数据可见 |
| `2` | 自定义 | 由 `sys_role_dept` 表关联指定的若干部门（含下钻） |
| `3` | 本部门 | `user.dept_id` 下的员工，**不下钻** |
| `4` | 本部门及以下 | `user.dept_id` + 所有后代部门（沿 `sys_dept.ancestors` 下钻） |
| `5` | 仅本人 | 只能看到自己创建/参与的数据 |

**多角色取并集，最宽胜出**。一人挂多个角色时，按 data_scope 数字最小（最宽）的那条算。

---

## 三、当前角色清单（V72 之后）

| code | 中文名 | data_scope | 适用岗位 |
|---|---|---|---|
| `admin` | 超级管理员 | 1 全部 | 系统运维、技术管理员 |
| `chairman` | 董事长 | 1 全部 | 董事长（V72 新建）|
| `general_manager` | 总经理 | 1 全部 | 总经理 |
| `hr` | 人事 | 1 全部 | HR 经理 / HR 专员 |
| `finance` | 财务 | 1 全部 | 财务经理 / 财务专员 |
| `dept_manager` | 部门经理 | 4 本部门及下级 | 销售部 / 技术部 / 市场部 等部门经理 |
| `employee` | 普通员工 | 5 仅本人 | 普通员工（销售员 / 开发 / 设计 / 行政 等）|

---

## 四、岗位 → 角色对照表

**最简法则**：一人一个主角色，按"这个人主要做什么"挂。

| 岗位 | 应挂角色 | 备注 |
|---|---|---|
| 公司董事长 | `chairman` | + 看你是否给 admin |
| 公司总经理 | `general_manager` | |
| HR 经理 / HR 专员 | `hr` | **不要叠加 `dept_manager`**：data_scope=1 已经覆盖本部门 |
| 财务经理 / 财务专员 | `finance` | 同上 |
| 销售部 / 技术部 等部门经理 | `dept_manager` | 看本部门 + 下级；自动是部门 leader |
| 销售员 / 开发 / 设计 / 行政 等普通员工 | `employee` | 仅本人 |
| 系统运维 / 实施 | `admin` | 系统超管，慎挂 |

### 几个关键的"不要"

1. **不要给 HR / 财务 经理 叠挂 `dept_manager`**。HR 角色 data_scope=1 已经包含本部门；多挂只会增加配置噪音
2. **不要给部门经理叠挂 `admin` / `general_manager`**。这两个 data_scope=1 会让 `dept_manager` 的"本部门限制"失效
3. **不要让普通员工挂多角色凑权限**。某人需要看本部门数据 → 改挂 `dept_manager`，不要 `employee + admin`

---

## 五、部门负责人（`sys_dept.leader_id`）

**这是和角色完全独立的另一个字段**。在 **部门管理 → 编辑部门 → 部门负责人** 设置。

控制两件事：

### 1. 审批流"直接上级"解析

请假 / 报销等流程的第一节点是"直接上级"，解析顺序：

1. `user.leader_id`（极少数跨部门汇报的显式覆盖，一般不用）
2. 申请人所在部门 `dept.leader_id`（前提：不等于申请人自己）
3. 沿 `sys_dept.ancestors` 向上找最近一个有 `leader_id` 的祖先部门
4. 找不到 → 该节点跳过

**所以 HR 经理是 HR 部下属的"直接上级"，靠的是 `sys_dept[HR部].leader_id=钱总监`，不是 `hr` 角色。**

### 2. 历史兼容：数据权限的"主管自动可见"兜底

即便某用户的角色 data_scope 不是 4，只要他被设为某部门的 `leader_id`，考勤模块会自动给他"本部门及下级"可见权（`AttendanceScopeHelper` 里的兜底分支）。这是 V68 引入的过渡机制，让没及时配 `dept_manager` 角色的主管也不至于看不见下属。

> 长期目标：废弃这个兜底，统一只看 `data_scope`。`dept_manager` 角色挂上就行，不再依赖 `leader_id` 做数据隔离。

---

## 六、审批流路由

审批节点的"候选审批人"来自三种类型：

| approver_type | 解析逻辑 |
|---|---|
| `direct_supervisor` | 按上节方式找申请人直接上级 |
| `applicant_dept_manager` | 申请人所在部门的负责人（同上链路，跳过 user.leader_id） |
| `user` | `approver_value` 配置的具体 user id 列表 |
| `role` | `approver_value` 配置的 role id 列表 → 拉所有挂该角色的用户 |

**自动排除申请人自己**（`ApprovalFlowServiceImpl.start` 行 80）：

```java
List<Long> approvers = raw.stream()
        .filter(id -> id != null && !id.equals(applicantId))
        .distinct()
        .collect(Collectors.toList());
if (approvers.isEmpty()) {
    continue;  // 候选为空就跳过本节点
}
```

**自动跳过候选空节点**：例如马财务（唯一财务）报销，"财务备案"节点候选只剩自己 → 排除后为空 → 跳过；流程在"直接上级"批完后即结。

### 典型业务流程模板（数据库 `approval_flow_node` 现状）

| 业务 | Node 1 | Node 2 |
|---|---|---|
| `leave` 请假 | 直接上级 | HR 备案（role=hr） |
| `expense` 报销 | 直接上级 | 财务（role=finance） |
| `advance` 借款 | 直接上级 | 财务（role=finance） |
| `prepay` 预支 | 直接上级 | 财务（role=finance） |
| `overtime` 加班 | 直接上级 | — |
| `compensate` 调休 | 直接上级 | — |

---

## 七、配置 Checklist（HR / 管理员实操）

### 新员工入职

1. **用户管理 → 新增**：填基本信息、设 `dept_id`
2. **挂角色**：参照"岗位 → 角色对照表"挂一个主角色

### 新部门成立

1. **部门管理 → 新增**：设 `dept_name`、`parent_id`、`leader_id`（**必须填**）
2. 注意 `ancestors` 字段会自动按 `parent_id` 链路生成，无需手动维护
3. 把该部门员工的 `user.dept_id` 调整到新部门

### 部门换经理

1. **部门管理 → 编辑该部门 → 部门负责人**：改成新经理 user id
2. 旧经理的角色按需调整（若不再管理部门，由 `dept_manager` 改回 `employee`）
3. 新经理的角色挂 `dept_manager`（如不是 HR / 财务岗位）

### 离职处理

1. **用户管理 → 禁用账号**（不删，保留历史数据归属）
2. 如该人是某部门 `leader_id`，**先在部门管理里改 leader_id 再禁用**，否则审批流"直接上级"会找不到人

---

## 八、常见误配置 & 排查

| 现象 | 可能原因 | 排查 |
|---|---|---|
| 部门经理却看到全公司数据 | 同时挂了 data_scope=1 的角色（`admin` / `general_manager` 等） | 查 `sys_user_role` 看是否多挂；按"一人一主角色"清理 |
| 员工提交请假找不到上级 | 部门 `leader_id` 为 NULL，且祖先部门也没设 | 部门管理→编辑→部门负责人 |
| HR 角色看不到全公司考勤 | `hr.data_scope` 被误调低 / HR 用户没挂 `hr` 角色 | 查 `sys_role` + `sys_user_role` |
| `dept_manager` 挂了但只看到自己 | `user.dept_id` 为 NULL 或指向根部门 | 用户管理→编辑→所属部门 |
| 销售员看到了同部门同事的客户 | 同时挂了 `dept_manager` 或 data_scope ≥ 3 的角色 | 检查多挂；正常应只挂 `employee`（data_scope=5）|
| 审批流"直接上级"卡住 | 全链路都找不到 leader_id | 顶层部门必须有 leader_id；不希望走到顶就给中间部门补 leader_id |

---

## 九、数据库表速查

- `sys_role` — 角色清单（`id` / `code` / `name` / `data_scope` / `status`）
- `sys_user_role` — 用户↔角色 多对多关联
- `sys_role_menu` — 角色↔菜单 多对多关联（菜单可见性）
- `sys_role_dept` — 角色↔部门 多对多关联（仅当 data_scope=2 时使用）
- `sys_dept` — 部门树（`id` / `dept_name` / `parent_id` / `ancestors` / `leader_id`）
- `sys_user` — 用户（`id` / `dept_id` / `leader_id` 等）
- `approval_flow_node` — 审批节点模板（按 `business_type` 配置）
- `approval_record_node` — 实际生成的审批节点（每次申请快照）

---

## 十、相关代码

| 文件 | 作用 |
|---|---|
| `pengcheng-api/pengcheng-admin-api/.../hr/AttendanceScopeHelper.java` | 考勤模块的可见 userId 解析（支持完整 data_scope 1/3/4/5 + leader_id 兜底） |
| `pengcheng-infra/pengcheng-db/.../interceptor/DataPermissionInterceptor.java` | 通用数据权限拦截器（客户 / 销售模块走这条） |
| `pengcheng-core/pengcheng-hr/.../approval/service/impl/ApprovalFlowServiceImpl.java` | 审批流引擎，`resolveApprovers` / `resolveDirectSupervisor` 在这里 |
| `pengcheng-starter/.../db/migration/V67__customer_data_scope_roles.sql` | 销售线角色定义 + data_scope 5 档语义 |
| `pengcheng-starter/.../db/migration/V70__user_role_data_scope_self_only.sql` | `user` 角色 data_scope 3→5 |
| `pengcheng-starter/.../db/migration/V71__manager_role_data_scope_dept_subtree.sql` | `manager` 角色 data_scope 1→4 |
| `pengcheng-starter/.../db/migration/V72__unify_role_system.sql` | 角色重命名 + flow_* 迁移 + 新建 chairman |

---

最后更新：V72 落地后（2026-05-13）
