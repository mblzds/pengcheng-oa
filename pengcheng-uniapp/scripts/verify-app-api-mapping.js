#!/usr/bin/env node

const fs = require('fs')
const path = require('path')

const uniappRoot = path.resolve(__dirname, '..')
const projectRoot = path.resolve(uniappRoot, '..')
const apiFile = path.join(uniappRoot, 'utils', 'api.js')
const controllerRoot = path.join(projectRoot, 'pengcheng-api', 'pengcheng-app-api', 'src', 'main', 'java', 'com', 'pengcheng', 'app', 'controller')

const normalizePath = (value) => {
  return String(value || '')
    .replace(/\?.*$/, '')
    .replace(/\$\{[^}]+\}/g, '{param}')
    .replace(/\{[^}]+\}/g, '{param}')
    .replace(/\/+/g, '/')
    .replace(/\/$/, '')
}

const parseFrontendAppApis = () => {
  const content = fs.readFileSync(apiFile, 'utf8')
  const matches = content.match(/\/api\/app\/[A-Za-z0-9_/$?{}.-]*/g) || []
  return new Set(matches.map(normalizePath))
}

const parseControllerMappings = () => {
  const routes = new Set()
  const files = fs.readdirSync(controllerRoot).filter(file => file.endsWith('.java'))

  files.forEach(file => {
    const content = fs.readFileSync(path.join(controllerRoot, file), 'utf8')
    const classMatch = content.match(/@RequestMapping\("([^"]+)"\)/)
    const classPath = classMatch ? classMatch[1] : ''

    const methodRegex = /@(GetMapping|PostMapping|PutMapping|DeleteMapping)(?:\("([^"]*)"\))?/g
    let match
    while ((match = methodRegex.exec(content)) !== null) {
      const methodPath = match[2] || ''
      const joined = `/api${classPath}${methodPath ? (methodPath.startsWith('/') ? methodPath : `/${methodPath}`) : ''}`
      routes.add(normalizePath(joined))
    }
  })

  return routes
}

const frontendApis = parseFrontendAppApis()
const backendApis = parseControllerMappings()

const missingInBackend = [...frontendApis].filter(route => !backendApis.has(route))
const missingInFrontend = [...backendApis].filter(route => !frontendApis.has(route))

console.log('[verify-app-api-mapping] 扫描完成')
console.log(`前端接口: ${frontendApis.size}，后端接口: ${backendApis.size}`)

if (missingInBackend.length) {
  console.error('\n前端存在但后端未发现:')
  missingInBackend.forEach(route => console.error(`- ${route}`))
}

if (missingInFrontend.length) {
  console.warn('\n后端存在但前端未使用:')
  missingInFrontend.forEach(route => console.warn(`- ${route}`))
}

if (missingInBackend.length) {
  process.exit(1)
}

