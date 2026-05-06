/**
 * MasterLife API 接口压测脚本（k6）
 * 使用方式：k6 run scripts/perf/api-smoke.js
 * 需先设置环境变量 BASE_URL（默认 http://localhost:80）和可选 TOKEN（登录后 Token）
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:80';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 20 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.05'],
  },
};

export default function () {
  const token = __ENV.TOKEN || '';
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = http.get(`${BASE_URL}/api/captcha`, { headers });
  check(res, { 'captcha status 200': (r) => r.status === 200 });
  sleep(0.5);
}
