<template>
  <div class="approval-flow-container">
    <n-card>
      <template #header>
        <span>审批流配置</span>
      </template>

      <!-- 顶部使用说明（可折叠） -->
      <n-collapse :default-expanded-names="['intro']" style="margin-bottom: 12px">
        <n-collapse-item title="📖 配置说明（首次使用请展开）" name="intro">
          <ul class="intro-list">
            <li>节点按从上到下顺序<strong>串行审批</strong>，前一节点通过才进入下一节</li>
            <li>保存后立即对<strong>新提交</strong>的申请生效；已在审批中的单不受影响（提交时已快照审批人）</li>
            <li><strong>「审批人来源」</strong>决定 <em>谁来审</em>；<strong>「适用申请人角色」</strong>决定 <em>谁的单触发本节点</em>，留空 = 全员适用</li>
            <li>"适用申请人角色"是 <strong>加严</strong> 用的——只在"少数岗位才走"的节点上选；普通员工的链路 = 全员节点的并集，不需要专门为他们配</li>
            <li>配完想验证：在小程序提交一条对应类型的申请，进"申请记录"看实际流转</li>
          </ul>
        </n-collapse-item>
      </n-collapse>

      <div class="type-bar">
        <span class="type-bar-title">业务类型</span>
        <n-space>
          <n-button size="small" @click="openCreateTypeDialog">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            新增类型
          </n-button>
          <n-button size="small" :disabled="!currentTypeOption" @click="openEditTypeDialog">编辑</n-button>
          <n-button
            size="small"
            type="error"
            :disabled="!currentTypeOption || currentTypeOption.builtin === 1"
            @click="confirmDeleteType"
          >
            删除
          </n-button>
        </n-space>
      </div>

      <n-tabs v-model:value="activeBusinessType" type="line" animated @update:value="onTabChange">
        <n-tab-pane
          v-for="opt in businessTypeOptions"
          :key="opt.businessType"
          :name="opt.businessType"
          :tab="opt.builtin === 1 ? `${opt.label}审批流` : `${opt.label}审批流（自定义）`"
        />
      </n-tabs>

      <!-- 业务类型 新建 / 编辑 弹窗 -->
      <n-modal
        v-model:show="showTypeDialog"
        preset="card"
        :title="typeForm.id ? '编辑业务类型' : '新增业务类型'"
        style="width: 480px"
        :mask-closable="false"
      >
        <n-form label-placement="left" :label-width="120" require-mark-placement="right-hanging">
          <n-form-item label="key（business_type）" required>
            <n-input
              v-model:value="typeForm.businessType"
              placeholder="小写字母开头，如 travel / overtime"
              :disabled="typeForm.id != null && typeForm.builtin === 1"
              :input-props="{ maxlength: 64 }"
            />
          </n-form-item>
          <n-form-item label="显示名" required>
            <n-input v-model:value="typeForm.label" placeholder="如 出差申请" :input-props="{ maxlength: 64 }" />
          </n-form-item>
          <n-form-item label="说明">
            <n-input
              v-model:value="typeForm.description"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 4 }"
              placeholder="可选，用于运营自己备忘"
            />
          </n-form-item>
          <n-form-item label="排序">
            <n-input-number v-model:value="typeForm.sort" :min="0" :max="9999" placeholder="数字越小越靠前" />
          </n-form-item>
          <n-alert v-if="!typeForm.id" type="info" :show-icon="false" style="margin-top: 8px">
            新建类型仅完成 tab 注册；要让员工能在小程序提交申请，仍需后端为该 key 提供业务表与提交入口。
          </n-alert>
        </n-form>
        <template #footer>
          <n-space justify="end">
            <n-button @click="showTypeDialog = false">取消</n-button>
            <n-button type="primary" :loading="typeSaving" @click="onSaveType">保存</n-button>
          </n-space>
        </template>
      </n-modal>

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
              <n-tag type="info" round>节点 {{ idx + 1 }}</n-tag>
            </div>

            <div class="node-fields">
              <n-grid :cols="24" :x-gap="12" :y-gap="10">
                <!-- Row 1: 节点名 + 审批人来源 -->
                <n-gi :span="3" class="field-label">节点名</n-gi>
                <n-gi :span="9">
                  <n-auto-complete
                    v-model:value="node.nodeName"
                    :options="nodeNameSuggestions"
                    placeholder="如：直接上级 / HR 备案 / 总经理签字"
                    :input-props="{ maxlength: 32 }"
                  />
                </n-gi>
                <n-gi :span="3" class="field-label">审批人来源</n-gi>
                <n-gi :span="9">
                  <n-select
                    v-model:value="node.approverType"
                    :options="approverTypeOptions"
                    @update:value="onApproverTypeChange(node)"
                  />
                </n-gi>
                <!-- Row 2: 审批人值 -->
                <n-gi :span="3" class="field-label">审批人</n-gi>
                <n-gi :span="21">
                  <n-text v-if="node.approverType === 'direct_supervisor'" depth="3">
                    自动解析：优先 user.leader_id，缺失时回退 dept.leader_id，沿父部门回溯，自动跳过申请人本人。部门负责人申请时会自动找上级部门负责人。
                  </n-text>
                  <n-text v-else-if="node.approverType === 'applicant_dept_manager'" depth="3">
                    （历史遗留类型，已合并入"直接上级"，不建议新建）自动解析为申请人所在部门的负责人，自我排除 + 沿父部门回溯。
                  </n-text>
                  <n-select
                    v-else-if="node.approverType === 'role'"
                    :value="parseIds(node.approverValue)"
                    multiple
                    filterable
                    :options="roleOptions"
                    placeholder="选一个或多个角色，任一角色持有人都可审批"
                    @update:value="(v) => node.approverValue = (v || []).join(',')"
                  />
                  <n-select
                    v-else-if="node.approverType === 'user'"
                    :value="parseIds(node.approverValue)"
                    multiple
                    filterable
                    :options="userOptions"
                    placeholder="指定一个或多个具体用户审批"
                    @update:value="(v) => node.approverValue = (v || []).join(',')"
                  />
                </n-gi>
                <!-- Row 3: 适用申请人角色 -->
                <n-gi :span="3" class="field-label">适用申请人</n-gi>
                <n-gi :span="21">
                  <n-select
                    :value="parseIds(node.appliesToRoleIds)"
                    multiple
                    filterable
                    clearable
                    :options="roleOptions"
                    placeholder="留空 = 全员都走本节点；选角色 = 仅持有任一角色的申请人才触发"
                    @update:value="(v) => node.appliesToRoleIds = (v && v.length) ? v.join(',') : null"
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
import { ref, computed, onMounted } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import { ArrowUpOutline, ArrowDownOutline, TrashOutline, AddOutline } from '@vicons/ionicons5'
import { approvalFlowApi, type ApprovalFlowNodeVO, type BusinessTypeOption, type BusinessTypeInput } from '@/api/approval'
import { roleApi, userApi, type SysRole, type SysUser } from '@/api/system'

const message = useMessage()
const dialog = useDialog()

const activeBusinessType = ref<string>('leave')
const businessTypeOptions = ref<BusinessTypeOption[]>([])
const nodes = ref<ApprovalFlowNodeVO[]>([])
const loading = ref(false)
const saving = ref(false)

// 业务类型 新建/编辑 弹窗状态
const showTypeDialog = ref(false)
const typeSaving = ref(false)
const typeForm = ref<BusinessTypeInput & { builtin?: number }>({
  businessType: '',
  label: '',
  description: '',
  sort: 100
})

const currentTypeOption = computed<BusinessTypeOption | undefined>(() =>
  businessTypeOptions.value.find(o => o.businessType === activeBusinessType.value)
)

const approverTypeOptions = [
  { label: '直接上级', value: 'direct_supervisor' },
  { label: '指定角色', value: 'role' },
  { label: '指定用户', value: 'user' }
]

// 节点名常用建议（n-auto-complete 直接接收 string[]）
const nodeNameSuggestions = [
  '直接上级',
  '部门经理审批',
  'HR 备案',
  'HR 审批',
  '财务审核',
  '总经理签字',
  '总经理审批'
]

// 切换审批人来源时的智能默认节点名
const defaultNodeNameByType: Record<string, string> = {
  direct_supervisor: '直接上级'
}

const roleOptions = ref<{ label: string; value: number }[]>([])
const userOptions = ref<{ label: string; value: number }[]>([])

onMounted(async () => {
  await Promise.all([loadRoleOptions(), loadUserOptions(), loadBusinessTypes()])
  await loadNodes()
})

async function loadBusinessTypes() {
  const list = await approvalFlowApi.businessTypes()
  businessTypeOptions.value = list || []
  // 服务端无返回兜底：默认 leave；当前选中类型不在列表里时切到第一项
  if (businessTypeOptions.value.length === 0) {
    return
  }
  if (!businessTypeOptions.value.find(o => o.businessType === activeBusinessType.value)) {
    activeBusinessType.value = businessTypeOptions.value[0].businessType
  }
}

function openCreateTypeDialog() {
  typeForm.value = {
    businessType: '',
    label: '',
    description: '',
    sort: 100
  }
  showTypeDialog.value = true
}

function openEditTypeDialog() {
  const cur = currentTypeOption.value
  if (!cur) return
  typeForm.value = {
    id: cur.id,
    businessType: cur.businessType,
    label: cur.label,
    description: cur.description || '',
    sort: cur.sort ?? 100,
    builtin: cur.builtin
  }
  showTypeDialog.value = true
}

async function onSaveType() {
  const form = typeForm.value
  if (!form.businessType?.trim()) return message.error('key 不能为空')
  if (!/^[a-z][a-z0-9_]{0,63}$/.test(form.businessType)) {
    return message.error('key 仅支持小写字母 / 数字 / 下划线，且必须以字母开头')
  }
  if (!form.label?.trim()) return message.error('显示名不能为空')
  typeSaving.value = true
  try {
    if (form.id) {
      await approvalFlowApi.updateBusinessType(form.id, {
        id: form.id,
        businessType: form.businessType.trim(),
        label: form.label.trim(),
        description: form.description?.trim() || null,
        sort: form.sort
      })
      message.success('已保存')
    } else {
      await approvalFlowApi.createBusinessType({
        businessType: form.businessType.trim(),
        label: form.label.trim(),
        description: form.description?.trim() || null,
        sort: form.sort
      })
      message.success('已新建')
    }
    showTypeDialog.value = false
    const newKey = form.businessType.trim()
    await loadBusinessTypes()
    // 新建后切到新 tab；编辑后保持原 tab
    if (!form.id && businessTypeOptions.value.find(o => o.businessType === newKey)) {
      activeBusinessType.value = newKey
      await loadNodes()
    }
  } catch (err: any) {
    // 后端的 IllegalArgumentException 已被 GlobalExceptionHandler 转成 code=400+message
    message.error(err?.message || err?.msg || '保存失败')
  } finally {
    typeSaving.value = false
  }
}

function confirmDeleteType() {
  const cur = currentTypeOption.value
  if (!cur || cur.builtin === 1) return
  dialog.warning({
    title: '删除业务类型',
    content: `确定删除「${cur.label}」？已配置的节点不会自动清理，但前端不再展示该 tab。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await approvalFlowApi.deleteBusinessType(cur.id!)
        message.success('已删除')
        await loadBusinessTypes()
        if (businessTypeOptions.value.length > 0) {
          activeBusinessType.value = businessTypeOptions.value[0].businessType
          await loadNodes()
        }
      } catch (err: any) {
        message.error(err?.message || err?.msg || '删除失败')
      }
    }
  })
}

async function loadRoleOptions() {
  const list: SysRole[] = await roleApi.list()
  roleOptions.value = (list || []).map(r => ({ label: r.name || r.code || `角色#${r.id}`, value: r.id! }))
}

async function loadUserOptions() {
  // 用通讯录接口（权限码 sys:chat:list），避免 HR / 部门经理等非 admin 角色进入审批流配置时被 sys:user:list 拦截
  const result = await userApi.contactsPage({ page: 1, pageSize: 9999 })
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
  // 节点名为空时按类型给个建议默认值，让新手不用想"该叫啥"
  if (!node.nodeName || !node.nodeName.trim()) {
    const suggested = defaultNodeNameByType[node.approverType]
    if (suggested) node.nodeName = suggested
  }
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
.intro-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #555;
  line-height: 1.8;
}
.intro-list strong {
  color: #18a058;
}
.intro-list em {
  font-style: normal;
  color: #2080f0;
}
.field-label {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #666;
  justify-content: flex-end;
  padding-right: 4px;
}
.type-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.type-bar-title {
  font-size: 13px;
  color: #666;
}
</style>
