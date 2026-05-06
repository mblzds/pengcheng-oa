#!/usr/bin/env node

const fs = require('fs')
const path = require('path')

const projectRoot = path.resolve(__dirname, '..')
const pagesJsonPath = path.join(projectRoot, 'pages.json')
const allowLocalhostFiles = new Set([
  path.join(projectRoot, 'utils', 'config.js')
])

const errors = []
const warnings = []

const readText = (filePath) => fs.readFileSync(filePath, 'utf8')

const walkFiles = (dir, filterExt = ['.vue', '.js']) => {
  const items = fs.readdirSync(dir, { withFileTypes: true })
  const files = []
  for (const item of items) {
    const fullPath = path.join(dir, item.name)
    if (item.isDirectory()) {
      files.push(...walkFiles(fullPath, filterExt))
      continue
    }
    if (filterExt.includes(path.extname(item.name))) {
      files.push(fullPath)
    }
  }
  return files
}

const parsePagesJson = () => {
  try {
    const data = JSON.parse(readText(pagesJsonPath))
    return data
  } catch (err) {
    errors.push(`pages.json 解析失败: ${err.message}`)
    return null
  }
}

const checkPagesDefinition = (pagesJson) => {
  if (!pagesJson) return
  const pages = Array.isArray(pagesJson.pages) ? pagesJson.pages : []
  const pagePathSet = new Set()

  for (const page of pages) {
    if (!page || !page.path) {
      warnings.push('发现无效页面定义（缺少 path）')
      continue
    }
    pagePathSet.add(page.path)
    const pageFile = path.join(projectRoot, `${page.path}.vue`)
    if (!fs.existsSync(pageFile)) {
      errors.push(`页面未找到: ${path.relative(projectRoot, pageFile)}`)
    }
  }

  const tabList = pagesJson.tabBar?.list || []
  for (const item of tabList) {
    if (!item?.pagePath) continue
    if (!pagePathSet.has(item.pagePath)) {
      errors.push(`tabBar pagePath 未在 pages 中注册: ${item.pagePath}`)
    }
  }
}

const checkHardcodedBaseUrl = (filePath, content) => {
  const lines = content.split(/\r?\n/)
  const pattern = /https?:\/\/(?:localhost|127\.0\.0\.1):\d+/
  lines.forEach((line, index) => {
    if (pattern.test(line) && !allowLocalhostFiles.has(filePath)) {
      errors.push(`硬编码本地地址: ${path.relative(projectRoot, filePath)}:${index + 1}`)
    }
  })
}

const checkViewportRisks = (filePath, content) => {
  const hasVh = /(?:min-height|height):\s*100vh/.test(content)
  const hasDvh = /100dvh/.test(content)
  if (hasVh && !hasDvh) {
    warnings.push(`页面仅使用 100vh，建议补充 100dvh: ${path.relative(projectRoot, filePath)}`)
  }

  const fixedBottomBlocks = content.match(/[^{]+\{[^{}]*position:\s*fixed[^{}]*bottom:\s*0[^{}]*\}/gs) || []
  for (const block of fixedBottomBlocks) {
    // 全屏遮罩（top:0 + bottom:0）不需要额外安全区
    if (/top:\s*0/.test(block)) {
      continue
    }
    const hasSafeArea = /safe-area|env\(safe-area-inset-bottom\)|constant\(safe-area-inset-bottom\)/.test(block)
    if (!hasSafeArea) {
      warnings.push(`固定底部区域缺少安全区适配: ${path.relative(projectRoot, filePath)}`)
      break
    }
  }
}

const run = () => {
  const pagesJson = parsePagesJson()
  checkPagesDefinition(pagesJson)

  const scanTargets = [
    path.join(projectRoot, 'App.vue'),
    ...walkFiles(path.join(projectRoot, 'pages')),
    ...walkFiles(path.join(projectRoot, 'utils'))
  ]

  for (const filePath of scanTargets) {
    const content = readText(filePath)
    checkHardcodedBaseUrl(filePath, content)
    checkViewportRisks(filePath, content)
  }

  console.log('[verify-mobile-miniapp] 扫描完成')
  console.log(`错误: ${errors.length}，警告: ${warnings.length}`)

  if (warnings.length) {
    console.log('\n警告明细:')
    warnings.forEach(item => console.log(`- ${item}`))
  }

  if (errors.length) {
    console.error('\n错误明细:')
    errors.forEach(item => console.error(`- ${item}`))
    process.exit(1)
  }
}

run()
