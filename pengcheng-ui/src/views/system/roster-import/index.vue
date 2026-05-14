<template>
  <div class="roster-import-page">
    <n-card>
      <template #header>
        <n-space justify="space-between" align="center">
          <span>员工花名册批量导入</span>
          <n-button text type="primary" @click="downloadTemplate">
            <template #icon><n-icon><DownloadOutline /></n-icon></template>
            下载 CSV 模板
          </n-button>
        </n-space>
      </template>

      <!-- 步骤指示 -->
      <n-steps :current="currentStep" size="small" style="margin-bottom: 20px">
        <n-step title="选择文件" />
        <n-step title="预览校验" />
        <n-step title="确认导入" />
      </n-steps>

      <!-- Step 1: 上传 -->
      <div v-if="currentStep === 1">
        <n-upload
          :default-upload="false"
          :max="1"
          accept=".csv"
          @change="onFileChange"
        >
          <n-upload-dragger>
            <div style="margin-bottom: 12px">
              <n-icon size="48" :depth="3"><CloudUploadOutline /></n-icon>
            </div>
            <n-text style="font-size: 16px">点击或拖拽 CSV 文件到此处</n-text>
            <n-p depth="3" style="margin: 8px 0 0">
              请按下载的模板填写。表头必须保留，数据行从第 2 行开始。
            </n-p>
          </n-upload-dragger>
        </n-upload>

        <n-collapse style="margin-top: 16px">
          <n-collapse-item title="📖 字段说明（首次使用展开看）" name="hint">
            <ul class="intro-list">
              <li><strong>工号</strong>：必填，唯一。重复导入时按工号 upsert（已存在则更新）</li>
              <li><strong>姓名</strong>：必填。直接上级姓名引用此字段，需确保不重名</li>
              <li><strong>登录名</strong>：可空，空则使用员工姓名作为登录名（同名同姓需在此列显式填写区分）</li>
              <li><strong>手机</strong>：在职员工必填，11 位数字。与系统内已有冲突会拒绝</li>
              <li><strong>部门路径</strong>：以 / 分隔，如 <code>朋诚科技/技术部/后端组</code>。不存在的部门会自动创建</li>
              <li><strong>本部门负责人</strong>：Y / N。Y 时该用户会被设为对应部门的 leader</li>
              <li><strong>直接上级姓名</strong>：可空。空时审批流默认走部门负责人；填了则按指定上级</li>
              <li><strong>角色</strong>：合法值 <code>超级管理员 / 部门经理 / HR / 人事 / 财务 / 普通员工</code>，多个用 <code>;</code> 分隔。审批流标签由系统按规则自动补</li>
              <li><strong>入职日期</strong>：格式 <code>YYYY-MM-DD</code>，如 <code>2024-06-01</code>。考勤月报会用此日期作为「考勤起算日」，早于该日期的工作日不算缺勤。可空，但建议填上，缺失会回退到系统级「考勤启用日期」</li>
              <li><strong>状态</strong>：在职 / 离职。离职会软删该用户</li>
              <li><strong>初始密码</strong>：固定 <code>123456</code>，新员工首次登录后建议改密</li>
            </ul>
          </n-collapse-item>
        </n-collapse>
      </div>

      <!-- Step 2: 预览 -->
      <div v-if="currentStep === 2 && preview">
        <n-grid :cols="4" :x-gap="12" style="margin-bottom: 16px">
          <n-gi>
            <n-statistic label="总行数" :value="preview.totalRows" />
          </n-gi>
          <n-gi>
            <n-statistic label="校验通过" :value="preview.validRows" :value-style="{ color: '#18a058' }" />
          </n-gi>
          <n-gi>
            <n-statistic label="校验失败" :value="preview.errorRows" :value-style="{ color: preview.errorRows ? '#d03050' : '#999' }" />
          </n-gi>
          <n-gi>
            <n-statistic label="新建部门" :value="preview.deptsToCreate" :value-style="{ color: '#2080f0' }" />
          </n-gi>
        </n-grid>

        <n-grid :cols="3" :x-gap="12" style="margin-bottom: 20px">
          <n-gi>
            <n-statistic label="将新建用户" :value="preview.usersToCreate" :value-style="{ color: '#18a058' }" />
          </n-gi>
          <n-gi>
            <n-statistic label="将更新用户" :value="preview.usersToUpdate" :value-style="{ color: '#2080f0' }" />
          </n-gi>
          <n-gi>
            <n-statistic label="将软删（离职）" :value="preview.usersToDeactivate" :value-style="{ color: '#f0a020' }" />
          </n-gi>
        </n-grid>

        <n-alert v-if="preview.deptsToCreate > 0" type="info" :show-icon="false" style="margin-bottom: 12px">
          <template #header>将新建以下部门</template>
          <div style="font-family: monospace; font-size: 12px">
            {{ preview.deptsToCreateList.join(', ') }}
          </div>
        </n-alert>

        <n-alert
          v-if="preview.errorRows > 0"
          type="error"
          :show-icon="false"
          style="margin-bottom: 12px"
        >
          <template #header>{{ preview.errorRows }} 行校验失败，将被跳过</template>
          <n-data-table
            :columns="errorColumns"
            :data="preview.errors"
            :pagination="{ pageSize: 10 }"
            size="small"
            style="margin-top: 8px"
          />
          <n-button text size="small" type="primary" style="margin-top: 8px" @click="downloadErrorReport(preview.errors)">
            下载错误报告 CSV
          </n-button>
        </n-alert>

        <n-alert v-else type="success" :show-icon="false" style="margin-bottom: 12px">
          全部校验通过，可以安全导入。
        </n-alert>

        <n-space justify="end">
          <n-button @click="reset">重新选择</n-button>
          <n-button type="primary" :disabled="preview.validRows === 0" :loading="importing" @click="confirmImport">
            确认导入（{{ preview.validRows }} 行）
          </n-button>
        </n-space>
      </div>

      <!-- Step 3: 完成 -->
      <div v-if="currentStep === 3 && result">
        <n-result status="success" title="导入完成">
          <template #footer>
            <n-grid :cols="4" :x-gap="12" style="margin: 16px 0">
              <n-gi><n-statistic label="新建部门" :value="result.deptsCreated" /></n-gi>
              <n-gi><n-statistic label="新建用户" :value="result.usersCreated" /></n-gi>
              <n-gi><n-statistic label="更新用户" :value="result.usersUpdated" /></n-gi>
              <n-gi><n-statistic label="离职软删" :value="result.usersDeactivated" /></n-gi>
            </n-grid>
            <n-alert v-if="result.usersCreated > 0" type="info" :show-icon="false" style="margin-bottom: 12px">
              新员工初始密码：<code>{{ result.defaultPassword }}</code>，请通知员工首次登录后修改
            </n-alert>
            <n-alert v-if="result.errorRows > 0" type="warning" :show-icon="false" style="margin-bottom: 12px">
              {{ result.errorRows }} 行被跳过，<n-button text type="primary" @click="downloadErrorReport(result.errors)">下载错误报告</n-button>
            </n-alert>
            <n-space justify="center">
              <n-button @click="reset">再导一份</n-button>
              <n-button type="primary" @click="goToUserList">前往用户列表</n-button>
            </n-space>
          </template>
        </n-result>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, h } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, type DataTableColumns } from 'naive-ui'
import { CloudUploadOutline, DownloadOutline } from '@vicons/ionicons5'
import { rosterApi, type RosterPreviewVO, type RosterImportResultVO, type RosterRowError } from '@/api/roster'

const message = useMessage()
const router = useRouter()

const currentStep = ref(1)
const selectedFile = ref<File | null>(null)
const preview = ref<RosterPreviewVO | null>(null)
const result = ref<RosterImportResultVO | null>(null)
const importing = ref(false)

const errorColumns: DataTableColumns<RosterRowError> = [
  { title: '行号', key: 'lineNo', width: 70 },
  { title: '工号', key: 'employeeNo', width: 100 },
  { title: '姓名', key: 'name', width: 100 },
  { title: '错误原因', key: 'reason' }
]

async function onFileChange({ file }: any) {
  if (!file?.file) return
  selectedFile.value = file.file as File
  try {
    preview.value = await rosterApi.preview(selectedFile.value)
    currentStep.value = 2
  } catch (err: any) {
    message.error(err?.message || '解析失败')
  }
}

async function confirmImport() {
  if (!selectedFile.value) return
  importing.value = true
  try {
    result.value = await rosterApi.importRoster(selectedFile.value)
    currentStep.value = 3
    message.success('导入完成')
  } catch (err: any) {
    message.error(err?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

function reset() {
  currentStep.value = 1
  selectedFile.value = null
  preview.value = null
  result.value = null
}

async function downloadTemplate() {
  try {
    await rosterApi.downloadTemplate()
  } catch (err: any) {
    message.error(err?.message || '下载模板失败')
  }
}

function downloadErrorReport(errors: RosterRowError[]) {
  const header = '行号,工号,姓名,错误原因\n'
  const body = errors.map(e =>
    `${e.lineNo},${csvEscape(e.employeeNo || '')},${csvEscape(e.name || '')},${csvEscape(e.reason || '')}`
  ).join('\n')
  const bom = '﻿'
  const blob = new Blob([bom + header + body], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `导入错误报告-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function csvEscape(s: string): string {
  if (s.includes(',') || s.includes('"') || s.includes('\n')) {
    return '"' + s.replace(/"/g, '""') + '"'
  }
  return s
}

function goToUserList() {
  router.push('/system/user')
}
</script>

<style scoped>
.roster-import-page {
  padding: 16px;
}
.intro-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #555;
  line-height: 2;
}
.intro-list code {
  background: #f5f5f5;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 12px;
}
.intro-list strong {
  color: #18a058;
}
</style>
