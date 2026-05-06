import { defineStore } from 'pinia'
import { ref } from 'vue'
import { configGroupApi } from '@/api/org'

/**
 * 站点配置 Store
 */
export const useSiteStore = defineStore('site', () => {
  // 站点名称
  const siteName = ref('MasterLife')
  // 站点描述
  const siteDescription = ref('现代化后台管理系统')
  // 站点 Logo
  const siteLogo = ref('')
  // 版权信息
  const copyright = ref('版权所有@朋诚科技')
  // ICP 备案号
  const icp = ref('')
  // 水印配置
  const watermarkEnabled = ref(true)
  const watermarkType = ref('username')
  const watermarkOpacity = ref(0.1)
  const loaded = ref(false)
  let loadingPromise: Promise<void> | null = null

  /**
   * 加载站点配置（防重复调用）
   */
  async function loadConfig() {
    if (loaded.value) return
    if (loadingPromise) return loadingPromise

    loadingPromise = _doLoadConfig()
    return loadingPromise
  }

  async function _doLoadConfig() {
    try {
      const config = await configGroupApi.getPublicConfig()
      if (config.system) {
        siteName.value = config.system.siteName || 'MasterLife'
        siteDescription.value = config.system.siteDescription || '现代化后台管理系统'
        siteLogo.value = config.system.siteLogo || ''
        copyright.value = config.system.copyright || '版权所有@朋诚科技'
        icp.value = config.system.icp || ''
        // 水印配置，默认开启
        watermarkEnabled.value = config.system.watermarkEnabled !== false
        watermarkType.value = config.system.watermarkType || 'username'
        watermarkOpacity.value = config.system.watermarkOpacity || 0.1
      }
      loaded.value = true
    } catch (error) {
      console.warn('加载站点配置失败，使用默认配置')
    } finally {
      loadingPromise = null
    }
  }

  return {
    siteName,
    siteDescription,
    siteLogo,
    copyright,
    icp,
    watermarkEnabled,
    watermarkType,
    watermarkOpacity,
    loaded,
    loadConfig
  }
})
