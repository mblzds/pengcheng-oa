import { request } from '@/utils/request'

export type RouteMode = 'rule' | 'agent' | 'hybrid'
export type PromptVersion = 'v1' | 'v2'

export interface ExperimentGuardStatus {
  routeExperimentAllowed: boolean
  routeBlockedUntilEpochMs: number
  routeFailureCount: number
  promptExperimentAllowed: boolean
  promptBlockedUntilEpochMs: number
  promptFailureCount: number
}

export interface ExperimentConfigSnapshot {
  routeAbExperimentEnabled: boolean
  routeAbRolloutPercent: number
  routeAbControlMode: RouteMode
  routeAbExperimentMode: RouteMode
  promptAbExperimentEnabled: boolean
  promptAbRolloutPercent: number
  promptAbControlVersion: PromptVersion
  promptAbExperimentVersion: PromptVersion
  experimentAutoRollbackEnabled: boolean
  experimentFailureThreshold: number
  experimentCooldownSeconds: number
  experimentAlertEnabled: boolean
  experimentAlertSuppressSeconds: number
  experimentAlertEmailEnabled: boolean
  experimentAlertEmailRecipients: string
  experimentAlertWebhookEnabled: boolean
  experimentAlertWebhookUrls: string
  experimentAlertWebhookTimeoutSeconds: number
  experimentAlertCriticalChannels: string
  experimentAlertWarningChannels: string
  experimentAlertDeliveryRetryEnabled: boolean
  experimentAlertDeliveryMaxAttempts: number
  experimentAlertDeliveryRetryDelaySeconds: number
  experimentAlertDeliveryHealthCheckEnabled: boolean
  experimentAlertDeliveryHealthCheckDays: number
  experimentAlertDeliveryHealthDeadRateThreshold: number
  experimentAlertDeliveryHealthPendingRateThreshold: number
  experimentAlertDeliveryHealthEscalationCooldownSeconds: number
}

export interface ExperimentConfigUpdateRequest {
  routeAbExperimentEnabled?: boolean
  routeAbRolloutPercent?: number
  routeAbControlMode?: RouteMode
  routeAbExperimentMode?: RouteMode
  promptAbExperimentEnabled?: boolean
  promptAbRolloutPercent?: number
  promptAbControlVersion?: PromptVersion
  promptAbExperimentVersion?: PromptVersion
  experimentAutoRollbackEnabled?: boolean
  experimentFailureThreshold?: number
  experimentCooldownSeconds?: number
  experimentAlertEnabled?: boolean
  experimentAlertSuppressSeconds?: number
  experimentAlertEmailEnabled?: boolean
  experimentAlertEmailRecipients?: string
  experimentAlertWebhookEnabled?: boolean
  experimentAlertWebhookUrls?: string
  experimentAlertWebhookTimeoutSeconds?: number
  experimentAlertCriticalChannels?: string
  experimentAlertWarningChannels?: string
  experimentAlertDeliveryRetryEnabled?: boolean
  experimentAlertDeliveryMaxAttempts?: number
  experimentAlertDeliveryRetryDelaySeconds?: number
  experimentAlertDeliveryHealthCheckEnabled?: boolean
  experimentAlertDeliveryHealthCheckDays?: number
  experimentAlertDeliveryHealthDeadRateThreshold?: number
  experimentAlertDeliveryHealthPendingRateThreshold?: number
  experimentAlertDeliveryHealthEscalationCooldownSeconds?: number
}

export interface ConfigAuditItem {
  id: number
  changeType: string
  source?: string
  operatorId?: number
  operatorName?: string
  rollbackFromAuditId?: number
  previousConfigValue?: string
  configValue?: string
  remark?: string
  createTime?: string
}

export interface ExperimentAlertLogItem {
  id: number
  alertType: string
  experimentType: string
  triggerSource: string
  title: string
  content: string
  dedupeKey: string
  suppressed: boolean
  suppressedUntilEpochMs?: number
  metadataJson?: string
  createTime?: string
}

export interface AlertSuppressionState {
  clearedKeyCount: number
  suppressSeconds: number
}

export interface ExperimentAlertDeliveryLogItem {
  id: number
  alertLogId: number
  alertType: string
  alertLevel: string
  channel: string
  targetValue: string
  status: string
  attemptCount: number
  maxAttempts: number
  nextRetryTime?: string
  lastResponseCode?: number
  lastErrorMessage?: string
  createTime?: string
  updateTime?: string
}

export interface AlertDeliveryRetrySummary {
  pickedCount: number
  successCount: number
  deadCount: number
  pendingCount: number
}

export interface AlertDeliveryReplayResult {
  sourceDeliveryId: number
  replayDeliveryId: number
  replayStatus: string
}

export interface AlertDeliveryBatchReplayResult {
  requestedCount: number
  acceptedCount: number
  successCount: number
  pendingCount: number
  deadCount: number
  closedCount: number
  skippedCount: number
}

export interface AlertDeliveryCloseResult {
  deliveryId: number
  status: string
  reason: string
  closeTime?: string
}

export interface AlertDeliveryBatchCloseResult {
  requestedCount: number
  closedCount: number
  skippedCount: number
}

export interface AlertDeliverySummary {
  rangeDays: number
  startTime?: string
  endTime?: string
  totalCount: number
  successCount: number
  pendingCount: number
  deadCount: number
  closedCount: number
  failedCount: number
}

export interface AlertDeliveryHealthSummary {
  rangeDays: number
  startTime?: string
  endTime?: string
  totalCount: number
  successCount: number
  pendingCount: number
  deadCount: number
  closedCount: number
  failedCount: number
  deadRate: number
  pendingRate: number
  deadRateThreshold: number
  pendingRateThreshold: number
  healthLevel: 'HEALTHY' | 'WARNING' | 'CRITICAL'
  suggestion: string
}

export interface AlertDeliveryHealthCheckResult {
  healthSummary: AlertDeliveryHealthSummary
  escalated: boolean
  suppressed: boolean
  warningNotified: boolean
  recoveryNotified: boolean
  previousHealthLevel?: string
  currentHealthLevel?: string
  levelChanged: boolean
  checkTime?: string
  reason: string
}

export interface AlertDeliveryHealthLogItem {
  id: number
  rangeDays: number
  healthLevel: 'HEALTHY' | 'WARNING' | 'CRITICAL'
  previousHealthLevel?: string
  levelChanged: boolean
  escalated: boolean
  suppressed: boolean
  warningNotified: boolean
  recoveryNotified: boolean
  reason: string
  deadRate: number
  pendingRate: number
  deadRateThreshold: number
  pendingRateThreshold: number
  totalCount: number
  successCount: number
  pendingCount: number
  deadCount: number
  closedCount: number
  failedCount: number
  suggestion?: string
  checkSource?: string
  checkTime?: string
  createTime?: string
}

export interface AlertDeliveryHealthTrendSummary {
  rangeDays: number
  startTime?: string
  endTime?: string
  totalChecks: number
  healthyCount: number
  warningCount: number
  criticalCount: number
  levelChangedCount: number
  escalatedCount: number
  warningNotifiedCount: number
  recoveryNotifiedCount: number
  latest?: AlertDeliveryHealthLogItem
}

export interface AlertDeliveryHealthDailyTrendItem {
  date: string
  totalChecks: number
  healthyCount: number
  warningCount: number
  criticalCount: number
  escalatedCount: number
  avgDeadRate: number
  avgPendingRate: number
  maxDeadRate: number
  maxPendingRate: number
}

export interface AlertDeliveryHealthRiskDayItem {
  date: string
  riskScore: number
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  insight: string
  totalChecks: number
  healthyCount: number
  warningCount: number
  criticalCount: number
  escalatedCount: number
  avgDeadRate: number
  avgPendingRate: number
  maxDeadRate: number
  maxPendingRate: number
}

export interface AlertDeliveryHealthRiskSummary {
  rangeDays: number
  startTime?: string
  endTime?: string
  totalRiskDays: number
  highRiskDays: number
  mediumRiskDays: number
  lowRiskDays: number
  maxRiskScore: number
  maxRiskDate?: string
  topHighRiskDates: string[]
}

export interface AlertDeliveryHealthGovernanceAdvice {
  rangeDays: number
  startTime?: string
  endTime?: string
  overallRiskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  keyFindings: string[]
  recommendedActions: string[]
  generatedAt?: string
}

export interface AlertDeliveryHealthGovernanceReport {
  rangeDays: number
  generatedAt?: string
  overallRiskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  content: string
}

export interface AlertDeliveryHealthLogStorageSummary {
  retainDays: number
  cutoffTime?: string
  totalCount: number
  expiredCount: number
}

export interface AlertDeliveryHealthThresholdSuggestion {
  rangeDays: number
  startTime?: string
  endTime?: string
  sampleCount: number
  avgDeadRate: number
  avgPendingRate: number
  p90DeadRate: number
  p90PendingRate: number
  currentDeadRateThreshold: number
  currentPendingRateThreshold: number
  recommendedDeadRateThreshold: number
  recommendedPendingRateThreshold: number
  suggestion: string
  confidenceLevel: 'LOW' | 'MEDIUM' | 'HIGH'
}

export interface AlertDeliveryHealthLogCleanupResult {
  retainDays: number
  limit: number
  deletedCount: number
  cutoffTime?: string
  triggerSource: 'manual' | 'scheduler'
  cleanupTime?: string
}

export interface RouteExperimentStat {
  date: string
  scene: string
  experimentGroup: string
  routeMode: string
  total: number
  success: number
  failure: number
  successRate: number
  avgLatencyMs: number
}

export interface RouteExperimentSummary {
  total: number
  success: number
  failure: number
  successRate: number
  avgLatencyMs: number
}

export interface RouteExperimentStatsResponse {
  startDate: string
  endDate: string
  items: RouteExperimentStat[]
  summary: RouteExperimentSummary
}

export interface PromptExperimentStat {
  date: string
  scene: string
  experimentGroup: string
  promptVersion: string
  total: number
  success: number
  failure: number
  successRate: number
  avgLatencyMs: number
}

export interface PromptExperimentSummary {
  total: number
  success: number
  failure: number
  successRate: number
  avgLatencyMs: number
}

export interface PromptExperimentStatsResponse {
  startDate: string
  endDate: string
  items: PromptExperimentStat[]
  summary: PromptExperimentSummary
}

export const aiExperimentApi = {
  routeStats(params: { startDate?: string; endDate?: string; scene?: string }) {
    return request<RouteExperimentStatsResponse>({
      url: '/admin/ai/experiment/route-stats',
      method: 'get',
      params
    })
  },

  promptStats(params: { startDate?: string; endDate?: string; scene?: string }) {
    return request<PromptExperimentStatsResponse>({
      url: '/admin/ai/experiment/prompt-stats',
      method: 'get',
      params
    })
  },

  status() {
    return request<ExperimentGuardStatus>({
      url: '/admin/ai/experiment/status',
      method: 'get'
    })
  },

  config() {
    return request<ExperimentConfigSnapshot>({
      url: '/admin/ai/experiment/config',
      method: 'get'
    })
  },

  updateConfig(data: ExperimentConfigUpdateRequest) {
    return request<ExperimentConfigSnapshot>({
      url: '/admin/ai/experiment/config',
      method: 'post',
      data
    })
  },

  reloadConfig() {
    return request<ExperimentConfigSnapshot>({
      url: '/admin/ai/experiment/config/reload',
      method: 'post'
    })
  },

  configAudits(limit?: number) {
    return request<ConfigAuditItem[]>({
      url: '/admin/ai/experiment/config/audits',
      method: 'get',
      params: { limit }
    })
  },

  rollbackConfig(auditId: number) {
    return request<ExperimentConfigSnapshot>({
      url: '/admin/ai/experiment/config/rollback',
      method: 'post',
      params: { auditId }
    })
  },

  resetGuard(type: 'route' | 'prompt' | 'all') {
    return request<ExperimentGuardStatus>({
      url: '/admin/ai/experiment/guard/reset',
      method: 'post',
      params: { type }
    })
  },

  blockGuard(type: 'route' | 'prompt', cooldownSeconds?: number) {
    return request<ExperimentGuardStatus>({
      url: '/admin/ai/experiment/guard/block',
      method: 'post',
      params: { type, cooldownSeconds }
    })
  },

  alerts(params: { limit?: number; includeSuppressed?: boolean }) {
    return request<ExperimentAlertLogItem[]>({
      url: '/admin/ai/experiment/alerts',
      method: 'get',
      params
    })
  },

  resetAlertSuppression() {
    return request<AlertSuppressionState>({
      url: '/admin/ai/experiment/alerts/suppression/reset',
      method: 'post'
    })
  },

  deliveryLogs(params: { limit?: number; status?: string }) {
    return request<ExperimentAlertDeliveryLogItem[]>({
      url: '/admin/ai/experiment/alerts/deliveries',
      method: 'get',
      params
    })
  },

  retryDeliveries(limit?: number) {
    return request<AlertDeliveryRetrySummary>({
      url: '/admin/ai/experiment/alerts/deliveries/retry',
      method: 'post',
      params: { limit }
    })
  },

  deadDeliveryLogs(limit?: number) {
    return request<ExperimentAlertDeliveryLogItem[]>({
      url: '/admin/ai/experiment/alerts/deliveries/dead',
      method: 'get',
      params: { limit }
    })
  },

  replayDelivery(deliveryId: number) {
    return request<AlertDeliveryReplayResult>({
      url: '/admin/ai/experiment/alerts/deliveries/replay',
      method: 'post',
      params: { deliveryId }
    })
  },

  replayDeliveriesBatch(deliveryIds: number[]) {
    return request<AlertDeliveryBatchReplayResult>({
      url: '/admin/ai/experiment/alerts/deliveries/replay/batch',
      method: 'post',
      data: { deliveryIds }
    })
  },

  closeDeadDelivery(deliveryId: number, reason?: string) {
    return request<AlertDeliveryCloseResult>({
      url: '/admin/ai/experiment/alerts/deliveries/dead/close',
      method: 'post',
      params: { deliveryId, reason }
    })
  },

  closeDeadDeliveriesBatch(deliveryIds: number[], reason?: string) {
    return request<AlertDeliveryBatchCloseResult>({
      url: '/admin/ai/experiment/alerts/deliveries/dead/close/batch',
      method: 'post',
      data: { deliveryIds, reason }
    })
  },

  deliverySummary(days?: number) {
    return request<AlertDeliverySummary>({
      url: '/admin/ai/experiment/alerts/deliveries/summary',
      method: 'get',
      params: { days }
    })
  },

  deliveryHealth(days?: number, deadRateThreshold?: number, pendingRateThreshold?: number) {
    return request<AlertDeliveryHealthSummary>({
      url: '/admin/ai/experiment/alerts/deliveries/health',
      method: 'get',
      params: { days, deadRateThreshold, pendingRateThreshold }
    })
  },

  deliveryHealthLogs(params: { limit?: number; level?: string; date?: string }) {
    return request<AlertDeliveryHealthLogItem[]>({
      url: '/admin/ai/experiment/alerts/deliveries/health/logs',
      method: 'get',
      params
    })
  },

  deliveryHealthTrend(days?: number) {
    return request<AlertDeliveryHealthTrendSummary>({
      url: '/admin/ai/experiment/alerts/deliveries/health/trend',
      method: 'get',
      params: { days }
    })
  },

  deliveryHealthDailyTrend(days?: number) {
    return request<AlertDeliveryHealthDailyTrendItem[]>({
      url: '/admin/ai/experiment/alerts/deliveries/health/trend/daily',
      method: 'get',
      params: { days }
    })
  },

  deliveryHealthRiskDays(params: { days?: number; limit?: number }) {
    return request<AlertDeliveryHealthRiskDayItem[]>({
      url: '/admin/ai/experiment/alerts/deliveries/health/trend/risk-days',
      method: 'get',
      params
    })
  },

  deliveryHealthRiskSummary(days?: number) {
    return request<AlertDeliveryHealthRiskSummary>({
      url: '/admin/ai/experiment/alerts/deliveries/health/trend/risk-summary',
      method: 'get',
      params: { days }
    })
  },

  deliveryHealthGovernanceAdvice(days?: number) {
    return request<AlertDeliveryHealthGovernanceAdvice>({
      url: '/admin/ai/experiment/alerts/deliveries/health/governance-advice',
      method: 'get',
      params: { days }
    })
  },

  deliveryHealthGovernanceReport(days?: number) {
    return request<AlertDeliveryHealthGovernanceReport>({
      url: '/admin/ai/experiment/alerts/deliveries/health/governance-report',
      method: 'get',
      params: { days }
    })
  },

  deliveryHealthThresholdSuggestion(days?: number) {
    return request<AlertDeliveryHealthThresholdSuggestion>({
      url: '/admin/ai/experiment/alerts/deliveries/health/threshold-suggestion',
      method: 'get',
      params: { days }
    })
  },

  deliveryHealthLogStorage(retainDays?: number) {
    return request<AlertDeliveryHealthLogStorageSummary>({
      url: '/admin/ai/experiment/alerts/deliveries/health/logs/storage',
      method: 'get',
      params: { retainDays }
    })
  },

  cleanupDeliveryHealthLogs(retainDays?: number, limit?: number) {
    return request<AlertDeliveryHealthLogCleanupResult>({
      url: '/admin/ai/experiment/alerts/deliveries/health/logs/cleanup',
      method: 'post',
      params: { retainDays, limit }
    })
  },

  checkDeliveryHealth(days?: number, deadRateThreshold?: number, pendingRateThreshold?: number) {
    return request<AlertDeliveryHealthCheckResult>({
      url: '/admin/ai/experiment/alerts/deliveries/health/check',
      method: 'post',
      params: { days, deadRateThreshold, pendingRateThreshold }
    })
  }
}
