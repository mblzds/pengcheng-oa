# temp-disabled 目录保留说明

本目录用于临时停用但**未删除**的代码/资源。用途是让"暂停使用但不允许丢失"的资产有一个明确的物理位置，方便审计与随时恢复。

## 保留原则

1. **微信 / 支付宝登录与支付**：按 Feature Flag（`pengcheng.feature.alipay|wechat.mp|wechat.mini|wechat.pay`）维护，**整条资产链路保留**，包括：
   - `pengcheng-infra/pengcheng-pay/**`
   - `pengcheng-infra/pengcheng-wechat/**`
   - `pengcheng-infra/pengcheng-social/src/main/java/com/pengcheng/social/impl/*Alipay*`、`*Wechat*`
   - `pengcheng-api/pengcheng-admin-api/**/controller/pay/**`
   - `pengcheng-api/pengcheng-admin-api/**/controller/auth/WechatMpAuthController*`
   - `pengcheng-api/pengcheng-app-api/**/controller/AppPayController*`
   - `pengcheng-core/pengcheng-auth/**/MiniProgramLoginStrategy*`
   - 以及 `pengcheng-uniapp` 中登录/支付相关页面
2. 仅允许对这些资产进行：**性能优化、安全加固、稳定性修复**。
3. 禁止：**删除、重命名、下线、将调用点迁移到其它实现**。

## 开关方式

线上默认关闭：
```yaml
pengcheng:
  feature:
    alipay.enabled: false
    wechat:
      mp.enabled: false
      mini.enabled: false
      pay.enabled: false
```

需要启用任一渠道时，置 `true` 并配置对应的 `appId / secret / merchantId` 等凭证。

## 相关文档

- `doc/DEV-PLAN-V3.2.md` §WP-S6-G（微信/支付宝加固）
- `doc/PRD-V4.0-新产品需求文档.md`（Feature Flag 说明）
- `doc/LOGIN-PAY-IMPLEMENTATION.md`
- `doc/WECHAT-LOGIN-PAY-CONFIG.md`
- `doc/LOGIN-PAY-OFFICIAL-COMPLIANCE.md`

## 变更审计

任何向此目录新增/移除条目的 PR，必须在描述中说明理由，并在 `doc/DEV-PLAN-V3.2.md` 或继任文档中留有对应工作包。
