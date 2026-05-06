<template>
  <n-config-provider :theme="themeStore.naiveTheme" :theme-overrides="currentThemeOverrides">
    <n-loading-bar-provider>
      <n-message-provider>
        <n-dialog-provider>
          <GlobalApiProvider />
          <router-view />
          <Watermark />
        </n-dialog-provider>
      </n-message-provider>
    </n-loading-bar-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import type { GlobalThemeOverrides } from 'naive-ui'
import { useMessage, useDialog, useLoadingBar } from 'naive-ui'
import { defineComponent, computed, onMounted } from 'vue'
import { useThemeStore } from '@/stores/theme'
import Watermark from '@/components/Watermark/index.vue'

const themeStore = useThemeStore()

// 确保主题状态正确应用到 body
onMounted(() => {
  themeStore.updateBodyClass()
})

// 立即更新 body 类名（确保在渲染前应用）
themeStore.updateBodyClass()

// 注入全局API
const GlobalApiProvider = defineComponent({
  setup() {
    window.$message = useMessage()
    window.$dialog = useDialog()
    window.$loadingBar = useLoadingBar()
    return () => null
  }
})

// 亮色主题配置
const lightThemeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#111827',
    primaryColorHover: '#000000',
    primaryColorPressed: '#1F2937',
    primaryColorSuppl: '#374151',
    warningColor: '#111827',
    warningColorHover: '#000000',
    warningColorPressed: '#1F2937',
    warningColorSuppl: '#374151',
    infoColor: '#111827',
    infoColorHover: '#000000',
    infoColorPressed: '#1F2937',
    infoColorSuppl: '#374151',
    successColor: '#111827',
    successColorHover: '#000000',
    successColorPressed: '#1F2937',
    successColorSuppl: '#374151',
    textColorBase: '#1F2937',
    textColor1: '#1F2937',
    textColor2: '#6B7280',
    textColor3: '#9CA3AF',
    borderColor: '#E5E7EB',
    dividerColor: '#E5E7EB',
    inputColor: '#F9FAFB',
    tableColor: '#FFFFFF',
    cardColor: '#FFFFFF',
    modalColor: '#FFFFFF',
    bodyColor: '#F3F4F6',
    hoverColor: '#F3F4F6',
    borderRadius: '8px',
    borderRadiusSmall: '6px'
  },
  Button: {
    borderRadiusMedium: '8px',
    borderRadiusSmall: '6px',
    heightMedium: '36px'
  },
  Card: {
    borderRadius: '12px',
    paddingMedium: '20px',
    titleFontSizeMedium: '16px'
  },
  DataTable: {
    borderRadius: '12px',
    thColor: '#F9FAFB',
    thTextColor: '#6B7280',
    thFontWeight: '600',
    tdColor: '#FFFFFF'
  },
  Input: {
    borderRadius: '8px',
    heightMedium: '36px'
  },
  Form: {
    labelFontSizeTopMedium: '14px',
    labelTextColor: '#374151'
  },
  Menu: {
    itemHeight: '44px',
    borderRadius: '8px',
    itemColorActive: '#F3F4F6',
    itemColorActiveHover: '#E5E7EB',
    itemTextColorActive: '#111827',
    itemTextColorActiveHover: '#111827',
    itemIconColorActive: '#111827',
    itemIconColorActiveHover: '#111827'
  },
  Tag: {
    borderRadius: '6px'
  },
  Dialog: {
    borderRadius: '12px'
  }
}

// 暗色主题配置
const darkThemeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#60A5FA',
    primaryColorHover: '#93C5FD',
    primaryColorPressed: '#3B82F6',
    primaryColorSuppl: '#2563EB',
    bodyColor: '#101014',
    cardColor: '#18181c',
    modalColor: '#18181c',
    popoverColor: '#27272a',
    tableColor: '#18181c',
    inputColor: '#27272a',
    borderColor: '#3f3f46',
    dividerColor: '#3f3f46',
    hoverColor: '#27272a',
    borderRadius: '8px',
    borderRadiusSmall: '6px'
  },
  Button: {
    borderRadiusMedium: '8px',
    borderRadiusSmall: '6px',
    heightMedium: '36px',
    colorSecondary: '#27272a',
    colorSecondaryHover: '#3f3f46',
    colorSecondaryPressed: '#52525b'
  },
  Card: {
    borderRadius: '12px',
    paddingMedium: '20px',
    titleFontSizeMedium: '16px',
    color: '#18181c',
    borderColor: '#3f3f46'
  },
  DataTable: {
    borderRadius: '12px',
    thFontWeight: '600',
    thColor: '#262629',
    tdColor: '#18181c',
    tdColorHover: '#262629',
    borderColor: '#3f3f46'
  },
  Input: {
    borderRadius: '8px',
    heightMedium: '36px',
    color: '#27272a',
    colorFocus: '#27272a',
    border: '1px solid #3f3f46',
    borderHover: '1px solid #52525b',
    borderFocus: '1px solid #60A5FA'
  },
  Form: {
    labelFontSizeTopMedium: '14px'
  },
  Menu: {
    itemHeight: '44px',
    borderRadius: '8px',
    color: '#18181c',
    itemColorActive: '#27272a',
    itemColorActiveHover: '#3f3f46'
  },
  Tag: {
    borderRadius: '6px'
  },
  Dialog: {
    borderRadius: '12px',
    color: '#18181c'
  },
  Popover: {
    color: '#27272a'
  },
  Dropdown: {
    color: '#27272a'
  },
  InternalSelection: {
    color: '#27272a',
    colorActive: '#27272a',
    border: '1px solid #3f3f46',
    borderHover: '1px solid #52525b',
    borderActive: '1px solid #60A5FA',
    borderFocus: '1px solid #60A5FA'
  }
}

// 根据当前主题选择配置
const currentThemeOverrides = computed(() => {
  return themeStore.isDark ? darkThemeOverrides : lightThemeOverrides
})
</script>
