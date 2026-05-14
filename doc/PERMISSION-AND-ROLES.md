# 权限与角色体系说明

本文档说明 MasterLife OA 平台后台的角色、数据权限、审批流之间的关系，以及给具体岗位挂角色的规则。

> 适用范围：管理后台（admin）。小程序 / App 端权限边界另行处理。

---

## 一、三段权限模型

权限分三段，串行决定"我能看哪些数据 / 进哪些功能"：

| 段 | 字段 / 配置 | 控制什么 | 在哪里配置 |
|---|---|---|---|
| **A. 基础可见范围** | `sys_role.data_scope`（按职级角色） | 能看几行数据的"底色" | 角色管理 → 编辑角色 → 数据范围 |
| **B. 业务模块加成** | `sys_config_group(<module>).fullScopeRoleCodes` | 在某业务模块**升级**为全员可见 | 系统配置 → 各业务设置 |
| **C. 模块入口闸** | `sys_role_menu` | 能进哪些页面 / 按哪些按钮 | 角色管理 → 编辑角色 → 菜单权限 |

**核心思路**：
- 基础（A）按"职级"：员工 / 部门经理 / 高管，跟职能无关
- 加成（B）按"职能"：财务在付款全员、HR 在考勤全员，跟职级正交
- 入口（C）控制谁能进入哪个模块；进入后由 A+B 一起决定看多少行

举例（这次设计的关键 use case）：

| 用户 | 挂载角色 | 看自己考勤 | 看全公司报销 |
|---|---|---|---|
| 马财务 | `employee + finance` | ✅ A=5 自己 + B 不在 attendance 加成 | ✅ A=5 自己 + B finance 在 payment 加成 = 全员 |
| 王人事 | `employee + hr` | ✅ A=5 自己 + B hr 在 attendance 加成 = 全员 | ❌ A=5 自己（报销不属 hr 职能） |
| 李销售经理 | `dept_manager + sales(无)` | 看销售部+下级 | 自己（payment 加成不含 sales）|
| HR 总监 | `dept_manager + hr` | A=4 部门 ∪ B hr=全员 → 全员 | A=4 部门 |
| 董事长 | `chairman` | A=1 全员 | A=1 全员 |

---

## 二、A. `data_scope` 五档语义（基础可见）

| 值 | 含义 | 展开规则 |
|---|---|---|
| `1` | 全部 | 不限，所有数据可见 |
| `2` | 自定义 | 由 `sys_role_dept` 表关联指定的若干部门（含下钻） |
| `3` | 本部门 | `user.dept_id` 下的员工，**不下钻** |
| `4` | 本部门及以下 | `user.dept_id` + 所有后代部门（沿 `sys_dept.ancestors` 下钻） |
| `5` | 仅本人 | 只能看到自己创建/参与的数据 |

**多角色取并集，最宽胜出**。

> ⚠️ V73 起：`finance` / `hr` 不再走 data_scope=1。它们是"职能角色"，基础 data_scope=5（仅本人），全员加成靠 B 段实现。

---

## 三、B. 业务模块加成（按职能）

每个业务模块在 `sys_config_group(<module>)` 配一个 `fullScopeRoleCodes` 字段（CSV 角色 code），命中的角色在该模块升级为全员可见：

| 模块 | 配置 key | 默认值 | 语义 |
|---|---|---|---|
| 考勤 / 请假 / 调休 | `attendance.fullScopeRoleCodes` | `"hr"` | hr 角色在考勤升级全员 |
| 付款 / 报销 | `payment.fullScopeRoleCodes` | `"finance"` | finance 角色在付款升级全员 |
| 佣金 | `commission.fullScopeRoleCodes` | `"finance"` | finance 角色在佣金升级全员 |

**注意**：
- `admin / chairman / general_manager` 通过 A 段 data_scope=1 已经全员，**不需要**列入加成清单
- 运行时改 `sys_config_group` 立即生效，无需重启代码
- 想新加一个职能角色（如 `legal_counsel` 在合同模块全员），加进对应模块的清单即可

---

## 四、当前角色清单（V73 之后）

| code | 中文名 | data_scope | 加成 | 适用岗位 |
|---|---|---|---|---|
| `admin` | 超级管理员 | 1 全部 | — | 系统运维、技术管理员 |
| `chairman` | 董事长 | 1 全部 | — | 董事长 |
| `general_manager` | 总经理 | 1 全部 | — | 总经理 |
| `hr` | 人事 | **5 仅本人** | 考勤模块全员 | HR 经理 / HR 专员（**叠挂 employee 或 dept_manager**）|
| `finance` | 财务 | **5 仅本人** | 付款 / 佣金模块全员 | 财务经理 / 财务专员（**叠挂 employee 或 dept_manager**）|
| `dept_manager` | 部门经理 | 4 本部门及下级 | — | 销售部 / 技术部 / 市场部 等部门经理 |
| `employee` | 普通员工 | 5 仅本人 | — | 普通员工 |

---

## 五、岗位 → 角色挂法

**新模型核心**：财务/HR 必须**叠挂基础角色** + **职能角色**，因为他们既是"员工/经理"又是"职能岗位"。

| 岗位 | 应挂角色 | 备注 |
|---|---|---|
| 公司董事长 | `chairman` | A=1 全员，不需职能加成 |
| 公司总经理 | `general_manager` | A=1 全员 |
| HR 经理（管 HR 部） | `dept_manager + hr` | A=4 HR部 + B 考勤=全员；报销=HR部 |
| HR 专员 | `employee + hr` | A=5 自己 + B 考勤=全员；报销=自己 |
| 财务经理（管财务部） | `dept_manager + finance` | A=4 财务部 + B 付款/佣金=全员；考勤=财务部 |
| 财务专员 | `employee + finance` | A=5 自己 + B 付款/佣金=全员；考勤=自己 |
| 其他部门经理 | `dept_manager` | 看本部门 + 下级 |
| 普通员工 | `employee` | 仅本人 |
| 系统运维 | `admin` | 慎挂 |

### 几个"不要"

1. **不要只挂 `finance` 不挂基础角色**。否则 data_scope=5 后该用户连"自己提的请假"都看不见——基础职级层失守
2. **不要给 HR / 财务 叠挂 `chairman / general_manager / admin`**。那会让 A 段直接全员，B 段加成失效，反而看到一切（包括不该看的）
3. **不要让普通员工挂 `dept_manager` 凑权限**。改职级用 `dept_manager`，不要叠加 `employee + dept_manager`

### V73 自动迁移

V73 已自动处理：
- `finance.data_scope: 1 → 5`、`hr.data_scope: 1 → 5`
- 给所有挂 `finance` 的用户**追加挂 `employee`** —— 保证基础职级层不丢
- 给所有挂 `hr` 的用户**追加挂 `employee`** —— 同上

**HR 后台仍需手工补**：原 HR 经理 / 财务经理 这种是部门经理身份的，要从 `employee` 调整为 `dept_manager`（迁移脚本不自动判定"部门经理"避免误升权）

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
| 部门经理却看到全公司数据 | 同时挂了 data_scope=1 的角色（`admin` / `chairman` / `general_manager` 等） | 查 `sys_user_role` 看是否多挂 |
| 财务能看销售部考勤 | `attendance.fullScopeRoleCodes` 误把 finance 加进去；或 finance.data_scope=1 没改成 5 | 检查 `sys_config_group(attendance)` 配置 + sys_role.data_scope |
| HR 能看全公司报销 | `payment.fullScopeRoleCodes` 误把 hr 加进去 | 检查 `sys_config_group(payment)` 配置 |
| 财务专员连自己提的请假都看不到 | 用户只挂了 `finance` 没挂基础角色（V73 之后 finance.data_scope=5） | sys_user_role 追加 `employee` |
| 员工提交请假找不到上级 | 部门 `leader_id` 为 NULL，且祖先部门也没设 | 部门管理→编辑→部门负责人 |
| HR 角色看不到全公司考勤 | `attendance.fullScopeRoleCodes` 配置缺 hr / 或用户没挂 `hr` 角色 | 查配置 + sys_user_role |
| `dept_manager` 挂了但只看到自己 | `user.dept_id` 为 NULL 或指向根部门 | 用户管理→编辑→所属部门 |
| 销售员看到了同部门同事的客户 | 同时挂了 `dept_manager` 或 data_scope ≥ 3 的角色 | 检查多挂；正常应只挂 `employee`（data_scope=5）|
| 审批流"直接上级"卡住 | 全链路都找不到 leader_id | 顶层部门必须有 leader_id |

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
| `pengcheng-api/pengcheng-admin-api/.../hr/AttendanceScopeHelper.java` | 通用 ScopeHelper：`visibleUserIds()` / `visibleUserIdsForPayment()` / `visibleUserIdsForCommission()` / 通用 `visibleUserIdsForModule(roleCodes)` |
| `pengcheng-core/pengcheng-system/.../helper/SystemConfigHelper.java` | `getAttendanceFullScopeRoleCodes()` / `getPaymentFullScopeRoleCodes()` / `getCommissionFullScopeRoleCodes()` — 业务加成配置读取 |
| `pengcheng-api/pengcheng-admin-api/.../realty/PaymentController.java` | 付款列表注入可见集合 |
| `pengcheng-api/pengcheng-admin-api/.../realty/CommissionController.java` | 佣金列表注入可见集合 |
| `pengcheng-infra/pengcheng-db/.../interceptor/DataPermissionInterceptor.java` | 通用数据权限拦截器（客户 / 销售模块走这条） |
| `pengcheng-core/pengcheng-hr/.../approval/service/impl/ApprovalFlowServiceImpl.java` | 审批流引擎，`resolveApprovers` / `resolveDirectSupervisor` |
| `pengcheng-starter/.../db/migration/V67__customer_data_scope_roles.sql` | 销售线角色定义 + data_scope 5 档语义 |
| `pengcheng-starter/.../db/migration/V70__user_role_data_scope_self_only.sql` | `user` 角色 data_scope 3→5 |
| `pengcheng-starter/.../db/migration/V71__manager_role_data_scope_dept_subtree.sql` | `manager` 角色 data_scope 1→4 |
| `pengcheng-starter/.../db/migration/V72__unify_role_system.sql` | 角色重命名 + flow_* 迁移 + 新建 chairman |
| `pengcheng-starter/.../db/migration/V73__split_role_basic_and_business_scope.sql` | finance/hr.data_scope 1→5；为现有 finance/hr 用户追加挂 employee |

---

最后更新：V73 落地后（2026-05-14）
