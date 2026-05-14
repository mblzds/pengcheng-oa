import App from './App.vue'
import { createSSRApp } from 'vue'
import uviewPlus from 'uview-plus'
import PcIcon from './components/pc-icon/pc-icon.vue'

export function createApp() {
  const app = createSSRApp(App)
  app.use(uviewPlus)
  app.component('pc-icon', PcIcon)
  return {
    app
  }
}
