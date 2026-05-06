<template>
  <div class="chat-sidebar">
    <!-- 搜索框 -->
    <div class="sidebar-header">
      <n-input v-model:value="searchKeyword" placeholder="搜索" clearable size="small">
        <template #prefix>
          <n-icon><SearchOutline /></n-icon>
        </template>
      </n-input>
    </div>

    <!-- 消息分组筛选 -->
    <div class="filter-tabs">
      <div
        v-for="ft in filterTabs"
        :key="ft.key"
        class="filter-tab"
        :class="{ active: activeFilter === ft.key }"
        @click="activeFilter = ft.key"
      >
        {{ ft.label }}
        <span v-if="ft.count > 0" class="filter-count">{{ ft.count > 99 ? '99+' : ft.count }}</span>
      </div>
    </div>

    <!-- 标签切换 -->
    <div class="sidebar-tabs" v-if="activeFilter === 'all' || activeFilter === 'private' || activeFilter === 'group'">
      <div class="tab-item" :class="{ active: activeTab === 'private' }" @click="$emit('update:activeTab', 'private')">
        私聊
        <span v-if="totalPrivateUnread > 0" class="unread-total">{{ totalPrivateUnread }}</span>
      </div>
      <div class="tab-item" :class="{ active: activeTab === 'group' }" @click="$emit('update:activeTab', 'group')">
        群聊
        <span v-if="totalGroupUnread > 0" class="unread-total">{{ totalGroupUnread }}</span>
      </div>
      <n-button v-if="activeTab === 'group'" text size="small" type="primary" @click="$emit('createGroup')">
        <template #icon><n-icon><AddOutline /></n-icon></template>
      </n-button>
    </div>

    <!-- 私聊列表 -->
    <div v-if="activeTab === 'private'" class="contact-list">
      <!-- 特别关注置顶区 -->
      <template v-if="focusedUsers.length > 0 && activeFilter !== 'unread'">
        <div class="section-label">特别关注</div>
        <div
          v-for="user in focusedUsers"
          :key="'f-' + user.id"
          class="contact-item focus-item"
          :class="{ active: selectedId === user.id }"
          @click="$emit('selectUser', user)"
          @contextmenu.prevent="showContextMenu($event, user, 'private')"
        >
          <div class="avatar-wrapper">
            <n-avatar round size="small" :src="user.avatar || undefined">
              {{ user.nickname?.charAt(0) || 'U' }}
            </n-avatar>
            <span v-if="onlineStatus[user.id]" class="online-indicator"></span>
          </div>
          <div class="contact-info">
            <div class="contact-header">
              <span class="contact-name">
                <n-icon v-if="user.priority === 2" size="12" color="#d03050" style="margin-right: 2px"><FlameOutline /></n-icon>
                {{ user.nickname }}
              </span>
              <span class="contact-time" v-if="user.lastMessageTime">{{ formatTime(user.lastMessageTime) }}</span>
            </div>
            <div class="contact-last-msg" :class="{ 'urgent-msg': user.priority === 2 }">
              <span v-if="user.lastMessage">{{ truncate(user.lastMessage, 20) }}</span>
              <span v-else class="no-message">暂无消息</span>
            </div>
          </div>
          <div v-if="user.unreadCount && user.unreadCount > 0" class="unread-badge">
            {{ user.unreadCount > 99 ? '99+' : user.unreadCount }}
          </div>
        </div>
        <div class="section-label">消息</div>
      </template>

      <div
        v-for="user in displayUsers"
        :key="user.id"
        class="contact-item"
        :class="{ active: selectedId === user.id, blocked: user.isBlocked, 'muted-item': user.category === 'muted' }"
        @click="$emit('selectUser', user)"
        @contextmenu.prevent="showContextMenu($event, user, 'private')"
      >
        <div class="avatar-wrapper">
          <n-avatar round size="small" :src="user.avatar || undefined">
            {{ user.nickname?.charAt(0) || 'U' }}
          </n-avatar>
          <span v-if="onlineStatus[user.id] && !user.isBlocked" class="online-indicator"></span>
        </div>
        <div class="contact-info">
          <div class="contact-header">
            <span class="contact-name" :class="{ 'blocked-text': user.isBlocked }">
              <n-icon v-if="user.priority === 2" size="12" color="#d03050" style="margin-right: 2px"><FlameOutline /></n-icon>
              {{ user.nickname }}
            </span>
            <span class="contact-time" v-if="user.lastMessageTime">{{ formatTime(user.lastMessageTime) }}</span>
          </div>
          <div class="contact-last-msg" :class="{ 'urgent-msg': user.priority === 2 }">
            <span v-if="user.isBlocked" class="blocked-msg">已拉黑</span>
            <span v-else-if="user.lastMessage">{{ truncate(user.lastMessage, 20) }}</span>
            <span v-else class="no-message">暂无消息</span>
          </div>
        </div>
        <div v-if="user.unreadCount && user.unreadCount > 0" class="unread-badge" :class="{ 'urgent-badge': user.priority === 2 }">
          {{ user.unreadCount > 99 ? '99+' : user.unreadCount }}
        </div>
      </div>
      <n-empty v-if="displayUsers.length === 0 && focusedUsers.length === 0" description="暂无联系人" size="small" />
    </div>

    <!-- 群聊列表 -->
    <div v-else class="contact-list">
      <div
        v-for="group in displayGroups"
        :key="group.id"
        class="contact-item"
        :class="{ active: selectedId === group.id, 'muted-item': group.category === 'muted' }"
        @click="$emit('selectGroup', group)"
        @contextmenu.prevent="showContextMenu($event, group, 'group')"
      >
        <n-avatar round size="small" :style="{ background: '#18a058' }">
          {{ group.name?.charAt(0) || 'G' }}
        </n-avatar>
        <div class="contact-info">
          <div class="contact-header">
            <span class="contact-name">
              <n-icon v-if="group.priority === 2" size="12" color="#d03050" style="margin-right: 2px"><FlameOutline /></n-icon>
              {{ group.name }}
            </span>
            <span class="contact-time" v-if="group.lastMessageTime">{{ formatTime(group.lastMessageTime) }}</span>
            <span v-else class="member-count-badge">{{ group.memberCount }}人</span>
          </div>
          <div class="contact-last-msg" :class="{ 'urgent-msg': group.priority === 2 }">
            <span v-if="group.lastMessage">{{ truncate(group.lastMessage, 20) }}</span>
            <span v-else class="no-message">暂无消息</span>
          </div>
        </div>
        <div v-if="group.unreadCount && group.unreadCount > 0" class="unread-badge" :class="{ 'urgent-badge': group.priority === 2 }">
          {{ group.unreadCount > 99 ? '99+' : group.unreadCount }}
        </div>
      </div>
      <n-empty v-if="displayGroups.length === 0" description="暂无群聊" size="small" />
    </div>

    <!-- 右键菜单 -->
    <n-dropdown
      :show="contextMenuVisible"
      :options="contextMenuOptions"
      :x="contextMenuX"
      :y="contextMenuY"
      placement="bottom-start"
      @clickoutside="contextMenuVisible = false"
      @select="handleContextAction"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NInput, NIcon, NButton, NAvatar, NEmpty, NDropdown } from 'naive-ui'
import { SearchOutline, AddOutline, FlameOutline } from '@vicons/ionicons5'
import { request } from '@/utils/request'

interface User {
  id: number
  nickname: string
  avatar?: string
  isBlocked?: boolean
  lastMessage?: string
  lastMessageTime?: string
  unreadCount?: number
  category?: string
  priority?: number
}

interface Group {
  id: number
  name: string
  memberCount: number
  lastMessage?: string
  lastMessageTime?: string
  unreadCount?: number
  category?: string
  priority?: number
}

const props = defineProps<{
  users: User[]
  groups: Group[]
  onlineStatus: Record<number, boolean>
  activeTab: 'private' | 'group'
  selectedId?: number
}>()

const emit = defineEmits<{
  'update:activeTab': [tab: 'private' | 'group']
  selectUser: [user: User]
  selectGroup: [group: Group]
  createGroup: []
}>()

const searchKeyword = ref('')
const activeFilter = ref<string>('all')

const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
let contextTarget: any = null
let contextType: 'private' | 'group' = 'private'

/** 根据当前会话的标签动态生成右键菜单：有标签时突出「删除/取消」选项 */
const contextMenuOptions = computed(() => {
  if (!contextTarget) return []
  const cat = contextTarget.category
  const options: { label: string; key: string }[] = []
  if (cat === 'focus') {
    options.push({ label: '取消关注', key: 'normal' })
    options.push({ label: '静音', key: 'muted' })
  } else if (cat === 'muted') {
    options.push({ label: '取消静音', key: 'normal' })
    options.push({ label: '设为特别关注', key: 'focus' })
  } else if (cat === 'starred') {
    options.push({ label: '取消星标', key: 'normal' })
    options.push({ label: '静音', key: 'muted' })
  } else {
    options.push({ label: '设为特别关注', key: 'focus' })
    options.push({ label: '设为星标', key: 'starred' })
    options.push({ label: '静音', key: 'muted' })
  }
  return options
})

function showContextMenu(e: MouseEvent, target: any, type: 'private' | 'group') {
  contextTarget = target
  contextType = type
  contextMenuX.value = e.clientX
  contextMenuY.value = e.clientY
  contextMenuVisible.value = true
}

async function handleContextAction(key: string) {
  contextMenuVisible.value = false
  if (!contextTarget) return
  try {
    await request({
      url: '/sys/chat/category',
      method: key === 'normal' ? 'delete' : 'post',
      ...(key === 'normal'
        ? { params: { chatType: contextType, targetId: contextTarget.id } }
        : { data: { chatType: contextType, targetId: contextTarget.id, category: key } }
      )
    })
    contextTarget.category = key === 'normal' ? undefined : key
  } catch { /* ignore */ }
}

const filterTabs = computed(() => [
  { key: 'all', label: '全部', count: totalPrivateUnread.value + totalGroupUnread.value },
  { key: 'unread', label: '未读', count: unreadChatsCount.value },
  { key: 'focus', label: '关注', count: focusedCount.value },
  { key: 'private', label: '单聊', count: totalPrivateUnread.value },
  { key: 'group', label: '群聊', count: totalGroupUnread.value }
])

const unreadChatsCount = computed(() =>
  props.users.filter(u => (u.unreadCount || 0) > 0).length +
  props.groups.filter(g => (g.unreadCount || 0) > 0).length
)

const focusedCount = computed(() =>
  props.users.filter(u => u.category === 'focus').length +
  props.groups.filter(g => g.category === 'focus').length
)

const focusedUsers = computed(() =>
  props.users.filter(u => u.category === 'focus')
)

const filteredUsers = computed(() => {
  let list = props.users
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(u => u.nickname?.toLowerCase().includes(kw))
  }
  return list
})

const filteredGroups = computed(() => {
  let list = props.groups
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(g => g.name?.toLowerCase().includes(kw))
  }
  return list
})

const displayUsers = computed(() => {
  let list = filteredUsers.value
  switch (activeFilter.value) {
    case 'unread':
      list = list.filter(u => (u.unreadCount || 0) > 0)
      break
    case 'focus':
      list = list.filter(u => u.category === 'focus')
      break
  }
  return list.filter(u => u.category !== 'focus').sort((a, b) => {
    if ((b.priority || 0) !== (a.priority || 0)) return (b.priority || 0) - (a.priority || 0)
    return 0
  })
})

const displayGroups = computed(() => {
  let list = filteredGroups.value
  switch (activeFilter.value) {
    case 'unread':
      list = list.filter(g => (g.unreadCount || 0) > 0)
      break
    case 'focus':
      list = list.filter(g => g.category === 'focus')
      break
  }
  return list.sort((a, b) => {
    if ((b.priority || 0) !== (a.priority || 0)) return (b.priority || 0) - (a.priority || 0)
    return 0
  })
})

const totalPrivateUnread = computed(() =>
  props.users.reduce((sum, u) => sum + (u.unreadCount || 0), 0)
)

const totalGroupUnread = computed(() =>
  props.groups.reduce((sum, g) => sum + (g.unreadCount || 0), 0)
)

function formatTime(time: string): string {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) {
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) {
    return '昨天'
  }
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function truncate(text: string, maxLen: number): string {
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}
</script>

<style scoped>
.chat-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  border-right: 1px solid #e8e8e8;
  background: #fff;
}
.sidebar-header {
  padding: 12px;
}
.sidebar-tabs {
  display: flex;
  align-items: center;
  padding: 0 12px 8px;
  gap: 4px;
}
.tab-item {
  padding: 4px 12px;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  color: #666;
  position: relative;
}
.tab-item.active {
  color: #18a058;
  font-weight: 500;
  background: rgba(24, 160, 88, 0.08);
}
.unread-total {
  position: absolute;
  top: -4px;
  right: -8px;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  text-align: center;
  font-size: 10px;
  color: #fff;
  background: #d03050;
  border-radius: 8px;
  padding: 0 4px;
}
.contact-list {
  flex: 1;
  overflow-y: auto;
}
.contact-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  gap: 10px;
  position: relative;
}
.contact-item:hover {
  background: #f5f5f5;
}
.contact-item.active {
  background: rgba(24, 160, 88, 0.08);
}
.contact-item.blocked {
  opacity: 0.6;
}
.avatar-wrapper {
  position: relative;
}
.online-indicator {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 8px;
  height: 8px;
  background: #18a058;
  border-radius: 50%;
  border: 2px solid #fff;
}
.contact-info {
  flex: 1;
  min-width: 0;
}
.contact-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.contact-name {
  font-size: 14px;
  font-weight: 500;
}
.blocked-text {
  text-decoration: line-through;
  color: #999;
}
.contact-time {
  font-size: 11px;
  color: #999;
}
.member-count-badge {
  font-size: 11px;
  color: #999;
}
.contact-last-msg {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}
.blocked-msg {
  color: #d03050;
}
.filter-tabs {
  display: flex;
  padding: 0 8px 6px;
  gap: 2px;
  border-bottom: 1px solid #f0f0f0;
}
.filter-tab {
  padding: 3px 8px;
  font-size: 12px;
  cursor: pointer;
  border-radius: 12px;
  color: #999;
  position: relative;
  white-space: nowrap;
}
.filter-tab.active {
  color: #18a058;
  background: rgba(24, 160, 88, 0.08);
  font-weight: 500;
}
.filter-count {
  font-size: 10px;
  color: #d03050;
  margin-left: 2px;
}
.section-label {
  padding: 6px 12px 2px;
  font-size: 11px;
  color: #999;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.focus-item {
  border-left: 3px solid #f0a020;
}
.muted-item {
  opacity: 0.5;
}
.urgent-msg {
  color: #d03050 !important;
  font-weight: 500;
}
.urgent-badge {
  background: #d03050 !important;
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}
.no-message {
  color: #ccc;
}
.unread-badge {
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  font-size: 11px;
  color: #fff;
  background: #d03050;
  border-radius: 9px;
  padding: 0 5px;
  flex-shrink: 0;
}
</style>
