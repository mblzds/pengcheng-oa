<template>
  <div class="approval-flow-container">
    <n-card>
      <template #header>
        <n-space justify="space-between" align="center">
          <span>审批流配置</span>
          <n-text depth="3" style="font-size: 12px">
            按业务类型配置请假/调休的审批节点链；提交申请时按"开始时一次性快照"原则解析候选审批人，后续人事变动不影响在途流程。
          </n-text>
        </n-space>
      </template>

      <n-tabs v-model:value="activeBusinessType" type="line" animated @update:value="onTabChange">
        <n-tab-pane name="leave" tab="请假审批流" />
        <n-tab-pane name="compensate" tab="调休审批流" />
      </n-tabs>

      <n-spin :show="loading">
        <n-space vertical :size="12">
          <n-alert v-if="nodes.length === 0" type="info" :show-icon="false">
            尚未配置任何节点，提交申请会失败。请添加至少一个节点。
          </n-alert>

          <div
            v-for="(node, idx) in nodes"
            :key="idx"
            class="node-row"
          >
            <div class="node-seq">
              <n-tag type="info" round>{{ idx + 1 }}</n-tag>
            </div>

            <div class="node-fields">
              <n-grid :cols="24" :x-gap="12" :y-gap="8">
                <n-gi :span="6">
                  <n-input
                    v-model:value="node.nodeName"
                    placeholder="节点名（如 直接上级）"
                    maxlength="32"
                  />
                </n-gi>
                <n-gi :span="6">
                  <n-select
                    v-model:value="node.approverType"
                    :options="approverTypeOptions"
                    @update:value="onApproverTypeChange(node)"
                  />
                </n-gi>
                <n-gi :span="12">
                  <!-- 直接上级：无需配置 -->
                  <n-text v-if="node.approverType === 'direct_supervisor'" depth="3">
                    自动解析为申请人 sys_user.leader_id（缺失时回退 sys_dept.leader_id）
                  </n-text>
                  <!-- 角色 -->
                  <n-select
                    v-else-if="node.approverType === 'role'"
                    :value="parseIds(node.approverValue)"
                    multiple
                    filterable
                    :options="roleOptions"
                    placeholder="选择角色"
                    @update:value="(v) => node.approverValue = (v || []).join(',')"
                  />
                  <!-- 用户 -->
                  <n-select
                    v-else-if="node.approverType === 'user'"
                    :value="parseIds(node.approverValue)"
                    multiple
                    filterable
                    :options="userOptions"
                    placeholder="选择审批人"
                    @update:value="(v) => node.approverValue = (v || []).join(',')"
                  />
                </n-gi>
              </n-grid>
            </div>

            <div class="node-actions">
              <n-button-group>
                <n-button size="small" :disabled="idx === 0" @click="moveUp(idx)">
                  <template #icon>
                    <n-icon><ArrowUpOutline /></n-icon>
                  </template>
                </n-button>
                <n-button size="small" :disabled="idx === nodes.length - 1" @click="moveDown(idx)">
                  <template #icon>
                    <n-icon><ArrowDownOutline /></n-icon>
                  </template>
                </n-button>
                <n-button size="small" type="error" @click="removeNode(idx)">
                  <template #icon>
                    <n-icon><TrashOutline /></n-icon>
                  </template>
                </n-button>
              </n-button-group>
            </div>
          </div>

          <n-button dashed block @click="addNode">
            <template #icon>
              <n-icon><AddOutline /></n-icon>
            </template>
            添加节点
          </n-button>

          <n-divider />

          <n-space justify="end">
            <n-button @click="loadNodes">取消</n-button>
            <n-button type="primary" :loading="saving" @click="onSave">保存</n-button>
          </n-space>
        </n-space>
      </n-spin>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { ArrowUpOutline, ArrowDownOutline, TrashOutline, AddOutline } from '@vicons/ionicons5'
import { approvalFlowApi, type ApprovalFlowNodeVO } from '@/api/approval'
import { roleApi, userApi, type SysRole, type SysUser } from '@/api/system'

const message = useMessage()

const activeBusinessType = ref<string>('leave')
const nodes = ref<ApprovalFlowNodeVO[]>([])
const loading = ref(false)
const saving = ref(false)

const approverTypeOptions = [
  { label: '直接上级', value: 'direct_supervisor' },
  { label: '指定角色', value: 'role' },
  { label: '指定用户', value: 'user' }
]

const roleOptions = ref<{ label: string; value: number }[]>([])
const userOptions = ref<{ label: string; value: number }[]>([])

onMounted(async () => {
  await Promise.all([loadRoleOptions(), loadUserOptions()])
  await loadNodes()
})

async function loadRoleOptions() {
  const list: SysRole[] = await roleApi.list()
  roleOptions.value = (list || []).map(r => ({ label: r.name || r.code || `角色#${r.id}`, value: r.id! }))
}

async function loadUserOptions() {
  // 拉取前 9999 条用户作为可选项；超大用户量场景可改为远程搜索
  const result = await userApi.page({ page: 1, pageSize: 9999 })
  const list: SysUser[] = result?.list || []
  userOptions.value = list.map(u => ({
    label: `${u.nickname || u.username}（${u.username}）`,
    value: u.id!
  }))
}

async function loadNodes() {
  loading.value = true
  try {
    const list = await approvalFlowApi.list(activeBusinessType.value)
    nodes.value = (list || []).map(n => ({
      ...n,
      // 后端返回的 enabled 默认 1
      enabled: n.enabled ?? 1
    }))
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  loadNodes()
}

function addNode() {
  nodes.value.push({
    businessType: activeBusinessType.value,
    nodeName: '',
    approverType: 'direct_supervisor',
    approverValue: null,
    enabled: 1
  })
}

function removeNode(idx: number) {
  nodes.value.splice(idx, 1)
}

function moveUp(idx: number) {
  if (idx <= 0) return
  const arr = nodes.value
  ;[arr[idx - 1], arr[idx]] = [arr[idx], arr[idx - 1]]
}

function moveDown(idx: number) {
  if (idx >= nodes.value.length - 1) return
  const arr = nodes.value
  ;[arr[idx], arr[idx + 1]] = [arr[idx + 1], arr[idx]]
}

function onApproverTypeChange(node: ApprovalFlowNodeVO) {
  // 切换审批人类型时清空旧值，避免脏数据落库
  node.approverValue = null
}

function parseIds(csv?: string | null): number[] {
  if (!csv) return []
  return csv.split(',').map(s => Number(s.trim())).filter(n => !Number.isNaN(n))
}

async function onSave() {
  if (nodes.value.length === 0) {
    message.warning('至少需要一个审批节点')
    return
  }
  for (const [i, n] of nodes.value.entries()) {
    if (!n.nodeName || !n.nodeName.trim()) {
      message.warning(`第 ${i + 1} 个节点未填写名称`)
      return
    }
    if ((n.approverType === 'role' || n.approverType === 'user') && !n.approverValue) {
      message.warning(`第 ${i + 1} 个节点【${n.nodeName}】未选择${n.approverType === 'role' ? '角色' : '审批人'}`)
      return
    }
  }
  saving.value = true
  try {
    await approvalFlowApi.save(activeBusinessType.value, nodes.value)
    message.success('保存成功')
    await loadNodes()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.approval-flow-container {
  padding: 16px;
}
.node-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 12px 16px;
  background: #FAFAFA;
  border: 1px solid #EBEEF5;
  border-radius: 6px;
}
.node-seq {
  flex-shrink: 0;
  padding-top: 6px;
}
.node-fields {
  flex: 1;
  min-width: 0;
}
.node-actions {
  flex-shrink: 0;
}
</style>
