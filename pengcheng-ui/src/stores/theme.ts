import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { darkTheme, type GlobalTheme } from 'naive-ui'

export const useThemeStore = defineStore('theme', () => {
  // 主题模式: dark / light
  const mode = ref<'dark' | 'light'>(
    (localStorage.getItem('layout-theme') as 'dark' | 'light') || 'light'
  )

  // 菜单位置
  const siderPosition = ref<'left' | 'right' | 'top'>(
    (localStorage.getItem('layout-position') as 'left' | 'right' | 'top') || 'left'
  )

  // 是否显示页签
  const showTabs = ref<boolean>(
    localStorage.getItem('layout-show-tabs') !== 'false'
  )

  // 是否是暗色主题
  const isDark = computed(() => mode.value === 'dark')

  // Naive UI 主题
  const naiveTheme = computed<GlobalTheme | null>(() => {
    return isDark.value ? darkTheme : null
  })

  // 设置主题
  function setMode(newMode: 'dark' | 'light') {
    mode.value = newMode
    localStorage.setItem('layout-theme', newMode)
    // 更新 body 类名
    updateBodyClass()
  }

  // 设置菜单位置
  function setSiderPosition(position: 'left' | 'right' | 'top') {
    siderPosition.value = position
    localStorage.setItem('layout-position', position)
  }

  // 设置是否显示页签
  function setShowTabs(show: boolean) {
    showTabs.value = show
    localStorage.setItem('layout-show-tabs', String(show))
  }

  // 更新 body 类名
  function updateBodyClass() {
    if (isDark.value) {
      document.body.classList.add('dark-theme')
      document.body.classList.remove('light-theme')
    } else {
      document.body.classList.add('light-theme')
      document.body.classList.remove('dark-theme')
    }
  }

  // 初始化时更新 body 类名
  updateBodyClass()

  return {
    mode,
    siderPosition,
    showTabs,
    isDark,
    naiveTheme,
    setMode,
    setSiderPosition,
    setShowTabs,
    updateBodyClass
  }
}, {
  persist: false // 使用 localStorage 手动管理
})
