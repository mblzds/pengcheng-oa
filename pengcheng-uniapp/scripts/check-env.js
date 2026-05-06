#!/usr/bin/env node

/**
 * MasterLife APP 打包环境检查工具
 * 用于检查打包环境是否就绪
 */

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const COLORS = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m'
};

function log(message, color = 'reset') {
  console.log(`${COLORS[color]}${message}${COLORS.reset}`);
}

function checkCommand(cmd, name) {
  try {
    execSync(`${cmd} --version`, { stdio: 'ignore' });
    const version = execSync(`${cmd} --version`, { encoding: 'utf8' }).trim();
    log(`✓ ${name} 已安装: ${version}`, 'green');
    return true;
  } catch (error) {
    log(`✗ ${name} 未安装`, 'red');
    return false;
  }
}

function checkFile(filePath, description) {
  if (fs.existsSync(filePath)) {
    log(`✓ ${description} 存在`, 'green');
    return true;
  } else {
    log(`✗ ${description} 不存在: ${filePath}`, 'red');
    return false;
  }
}

function checkDir(dirPath, description) {
  if (fs.existsSync(dirPath) && fs.statSync(dirPath).isDirectory()) {
    log(`✓ ${description} 存在`, 'green');
    return true;
  } else {
    log(`✗ ${description} 不存在: ${dirPath}`, 'red');
    return false;
  }
}

function main() {
  log('\n========================================', 'blue');
  log('  MasterLife APP 打包环境检查', 'blue');
  log('========================================\n', 'blue');

  let passed = 0;
  let total = 0;

  // 检查 Node.js
  total++;
  if (checkCommand('node', 'Node.js')) passed++;

  // 检查 npm
  total++;
  if (checkCommand('npm', 'npm')) passed++;

  // 检查项目目录
  log('\n--- 项目结构检查 ---', 'yellow');
  
  total++;
  if (checkDir('pages', '页面目录')) passed++;
  
  total++;
  if (checkDir('static', '静态资源目录')) passed++;
  
  total++;
  if (checkFile('manifest.json', 'manifest.json')) passed++;
  
  total++;
  if (checkFile('pages.json', 'pages.json')) passed++;
  
  total++;
  if (checkFile('App.vue', 'App.vue')) passed++;
  
  total++;
  if (checkFile('main.js', 'main.js')) passed++;

  // 检查依赖
  log('\n--- 依赖检查 ---', 'yellow');
  
  total++;
  if (checkFile('package.json', 'package.json')) passed++;
  
  if (fs.existsSync('package.json')) {
    const pkg = JSON.parse(fs.readFileSync('package.json', 'utf8'));
    const deps = pkg.dependencies || {};
    
    total++;
    if (deps['uview-plus']) {
      log(`✓ uview-plus 已安装: ${deps['uview-plus']}`, 'green');
      passed++;
    } else {
      log(`✗ uview-plus 未安装`, 'red');
    }
    
    total++;
    if (deps['crypto-js']) {
      log(`✓ crypto-js 已安装: ${deps['crypto-js']}`, 'green');
      passed++;
    } else {
      log(`✗ crypto-js 未安装`, 'red');
    }
  }

  // 检查 node_modules
  total++;
  if (checkDir('node_modules', 'node_modules')) passed++;

  // 总结
  log('\n========================================', 'blue');
  log(`  检查结果：${passed}/${total} 通过`, passed === total ? 'green' : 'yellow');
  log('========================================\n', 'blue');

  if (passed === total) {
    log('✓ 环境检查通过！可以开始打包', 'green');
    log('\n下一步操作:', 'yellow');
    log('1. 使用 HBuilderX 打开本项目', 'blue');
    log('2. 配置 manifest.json 中的证书和包名', 'blue');
    log('3. 菜单：发行 → 原生 App-云打包', 'blue');
    log('4. 等待打包完成后下载安装包', 'blue');
  } else {
    log('✗ 环境检查未通过，请先修复上述问题', 'red');
    log('\n修复建议:', 'yellow');
    log('1. 安装 Node.js: https://nodejs.org/', 'blue');
    log('2. 运行 npm install 安装依赖', 'blue');
    log('3. 确保项目文件完整', 'blue');
  }

  console.log('');
  process.exit(passed === total ? 0 : 1);
}

main();
