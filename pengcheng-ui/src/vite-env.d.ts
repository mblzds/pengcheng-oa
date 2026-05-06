/// <reference types="vite/client" />

import 'axios'

declare global {
  interface Window {
    $message: import('naive-ui').MessageApi
    $dialog: import('naive-ui').DialogApi
    $loadingBar: import('naive-ui').LoadingBarApi
  }
}

declare module 'axios' {
  interface InternalAxiosRequestConfig {
    _silent?: boolean
  }
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
