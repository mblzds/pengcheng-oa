# 调休流程现状与缺口分析

> 分析日期：2026-05-10
> 状态：待处理（用户决定后续再统一改造，未定短期/长期方向）

## 1. 当前实现的全貌

### 1.1 小程序提交字段
文件：`pengcheng-uniapp/pages/apply/compensate.vue`

仅采集 2 个字段：
- `compensateDate`（单天 date picker）
- `reason`（textarea）

无时长档位、无加班关联、无半天/小时粒度。

### 1.2 后端入口校验
文件：`pengcheng-core/pengcheng-hr/src/main/java/com/pengcheng/hr/attendance/service/impl/AttendanceServiceImpl.java`，`submitCompensateRequest` 方法（约 152 行）

仅校验：
- `userId` 非空
- `compensateDate` 非空

无任何业务前置校验（不拦过去日期、不拦周末、不拦冲突、不拦余额）。

### 1.3 审批链
表：`approval_flow_node`，`business_type='compensate'`

当前形态：仅 1 节点 **「直接上级」**（`approver_type=direct_supervisor`）。

历史：
- V45 种子：seq=1「直接上级」
- V53：补加 seq=2「HR 备案」（role=51）
- V54：撤回 V53 的 HR 备案
- V58：仅 simplify `leave`，未动 `compensate`

对比请假当前形态：「直接上级 → HR 备案」两节点。

### 1.4 通过后副作用
文件：`pengcheng-core/pengcheng-hr/src/main/java/com/pengcheng/hr/attendance/event/AttendanceExemptListener.java`，`handleCompensate` 方法

审批通过后：当天 `attendance_record.exempt_reason='compensate'`，缺卡判定时整天豁免。粒度按天。

### 1.5 关键缺位
- `AttendanceMonthlyVO.overtimeHours` 硬编码 `0.0`
- 全项目无 `overtime_record` 表
- 无调休余额账户

## 2. 已识别的问题

| # | 问题 | 影响 |
|---|---|---|
| 1 | 无"加班 → 调休"勾稽 | 员工可随意调休，不消耗任何额度，调休沦为"换名字的事假" |
| 2 | 审批链比请假轻 | HR 完全不感知调休发生，无法核账 |
| 3 | 日期类校验全缺 | 可选过去日期、周末、节假日（这些日子本就不上班，调休无意义） |
| 4 | 无去重 | 同一天可重复提交多条调休（writeExempt 仅"不覆盖"，业务层未拒） |
| 5 | 粒度只有"天" | 无法表达半天/小时调休；无 startTime/endTime 字段（不像请假） |
| 6 | 无与请假/其他调休的同日冲突校验 | 已休一天又调一天的情况不报错 |

## 3. 调休前置条件应有的维度

| 维度 | 应有 | 当前状态 |
|---|---|---|
| 余额 | 调休账户 ≥ 申请时长（来源：审批通过的加班记录） | 缺 |
| 时长粒度 | 小时 / 半天 / 天 三档 | 仅天 |
| 日期合法性 | 工作日；不过去；不重合法定节假日 | 缺 |
| 冲突 | 同日不得已存在审批中/已通过的 leave 或 compensate | 缺 |
| 来源凭证 | 关联加班记录 ID（防止凭空调休） | 缺 |
| 时效 | 加班产生的额度有有效期（如半年内必须用） | 缺 |
| 审批层级 | 直接上级 → HR 备案/核账（核对余额扣减） | 仅直接上级 |

## 4. 改造方向

### 4.1 短期（最小动作堵住明显口子）
- **前端**：日期合法性校验（非过去、非周末/节假日）+ 同日冲突前置查询提示
- **后端**：`submitCompensateRequest` 加同样校验
- **审批链**：补 HR 备案节点对齐请假
- **粒度**：暂保持按天

### 4.2 长期（补完整链路）
- 建 `overtime_record` 表登记加班并攒额度
- `compensate_request` 加时长字段（小时数 / 半天 / 全天）
- 提交时校验并冻结余额，审批通过后扣减
- 加班额度时效（如 6 个月过期）
- 否则调休永远只是"事假替身"

## 5. 关键文件参照

| 用途 | 路径 |
|---|---|
| 小程序提交页 | `pengcheng-uniapp/pages/apply/compensate.vue` |
| 后端 submit | `pengcheng-core/pengcheng-hr/src/main/java/com/pengcheng/hr/attendance/service/impl/AttendanceServiceImpl.java`（152 行附近） |
| 审批通过后挂载考勤豁免 | `pengcheng-core/pengcheng-hr/src/main/java/com/pengcheng/hr/attendance/event/AttendanceExemptListener.java` |
| compensate 种子审批节点 | `pengcheng-starter/src/main/resources/db/migration/V45__approval_flow.sql` |
| HR 备案加了又撤的历史 | `pengcheng-starter/src/main/resources/db/migration/V53__approval_flow_end_with_hr.sql` / `V54__revert_approval_flow_hr.sql` |
| leave 化简形态参考 | `pengcheng-starter/src/main/resources/db/migration/V58__simplify_leave_flow.sql` |
| compensate_request 建表 | `sql/V3__notification_compensate.sql` |
| 调休实体 | `pengcheng-core/pengcheng-hr/src/main/java/com/pengcheng/hr/attendance/entity/CompensateRequest.java` |
| 月度考勤 VO（overtime 占位） | `pengcheng-core/pengcheng-hr/src/main/java/com/pengcheng/hr/attendance/dto/AttendanceMonthlyVO.java` |
