/**
 * 坐标系转换 — 移动端只需要 GCJ-02 ↔ WGS-84
 *
 * 微信小程序 uni.getLocation 默认返回 GCJ-02，<map> 组件也按 GCJ-02 渲染。
 * 但考勤后端按 WGS-84 存储合规位置（与设备真实 GPS / 后台百度选点一致），
 * 上报时必须先转一次，否则距离判定误差 50–200 米。
 */

const PI = Math.PI
const A = 6378245.0
const EE = 0.00669342162296594323

function transformLat(x, y) {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(y * PI) + 40.0 * Math.sin((y / 3.0) * PI)) * 2.0) / 3.0
  ret += ((160.0 * Math.sin((y / 12.0) * PI) + 320 * Math.sin((y * PI) / 30.0)) * 2.0) / 3.0
  return ret
}

function transformLng(x, y) {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(x * PI) + 40.0 * Math.sin((x / 3.0) * PI)) * 2.0) / 3.0
  ret += ((150.0 * Math.sin((x / 12.0) * PI) + 300.0 * Math.sin((x / 30.0) * PI)) * 2.0) / 3.0
  return ret
}

function gcjOffset(lng, lat) {
  const dLat = transformLat(lng - 105.0, lat - 35.0)
  const dLng = transformLng(lng - 105.0, lat - 35.0)
  const radLat = (lat / 180.0) * PI
  let magic = Math.sin(radLat)
  magic = 1 - EE * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  const correctedDLat = (dLat * 180.0) / (((A * (1 - EE)) / (magic * sqrtMagic)) * PI)
  const correctedDLng = (dLng * 180.0) / ((A / sqrtMagic) * Math.cos(radLat) * PI)
  return [correctedDLng, correctedDLat]
}

/** GCJ-02 → WGS-84（近似还原，精度约 1m） */
export function gcj02ToWgs84(lng, lat) {
  const [dLng, dLat] = gcjOffset(lng, lat)
  return [lng - dLng, lat - dLat]
}
