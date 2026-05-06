<template>
  <div class="global-search-trigger" @click="showSearch = true">
    <n-icon size="18"><SearchOutline /></n-icon>
    <span class="search-placeholder">搜索客户、项目、消息...</span>
    <kbd>⌘K</kbd>
  </div>

  <n-modal v-model:show="showSearch" :mask-closable="true" :close-on-esc="true" transform-origin="center" preset="card" style="width: 640px; margin-top: 80px;" :bordered="false" :segmented="{ content: true }">
    <template #header>
      <n-input
        ref="searchInputRef"
        v-model:value="keyword"
        placeholder="搜索客户、项目、联盟商、消息、通知..."
        clearable
        size="large"
        @input="debouncedSearch"
      >
        <template #prefix>
          <n-icon size="20"><SearchOutline /></n-icon>
        </template>
      </n-input>
    </template>

    <div class="search-body">
      <!-- 加载中 -->
      <div v-if="loading" class="search-loading">
        <n-spin size="small" />
        <span>搜索中...</span>
      </div>

      <!-- 搜索结果 -->
      <template v-else-if="result && keyword">
        <div class="search-meta">
          找到 {{ result.totalCount }} 个结果（{{ result.costMs }}ms）
        </div>

        <div v-for="cat in result.categories" :key="cat.scope" class="search-category">
          <div class="category-header">
            <span class="category-label">{{ cat.label }}</span>
            <span class="category-count">{{ cat.count }}</span>
          </div>
          <div
            v-for="item in cat.items"
            :key="item.id"
            class="search-item"
            @click="navigateTo(item)"
          >
            <div class="item-icon">{{ typeIcon(item.type) }}</div>
            <div class="item-content">
              <div class="item-title" v-html="highlightKeyword(item.title)"></div>
              <div class="item-snippet" v-html="highlightKeyword(item.snippet)"></div>
            </div>
            <div class="item-time" v-if="item.updatedAt">{{ formatTime(item.updatedAt) }}</div>
          </div>
        </div>

        <n-empty v-if="result.categories.length === 0" description="未找到相关结果" />
      </template>

      <!-- 初始状态 -->
      <template v-else>
        <div v-if="searchHistory.length > 0" class="search-section">
          <div class="section-header">
            <span>最近搜索</span>
            <n-button text size="small" @click="searchHistory = []">清空</n-button>
          </div>
          <div class="history-tags">
            <n-tag
              v-for="kw in searchHistory"
              :key="kw"
              size="small"
              round
              class="history-tag"
              @click="quickSearch(kw)"
            >{{ kw }}</n-tag>
          </div>
        </div>
        <div v-if="hotSearches.length > 0" class="search-section">
          <div class="section-header">热门搜索</div>
          <div class="history-tags">
            <n-tag
              v-for="item in hotSearches"
              :key="item.keyword"
              size="small"
              round
              type="success"
              class="history-tag"
              @click="quickSearch(item.keyword)"
            >{{ item.keyword }}</n-tag>
          </div>
        </div>
      </template>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { NModal, NInput, NIcon, NTag, NButton, NSpin, NEmpty } from 'naive-ui'
import { SearchOutline } from '@vicons/ionicons5'
import { request } from '@/utils/request'

const router = useRouter()

const showSearch = ref(false)
const keyword = ref('')
const loading = ref(false)
const result = ref<any>(null)
const searchHistory = ref<string[]>([])
const hotSearches = ref<any[]>([])
const searchInputRef = ref()

let debounceTimer: NodeJS.Timeout | null = null

function debouncedSearch() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => doSearch(), 300)
}

async function doSearch() {
  if (!keyword.value?.trim()) {
    result.value = null
    return
  }
  loading.value = true
  try {
    const res = await request({ url: '/search', method: 'get', params: { q: keyword.value, scope: 'all' } })
    result.value = res
  } finally {
    loading.value = false
  }
}

function quickSearch(kw: string) {
  keyword.value = kw
  doSearch()
}

function navigateTo(item: any) {
  showSearch.value = false
  if (item.route) {
    router.push(item.route)
  }
}

function typeIcon(type: string): string {
  const icons: Record<string, string> = {
    customer: '👤',
    project: '🏢',
    alliance: '🤝',
    chat: '💬',
    notice: '📢',
    knowledge: '📚'
  }
  return icons[type] || '📄'
}

function highlightKeyword(text: string): string {
  if (!text || !keyword.value) return text || ''
  const kw = keyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${kw})`, 'gi'), '<mark>$1</mark>')
}

function formatTime(time: string): string {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

async function loadInitialData() {
  try {
    const [histRes, hotRes] = await Promise.all([
      request({ url: '/search/history', method: 'get', params: { limit: 8 } }),
      request({ url: '/search/hot', method: 'get', params: { limit: 10 } })
    ])
    searchHistory.value = histRes.data || []
    hotSearches.value = hotRes.data || []
  } catch { /* ignore */ }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
    e.preventDefault()
    showSearch.value = !showSearch.value
  }
}

watch(showSearch, async (val) => {
  if (val) {
    keyword.value = ''
    result.value = null
    loadInitialData()
    await nextTick()
    searchInputRef.value?.focus()
  }
})

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.global-search-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: #f5f5f5;
  border-radius: 8px;
  cursor: pointer;
  color: #999;
  font-size: 13px;
  transition: background 0.2s;
}
.global-search-trigger:hover {
  background: #e8e8e8;
}
.search-placeholder {
  flex: 1;
}
kbd {
  font-size: 11px;
  padding: 2px 6px;
  background: #e0e0e0;
  border-radius: 4px;
  color: #666;
}
.search-body {
  max-height: 500px;
  overflow-y: auto;
}
.search-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 0;
  color: #999;
}
.search-meta {
  font-size: 12px;
  color: #999;
  padding: 4px 0 12px;
}
.search-category {
  margin-bottom: 16px;
}
.category-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.category-label {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}
.category-count {
  font-size: 11px;
  color: #999;
  background: #f0f0f0;
  padding: 1px 6px;
  border-radius: 10px;
}
.search-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
}
.search-item:hover {
  background: #f5f5f5;
}
.item-icon {
  font-size: 18px;
  flex-shrink: 0;
  margin-top: 2px;
}
.item-content {
  flex: 1;
  min-width: 0;
}
.item-title {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-snippet {
  font-size: 12px;
  color: #888;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-time {
  font-size: 11px;
  color: #bbb;
  flex-shrink: 0;
}
.search-section {
  margin-bottom: 16px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}
.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.history-tag {
  cursor: pointer;
}
:deep(mark) {
  background: rgba(24, 160, 88, 0.2);
  color: #18a058;
  padding: 0 1px;
  border-radius: 2px;
}
</style>
