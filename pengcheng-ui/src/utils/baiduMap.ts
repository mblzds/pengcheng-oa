/**
 * 百度地图 JS API 加载器（单例）
 *
 * 调用 loadBaiduMap(ak) 注入 <script>，等待全局 callback 后 resolve `window.BMap`。
 * 后续重复调用直接复用首次的 promise；AK 变化不会重新加载（百度脚本一旦加载
 * 全局对象就锁死，需要刷新页面才能换 AK，UI 上需要给出明确提示）。
 */

declare global {
  interface Window {
    BMap?: any
  }
}

let loaderPromise: Promise<any> | null = null
let loadedAk: string | null = null

export function loadBaiduMap(ak: string): Promise<any> {
  if (window.BMap) return Promise.resolve(window.BMap)
  if (loaderPromise) return loaderPromise
  if (!ak) return Promise.reject(new Error('请先在「考勤设置」上方填入百度地图 AK'))

  loaderPromise = new Promise((resolve, reject) => {
    const callbackName = `__bmapReady__${Date.now()}`
    ;(window as any)[callbackName] = () => {
      delete (window as any)[callbackName]
      if (window.BMap) {
        resolve(window.BMap)
      } else {
        loaderPromise = null
        reject(new Error('百度地图加载完成但 window.BMap 不存在'))
      }
    }
    const script = document.createElement('script')
    script.src = `https://api.map.baidu.com/api?v=3.0&ak=${encodeURIComponent(ak)}&callback=${callbackName}`
    script.async = true
    script.onerror = () => {
      loaderPromise = null
      reject(new Error('百度地图脚本加载失败，检查 AK 是否正确或网络'))
    }
    document.head.appendChild(script)
  })
  loadedAk = ak
  return loaderPromise
}

/** AK 与已加载的脚本不一致时返回 true，UI 可据此提示用户刷新 */
export function isAkChanged(currentAk: string): boolean {
  return loadedAk !== null && currentAk !== loadedAk
}
