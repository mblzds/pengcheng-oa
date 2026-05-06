# 性能压测脚本

本目录为 MasterLife 接口压测脚本，使用 [k6](https://k6.io/) 运行。

## 环境准备

安装 k6（任选其一）：

- macOS: `brew install k6`
- 或见 [k6 安装文档](https://grafana.com/docs/k6/latest/set-up/install-k6/)

## 脚本说明

| 脚本 | 说明 |
|------|------|
| `api-smoke.js` | 接口冒烟压测：阶梯 0→10→20 VU，约 2 分钟，默认请求 `GET /api/captcha` |

## 运行方式

```bash
# 默认 BASE_URL=http://localhost:80
k6 run scripts/perf/api-smoke.js

# 指定 base URL 与可选 Token（需先登录获取）
BASE_URL=https://your-domain.com TOKEN=your-jwt k6 run scripts/perf/api-smoke.js
```

## 阈值

- `http_req_duration`：P95 < 2000ms
- `http_req_failed`：失败率 < 5%

可根据需要修改脚本内 `options.thresholds`。
