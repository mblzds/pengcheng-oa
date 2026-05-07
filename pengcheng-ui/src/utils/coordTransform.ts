/**
 * 坐标系互转工具
 *
 * 三种常见坐标系：
 *   WGS-84 — 国际标准 / 设备 GPS 原始坐标
 *   GCJ-02 — 国测局加密（高德 / 腾讯 / 谷歌中国地图）
 *   BD-09  — 百度地图（在 GCJ-02 基础上再加密）
 *
 * 同一物理位置在三个坐标系下的经纬度可能差 50–500 米。考勤距离判定
 * 必须保证 *存储的合规位置* 与 *设备上报的 GPS 坐标* 在同一坐标系下，
 * 本项目统一用 WGS-84。百度地图选点拿到的是 BD-09，需要转回 WGS-84
 * 后再写入配置。
 *
 * 转换公式来自坊间通用实现，精度约 1–2 米，足以满足打卡半径用例。
 */

const PI = Math.PI
const X_PI = (PI * 3000.0) / 180.0
const A = 6378245.0
const EE = 0.00669342162296594323

function transformLat(x: number, y: number): number {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(y * PI) + 40.0 * Math.sin((y / 3.0) * PI)) * 2.0) / 3.0
  ret += ((160.0 * Math.sin((y / 12.0) * PI) + 320 * Math.sin((y * PI) / 30.0)) * 2.0) / 3.0
  return ret
}

function transformLng(x: number, y: number): number {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(x * PI) + 40.0 * Math.sin((x / 3.0) * PI)) * 2.0) / 3.0
  ret += ((150.0 * Math.sin((x / 12.0) * PI) + 300.0 * Math.sin((x / 30.0) * PI)) * 2.0) / 3.0
  return ret
}

function gcjOffset(lng: number, lat: number): [number, number] {
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

/** WGS-84 → GCJ-02 */
export function wgs84ToGcj02(lng: number, lat: number): [number, number] {
  const [dLng, dLat] = gcjOffset(lng, lat)
  return [lng + dLng, lat + dLat]
}

/** GCJ-02 → WGS-84（近似还原，单步迭代精度约 1m） */
export function gcj02ToWgs84(lng: number, lat: number): [number, number] {
  const [dLng, dLat] = gcjOffset(lng, lat)
  return [lng - dLng, lat - dLat]
}

/** GCJ-02 → BD-09 */
export function gcj02ToBd09(lng: number, lat: number): [number, number] {
  const z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * X_PI)
  const theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * X_PI)
  return [z * Math.cos(theta) + 0.0065, z * Math.sin(theta) + 0.006]
}

/** BD-09 → GCJ-02 */
export function bd09ToGcj02(lng: number, lat: number): [number, number] {
  const x = lng - 0.0065
  const y = lat - 0.006
  const z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * X_PI)
  const theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * X_PI)
  return [z * Math.cos(theta), z * Math.sin(theta)]
}

/** WGS-84 → BD-09（百度地图打点用） */
export function wgs84ToBd09(lng: number, lat: number): [number, number] {
  const [gLng, gLat] = wgs84ToGcj02(lng, lat)
  return gcj02ToBd09(gLng, gLat)
}

/** BD-09 → WGS-84（百度选点回写用） */
export function bd09ToWgs84(lng: number, lat: number): [number, number] {
  const [gLng, gLat] = bd09ToGcj02(lng, lat)
  return gcj02ToWgs84(gLng, gLat)
}
