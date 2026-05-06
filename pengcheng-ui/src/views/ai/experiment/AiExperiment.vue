<template>
  <div class="page-container">
    <n-space vertical :size="16">
      <n-card title="实验统计查询">
        <n-form inline :model="queryForm">
          <n-form-item label="时间范围">
            <n-date-picker v-model:value="queryForm.dateRange" type="daterange" clearable style="width: 280px" />
          </n-form-item>
          <n-form-item label="场景">
            <n-select
              v-model:value="queryForm.scene"
              :options="sceneOptions"
              clearable
              placeholder="全部场景"
              style="width: 180px"
            />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" :loading="statsLoading" @click="loadStats">刷新统计</n-button>
          </n-form-item>
        </n-form>
      </n-card>

      <n-grid cols="1 s:2 m:4" :x-gap="12" :y-gap="12">
        <n-gi>
          <n-card>
            <n-statistic label="路由成功率" :value="toPercent(routeStats.summary.successRate)" suffix="%" />
          </n-card>
        </n-gi>
        <n-gi>
          <n-card>
            <n-statistic label="路由平均耗时" :value="routeStats.summary.avgLatencyMs" suffix="ms" />
          </n-card>
        </n-gi>
        <n-gi>
          <n-card>
            <n-statistic label="提示词成功率" :value="toPercent(promptStats.summary.successRate)" suffix="%" />
          </n-card>
        </n-gi>
        <n-gi>
          <n-card>
            <n-statistic label="提示词平均耗时" :value="promptStats.summary.avgLatencyMs" suffix="ms" />
          </n-card>
        </n-gi>
      </n-grid>

      <n-card title="实验保护状态">
        <n-space vertical :size="12">
          <n-grid cols="1 m:2" :x-gap="12" :y-gap="8">
            <n-gi>
              <div class="guard-row">
                <span class="guard-label">路由实验状态</span>
                <n-tag :type="guardStatus.routeExperimentAllowed ? 'success' : 'error'">
                  {{ guardStatus.routeExperimentAllowed ? '允许' : '封禁中' }}
                </n-tag>
                <span class="guard-text">
                  失败计数: {{ guardStatus.routeFailureCount }}，封禁截止: {{ formatBlockedUntil(guardStatus.routeBlockedUntilEpochMs) }}
                </span>
              </div>
            </n-gi>
            <n-gi>
              <div class="guard-row">
                <span class="guard-label">提示词实验状态</span>
                <n-tag :type="guardStatus.promptExperimentAllowed ? 'success' : 'error'">
                  {{ guardStatus.promptExperimentAllowed ? '允许' : '封禁中' }}
                </n-tag>
                <span class="guard-text">
                  失败计数: {{ guardStatus.promptFailureCount }}，封禁截止: {{ formatBlockedUntil(guardStatus.promptBlockedUntilEpochMs) }}
                </span>
              </div>
            </n-gi>
          </n-grid>
          <n-space>
            <n-button @click="handleResetGuard('all')">重置全部保护状态</n-button>
            <n-button @click="handleResetGuard('route')">重置路由保护</n-button>
            <n-button @click="handleResetGuard('prompt')">重置提示词保护</n-button>
          </n-space>
          <n-space>
            <n-input-number
              v-model:value="routeBlockCooldownSeconds"
              :min="1"
              placeholder="路由封禁秒数(可选)"
              style="width: 220px"
            />
            <n-button type="warning" @click="handleBlockGuard('route')">封禁路由实验</n-button>
            <n-input-number
              v-model:value="promptBlockCooldownSeconds"
              :min="1"
              placeholder="提示词封禁秒数(可选)"
              style="width: 220px"
            />
            <n-button type="warning" @click="handleBlockGuard('prompt')">封禁提示词实验</n-button>
          </n-space>
        </n-space>
      </n-card>

      <n-card title="实验配置">
        <n-space vertical :size="12">
          <n-form label-placement="left" label-width="180">
            <n-grid cols="1 m:2" :x-gap="16">
              <n-gi>
                <n-form-item label="路由实验开关">
                  <n-switch v-model:value="configForm.routeAbExperimentEnabled" />
                </n-form-item>
                <n-form-item label="路由实验流量占比">
                  <n-input-number v-model:value="configForm.routeAbRolloutPercent" :min="0" :max="100" />
                </n-form-item>
                <n-form-item label="路由控制组模式">
                  <n-select v-model:value="configForm.routeAbControlMode" :options="routeModeOptions" />
                </n-form-item>
                <n-form-item label="路由实验组模式">
                  <n-select v-model:value="configForm.routeAbExperimentMode" :options="routeModeOptions" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="提示词实验开关">
                  <n-switch v-model:value="configForm.promptAbExperimentEnabled" />
                </n-form-item>
                <n-form-item label="提示词实验流量占比">
                  <n-input-number v-model:value="configForm.promptAbRolloutPercent" :min="0" :max="100" />
                </n-form-item>
                <n-form-item label="提示词控制组版本">
                  <n-select v-model:value="configForm.promptAbControlVersion" :options="promptVersionOptions" />
                </n-form-item>
                <n-form-item label="提示词实验组版本">
                  <n-select v-model:value="configForm.promptAbExperimentVersion" :options="promptVersionOptions" />
                </n-form-item>
              </n-gi>
            </n-grid>
            <n-grid cols="1 m:12" :x-gap="16">
              <n-gi>
                <n-form-item label="自动回滚开关">
                  <n-switch v-model:value="configForm.experimentAutoRollbackEnabled" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="失败阈值">
                  <n-input-number v-model:value="configForm.experimentFailureThreshold" :min="1" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="冷却时长(秒)">
                  <n-input-number v-model:value="configForm.experimentCooldownSeconds" :min="1" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="异常告警开关">
                  <n-switch v-model:value="configForm.experimentAlertEnabled" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="告警抑制窗口(秒)">
                  <n-input-number v-model:value="configForm.experimentAlertSuppressSeconds" :min="0" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="邮件告警开关">
                  <n-switch v-model:value="configForm.experimentAlertEmailEnabled" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="邮件收件人">
                  <n-input
                    v-model:value="configForm.experimentAlertEmailRecipients"
                    placeholder="多个邮箱用逗号分隔"
                  />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="Webhook告警开关">
                  <n-switch v-model:value="configForm.experimentAlertWebhookEnabled" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="Webhook地址">
                  <n-input
                    v-model:value="configForm.experimentAlertWebhookUrls"
                    placeholder="多个地址用逗号分隔"
                  />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="Webhook超时(秒)">
                  <n-input-number v-model:value="configForm.experimentAlertWebhookTimeoutSeconds" :min="1" :max="30" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="Critical渠道">
                  <n-input
                    v-model:value="configForm.experimentAlertCriticalChannels"
                    placeholder="notice,email,webhook"
                  />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="Warning渠道">
                  <n-input
                    v-model:value="configForm.experimentAlertWarningChannels"
                    placeholder="notice,email"
                  />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="投递重试开关">
                  <n-switch v-model:value="configForm.experimentAlertDeliveryRetryEnabled" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="最大尝试次数">
                  <n-input-number v-model:value="configForm.experimentAlertDeliveryMaxAttempts" :min="1" :max="10" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="重试间隔(秒)">
                  <n-input-number v-model:value="configForm.experimentAlertDeliveryRetryDelaySeconds" :min="5" :max="3600" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="健康巡检开关">
                  <n-switch v-model:value="configForm.experimentAlertDeliveryHealthCheckEnabled" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="健康巡检窗口(天)">
                  <n-input-number v-model:value="configForm.experimentAlertDeliveryHealthCheckDays" :min="1" :max="90" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="死信率阈值(0-1)">
                  <n-input-number
                    v-model:value="configForm.experimentAlertDeliveryHealthDeadRateThreshold"
                    :min="0"
                    :max="1"
                    :step="0.01"
                    :precision="2"
                  />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="待重试率阈值(0-1)">
                  <n-input-number
                    v-model:value="configForm.experimentAlertDeliveryHealthPendingRateThreshold"
                    :min="0"
                    :max="1"
                    :step="0.01"
                    :precision="2"
                  />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="升级冷却(秒)">
                  <n-input-number
                    v-model:value="configForm.experimentAlertDeliveryHealthEscalationCooldownSeconds"
                    :min="30"
                    :max="86400"
                  />
                </n-form-item>
              </n-gi>
            </n-grid>
          </n-form>
          <n-space>
            <n-button type="primary" :loading="configSubmitting" @click="handleSaveConfig">保存并生效</n-button>
            <n-button :loading="configReloading" @click="handleReloadConfig">从配置中心重载</n-button>
          </n-space>
        </n-space>
      </n-card>

      <n-card title="实验统计明细">
        <n-tabs type="line" animated>
          <n-tab-pane name="route" tab="路由实验">
            <n-data-table
              :columns="routeColumns"
              :data="routeStats.items"
              :loading="statsLoading"
              :pagination="{ pageSize: 10 }"
              size="small"
            />
          </n-tab-pane>
          <n-tab-pane name="prompt" tab="提示词实验">
            <n-data-table
              :columns="promptColumns"
              :data="promptStats.items"
              :loading="statsLoading"
              :pagination="{ pageSize: 10 }"
              size="small"
            />
          </n-tab-pane>
        </n-tabs>
      </n-card>

      <n-card title="配置变更审计">
        <n-space vertical :size="12">
          <n-space>
            <n-input-number v-model:value="auditLimit" :min="1" :max="100" style="width: 140px" />
            <n-button :loading="auditLoading" @click="loadAudits">刷新审计</n-button>
          </n-space>
          <n-data-table
            :columns="auditColumns"
            :data="audits"
            :loading="auditLoading"
            :pagination="{ pageSize: 10 }"
            size="small"
          />
        </n-space>
      </n-card>

      <n-card title="异常告警日志">
        <n-space vertical :size="12">
          <n-space>
            <n-input-number v-model:value="alertLimit" :min="1" :max="200" style="width: 140px" />
            <n-switch v-model:value="includeSuppressedAlerts" />
            <span class="guard-text">包含被抑制告警</span>
            <n-button :loading="alertLoading" @click="loadAlertLogs">刷新告警</n-button>
            <n-button @click="handleResetAlertSuppression">重置抑制状态</n-button>
          </n-space>
          <n-data-table
            :columns="alertColumns"
            :data="alertLogs"
            :loading="alertLoading"
            :pagination="{ pageSize: 10 }"
            size="small"
          />
        </n-space>
      </n-card>

      <n-card title="投递统计概览">
        <n-space vertical :size="12">
          <n-space>
            <n-input-number v-model:value="deliverySummaryDays" :min="1" :max="90" style="width: 140px" />
            <n-button :loading="summaryLoading" @click="loadDeliveryOverview">刷新统计</n-button>
            <n-button type="warning" :loading="deliveryHealthChecking" @click="handleCheckDeliveryHealth">执行健康巡检</n-button>
            <n-button :loading="thresholdSuggesting" @click="handleSuggestHealthThresholds">生成阈值建议</n-button>
            <n-button :loading="governanceAdviceLoading" @click="handleGenerateGovernanceAdvice">生成治理建议</n-button>
            <n-button :loading="governanceReportLoading" @click="handleGenerateGovernanceReport">生成治理报告</n-button>
            <n-button
              type="primary"
              ghost
              :disabled="deliveryHealthThresholdSuggestion.sampleCount < 1"
              :loading="thresholdApplying"
              @click="handleApplySuggestedThresholds"
            >
              应用建议阈值
            </n-button>
            <span class="guard-text">统计区间：近 {{ deliverySummary.rangeDays }} 天</span>
            <n-tag :type="deliveryHealthTagType(deliveryHealth.healthLevel)">
              {{ deliveryHealth.healthLevel }}
            </n-tag>
            <span class="guard-text">
              死信率 {{ toPercent(deliveryHealth.deadRate) }}% / 阈值 {{ toPercent(deliveryHealth.deadRateThreshold) }}%，
              待重试率 {{ toPercent(deliveryHealth.pendingRate) }}% / 阈值 {{ toPercent(deliveryHealth.pendingRateThreshold) }}%
            </span>
          </n-space>
          <span class="guard-text">{{ deliveryHealth.suggestion }}</span>
          <span class="guard-text">
            阈值建议(样本 {{ deliveryHealthThresholdSuggestion.sampleCount }}，置信度 {{ deliveryHealthThresholdSuggestion.confidenceLevel }}):
            dead <= {{ toPercent(deliveryHealthThresholdSuggestion.recommendedDeadRateThreshold) }}%，
            pending <= {{ toPercent(deliveryHealthThresholdSuggestion.recommendedPendingRateThreshold) }}%
          </span>
          <span class="guard-text">{{ deliveryHealthThresholdSuggestion.suggestion }}</span>
          <n-space>
            <n-tag :type="healthRiskLevelTagType(deliveryHealthGovernanceAdvice.overallRiskLevel)">
              {{ deliveryHealthGovernanceAdvice.overallRiskLevel }}
            </n-tag>
            <span class="guard-text">治理建议生成时间：{{ formatDateTime(deliveryHealthGovernanceAdvice.generatedAt) }}</span>
          </n-space>
          <ul class="advice-list">
            <li v-for="item in deliveryHealthGovernanceAdvice.keyFindings" :key="`finding-${item}`">{{ item }}</li>
          </ul>
          <ul class="advice-list">
            <li v-for="item in deliveryHealthGovernanceAdvice.recommendedActions" :key="`action-${item}`">{{ item }}</li>
          </ul>
          <n-input
            v-model:value="deliveryHealthGovernanceReport.content"
            type="textarea"
            :rows="10"
            readonly
            placeholder="点击“生成治理报告”后展示文本报告"
          />
          <n-grid cols="1 s:2 m:6" :x-gap="12" :y-gap="12">
            <n-gi>
              <n-statistic label="总投递" :value="deliverySummary.totalCount" />
            </n-gi>
            <n-gi>
              <n-statistic label="成功" :value="deliverySummary.successCount" />
            </n-gi>
            <n-gi>
              <n-statistic label="待重试" :value="deliverySummary.pendingCount" />
            </n-gi>
            <n-gi>
              <n-statistic label="死信" :value="deliverySummary.deadCount" />
            </n-gi>
            <n-gi>
              <n-statistic label="已关闭" :value="deliverySummary.closedCount" />
            </n-gi>
            <n-gi>
              <n-statistic label="失败(旧状态)" :value="deliverySummary.failedCount" />
            </n-gi>
          </n-grid>
        </n-space>
      </n-card>

      <n-card title="健康巡检历史与趋势">
        <n-space vertical :size="12">
          <n-space>
            <n-input-number v-model:value="healthTrendDays" :min="1" :max="90" style="width: 160px" />
            <n-input-number v-model:value="healthLogLimit" :min="1" :max="200" style="width: 140px" />
            <n-input-number v-model:value="healthRiskDayLimit" :min="1" :max="50" style="width: 140px" />
            <n-select v-model:value="healthLogLevel" :options="healthLevelOptions" style="width: 160px" />
            <n-input v-model:value="healthLogDateFilter" placeholder="日志日期(YYYY-MM-DD)" clearable style="width: 200px" />
            <n-button :loading="healthTrendLoading || healthDailyTrendLoading || healthRiskDaysLoading || healthRiskSummaryLoading || healthLogLoading || healthStorageLoading" @click="loadDeliveryHealthInsights">刷新历史</n-button>
            <n-button :disabled="!healthLogDateFilter" @click="handleClearHealthLogDateFilter">清除日期筛选</n-button>
            <n-input-number v-model:value="healthLogRetentionDays" :min="1" :max="3650" style="width: 160px" />
            <n-input-number v-model:value="healthLogCleanupLimit" :min="1" :max="5000" style="width: 140px" />
            <n-button type="warning" :loading="healthCleanupLoading" @click="handleCleanupDeliveryHealthLogs">清理过期日志</n-button>
            <span class="guard-text">
              总量/过期：{{ deliveryHealthLogStorage.totalCount }}/{{ deliveryHealthLogStorage.expiredCount }}
            </span>
            <span class="guard-text">最近巡检：{{ formatDateTime(deliveryHealthTrend.latest?.checkTime) }}</span>
          </n-space>
          <n-grid cols="1 s:2 m:7" :x-gap="12" :y-gap="12">
            <n-gi>
              <n-statistic label="巡检次数" :value="deliveryHealthTrend.totalChecks" />
            </n-gi>
            <n-gi>
              <n-statistic label="HEALTHY" :value="deliveryHealthTrend.healthyCount" />
            </n-gi>
            <n-gi>
              <n-statistic label="WARNING" :value="deliveryHealthTrend.warningCount" />
            </n-gi>
            <n-gi>
              <n-statistic label="CRITICAL" :value="deliveryHealthTrend.criticalCount" />
            </n-gi>
            <n-gi>
              <n-statistic label="级别迁移" :value="deliveryHealthTrend.levelChangedCount" />
            </n-gi>
            <n-gi>
              <n-statistic label="升级通知" :value="deliveryHealthTrend.escalatedCount" />
            </n-gi>
            <n-gi>
              <n-statistic label="预警/恢复" :value="`${deliveryHealthTrend.warningNotifiedCount}/${deliveryHealthTrend.recoveryNotifiedCount}`" />
            </n-gi>
          </n-grid>
          <n-data-table
            :columns="healthDailyTrendColumns"
            :data="deliveryHealthDailyTrend"
            :loading="healthDailyTrendLoading"
            :pagination="{ pageSize: 7 }"
            size="small"
          />
          <n-grid cols="1 s:2 m:5" :x-gap="12" :y-gap="12">
            <n-gi>
              <n-statistic label="风险天数" :value="deliveryHealthRiskSummary.totalRiskDays" />
            </n-gi>
            <n-gi>
              <n-statistic label="高风险天数" :value="deliveryHealthRiskSummary.highRiskDays" />
            </n-gi>
            <n-gi>
              <n-statistic label="中风险天数" :value="deliveryHealthRiskSummary.mediumRiskDays" />
            </n-gi>
            <n-gi>
              <n-statistic label="低风险天数" :value="deliveryHealthRiskSummary.lowRiskDays" />
            </n-gi>
            <n-gi>
              <n-statistic label="最高风险分" :value="deliveryHealthRiskSummary.maxRiskScore.toFixed(1)" />
            </n-gi>
          </n-grid>
          <span class="guard-text">
            最高风险日期：{{ deliveryHealthRiskSummary.maxRiskDate || '-' }}，
            高风险 Top：{{ deliveryHealthRiskSummary.topHighRiskDates.join(', ') || '-' }}
          </span>
          <n-data-table
            :columns="healthRiskDayColumns"
            :data="deliveryHealthRiskDays"
            :loading="healthRiskDaysLoading"
            :row-props="healthRiskRowProps"
            :pagination="{ pageSize: 7 }"
            size="small"
          />
          <n-data-table
            :columns="healthLogColumns"
            :data="deliveryHealthLogs"
            :loading="healthLogLoading"
            :pagination="{ pageSize: 10 }"
            size="small"
          />
        </n-space>
      </n-card>

      <n-card title="告警投递日志">
        <n-space vertical :size="12">
          <n-space>
            <n-input-number v-model:value="deliveryLimit" :min="1" :max="200" style="width: 140px" />
            <n-select
              v-model:value="deliveryStatus"
              :options="deliveryStatusOptions"
              style="width: 160px"
            />
            <n-button :loading="deliveryLoading" @click="loadDeliveryLogs">刷新投递日志</n-button>
            <n-button type="warning" :loading="deliveryRetrying" @click="handleRetryDeliveries">手动触发重试</n-button>
          </n-space>
          <n-data-table
            :columns="deliveryColumns"
            :data="deliveryLogs"
            :loading="deliveryLoading"
            :pagination="{ pageSize: 10 }"
            size="small"
          />
        </n-space>
      </n-card>

      <n-card title="死信队列">
        <n-space vertical :size="12">
          <n-space>
            <n-input-number v-model:value="deadDeliveryLimit" :min="1" :max="200" style="width: 140px" />
            <n-input
              v-model:value="deadCloseReason"
              placeholder="关闭原因(可选)"
              clearable
              style="width: 260px"
            />
            <n-button :loading="deadDeliveryLoading" @click="loadDeadDeliveryLogs">刷新死信</n-button>
            <n-button
              type="warning"
              :disabled="deadSelectedRowKeys.length === 0"
              @click="handleBatchReplayDeadLetters"
            >
              批量重放选中
            </n-button>
            <n-button
              type="error"
              :disabled="deadSelectedRowKeys.length === 0"
              @click="handleBatchCloseDeadLetters"
            >
              批量关闭选中
            </n-button>
          </n-space>
          <n-data-table
            :columns="deadDeliveryColumns"
            :data="deadDeliveryLogs"
            :loading="deadDeliveryLoading"
            :pagination="{ pageSize: 10 }"
            :row-key="deadRowKey"
            :checked-row-keys="deadSelectedRowKeys"
            @update:checked-row-keys="handleDeadSelectionChange"
            size="small"
          />
        </n-space>
      </n-card>
    </n-space>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import { NButton, NTag, useDialog, useMessage, type DataTableColumns } from 'naive-ui'
import {
  aiExperimentApi,
  type AlertDeliveryHealthDailyTrendItem,
  type AlertDeliveryHealthLogCleanupResult,
  type AlertDeliveryHealthGovernanceAdvice,
  type AlertDeliveryHealthGovernanceReport,
  type AlertDeliveryHealthLogItem,
  type AlertDeliveryHealthLogStorageSummary,
  type AlertDeliveryHealthRiskDayItem,
  type AlertDeliveryHealthRiskSummary,
  type AlertDeliveryHealthSummary,
  type AlertDeliveryHealthThresholdSuggestion,
  type AlertDeliveryHealthTrendSummary,
  type AlertDeliverySummary,
  type ExperimentAlertDeliveryLogItem,
  type ConfigAuditItem,
  type ExperimentAlertLogItem,
  type ExperimentConfigSnapshot,
  type ExperimentConfigUpdateRequest,
  type ExperimentGuardStatus,
  type PromptExperimentStat,
  type PromptExperimentStatsResponse,
  type RouteExperimentStat,
  type RouteExperimentStatsResponse
} from '@/api/aiExperiment'

const message = useMessage()
const dialog = useDialog()

const sceneOptions = [
  { label: '通用', value: 'GENERAL' },
  { label: '报表', value: 'REPORT' },
  { label: '知识库', value: 'KNOWLEDGE' },
  { label: '文案', value: 'COPYWRITING' },
  { label: '审批', value: 'APPROVAL' },
  { label: '客户查询', value: 'CUSTOMER_QUERY' }
]

const routeModeOptions = [
  { label: 'rule', value: 'rule' },
  { label: 'agent', value: 'agent' },
  { label: 'hybrid', value: 'hybrid' }
]

const promptVersionOptions = [
  { label: 'v1', value: 'v1' },
  { label: 'v2', value: 'v2' }
]

const deliveryStatusOptions = [
  { label: '全部', value: '' },
  { label: 'pending', value: 'pending' },
  { label: 'success', value: 'success' },
  { label: 'failed', value: 'failed' },
  { label: 'dead', value: 'dead' },
  { label: 'closed', value: 'closed' }
]

const healthLevelOptions = [
  { label: '全部级别', value: '' },
  { label: 'HEALTHY', value: 'HEALTHY' },
  { label: 'WARNING', value: 'WARNING' },
  { label: 'CRITICAL', value: 'CRITICAL' }
]

const queryForm = reactive<{
  dateRange: [number, number] | null
  scene: string | null
}>({
  dateRange: null,
  scene: null
})

const routeStats = reactive<RouteExperimentStatsResponse>({
  startDate: '',
  endDate: '',
  items: [],
  summary: {
    total: 0,
    success: 0,
    failure: 0,
    successRate: 0,
    avgLatencyMs: 0
  }
})

const promptStats = reactive<PromptExperimentStatsResponse>({
  startDate: '',
  endDate: '',
  items: [],
  summary: {
    total: 0,
    success: 0,
    failure: 0,
    successRate: 0,
    avgLatencyMs: 0
  }
})

const guardStatus = reactive<ExperimentGuardStatus>({
  routeExperimentAllowed: true,
  routeBlockedUntilEpochMs: 0,
  routeFailureCount: 0,
  promptExperimentAllowed: true,
  promptBlockedUntilEpochMs: 0,
  promptFailureCount: 0
})

const configForm = reactive<ExperimentConfigSnapshot>({
  routeAbExperimentEnabled: false,
  routeAbRolloutPercent: 0,
  routeAbControlMode: 'rule',
  routeAbExperimentMode: 'hybrid',
  promptAbExperimentEnabled: false,
  promptAbRolloutPercent: 0,
  promptAbControlVersion: 'v1',
  promptAbExperimentVersion: 'v2',
  experimentAutoRollbackEnabled: true,
  experimentFailureThreshold: 3,
  experimentCooldownSeconds: 60,
  experimentAlertEnabled: true,
  experimentAlertSuppressSeconds: 300,
  experimentAlertEmailEnabled: false,
  experimentAlertEmailRecipients: '',
  experimentAlertWebhookEnabled: false,
  experimentAlertWebhookUrls: '',
  experimentAlertWebhookTimeoutSeconds: 5,
  experimentAlertCriticalChannels: 'notice,email,webhook',
  experimentAlertWarningChannels: 'notice,email',
  experimentAlertDeliveryRetryEnabled: true,
  experimentAlertDeliveryMaxAttempts: 3,
  experimentAlertDeliveryRetryDelaySeconds: 60,
  experimentAlertDeliveryHealthCheckEnabled: true,
  experimentAlertDeliveryHealthCheckDays: 7,
  experimentAlertDeliveryHealthDeadRateThreshold: 0.05,
  experimentAlertDeliveryHealthPendingRateThreshold: 0.1,
  experimentAlertDeliveryHealthEscalationCooldownSeconds: 600
})

const routeBlockCooldownSeconds = ref<number | null>(null)
const promptBlockCooldownSeconds = ref<number | null>(null)
const statsLoading = ref(false)
const configSubmitting = ref(false)
const configReloading = ref(false)
const auditLoading = ref(false)
const alertLoading = ref(false)
const deliveryLoading = ref(false)
const deadDeliveryLoading = ref(false)
const summaryLoading = ref(false)
const deliveryHealthChecking = ref(false)
const deliveryRetrying = ref(false)
const healthLogLoading = ref(false)
const healthTrendLoading = ref(false)
const healthDailyTrendLoading = ref(false)
const healthRiskDaysLoading = ref(false)
const healthRiskSummaryLoading = ref(false)
const healthStorageLoading = ref(false)
const healthCleanupLoading = ref(false)
const thresholdSuggesting = ref(false)
const thresholdApplying = ref(false)
const governanceAdviceLoading = ref(false)
const governanceReportLoading = ref(false)
const audits = ref<ConfigAuditItem[]>([])
const alertLogs = ref<ExperimentAlertLogItem[]>([])
const deliveryLogs = ref<ExperimentAlertDeliveryLogItem[]>([])
const deadDeliveryLogs = ref<ExperimentAlertDeliveryLogItem[]>([])
const deliveryHealthLogs = ref<AlertDeliveryHealthLogItem[]>([])
const deliveryHealthDailyTrend = ref<AlertDeliveryHealthDailyTrendItem[]>([])
const deliveryHealthRiskDays = ref<AlertDeliveryHealthRiskDayItem[]>([])
const auditLimit = ref<number | null>(20)
const alertLimit = ref<number | null>(20)
const deliveryLimit = ref<number | null>(30)
const deadDeliveryLimit = ref<number | null>(30)
const deliverySummaryDays = ref<number | null>(7)
const healthTrendDays = ref<number | null>(7)
const healthLogLimit = ref<number | null>(30)
const healthRiskDayLimit = ref<number | null>(10)
const healthLogRetentionDays = ref<number | null>(90)
const healthLogCleanupLimit = ref<number | null>(500)
const healthLogLevel = ref<string>('')
const healthLogDateFilter = ref('')
const deliveryStatus = ref<string>('')
const includeSuppressedAlerts = ref(true)
const deadCloseReason = ref('')
const deadSelectedRowKeys = ref<number[]>([])

const deliverySummary = reactive<AlertDeliverySummary>({
  rangeDays: 7,
  startTime: undefined,
  endTime: undefined,
  totalCount: 0,
  successCount: 0,
  pendingCount: 0,
  deadCount: 0,
  closedCount: 0,
  failedCount: 0
})

const deliveryHealth = reactive<AlertDeliveryHealthSummary>({
  rangeDays: 7,
  startTime: undefined,
  endTime: undefined,
  totalCount: 0,
  successCount: 0,
  pendingCount: 0,
  deadCount: 0,
  closedCount: 0,
  failedCount: 0,
  deadRate: 0,
  pendingRate: 0,
  deadRateThreshold: 0.05,
  pendingRateThreshold: 0.1,
  healthLevel: 'HEALTHY',
  suggestion: '暂无投递数据'
})

const deliveryHealthTrend = reactive<AlertDeliveryHealthTrendSummary>({
  rangeDays: 7,
  startTime: undefined,
  endTime: undefined,
  totalChecks: 0,
  healthyCount: 0,
  warningCount: 0,
  criticalCount: 0,
  levelChangedCount: 0,
  escalatedCount: 0,
  warningNotifiedCount: 0,
  recoveryNotifiedCount: 0,
  latest: undefined
})

const deliveryHealthLogStorage = reactive<AlertDeliveryHealthLogStorageSummary>({
  retainDays: 90,
  cutoffTime: undefined,
  totalCount: 0,
  expiredCount: 0
})

const deliveryHealthRiskSummary = reactive<AlertDeliveryHealthRiskSummary>({
  rangeDays: 7,
  startTime: undefined,
  endTime: undefined,
  totalRiskDays: 0,
  highRiskDays: 0,
  mediumRiskDays: 0,
  lowRiskDays: 0,
  maxRiskScore: 0,
  maxRiskDate: undefined,
  topHighRiskDates: []
})

const deliveryHealthThresholdSuggestion = reactive<AlertDeliveryHealthThresholdSuggestion>({
  rangeDays: 7,
  startTime: undefined,
  endTime: undefined,
  sampleCount: 0,
  avgDeadRate: 0,
  avgPendingRate: 0,
  p90DeadRate: 0,
  p90PendingRate: 0,
  currentDeadRateThreshold: 0.05,
  currentPendingRateThreshold: 0.1,
  recommendedDeadRateThreshold: 0.05,
  recommendedPendingRateThreshold: 0.1,
  suggestion: '暂无建议',
  confidenceLevel: 'LOW'
})

const deliveryHealthGovernanceAdvice = reactive<AlertDeliveryHealthGovernanceAdvice>({
  rangeDays: 7,
  startTime: undefined,
  endTime: undefined,
  overallRiskLevel: 'LOW',
  keyFindings: [],
  recommendedActions: [],
  generatedAt: undefined
})

const deliveryHealthGovernanceReport = reactive<AlertDeliveryHealthGovernanceReport>({
  rangeDays: 7,
  generatedAt: undefined,
  overallRiskLevel: 'LOW',
  content: ''
})

const routeColumns: DataTableColumns<RouteExperimentStat> = [
  { title: '日期', key: 'date', width: 110 },
  { title: '场景', key: 'scene', width: 120 },
  { title: '实验组', key: 'experimentGroup', width: 110 },
  { title: '路由模式', key: 'routeMode', width: 110 },
  { title: '总请求', key: 'total', width: 90 },
  { title: '成功', key: 'success', width: 90 },
  { title: '失败', key: 'failure', width: 90 },
  {
    title: '成功率',
    key: 'successRate',
    width: 100,
    render: row => `${toPercent(row.successRate)}%`
  },
  { title: '平均耗时(ms)', key: 'avgLatencyMs', width: 120 }
]

const promptColumns: DataTableColumns<PromptExperimentStat> = [
  { title: '日期', key: 'date', width: 110 },
  { title: '场景', key: 'scene', width: 120 },
  { title: '实验组', key: 'experimentGroup', width: 110 },
  { title: '版本', key: 'promptVersion', width: 100 },
  { title: '总请求', key: 'total', width: 90 },
  { title: '成功', key: 'success', width: 90 },
  { title: '失败', key: 'failure', width: 90 },
  {
    title: '成功率',
    key: 'successRate',
    width: 100,
    render: row => `${toPercent(row.successRate)}%`
  },
  { title: '平均耗时(ms)', key: 'avgLatencyMs', width: 120 }
]

const auditColumns: DataTableColumns<ConfigAuditItem> = [
  { title: '审计ID', key: 'id', width: 90 },
  {
    title: '变更类型',
    key: 'changeType',
    width: 100,
    render: row =>
      h(
        NTag,
        { type: row.changeType === 'rollback' ? 'warning' : 'info', size: 'small' },
        { default: () => (row.changeType || 'update').toUpperCase() }
      )
  },
  { title: '操作者', key: 'operatorName', width: 130 },
  { title: '来源', key: 'source', width: 90 },
  {
    title: '变更字段',
    key: 'changedFields',
    minWidth: 260,
    render: row => summarizeChangedFields(row)
  },
  {
    title: '时间',
    key: 'createTime',
    width: 180,
    render: row => formatDateTime(row.createTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: row =>
      h(
        NButton,
        {
          size: 'small',
          tertiary: true,
          type: 'warning',
          onClick: () => handleRollback(row.id)
        },
        { default: () => '回滚' }
      )
  }
]

const alertColumns: DataTableColumns<ExperimentAlertLogItem> = [
  { title: 'ID', key: 'id', width: 80 },
  {
    title: '类型',
    key: 'alertType',
    width: 120,
    render: row => row.alertType
  },
  { title: '实验', key: 'experimentType', width: 90 },
  { title: '来源', key: 'triggerSource', width: 90 },
  {
    title: '抑制',
    key: 'suppressed',
    width: 90,
    render: row => h(NTag, { type: row.suppressed ? 'warning' : 'success', size: 'small' }, { default: () => (row.suppressed ? '是' : '否') })
  },
  { title: '标题', key: 'title', minWidth: 180 },
  { title: '内容', key: 'content', minWidth: 280 },
  {
    title: '时间',
    key: 'createTime',
    width: 180,
    render: row => formatDateTime(row.createTime)
  }
]

const deliveryColumns: DataTableColumns<ExperimentAlertDeliveryLogItem> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '告警ID', key: 'alertLogId', width: 90 },
  { title: '级别', key: 'alertLevel', width: 90 },
  { title: '渠道', key: 'channel', width: 90 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: row => h(NTag, { type: deliveryStatusTagType(row.status), size: 'small' }, { default: () => row.status })
  },
  { title: '目标', key: 'targetValue', minWidth: 180 },
  {
    title: '尝试',
    key: 'attempt',
    width: 100,
    render: row => `${row.attemptCount}/${row.maxAttempts}`
  },
  { title: '响应码', key: 'lastResponseCode', width: 90 },
  { title: '错误信息', key: 'lastErrorMessage', minWidth: 180 },
  {
    title: '下次重试',
    key: 'nextRetryTime',
    width: 170,
    render: row => formatDateTime(row.nextRetryTime)
  },
  {
    title: '更新时间',
    key: 'updateTime',
    width: 170,
    render: row => formatDateTime(row.updateTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 110,
    render: row =>
      h(
        NButton,
        {
          size: 'small',
          tertiary: true,
          type: 'warning',
          disabled: !(row.status === 'failed' || row.status === 'dead'),
          onClick: () => handleReplayDelivery(row.id)
        },
        { default: () => '重放' }
      )
  }
]

const deadDeliveryColumns: DataTableColumns<ExperimentAlertDeliveryLogItem> = [
  { type: 'selection', multiple: true },
  { title: 'ID', key: 'id', width: 80 },
  { title: '告警ID', key: 'alertLogId', width: 90 },
  { title: '级别', key: 'alertLevel', width: 90 },
  { title: '渠道', key: 'channel', width: 90 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: row => h(NTag, { type: deliveryStatusTagType(row.status), size: 'small' }, { default: () => row.status })
  },
  { title: '目标', key: 'targetValue', minWidth: 180 },
  {
    title: '尝试',
    key: 'attempt',
    width: 100,
    render: row => `${row.attemptCount}/${row.maxAttempts}`
  },
  { title: '错误信息', key: 'lastErrorMessage', minWidth: 220 },
  {
    title: '更新时间',
    key: 'updateTime',
    width: 170,
    render: row => formatDateTime(row.updateTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 170,
    render: row =>
      h('div', { style: 'display:flex;gap:8px;' }, [
        h(
          NButton,
          {
            size: 'small',
            tertiary: true,
            type: 'warning',
            onClick: () => handleReplayDelivery(row.id)
          },
          { default: () => '重放' }
        ),
        h(
          NButton,
          {
            size: 'small',
            tertiary: true,
            type: 'error',
            onClick: () => handleCloseDeadDelivery(row.id)
          },
          { default: () => '关闭' }
        )
      ])
  }
]

const healthLogColumns: DataTableColumns<AlertDeliveryHealthLogItem> = [
  { title: 'ID', key: 'id', width: 80 },
  {
    title: '巡检时间',
    key: 'checkTime',
    width: 170,
    render: row => formatDateTime(row.checkTime)
  },
  {
    title: '来源',
    key: 'checkSource',
    width: 90,
    render: row => h(NTag, { type: row.checkSource === 'manual' ? 'info' : 'default', size: 'small' }, { default: () => (row.checkSource || '-') })
  },
  {
    title: '级别',
    key: 'healthLevel',
    width: 90,
    render: row => h(NTag, { type: deliveryHealthTagType(row.healthLevel), size: 'small' }, { default: () => row.healthLevel })
  },
  {
    title: '级别迁移',
    key: 'transition',
    width: 180,
    render: row => (row.levelChanged ? `${row.previousHealthLevel || '-'} -> ${row.healthLevel}` : row.healthLevel)
  },
  {
    title: '升级/抑制',
    key: 'escalation',
    width: 120,
    render: row => `${row.escalated ? 'Y' : 'N'}/${row.suppressed ? 'Y' : 'N'}`
  },
  {
    title: '预警/恢复',
    key: 'notice',
    width: 120,
    render: row => `${row.warningNotified ? 'Y' : 'N'}/${row.recoveryNotified ? 'Y' : 'N'}`
  },
  {
    title: '死信率/待重试率',
    key: 'rates',
    width: 220,
    render: row =>
      `${toPercent(row.deadRate)}%/${toPercent(row.deadRateThreshold)}% | ${toPercent(row.pendingRate)}%/${toPercent(row.pendingRateThreshold)}%`
  },
  {
    title: '样本',
    key: 'sample',
    width: 170,
    render: row => `T${row.totalCount} S${row.successCount} P${row.pendingCount} D${row.deadCount}`
  },
  { title: '原因', key: 'reason', width: 140 },
  { title: '建议', key: 'suggestion', minWidth: 180 }
]

const healthDailyTrendColumns: DataTableColumns<AlertDeliveryHealthDailyTrendItem> = [
  { title: '日期', key: 'date', width: 110 },
  { title: '巡检次数', key: 'totalChecks', width: 90 },
  { title: 'HEALTHY', key: 'healthyCount', width: 90 },
  { title: 'WARNING', key: 'warningCount', width: 90 },
  { title: 'CRITICAL', key: 'criticalCount', width: 90 },
  { title: '升级次数', key: 'escalatedCount', width: 90 },
  {
    title: '平均死信率',
    key: 'avgDeadRate',
    width: 110,
    render: row => `${toPercent(row.avgDeadRate)}%`
  },
  {
    title: '平均待重试率',
    key: 'avgPendingRate',
    width: 120,
    render: row => `${toPercent(row.avgPendingRate)}%`
  },
  {
    title: '峰值死信率',
    key: 'maxDeadRate',
    width: 110,
    render: row => `${toPercent(row.maxDeadRate)}%`
  },
  {
    title: '峰值待重试率',
    key: 'maxPendingRate',
    width: 120,
    render: row => `${toPercent(row.maxPendingRate)}%`
  }
]

const healthRiskDayColumns: DataTableColumns<AlertDeliveryHealthRiskDayItem> = [
  { title: '日期', key: 'date', width: 110 },
  {
    title: '风险分',
    key: 'riskScore',
    width: 90,
    render: row => row.riskScore.toFixed(1)
  },
  {
    title: '风险级别',
    key: 'riskLevel',
    width: 100,
    render: row => h(NTag, { type: healthRiskLevelTagType(row.riskLevel), size: 'small' }, { default: () => row.riskLevel })
  },
  { title: '巡检数', key: 'totalChecks', width: 80 },
  { title: 'CRITICAL', key: 'criticalCount', width: 90 },
  { title: 'WARNING', key: 'warningCount', width: 90 },
  { title: '升级', key: 'escalatedCount', width: 70 },
  {
    title: '峰值死信率',
    key: 'maxDeadRate',
    width: 110,
    render: row => `${toPercent(row.maxDeadRate)}%`
  },
  {
    title: '峰值待重试率',
    key: 'maxPendingRate',
    width: 120,
    render: row => `${toPercent(row.maxPendingRate)}%`
  },
  { title: '洞察', key: 'insight', minWidth: 260 }
]

function toDateString(ms?: number) {
  if (!ms) return undefined
  const date = new Date(ms)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function toPercent(value?: number) {
  if (!value) return 0
  return Number((value * 100).toFixed(2))
}

function deliveryStatusTagType(status?: string) {
  if (status === 'success') return 'success'
  if (status === 'failed' || status === 'dead') return 'error'
  if (status === 'closed') return 'info'
  return 'warning'
}

function deliveryHealthTagType(level?: string) {
  if (level === 'HEALTHY') return 'success'
  if (level === 'WARNING') return 'warning'
  if (level === 'CRITICAL') return 'error'
  return 'default'
}

function healthRiskLevelTagType(level?: string) {
  if (level === 'HIGH') return 'error'
  if (level === 'MEDIUM') return 'warning'
  return 'success'
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

function formatBlockedUntil(epochMs: number) {
  if (!epochMs || epochMs <= Date.now()) {
    return '未封禁'
  }
  return formatDateTime(new Date(epochMs).toISOString())
}

function deadRowKey(row: ExperimentAlertDeliveryLogItem) {
  return row.id
}

function handleDeadSelectionChange(rowKeys: Array<string | number>) {
  deadSelectedRowKeys.value = rowKeys
    .map(item => Number(item))
    .filter(item => Number.isFinite(item))
}

function healthRiskRowProps(row: AlertDeliveryHealthRiskDayItem) {
  return {
    style: 'cursor: pointer;',
    onClick: async () => {
      healthLogDateFilter.value = row.date
      await loadDeliveryHealthLogs()
      message.success(`已筛选巡检日志日期: ${row.date}`)
    }
  }
}

function buildStatsParams() {
  const [startMs, endMs] = queryForm.dateRange || []
  return {
    startDate: toDateString(startMs),
    endDate: toDateString(endMs),
    scene: queryForm.scene || undefined
  }
}

function parseConfig(value?: string): Record<string, unknown> | null {
  if (!value) return null
  try {
    const parsed: unknown = JSON.parse(value)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return null
    }
    return parsed as Record<string, unknown>
  } catch {
    return null
  }
}

function summarizeChangedFields(row: ConfigAuditItem) {
  const current = parseConfig(row.configValue)
  const previous = parseConfig(row.previousConfigValue)
  if (!current) {
    return '-'
  }
  const keySet = new Set<string>([...Object.keys(current), ...(previous ? Object.keys(previous) : [])])
  const changedKeys = Array.from(keySet).filter(key => {
    const prevValue = previous ? previous[key] : undefined
    const curValue = current[key]
    return JSON.stringify(prevValue) !== JSON.stringify(curValue)
  })
  if (changedKeys.length === 0) {
    return '-'
  }
  if (changedKeys.length <= 4) {
    return changedKeys.join(', ')
  }
  return `${changedKeys.slice(0, 4).join(', ')} +${changedKeys.length - 4}`
}

function assignGuardStatus(data: ExperimentGuardStatus) {
  Object.assign(guardStatus, data)
}

function assignConfig(data: ExperimentConfigSnapshot) {
  Object.assign(configForm, data)
}

async function loadStats() {
  statsLoading.value = true
  try {
    const params = buildStatsParams()
    const [routeRes, promptRes] = await Promise.all([
      aiExperimentApi.routeStats(params),
      aiExperimentApi.promptStats(params)
    ])
    Object.assign(routeStats, routeRes)
    Object.assign(promptStats, promptRes)
  } finally {
    statsLoading.value = false
  }
}

async function loadGuardStatus() {
  assignGuardStatus(await aiExperimentApi.status())
}

async function loadConfig() {
  assignConfig(await aiExperimentApi.config())
}

async function loadAudits() {
  auditLoading.value = true
  try {
    audits.value = await aiExperimentApi.configAudits(auditLimit.value || 20)
  } finally {
    auditLoading.value = false
  }
}

async function loadAlertLogs() {
  alertLoading.value = true
  try {
    alertLogs.value = await aiExperimentApi.alerts({
      limit: alertLimit.value || 20,
      includeSuppressed: includeSuppressedAlerts.value
    })
  } finally {
    alertLoading.value = false
  }
}

async function loadDeliveryLogs() {
  deliveryLoading.value = true
  try {
    deliveryLogs.value = await aiExperimentApi.deliveryLogs({
      limit: deliveryLimit.value || 30,
      status: deliveryStatus.value || undefined
    })
  } finally {
    deliveryLoading.value = false
  }
}

async function loadDeliverySummary() {
  summaryLoading.value = true
  try {
    Object.assign(deliverySummary, await aiExperimentApi.deliverySummary(deliverySummaryDays.value || 7))
  } finally {
    summaryLoading.value = false
  }
}

async function loadDeliveryHealth() {
  Object.assign(deliveryHealth, await aiExperimentApi.deliveryHealth(deliverySummaryDays.value || 7))
}

async function loadDeliveryHealthLogs() {
  healthLogLoading.value = true
  try {
    deliveryHealthLogs.value = await aiExperimentApi.deliveryHealthLogs({
      limit: healthLogLimit.value || 30,
      level: healthLogLevel.value || undefined,
      date: healthLogDateFilter.value || undefined
    })
  } finally {
    healthLogLoading.value = false
  }
}

async function loadDeliveryHealthTrend() {
  healthTrendLoading.value = true
  try {
    Object.assign(deliveryHealthTrend, await aiExperimentApi.deliveryHealthTrend(healthTrendDays.value || 7))
  } finally {
    healthTrendLoading.value = false
  }
}

async function loadDeliveryHealthDailyTrend() {
  healthDailyTrendLoading.value = true
  try {
    deliveryHealthDailyTrend.value = await aiExperimentApi.deliveryHealthDailyTrend(healthTrendDays.value || 7)
  } finally {
    healthDailyTrendLoading.value = false
  }
}

async function loadDeliveryHealthRiskDays() {
  healthRiskDaysLoading.value = true
  try {
    deliveryHealthRiskDays.value = await aiExperimentApi.deliveryHealthRiskDays({
      days: healthTrendDays.value || 7,
      limit: healthRiskDayLimit.value || 10
    })
  } finally {
    healthRiskDaysLoading.value = false
  }
}

async function loadDeliveryHealthRiskSummary() {
  healthRiskSummaryLoading.value = true
  try {
    Object.assign(deliveryHealthRiskSummary, await aiExperimentApi.deliveryHealthRiskSummary(healthTrendDays.value || 7))
  } finally {
    healthRiskSummaryLoading.value = false
  }
}

async function loadDeliveryHealthLogStorage() {
  healthStorageLoading.value = true
  try {
    Object.assign(deliveryHealthLogStorage, await aiExperimentApi.deliveryHealthLogStorage(healthLogRetentionDays.value || 90))
  } finally {
    healthStorageLoading.value = false
  }
}

async function loadDeliveryHealthInsights() {
  await Promise.all([
    loadDeliveryHealthTrend(),
    loadDeliveryHealthDailyTrend(),
    loadDeliveryHealthRiskDays(),
    loadDeliveryHealthRiskSummary(),
    loadDeliveryHealthLogs(),
    loadDeliveryHealthLogStorage()
  ])
}

async function loadDeliveryOverview() {
  await Promise.all([loadDeliverySummary(), loadDeliveryHealth()])
}

async function loadDeliveryHealthThresholdSuggestion() {
  thresholdSuggesting.value = true
  try {
    Object.assign(
      deliveryHealthThresholdSuggestion,
      await aiExperimentApi.deliveryHealthThresholdSuggestion(deliverySummaryDays.value || 7)
    )
  } finally {
    thresholdSuggesting.value = false
  }
}

async function loadDeliveryHealthGovernanceAdvice() {
  governanceAdviceLoading.value = true
  try {
    Object.assign(
      deliveryHealthGovernanceAdvice,
      await aiExperimentApi.deliveryHealthGovernanceAdvice(deliverySummaryDays.value || 7)
    )
  } finally {
    governanceAdviceLoading.value = false
  }
}

async function loadDeliveryHealthGovernanceReport() {
  governanceReportLoading.value = true
  try {
    Object.assign(
      deliveryHealthGovernanceReport,
      await aiExperimentApi.deliveryHealthGovernanceReport(deliverySummaryDays.value || 7)
    )
  } finally {
    governanceReportLoading.value = false
  }
}

async function loadDeadDeliveryLogs() {
  deadDeliveryLoading.value = true
  try {
    const items = await aiExperimentApi.deadDeliveryLogs(deadDeliveryLimit.value || 30)
    deadDeliveryLogs.value = items
    const latestIds = new Set(items.map(item => item.id))
    deadSelectedRowKeys.value = deadSelectedRowKeys.value.filter(id => latestIds.has(id))
  } finally {
    deadDeliveryLoading.value = false
  }
}

async function handleResetGuard(type: 'route' | 'prompt' | 'all') {
  assignGuardStatus(await aiExperimentApi.resetGuard(type))
  message.success('实验保护状态已重置')
}

async function handleBlockGuard(type: 'route' | 'prompt') {
  const cooldownSeconds = type === 'route' ? routeBlockCooldownSeconds.value : promptBlockCooldownSeconds.value
  assignGuardStatus(await aiExperimentApi.blockGuard(type, cooldownSeconds ?? undefined))
  message.success('实验分支已封禁')
}

function buildConfigUpdateRequest(): ExperimentConfigUpdateRequest {
  return {
    routeAbExperimentEnabled: configForm.routeAbExperimentEnabled,
    routeAbRolloutPercent: configForm.routeAbRolloutPercent,
    routeAbControlMode: configForm.routeAbControlMode,
    routeAbExperimentMode: configForm.routeAbExperimentMode,
    promptAbExperimentEnabled: configForm.promptAbExperimentEnabled,
    promptAbRolloutPercent: configForm.promptAbRolloutPercent,
    promptAbControlVersion: configForm.promptAbControlVersion,
    promptAbExperimentVersion: configForm.promptAbExperimentVersion,
    experimentAutoRollbackEnabled: configForm.experimentAutoRollbackEnabled,
    experimentFailureThreshold: configForm.experimentFailureThreshold,
    experimentCooldownSeconds: configForm.experimentCooldownSeconds,
    experimentAlertEnabled: configForm.experimentAlertEnabled,
    experimentAlertSuppressSeconds: configForm.experimentAlertSuppressSeconds,
    experimentAlertEmailEnabled: configForm.experimentAlertEmailEnabled,
    experimentAlertEmailRecipients: configForm.experimentAlertEmailRecipients,
    experimentAlertWebhookEnabled: configForm.experimentAlertWebhookEnabled,
    experimentAlertWebhookUrls: configForm.experimentAlertWebhookUrls,
    experimentAlertWebhookTimeoutSeconds: configForm.experimentAlertWebhookTimeoutSeconds,
    experimentAlertCriticalChannels: configForm.experimentAlertCriticalChannels,
    experimentAlertWarningChannels: configForm.experimentAlertWarningChannels,
    experimentAlertDeliveryRetryEnabled: configForm.experimentAlertDeliveryRetryEnabled,
    experimentAlertDeliveryMaxAttempts: configForm.experimentAlertDeliveryMaxAttempts,
    experimentAlertDeliveryRetryDelaySeconds: configForm.experimentAlertDeliveryRetryDelaySeconds,
    experimentAlertDeliveryHealthCheckEnabled: configForm.experimentAlertDeliveryHealthCheckEnabled,
    experimentAlertDeliveryHealthCheckDays: configForm.experimentAlertDeliveryHealthCheckDays,
    experimentAlertDeliveryHealthDeadRateThreshold: configForm.experimentAlertDeliveryHealthDeadRateThreshold,
    experimentAlertDeliveryHealthPendingRateThreshold: configForm.experimentAlertDeliveryHealthPendingRateThreshold,
    experimentAlertDeliveryHealthEscalationCooldownSeconds: configForm.experimentAlertDeliveryHealthEscalationCooldownSeconds
  }
}

async function handleSaveConfig() {
  configSubmitting.value = true
  try {
    const res = await aiExperimentApi.updateConfig(buildConfigUpdateRequest())
    assignConfig(res)
    await loadAudits()
    message.success('实验配置已保存并生效')
  } finally {
    configSubmitting.value = false
  }
}

async function handleReloadConfig() {
  configReloading.value = true
  try {
    const res = await aiExperimentApi.reloadConfig()
    assignConfig(res)
    message.success('已从配置中心重载')
  } finally {
    configReloading.value = false
  }
}

function handleRollback(auditId: number) {
  dialog.warning({
    title: '确认回滚',
    content: `确认回滚到审计记录 #${auditId} 吗？`,
    positiveText: '确认回滚',
    negativeText: '取消',
    onPositiveClick: async () => {
      const res = await aiExperimentApi.rollbackConfig(auditId)
      assignConfig(res)
      await loadAudits()
      message.success('配置回滚成功')
    }
  })
}

async function handleResetAlertSuppression() {
  const res = await aiExperimentApi.resetAlertSuppression()
  message.success(`已重置抑制状态，清理 key 数: ${res.clearedKeyCount}`)
}

async function handleCheckDeliveryHealth() {
  deliveryHealthChecking.value = true
  try {
    const res = await aiExperimentApi.checkDeliveryHealth(deliverySummaryDays.value || 7)
    Object.assign(deliveryHealth, res.healthSummary)
    const transitionText = res.levelChanged
      ? `${res.previousHealthLevel || '-'} -> ${res.currentHealthLevel || res.healthSummary.healthLevel}`
      : `${res.healthSummary.healthLevel}`
    if (res.escalated) {
      message.warning(`健康巡检触发升级告警: ${transitionText}`)
    } else if (res.warningNotified) {
      message.warning(`健康巡检触发预警通知: ${transitionText}`)
    } else if (res.recoveryNotified) {
      message.success(`健康巡检触发恢复通知: ${transitionText}`)
    } else if (res.suppressed) {
      message.info(`健康巡检命中冷却窗口: ${res.reason}`)
    } else {
      message.success(`健康巡检完成: ${transitionText}`)
    }
    Object.assign(deliverySummary, {
      rangeDays: res.healthSummary.rangeDays,
      startTime: res.healthSummary.startTime,
      endTime: res.healthSummary.endTime,
      totalCount: res.healthSummary.totalCount,
      successCount: res.healthSummary.successCount,
      pendingCount: res.healthSummary.pendingCount,
      deadCount: res.healthSummary.deadCount,
      closedCount: res.healthSummary.closedCount,
      failedCount: res.healthSummary.failedCount
    })
    await Promise.all([loadDeliveryHealthInsights(), loadDeliveryHealthGovernanceAdvice(), loadDeliveryHealthGovernanceReport()])
  } finally {
    deliveryHealthChecking.value = false
  }
}

async function handleSuggestHealthThresholds() {
  await loadDeliveryHealthThresholdSuggestion()
  message.success('阈值建议已更新')
}

async function handleGenerateGovernanceAdvice() {
  await loadDeliveryHealthGovernanceAdvice()
  message.success('治理建议已更新')
}

async function handleGenerateGovernanceReport() {
  await loadDeliveryHealthGovernanceReport()
  message.success('治理报告已更新')
}

async function handleApplySuggestedThresholds() {
  if (deliveryHealthThresholdSuggestion.sampleCount < 1) {
    message.warning('暂无足够样本生成建议阈值')
    return
  }
  thresholdApplying.value = true
  try {
    const payload = buildConfigUpdateRequest()
    payload.experimentAlertDeliveryHealthDeadRateThreshold = Number(
      deliveryHealthThresholdSuggestion.recommendedDeadRateThreshold.toFixed(4)
    )
    payload.experimentAlertDeliveryHealthPendingRateThreshold = Number(
      deliveryHealthThresholdSuggestion.recommendedPendingRateThreshold.toFixed(4)
    )
    const res = await aiExperimentApi.updateConfig(payload)
    assignConfig(res)
    await Promise.all([
      loadDeliveryOverview(),
      loadDeliveryHealthInsights(),
      loadDeliveryHealthGovernanceAdvice(),
      loadDeliveryHealthGovernanceReport(),
      loadAudits()
    ])
    message.success('建议阈值已应用并生效')
  } finally {
    thresholdApplying.value = false
  }
}

async function handleCleanupDeliveryHealthLogs() {
  healthCleanupLoading.value = true
  try {
    const res: AlertDeliveryHealthLogCleanupResult = await aiExperimentApi.cleanupDeliveryHealthLogs(
      healthLogRetentionDays.value || 90,
      healthLogCleanupLimit.value || 500
    )
    message.success(`清理完成: 删除 ${res.deletedCount} 条过期巡检日志`)
    await loadDeliveryHealthInsights()
  } finally {
    healthCleanupLoading.value = false
  }
}

async function handleClearHealthLogDateFilter() {
  healthLogDateFilter.value = ''
  await loadDeliveryHealthLogs()
}

async function handleRetryDeliveries() {
  deliveryRetrying.value = true
  try {
    const res = await aiExperimentApi.retryDeliveries(deliveryLimit.value || 30)
    message.success(`已触发重试: 选中${res.pickedCount} 成功${res.successCount} 死信${res.deadCount} 待重试${res.pendingCount}`)
    await Promise.all([loadDeliveryOverview(), loadDeliveryLogs(), loadDeadDeliveryLogs()])
  } finally {
    deliveryRetrying.value = false
  }
}

async function handleReplayDelivery(deliveryId: number) {
  const res = await aiExperimentApi.replayDelivery(deliveryId)
  message.success(`已重放 #${res.sourceDeliveryId}，新记录 #${res.replayDeliveryId}，状态 ${res.replayStatus}`)
  await Promise.all([loadDeliveryOverview(), loadDeliveryLogs(), loadDeadDeliveryLogs()])
}

async function handleBatchReplayDeadLetters() {
  if (deadSelectedRowKeys.value.length === 0) {
    message.warning('请先勾选死信记录')
    return
  }
  const res = await aiExperimentApi.replayDeliveriesBatch(deadSelectedRowKeys.value)
  message.success(
    `批量重放完成: 请求${res.requestedCount} 接受${res.acceptedCount} 成功${res.successCount} 待重试${res.pendingCount} 死信${res.deadCount} 跳过${res.skippedCount}`
  )
  deadSelectedRowKeys.value = []
  await Promise.all([loadDeliveryOverview(), loadDeliveryLogs(), loadDeadDeliveryLogs()])
}

async function handleCloseDeadDelivery(deliveryId: number) {
  const reason = deadCloseReason.value?.trim() || undefined
  const res = await aiExperimentApi.closeDeadDelivery(deliveryId, reason)
  message.success(`已关闭死信 #${res.deliveryId}，原因: ${res.reason || '-'}`)
  await Promise.all([loadDeliveryOverview(), loadDeliveryLogs(), loadDeadDeliveryLogs()])
}

async function handleBatchCloseDeadLetters() {
  if (deadSelectedRowKeys.value.length === 0) {
    message.warning('请先勾选死信记录')
    return
  }
  const reason = deadCloseReason.value?.trim() || undefined
  const res = await aiExperimentApi.closeDeadDeliveriesBatch(deadSelectedRowKeys.value, reason)
  message.success(`批量关闭完成: 请求${res.requestedCount} 已关闭${res.closedCount} 跳过${res.skippedCount}`)
  deadSelectedRowKeys.value = []
  await Promise.all([loadDeliveryOverview(), loadDeliveryLogs(), loadDeadDeliveryLogs()])
}

onMounted(async () => {
  await Promise.all([
    loadStats(),
    loadGuardStatus(),
    loadConfig(),
    loadAudits(),
    loadAlertLogs(),
    loadDeliveryOverview(),
    loadDeliveryHealthThresholdSuggestion(),
    loadDeliveryHealthGovernanceAdvice(),
    loadDeliveryHealthGovernanceReport(),
    loadDeliveryHealthInsights(),
    loadDeliveryLogs(),
    loadDeadDeliveryLogs()
  ])
})
</script>

<style scoped>
.guard-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 28px;
}

.guard-label {
  color: #222;
  font-weight: 600;
}

.guard-text {
  color: #666;
  font-size: 13px;
}

.advice-list {
  margin: 0;
  padding-left: 18px;
  color: #666;
  font-size: 13px;
}
</style>
