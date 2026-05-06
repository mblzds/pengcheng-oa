<template>
  <div class="page-container">
    <n-card class="config-card">
      <!-- Tab 导航 -->
      <n-tabs v-model:value="activeTab" type="line" animated @update:value="handleTabChange">
        <n-tab-pane v-for="group in configGroups" :key="group.groupCode" :name="group.groupCode" :tab="group.groupName">
          <div class="config-content">
            <!-- 系统配置 -->
            <template v-if="group.groupCode === 'system'">
              <n-form :model="configs.system" label-placement="left" label-width="120">
                <n-form-item label="站点名称">
                  <n-input v-model:value="configs.system.siteName" placeholder="请输入站点名称" />
                </n-form-item>
                <n-form-item label="站点描述">
                  <n-input v-model:value="configs.system.siteDescription" type="textarea" placeholder="请输入站点描述" />
                </n-form-item>
                <n-form-item label="站点Logo">
                  <div class="logo-upload">
                    <div class="logo-preview" v-if="configs.system.siteLogo">
                      <img :src="configs.system.siteLogo" alt="Logo" />
                      <n-button size="small" quaternary type="error" class="logo-delete" @click="configs.system.siteLogo = ''">
                        <template #icon><n-icon><CloseOutline /></n-icon></template>
                      </n-button>
                    </div>
                    <n-upload
                      v-else
                      :custom-request="handleLogoUpload"
                      :show-file-list="false"
                      accept="image/*"
                    >
                      <div class="logo-upload-trigger">
                        <n-icon size="24"><ImageOutline /></n-icon>
                        <span>点击上传Logo</span>
                      </div>
                    </n-upload>
                    <n-input v-model:value="configs.system.siteLogo" placeholder="或输入Logo地址" style="margin-top: 8px" />
                  </div>
                </n-form-item>
                <n-form-item label="版权信息">
                  <n-input v-model:value="configs.system.copyright" placeholder="请输入版权信息" />
                </n-form-item>
                <n-form-item label="ICP备案号">
                  <n-input v-model:value="configs.system.icp" placeholder="请输入ICP备案号" />
                </n-form-item>
                <n-divider />
                <n-form-item label="启用水印">
                  <n-switch v-model:value="configs.system.watermarkEnabled" />
                  <span class="form-hint">开启后页面将显示用户名水印</span>
                </n-form-item>
                <n-form-item label="水印内容" v-if="configs.system.watermarkEnabled">
                  <n-select v-model:value="configs.system.watermarkType" :options="watermarkTypeOptions" style="width: 200px" />
                </n-form-item>
                <n-form-item label="水印透明度" v-if="configs.system.watermarkEnabled">
                  <n-slider v-model:value="configs.system.watermarkOpacity" :min="0.01" :max="0.3" :step="0.01" style="width: 200px" />
                  <span class="form-hint" style="margin-left: 12px">{{ (configs.system.watermarkOpacity * 100).toFixed(0) }}%</span>
                </n-form-item>
              </n-form>
            </template>

            <!-- 注册配置 -->
            <template v-else-if="group.groupCode === 'register'">
              <n-form :model="configs.register" label-placement="left" label-width="120">
                <n-form-item label="开放注册">
                  <n-switch v-model:value="configs.register.enabled" />
                </n-form-item>
                <n-form-item label="邮箱验证">
                  <n-switch v-model:value="configs.register.verifyEmail" />
                </n-form-item>
                <n-form-item label="手机验证">
                  <n-switch v-model:value="configs.register.verifyPhone" />
                </n-form-item>
                <n-form-item label="默认角色">
                  <n-input v-model:value="configs.register.defaultRole" placeholder="请输入默认角色编码" />
                </n-form-item>
                <n-form-item label="需要审核">
                  <n-switch v-model:value="configs.register.needAudit" />
                </n-form-item>
              </n-form>
            </template>

            <!-- 登录配置 -->
            <template v-else-if="group.groupCode === 'login'">
              <n-form :model="configs.login" label-placement="left" label-width="120">
                <n-form-item label="验证码">
                  <n-switch v-model:value="configs.login.captchaEnabled" />
                </n-form-item>
                <n-form-item label="验证码类型" v-if="configs.login.captchaEnabled">
                  <n-select v-model:value="configs.login.captchaType" :options="captchaTypeOptions" style="width: 200px" />
                </n-form-item>
                <n-form-item label="最大重试次数">
                  <n-input-number v-model:value="configs.login.maxRetryCount" :min="1" :max="10" />
                </n-form-item>
                <n-form-item label="锁定时间(分钟)">
                  <n-input-number v-model:value="configs.login.lockTime" :min="1" :max="1440" />
                </n-form-item>
                <n-form-item label="记住我">
                  <n-switch v-model:value="configs.login.rememberMe" />
                </n-form-item>

              </n-form>
            </template>

            <!-- 密码配置 -->
            <template v-else-if="group.groupCode === 'password'">
              <n-form :model="configs.password" label-placement="left" label-width="150">
                <n-form-item label="最小长度">
                  <n-input-number v-model:value="configs.password.minLength" :min="4" :max="32" />
                </n-form-item>
                <n-form-item label="最大长度">
                  <n-input-number v-model:value="configs.password.maxLength" :min="6" :max="64" />
                </n-form-item>
                <n-form-item label="必须包含大写字母">
                  <n-switch v-model:value="configs.password.requireUppercase" />
                </n-form-item>
                <n-form-item label="必须包含小写字母">
                  <n-switch v-model:value="configs.password.requireLowercase" />
                </n-form-item>
                <n-form-item label="必须包含数字">
                  <n-switch v-model:value="configs.password.requireNumber" />
                </n-form-item>
                <n-form-item label="必须包含特殊字符">
                  <n-switch v-model:value="configs.password.requireSpecial" />
                </n-form-item>
                <n-form-item label="密码过期天数">
                  <n-input-number v-model:value="configs.password.expireDays" :min="0" :max="365" />
                  <span class="form-hint">0表示永不过期</span>
                </n-form-item>
              </n-form>
            </template>

            <!-- 邮件配置 -->
            <template v-else-if="group.groupCode === 'email'">
              <n-form :model="configs.email" label-placement="left" label-width="120">
                <n-form-item label="启用邮件">
                  <n-switch v-model:value="configs.email.enabled" />
                </n-form-item>
                <n-form-item label="SMTP服务器">
                  <n-input v-model:value="configs.email.host" placeholder="如: smtp.qq.com" />
                </n-form-item>
                <n-form-item label="端口">
                  <n-input-number v-model:value="configs.email.port" :min="1" :max="65535" />
                </n-form-item>
                <n-form-item label="用户名">
                  <n-input v-model:value="configs.email.username" placeholder="发件人邮箱" />
                </n-form-item>
                <n-form-item label="密码/授权码">
                  <n-input v-model:value="configs.email.password" type="password" show-password-on="click" placeholder="邮箱密码或授权码" />
                </n-form-item>
                <n-form-item label="发件人名称">
                  <n-input v-model:value="configs.email.fromName" placeholder="显示的发件人名称" />
                </n-form-item>
                <n-form-item label="SSL加密">
                  <n-switch v-model:value="configs.email.ssl" />
                </n-form-item>
                <n-divider />
                <n-form-item label="测试邮件">
                  <n-input-group>
                    <n-input v-model:value="testEmailAddress" placeholder="输入收件人邮箱" style="width: 280px" />
                    <n-button type="primary" @click="handleTestEmail" :loading="emailTesting" :disabled="!configs.email.enabled">
                      发送测试邮件
                    </n-button>
                  </n-input-group>
                </n-form-item>
              </n-form>
            </template>

            <!-- 邮件模板 -->
            <template v-else-if="group.groupCode === 'emailTemplate'">
              <n-form :model="configs.emailTemplate" label-placement="top">
                <n-form-item label="验证码邮件模板">
                  <n-input v-model:value="configs.emailTemplate.verifyCode" type="textarea" :rows="3" placeholder="支持变量: {code}, {expire}" />
                </n-form-item>
                <n-form-item label="重置密码邮件模板">
                  <n-input v-model:value="configs.emailTemplate.resetPassword" type="textarea" :rows="3" placeholder="支持变量: {code}, {expire}" />
                </n-form-item>
                <n-form-item label="欢迎邮件模板">
                  <n-input v-model:value="configs.emailTemplate.welcome" type="textarea" :rows="3" placeholder="支持变量: {siteName}, {username}" />
                </n-form-item>
              </n-form>
            </template>

            <!-- 短信配置 -->
            <template v-else-if="group.groupCode === 'sms'">
              <n-form :model="configs.sms" label-placement="left" label-width="120">
                <n-form-item label="启用短信">
                  <n-switch v-model:value="configs.sms.enabled" />
                </n-form-item>
                <n-form-item label="短信服务商">
                  <n-select v-model:value="configs.sms.provider" :options="smsProviderOptions" style="width: 200px" />
                </n-form-item>
                <n-form-item label="AccessKeyId">
                  <n-input v-model:value="configs.sms.accessKeyId" placeholder="请输入AccessKeyId" />
                </n-form-item>
                <n-form-item label="AccessKeySecret">
                  <n-input v-model:value="configs.sms.accessKeySecret" type="password" show-password-on="click" placeholder="请输入AccessKeySecret" />
                </n-form-item>
                <n-form-item label="签名">
                  <n-input v-model:value="configs.sms.signName" placeholder="短信签名" />
                </n-form-item>
              </n-form>
            </template>

            <!-- 短信模板 -->
            <template v-else-if="group.groupCode === 'smsTemplate'">
              <n-form :model="configs.smsTemplate" label-placement="left" label-width="150">
                <n-form-item label="验证码模板ID">
                  <n-input v-model:value="configs.smsTemplate.verifyCode" placeholder="请输入短信模板ID" />
                </n-form-item>
                <n-form-item label="重置密码模板ID">
                  <n-input v-model:value="configs.smsTemplate.resetPassword" placeholder="请输入短信模板ID" />
                </n-form-item>
                <n-form-item label="通知模板ID">
                  <n-input v-model:value="configs.smsTemplate.notification" placeholder="请输入短信模板ID" />
                </n-form-item>
              </n-form>
            </template>

            <!-- 文件配置 -->
            <template v-else-if="group.groupCode === 'storage'">
              <n-form :model="configs.storage" label-placement="left" label-width="150">
                <n-form-item label="存储方式">
                  <n-select v-model:value="configs.storage.provider" :options="storageProviderOptions" style="width: 200px" />
                </n-form-item>
                <n-form-item label="访问域名">
                  <n-input v-model:value="configs.storage.domain" placeholder="如: http://localhost:8080" />
                </n-form-item>
                <n-form-item label="最大文件大小(MB)">
                  <n-input-number v-model:value="configs.storage.maxSize" :min="1" :max="1024" />
                </n-form-item>
                <n-form-item label="允许的文件类型">
                  <n-input v-model:value="configs.storage.allowTypes" placeholder="如: jpg,png,pdf,doc,docx,xls,xlsx" />
                </n-form-item>

                <!-- 本地存储配置 -->
                <template v-if="configs.storage.provider === 'local'">
                  <n-divider>本地存储配置</n-divider>
                  <n-form-item label="存储路径">
                    <n-input v-model:value="configs.storage.localPath" placeholder="如: ./uploads" />
                  </n-form-item>
                </template>

                <!-- MinIO配置 -->
                <template v-if="configs.storage.provider === 'minio'">
                  <n-divider>MinIO配置</n-divider>
                  <n-form-item label="服务端点">
                    <n-input v-model:value="configs.storage.minioEndpoint" placeholder="如: http://localhost:9000" />
                  </n-form-item>
                  <n-form-item label="AccessKey">
                    <n-input v-model:value="configs.storage.minioAccessKey" placeholder="请输入AccessKey" />
                  </n-form-item>
                  <n-form-item label="SecretKey">
                    <n-input v-model:value="configs.storage.minioSecretKey" type="password" show-password-on="click" placeholder="请输入SecretKey" />
                  </n-form-item>
                  <n-form-item label="存储桶名称">
                    <n-input v-model:value="configs.storage.minioBucket" placeholder="请输入存储桶名称" />
                  </n-form-item>
                </template>

                <!-- 阿里云OSS配置 -->
                <template v-if="configs.storage.provider === 'aliyun'">
                  <n-divider>阿里云OSS配置</n-divider>
                  <n-form-item label="Endpoint">
                    <n-input v-model:value="configs.storage.aliyunEndpoint" placeholder="如: https://oss-cn-hangzhou.aliyuncs.com" />
                  </n-form-item>
                  <n-form-item label="AccessKeyId">
                    <n-input v-model:value="configs.storage.aliyunAccessKey" placeholder="请输入AccessKeyId" />
                  </n-form-item>
                  <n-form-item label="AccessKeySecret">
                    <n-input v-model:value="configs.storage.aliyunSecretKey" type="password" show-password-on="click" placeholder="请输入AccessKeySecret" />
                  </n-form-item>
                  <n-form-item label="存储桶名称">
                    <n-input v-model:value="configs.storage.aliyunBucket" placeholder="请输入Bucket名称" />
                  </n-form-item>
                </template>

                <!-- 腾讯云COS配置 -->
                <template v-if="configs.storage.provider === 'tencent'">
                  <n-divider>腾讯云COS配置</n-divider>
                  <n-form-item label="SecretId">
                    <n-input v-model:value="configs.storage.tencentSecretId" placeholder="请输入SecretId" />
                  </n-form-item>
                  <n-form-item label="SecretKey">
                    <n-input v-model:value="configs.storage.tencentSecretKey" type="password" show-password-on="click" placeholder="请输入SecretKey" />
                  </n-form-item>
                  <n-form-item label="存储桶名称">
                    <n-input v-model:value="configs.storage.tencentBucket" placeholder="请输入Bucket名称" />
                  </n-form-item>
                  <n-form-item label="地域">
                    <n-input v-model:value="configs.storage.tencentRegion" placeholder="如: ap-guangzhou" />
                  </n-form-item>
                </template>
              </n-form>
            </template>

            <!-- 推送配置 -->
            <template v-else-if="group.groupCode === 'push'">
              <n-form :model="configs.push" label-placement="left" label-width="120">
                <n-form-item label="启用推送">
                  <n-switch v-model:value="configs.push.enabled" />
                </n-form-item>
                <n-form-item label="推送服务商">
                  <n-select v-model:value="configs.push.provider" :options="pushProviderOptions" style="width: 200px" />
                </n-form-item>
                <n-form-item label="AppKey">
                  <n-input v-model:value="configs.push.appKey" placeholder="请输入AppKey" />
                </n-form-item>
                <n-form-item label="MasterSecret">
                  <n-input v-model:value="configs.push.masterSecret" type="password" show-password-on="click" placeholder="请输入MasterSecret" />
                </n-form-item>
              </n-form>
            </template>

            <!-- 第三方配置 -->
            <template v-else-if="group.groupCode === 'thirdParty'">
              <n-collapse>
                <n-collapse-item title="微信登录" name="wechat">
                  <n-form :model="configs.thirdParty.wechat" label-placement="left" label-width="100">
                    <n-form-item label="启用">
                      <n-switch v-model:value="configs.thirdParty.wechat.enabled" />
                    </n-form-item>
                    <n-form-item label="AppID">
                      <n-input v-model:value="configs.thirdParty.wechat.appId" placeholder="请输入AppID" />
                    </n-form-item>
                    <n-form-item label="AppSecret">
                      <n-input v-model:value="configs.thirdParty.wechat.appSecret" type="password" show-password-on="click" />
                    </n-form-item>
                  </n-form>
                </n-collapse-item>
                <n-collapse-item title="支付宝登录" name="alipay">
                  <n-form :model="configs.thirdParty.alipay" label-placement="left" label-width="100">
                    <n-form-item label="启用">
                      <n-switch v-model:value="configs.thirdParty.alipay.enabled" />
                    </n-form-item>
                    <n-form-item label="AppID">
                      <n-input v-model:value="configs.thirdParty.alipay.appId" placeholder="请输入AppID" />
                    </n-form-item>
                    <n-form-item label="私钥">
                      <n-input v-model:value="configs.thirdParty.alipay.privateKey" type="textarea" :rows="2" />
                    </n-form-item>
                    <n-form-item label="公钥">
                      <n-input v-model:value="configs.thirdParty.alipay.publicKey" type="textarea" :rows="2" />
                    </n-form-item>
                  </n-form>
                </n-collapse-item>
                <n-collapse-item title="GitHub登录" name="github">
                  <n-form :model="configs.thirdParty.github" label-placement="left" label-width="100">
                    <n-form-item label="启用">
                      <n-switch v-model:value="configs.thirdParty.github.enabled" />
                    </n-form-item>
                    <n-form-item label="Client ID">
                      <n-input v-model:value="configs.thirdParty.github.clientId" placeholder="请输入Client ID" />
                    </n-form-item>
                    <n-form-item label="Client Secret">
                      <n-input v-model:value="configs.thirdParty.github.clientSecret" type="password" show-password-on="click" />
                    </n-form-item>
                  </n-form>
                </n-collapse-item>
              </n-collapse>
            </template>

            <!-- 小程序配置 -->
            <template v-else-if="group.groupCode === 'wechatMiniProgram'">
              <n-form :model="configs.wechatMiniProgram" label-placement="left" label-width="120">
                <n-form-item label="启用小程序">
                  <n-switch v-model:value="configs.wechatMiniProgram.enabled" />
                </n-form-item>
                <n-form-item label="AppID">
                  <n-input v-model:value="configs.wechatMiniProgram.appId" placeholder="请输入小程序AppID" />
                </n-form-item>
                <n-form-item label="AppSecret">
                  <n-input v-model:value="configs.wechatMiniProgram.appSecret" type="password" show-password-on="click" placeholder="请输入小程序AppSecret" />
                </n-form-item>
                <n-divider />
                <n-alert type="info" title="使用说明" :bordered="false">
                  <p>1. 小程序登录接口: POST /api/wechat/miniprogram/login</p>
                  <p>2. 获取手机号接口: POST /api/wechat/miniprogram/phone</p>
                  <p>3. 前端需要调用 wx.login() 获取 code 后传给后端</p>
                </n-alert>
              </n-form>
            </template>

            <!-- 公众号配置 -->
            <template v-else-if="group.groupCode === 'wechatMp'">
              <n-form :model="configs.wechatMp" label-placement="left" label-width="150">
                <n-form-item label="启用公众号">
                  <n-switch v-model:value="configs.wechatMp.enabled" />
                </n-form-item>
                <n-form-item label="AppID">
                  <n-input v-model:value="configs.wechatMp.appId" placeholder="请输入公众号AppID" />
                </n-form-item>
                <n-form-item label="AppSecret">
                  <n-input v-model:value="configs.wechatMp.appSecret" type="password" show-password-on="click" placeholder="请输入公众号AppSecret" />
                </n-form-item>
                <n-form-item label="Token">
                  <n-input v-model:value="configs.wechatMp.token" placeholder="请输入服务器配置Token" />
                </n-form-item>
                <n-form-item label="EncodingAESKey">
                  <n-input v-model:value="configs.wechatMp.aesKey" placeholder="请输入消息加解密密钥（可选）" />
                </n-form-item>
                <n-form-item label="回调URL">
                  <n-input v-model:value="configs.wechatMp.callbackUrl" placeholder="如: https://api.example.com/api/wechat/callback" />
                  <span class="form-hint">需配置到微信公众号后台</span>
                </n-form-item>
                <n-form-item label="OAuth回调URL">
                  <n-input v-model:value="configs.wechatMp.oauthRedirectUrl" placeholder="如: https://www.example.com/login" />
                  <span class="form-hint">网页授权登录后的回调地址</span>
                </n-form-item>

                <n-divider>自定义菜单配置</n-divider>
                <div class="menu-editor">
                  <div class="menu-preview">
                    <div class="phone-frame">
                      <div class="phone-screen">
                        <div class="chat-area"></div>
                        <div class="menu-bar">
                          <div
                            v-for="(menu, index) in menuList"
                            :key="index"
                            class="menu-item"
                            :class="{ active: selectedMenuIndex === index && selectedSubIndex === -1 }"
                            @click="selectMenu(index)"
                          >
                            <span class="menu-name">{{ menu.name || '菜单名称' }}</span>
                            <div v-if="menu.sub_button && menu.sub_button.length > 0" class="sub-menu-list">
                              <div
                                v-for="(sub, subIndex) in menu.sub_button"
                                :key="subIndex"
                                class="sub-menu-item"
                                :class="{ active: selectedMenuIndex === index && selectedSubIndex === subIndex }"
                                @click.stop="selectSubMenu(index, subIndex)"
                              >
                                {{ sub.name || '子菜单' }}
                              </div>
                              <div
                                v-if="menu.sub_button.length < 5"
                                class="sub-menu-item add-sub"
                                @click.stop="addSubMenu(index)"
                              >
                                +
                              </div>
                            </div>
                          </div>
                          <div
                            v-if="menuList.length < 3"
                            class="menu-item add-menu"
                            @click="addMenu"
                          >
                            <n-icon size="20"><AddOutline /></n-icon>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div class="menu-config" v-if="selectedMenuIndex >= 0">
                    <n-card size="small" :title="selectedSubIndex >= 0 ? '子菜单配置' : '一级菜单配置'">
                      <template #header-extra>
                        <n-button size="small" type="error" quaternary @click="deleteCurrentMenu">
                          <template #icon><n-icon><TrashOutline /></n-icon></template>
                          删除
                        </n-button>
                      </template>

                      <n-form label-placement="left" label-width="80" size="small">
                        <n-form-item label="菜单名称">
                          <n-input
                            v-model:value="currentMenu.name"
                            placeholder="最多4个汉字或8个字母"
                            :maxlength="selectedSubIndex >= 0 ? 16 : 8"
                          />
                        </n-form-item>

                        <n-form-item label="菜单类型" v-if="!hasSubMenu">
                          <n-select
                            v-model:value="currentMenu.type"
                            :options="menuTypeOptions"
                            @update:value="handleMenuTypeChange"
                          />
                        </n-form-item>

                        <template v-if="!hasSubMenu">
                          <n-form-item label="网页链接" v-if="currentMenu.type === 'view'">
                            <n-input v-model:value="currentMenu.url" placeholder="https://" />
                          </n-form-item>

                          <n-form-item label="事件KEY" v-if="currentMenu.type === 'click'">
                            <n-input v-model:value="currentMenu.key" placeholder="用于消息接口推送" />
                          </n-form-item>

                          <template v-if="currentMenu.type === 'miniprogram'">
                            <n-form-item label="小程序AppID">
                              <n-input v-model:value="currentMenu.appid" placeholder="小程序的AppID" />
                            </n-form-item>
                            <n-form-item label="小程序路径">
                              <n-input v-model:value="currentMenu.pagepath" placeholder="pages/index/index" />
                            </n-form-item>
                            <n-form-item label="备用网页">
                              <n-input v-model:value="currentMenu.url" placeholder="不支持小程序时打开的网页" />
                            </n-form-item>
                          </template>
                        </template>

                        <n-form-item v-if="selectedSubIndex === -1 && !currentMenu.type">
                          <n-alert type="info" :bordered="false">
                            一级菜单有子菜单时，不能设置跳转动作
                          </n-alert>
                        </n-form-item>

                        <n-form-item v-if="selectedSubIndex === -1 && (!currentMenu.sub_button || currentMenu.sub_button.length === 0)">
                          <n-button size="small" @click="addSubMenu(selectedMenuIndex)">
                            <template #icon><n-icon><AddOutline /></n-icon></template>
                            添加子菜单
                          </n-button>
                        </n-form-item>
                      </n-form>
                    </n-card>
                  </div>

                  <div class="menu-config menu-empty" v-else>
                    <n-empty description="点击左侧菜单进行编辑" />
                  </div>
                </div>

                <n-form-item label="菜单操作" style="margin-top: 16px;">
                  <n-space>
                    <n-button type="primary" @click="handleSyncMenu" :loading="menuSyncing" :disabled="!configs.wechatMp.enabled || menuList.length === 0">
                      同步菜单到微信
                    </n-button>
                    <n-button @click="handleGetMenu" :loading="menuLoading" :disabled="!configs.wechatMp.enabled">
                      获取当前菜单
                    </n-button>
                    <n-button type="error" @click="handleDeleteMenu" :disabled="!configs.wechatMp.enabled">
                      删除菜单
                    </n-button>
                    <n-button @click="showMenuJson = true">
                      查看JSON
                    </n-button>
                  </n-space>
                </n-form-item>

                <!-- JSON预览弹窗 -->
                <n-modal v-model:show="showMenuJson" preset="card" title="菜单JSON" style="width: 600px">
                  <n-input
                    :value="menuJsonPreview"
                    type="textarea"
                    :rows="15"
                    readonly
                  />
                  <template #footer>
                    <n-button @click="copyMenuJson">复制JSON</n-button>
                  </template>
                </n-modal>

                <n-divider />
                <n-alert type="info" title="使用说明" :bordered="false">
                  <p>1. 公众号OAuth登录: 先调用 GET /api/wechat/mp/oauth-url 获取授权链接</p>
                  <p>2. 用户授权后回调到 oauthRedirectUrl，携带 code 参数</p>
                  <p>3. 前端用 code 调用 POST /api/wechat/mp/oauth-login 完成登录</p>
                </n-alert>
              </n-form>
            </template>

            <!-- 支付配置 -->
            <template v-else-if="group.groupCode === 'payment'">
              <n-collapse>
                <n-collapse-item title="微信支付" name="wechatPay">
                  <n-form :model="configs.payment.wechatPay" label-placement="left" label-width="120">
                    <n-form-item label="启用">
                      <n-switch v-model:value="configs.payment.wechatPay.enabled" />
                    </n-form-item>
                    <n-form-item label="商户号">
                      <n-input v-model:value="configs.payment.wechatPay.mchId" placeholder="请输入商户号" />
                    </n-form-item>
                    <n-form-item label="AppID">
                      <n-input v-model:value="configs.payment.wechatPay.appId" placeholder="请输入AppID" />
                    </n-form-item>
                    <n-form-item label="APIv3密钥">
                      <n-input v-model:value="configs.payment.wechatPay.apiV3Key" type="password" show-password-on="click" placeholder="请输入APIv3密钥" />
                    </n-form-item>
                    <n-form-item label="商户私钥">
                      <n-input v-model:value="configs.payment.wechatPay.privateKey" type="textarea" :rows="3" placeholder="请输入商户私钥" />
                    </n-form-item>
                    <n-form-item label="平台证书">
                      <n-input v-model:value="configs.payment.wechatPay.platformCert" type="textarea" :rows="3" placeholder="请输入微信支付平台证书" />
                    </n-form-item>
                    <n-form-item label="证书序列号">
                      <n-input v-model:value="configs.payment.wechatPay.certSerialNo" placeholder="请输入证书序列号" />
                    </n-form-item>
                    <n-form-item label="回调地址">
                      <n-input v-model:value="configs.payment.wechatPay.notifyUrl" placeholder="请输入支付回调地址" />
                    </n-form-item>
                    <n-form-item label="测试支付" v-if="configs.payment.wechatPay.enabled">
                      <n-button type="primary" @click="testPayment('wechat')" :loading="paymentTesting">
                        生成测试订单
                      </n-button>
                    </n-form-item>
                  </n-form>
                </n-collapse-item>
                <n-collapse-item title="支付宝支付" name="alipay">
                  <n-form :model="configs.payment.alipay" label-placement="left" label-width="120">
                    <n-form-item label="启用">
                      <n-switch v-model:value="configs.payment.alipay.enabled" />
                    </n-form-item>
                    <n-form-item label="AppID">
                      <n-input v-model:value="configs.payment.alipay.appId" placeholder="请输入AppID" />
                    </n-form-item>
                    <n-form-item label="应用私钥">
                      <n-input v-model:value="configs.payment.alipay.privateKey" type="textarea" :rows="3" placeholder="请输入应用私钥" />
                    </n-form-item>
                    <n-form-item label="支付宝公钥">
                      <n-input v-model:value="configs.payment.alipay.publicKey" type="textarea" :rows="3" placeholder="请输入支付宝公钥" />
                    </n-form-item>
                    <n-form-item label="签名类型">
                      <n-select v-model:value="configs.payment.alipay.signType" :options="[{label:'RSA2',value:'RSA2'},{label:'RSA',value:'RSA'}]" style="width: 150px" />
                    </n-form-item>
                    <n-form-item label="网关地址">
                      <n-select v-model:value="configs.payment.alipay.gatewayUrl" :options="alipayGatewayOptions" />
                    </n-form-item>
                    <n-form-item label="回调地址">
                      <n-input v-model:value="configs.payment.alipay.notifyUrl" placeholder="请输入支付回调地址" />
                    </n-form-item>
                    <n-form-item label="测试支付" v-if="configs.payment.alipay.enabled">
                      <n-button type="primary" @click="testPayment('alipay')" :loading="paymentTesting">
                        生成测试订单
                      </n-button>
                    </n-form-item>
                  </n-form>
                </n-collapse-item>
              </n-collapse>

              <!-- 测试支付弹窗 -->
              <n-modal v-model:show="showPaymentModal" preset="card" title="测试支付" style="width: 400px">
                <div class="payment-test-modal">
                  <div class="payment-info">
                    <p>支付方式：{{ paymentResult.type === 'wechat' ? '微信支付' : '支付宝' }}</p>
                    <p>订单号：{{ paymentResult.orderNo }}</p>
                    <p>金额：<span class="amount">¥ 0.01</span></p>
                  </div>
                  <div class="qrcode-container" v-if="paymentResult.qrcode">
                    <img :src="paymentResult.qrcode" alt="支付二维码" class="qrcode-img" />
                    <p class="qrcode-tip">请使用{{ paymentResult.type === 'wechat' ? '微信' : '支付宝' }}扫码支付</p>
                  </div>
                  <div class="payment-link" v-if="paymentResult.payUrl && paymentResult.type === 'alipay'">
                    <n-button type="primary" tag="a" :href="paymentResult.payUrl" target="_blank">
                      点击跳转支付
                    </n-button>
                  </div>
                </div>
              </n-modal>
            </template>

            <!-- 安全配置 -->
            <template v-else-if="group.groupCode === 'security'">
              <n-form :model="configs.security" label-placement="left" label-width="150">
                <n-form-item label="接口加密">
                  <n-switch v-model:value="configs.security.encryptEnabled" @update:value="handleEncryptChange" />
                </n-form-item>
                <template v-if="configs.security.encryptEnabled">
                  <n-form-item label="加密范围">
                    <n-radio-group v-model:value="configs.security.encryptScope">
                      <n-space>
                        <n-radio value="partial">
                          <span>部分加密</span>
                          <span class="form-hint">（仅加密带 @EncryptResponse 注解的接口）</span>
                        </n-radio>
                        <n-radio value="global">
                          <span>全局加密</span>
                          <span class="form-hint">（所有接口返回都加密）</span>
                        </n-radio>
                      </n-space>
                    </n-radio-group>
                  </n-form-item>
                  <n-form-item label="RSA公钥">
                    <n-input v-model:value="configs.security.encryptPublicKey" type="textarea" :rows="3" readonly placeholder="点击下方按钮生成密钥" />
                  </n-form-item>
                  <n-form-item label="RSA私钥">
                    <n-input v-model:value="configs.security.encryptPrivateKey" type="textarea" :rows="3" readonly placeholder="点击下方按钮生成密钥" />
                  </n-form-item>
                  <n-form-item label="生成密钥">
                    <n-button type="primary" @click="handleGenerateKeys" :loading="generatingKeys">
                      {{ configs.security.encryptPublicKey ? '重新生成密钥' : '生成RSA密钥对' }}
                    </n-button>
                    <span class="form-hint" v-if="!configs.security.encryptPublicKey" style="color: #f5222d">
                      请先生成密钥才能使用接口加密功能
                    </span>
                  </n-form-item>
                </template>
                <n-form-item label="XSS过滤">
                  <n-switch v-model:value="configs.security.xssFilter" />
                </n-form-item>
                <n-form-item label="SQL注入防护">
                  <n-switch v-model:value="configs.security.sqlInject" />
                </n-form-item>

                <n-divider>Token 配置（Sa-Token）</n-divider>
                <n-form-item label="Token 名称">
                  <n-input v-model:value="configs.security.tokenName" placeholder="请求Header中的参数名" style="width: 250px" disabled />
                  <span class="form-hint">同时也是 Cookie 名称、Header 提交的参数名称（不可修改）</span>
                </n-form-item>
                <n-form-item label="Token 有效期">
                  <n-input-number v-model:value="configs.security.tokenTimeout" :min="-1" :max="2592000" style="width: 180px" />
                  <span class="form-hint">
                    单位：秒，-1代表永不过期
                    <template v-if="configs.security.tokenTimeout > 0">
                      （≈{{ formatDuration(configs.security.tokenTimeout) }}）
                    </template>
                  </span>
                </n-form-item>
                <n-form-item label="活跃超时时间">
                  <n-input-number v-model:value="configs.security.tokenActiveTimeout" :min="-1" :max="2592000" style="width: 180px" />
                  <span class="form-hint">
                    单位：秒，超过此时间无操作则冻结，-1不限制
                    <template v-if="configs.security.tokenActiveTimeout > 0">
                      （≈{{ formatDuration(configs.security.tokenActiveTimeout) }}）
                    </template>
                  </span>
                </n-form-item>
                <n-form-item label="允许多端登录">
                  <n-switch v-model:value="configs.security.tokenIsConcurrent" />
                  <span class="form-hint">关闭后新登录将挤掉旧登录</span>
                </n-form-item>
                <n-form-item label="共用Token">
                  <n-switch v-model:value="configs.security.tokenIsShare" />
                  <span class="form-hint">多人登录同一账号时是否共用一个Token</span>
                </n-form-item>
                <n-form-item label="Token 风格">
                  <n-select v-model:value="configs.security.tokenStyle" :options="tokenStyleOptions" style="width: 250px" />
                </n-form-item>
                <n-form-item label="从Header读取">
                  <n-switch v-model:value="configs.security.tokenIsReadHeader" />
                </n-form-item>
                <n-form-item label="从Cookie读取">
                  <n-switch v-model:value="configs.security.tokenIsReadCookie" />
                </n-form-item>
                <n-form-item label="从请求体读取">
                  <n-switch v-model:value="configs.security.tokenIsReadBody" />
                </n-form-item>
                <n-form-item label="输出操作日志">
                  <n-switch v-model:value="configs.security.tokenIsLog" />
                  <span class="form-hint">是否输出 Sa-Token 操作日志</span>
                </n-form-item>
                <n-form-item label="打印版本信息">
                  <n-switch v-model:value="configs.security.tokenIsPrint" />
                  <span class="form-hint">启动时是否打印 Sa-Token 版本字符画</span>
                </n-form-item>

                <n-alert type="info" :bordered="false" style="margin-top: 12px">
                  <p>注意：修改 Token 有效期和活跃超时时间后，仅对新创建的 Token 生效，已登录用户的 Token 不受影响。</p>
                  <p>如果关闭了「允许多端登录」，已登录的多端 Token 在下次登录时才会互踢。</p>
                </n-alert>
              </n-form>
            </template>

            <!-- 其他配置 -->
            <template v-else-if="group.groupCode === 'other'">
              <n-empty description="暂无其他配置项" />
            </template>

            <!-- 保存按钮 -->
            <div class="config-footer">
              <n-button type="primary" :loading="saving" @click="handleSave">
                保存配置
              </n-button>
              <n-button @click="handleRefresh" style="margin-left: 12px">
                刷新缓存
              </n-button>
            </div>
          </div>
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useMessage, type UploadCustomRequestOptions } from 'naive-ui'
import { ImageOutline, CloseOutline, AddOutline, TrashOutline } from '@vicons/ionicons5'
import { configGroupApi, type SysConfigGroup } from '@/api/org'
import { fileApi } from '@/api/system'
import { wechatApi } from '@/api/wechat'
import { useSiteStore } from '@/stores/site'

const message = useMessage()
const siteStore = useSiteStore()

// 配置分组列表
const configGroups = ref<SysConfigGroup[]>([])
const activeTab = ref('system')
const saving = ref(false)

// 所有配置数据
const configs = reactive<Record<string, any>>({
  system: { siteName: 'MasterLife', siteDescription: '', siteLogo: '', copyright: '版权所有@朋诚科技', icp: '', watermarkEnabled: true, watermarkType: 'username', watermarkOpacity: 0.1 },
  register: { enabled: true, verifyEmail: false, verifyPhone: false, defaultRole: 'user', needAudit: false },
  login: { captchaEnabled: false, captchaType: 'image', maxRetryCount: 5, lockTime: 30, rememberMe: true, singleLogin: false },
  password: { minLength: 6, maxLength: 20, requireUppercase: false, requireLowercase: false, requireNumber: false, requireSpecial: false, expireDays: 0 },
  email: { host: '', port: 465, username: '', password: '', fromName: '', ssl: true, enabled: false },
  emailTemplate: { verifyCode: '', resetPassword: '', welcome: '' },
  sms: { provider: 'aliyun', accessKeyId: '', accessKeySecret: '', signName: '', enabled: false },
  smsTemplate: { verifyCode: '', resetPassword: '', notification: '' },
  storage: {
    provider: 'local',
    domain: 'http://localhost:8080',
    localPath: './uploads',
    maxSize: 10,
    allowTypes: 'jpg,jpeg,png,gif,webp,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,mp4,avi,mov,wmv,flv,mkv,mp3,wav,ogg,zip,rar,7z',
    // MinIO配置
    minioEndpoint: '',
    minioAccessKey: '',
    minioSecretKey: '',
    minioBucket: '',
    // 阿里云OSS配置
    aliyunEndpoint: '',
    aliyunAccessKey: '',
    aliyunSecretKey: '',
    aliyunBucket: '',
    // 腾讯云COS配置
    tencentSecretId: '',
    tencentSecretKey: '',
    tencentBucket: '',
    tencentRegion: ''
  },
  push: { enabled: false, provider: 'jpush', appKey: '', masterSecret: '' },
  thirdParty: {
    wechat: { enabled: false, appId: '', appSecret: '' },
    alipay: { enabled: false, appId: '', privateKey: '', publicKey: '' },
    github: { enabled: false, clientId: '', clientSecret: '' }
  },
  wechatMiniProgram: {
    enabled: false,
    appId: '',
    appSecret: ''
  },
  wechatMp: {
    enabled: false,
    appId: '',
    appSecret: '',
    token: '',
    aesKey: '',
    callbackUrl: '',
    oauthRedirectUrl: '',
    menuConfig: ''
  },
  payment: {
    wechatPay: { enabled: false, mchId: '', appId: '', apiV3Key: '', privateKey: '', platformCert: '', certSerialNo: '', notifyUrl: '' },
    alipay: { enabled: false, appId: '', privateKey: '', publicKey: '', signType: 'RSA2', gatewayUrl: 'https://openapi.alipay.com/gateway.do', notifyUrl: '', returnUrl: '' }
  },
  security: {
    encryptEnabled: false, encryptScope: 'partial', encryptPublicKey: '', encryptPrivateKey: '', xssFilter: true, sqlInject: true,
    // Token 配置
    tokenName: 'Authorization', tokenTimeout: 86400, tokenActiveTimeout: 86400,
    tokenIsConcurrent: true, tokenIsShare: true, tokenStyle: 'uuid',
    tokenIsLog: false, tokenIsReadBody: false, tokenIsReadCookie: false,
    tokenIsReadHeader: true, tokenIsPrint: true, tokenIsWriteHeader: false
  },
  other: {}
})

// 选项数据
const captchaTypeOptions = [
  { label: '图片验证码', value: 'image' },
  { label: '滑块验证码', value: 'slider' },
  { label: '短信验证码', value: 'sms' }
]

const watermarkTypeOptions = [
  { label: '用户名', value: 'username' },
  { label: '用户名+时间', value: 'username_time' },
  { label: '站点名称', value: 'sitename' },
  { label: '自定义文本', value: 'custom' }
]

const smsProviderOptions = [
  { label: '阿里云', value: 'aliyun' },
  { label: '腾讯云', value: 'tencent' },
  { label: '七牛云', value: 'qiniu' }
]

const storageProviderOptions = [
  { label: '本地存储', value: 'local' },
  { label: 'MinIO', value: 'minio' },
  { label: '阿里云OSS', value: 'aliyun' },
  { label: '腾讯云COS', value: 'tencent' }
]

const pushProviderOptions = [
  { label: '极光推送', value: 'jpush' },
  { label: '友盟推送', value: 'umeng' },
  { label: '个推', value: 'getui' }
]

const alipayGatewayOptions = [
  { label: '正式环境', value: 'https://openapi.alipay.com/gateway.do' },
  { label: '沙箱环境', value: 'https://openapi-sandbox.dl.alipaydev.com/gateway.do' }
]

const tokenStyleOptions = [
  { label: 'UUID 风格', value: 'uuid' },
  { label: '简洁 UUID（无下划线）', value: 'simple-uuid' },
  { label: '随机32位字符串', value: 'random-32' },
  { label: '随机64位字符串', value: 'random-64' },
  { label: '随机128位字符串', value: 'random-128' },
  { label: 'tik 风格', value: 'tik' }
]

// 将秒数格式化为友好的时间描述
function formatDuration(seconds: number): string {
  if (seconds <= 0) return '永不过期'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const mins = Math.floor((seconds % 3600) / 60)
  const parts: string[] = []
  if (days > 0) parts.push(`${days}天`)
  if (hours > 0) parts.push(`${hours}小时`)
  if (mins > 0 && days === 0) parts.push(`${mins}分钟`)
  return parts.join('') || `${seconds}秒`
}

// 邮件测试相关
const emailTesting = ref(false)
const testEmailAddress = ref('')

// 密钥生成相关
const generatingKeys = ref(false)

// 开启加密时检查是否有密钥
function handleEncryptChange(enabled: boolean) {
  if (enabled && !configs.security.encryptPublicKey) {
    message.warning('请生成RSA密钥对后再保存配置')
  }
}

// 生成RSA密钥对
async function handleGenerateKeys() {
  generatingKeys.value = true
  try {
    const keys = await configGroupApi.generateKeys()
    configs.security.encryptPublicKey = keys.publicKey
    configs.security.encryptPrivateKey = keys.privateKey
    message.success('密钥生成成功，请点击保存配置')
  } catch (error: any) {
    message.error(error.message || '生成密钥失败')
  } finally {
    generatingKeys.value = false
  }
}

// 测试发送邮件
async function handleTestEmail() {
  if (!testEmailAddress.value) {
    message.warning('请输入收件人邮箱')
    return
  }

  // 简单的邮箱格式验证
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(testEmailAddress.value)) {
    message.warning('请输入正确的邮箱格式')
    return
  }

  emailTesting.value = true
  try {
    await configGroupApi.testEmail(testEmailAddress.value)
    message.success('测试邮件发送成功，请查收')
  } catch (error: any) {
    message.error(error.message || '发送测试邮件失败')
  } finally {
    emailTesting.value = false
  }
}

// 公众号菜单操作相关
const menuSyncing = ref(false)
const menuLoading = ref(false)
const showMenuJson = ref(false)

// 菜单编辑器相关
interface MenuItem {
  name: string
  type?: string
  url?: string
  key?: string
  appid?: string
  pagepath?: string
  sub_button?: MenuItem[]
}

const menuList = ref<MenuItem[]>([])
const selectedMenuIndex = ref(-1)
const selectedSubIndex = ref(-1)

// 菜单类型选项
const menuTypeOptions = [
  { label: '跳转网页', value: 'view' },
  { label: '点击推事件', value: 'click' },
  { label: '跳转小程序', value: 'miniprogram' }
]

// 当前选中的菜单
const currentMenu = computed(() => {
  if (selectedMenuIndex.value < 0) return {} as MenuItem
  const menu = menuList.value[selectedMenuIndex.value]
  if (selectedSubIndex.value >= 0 && menu.sub_button) {
    return menu.sub_button[selectedSubIndex.value]
  }
  return menu
})

// 是否有子菜单
const hasSubMenu = computed(() => {
  if (selectedMenuIndex.value < 0 || selectedSubIndex.value >= 0) return false
  const menu = menuList.value[selectedMenuIndex.value]
  return menu.sub_button && menu.sub_button.length > 0
})

// 菜单JSON预览
const menuJsonPreview = computed(() => {
  const data = { button: menuList.value.map(formatMenuForApi) }
  return JSON.stringify(data, null, 2)
})

// 格式化菜单数据用于API
function formatMenuForApi(menu: MenuItem): any {
  const result: any = { name: menu.name }

  if (menu.sub_button && menu.sub_button.length > 0) {
    result.sub_button = menu.sub_button.map(formatMenuForApi)
  } else if (menu.type) {
    result.type = menu.type
    if (menu.type === 'view') {
      result.url = menu.url || ''
    } else if (menu.type === 'click') {
      result.key = menu.key || ''
    } else if (menu.type === 'miniprogram') {
      result.appid = menu.appid || ''
      result.pagepath = menu.pagepath || ''
      result.url = menu.url || ''
    }
  }

  return result
}

// 选择菜单
function selectMenu(index: number) {
  selectedMenuIndex.value = index
  selectedSubIndex.value = -1
}

// 选择子菜单
function selectSubMenu(menuIndex: number, subIndex: number) {
  selectedMenuIndex.value = menuIndex
  selectedSubIndex.value = subIndex
}

// 添加菜单
function addMenu() {
  if (menuList.value.length >= 3) {
    message.warning('最多只能添加3个一级菜单')
    return
  }
  menuList.value.push({
    name: '菜单' + (menuList.value.length + 1),
    type: 'view',
    url: ''
  })
  selectedMenuIndex.value = menuList.value.length - 1
  selectedSubIndex.value = -1
}

// 添加子菜单
function addSubMenu(menuIndex: number) {
  const menu = menuList.value[menuIndex]
  if (!menu.sub_button) {
    menu.sub_button = []
  }
  if (menu.sub_button.length >= 5) {
    message.warning('最多只能添加5个子菜单')
    return
  }
  // 添加子菜单时，清除父菜单的类型设置
  delete menu.type
  delete menu.url
  delete menu.key
  delete menu.appid
  delete menu.pagepath

  menu.sub_button.push({
    name: '子菜单' + (menu.sub_button.length + 1),
    type: 'view',
    url: ''
  })
  selectedMenuIndex.value = menuIndex
  selectedSubIndex.value = menu.sub_button.length - 1
}

// 删除当前菜单
function deleteCurrentMenu() {
  if (selectedMenuIndex.value < 0) return

  if (selectedSubIndex.value >= 0) {
    // 删除子菜单
    const menu = menuList.value[selectedMenuIndex.value]
    if (menu.sub_button) {
      menu.sub_button.splice(selectedSubIndex.value, 1)
      // 如果没有子菜单了，给父菜单设置默认类型
      if (menu.sub_button.length === 0) {
        delete menu.sub_button
        menu.type = 'view'
        menu.url = ''
      }
    }
    selectedSubIndex.value = -1
  } else {
    // 删除一级菜单
    menuList.value.splice(selectedMenuIndex.value, 1)
    selectedMenuIndex.value = -1
    selectedSubIndex.value = -1
  }
}

// 菜单类型变更
function handleMenuTypeChange(type: string) {
  if (selectedSubIndex.value >= 0) {
    const menu = menuList.value[selectedMenuIndex.value]
    if (menu.sub_button) {
      const subMenu = menu.sub_button[selectedSubIndex.value]
      // 清除其他类型的字段
      delete subMenu.url
      delete subMenu.key
      delete subMenu.appid
      delete subMenu.pagepath
    }
  } else {
    const menu = menuList.value[selectedMenuIndex.value]
    delete menu.url
    delete menu.key
    delete menu.appid
    delete menu.pagepath
  }
}

// 复制JSON
function copyMenuJson() {
  navigator.clipboard.writeText(menuJsonPreview.value)
  message.success('已复制到剪贴板')
}

// 从配置解析菜单列表
function parseMenuFromConfig() {
  try {
    if (configs.wechatMp.menuConfig) {
      const data = JSON.parse(configs.wechatMp.menuConfig)
      if (data.button) {
        menuList.value = data.button
      } else if (data.menu && data.menu.button) {
        menuList.value = data.menu.button
      }
    }
  } catch (e) {
    console.error('解析菜单配置失败', e)
  }
}

// 同步菜单到微信
async function handleSyncMenu() {
  if (menuList.value.length === 0) {
    message.warning('请先添加菜单')
    return
  }

  // 验证菜单
  for (const menu of menuList.value) {
    if (!menu.name) {
      message.warning('菜单名称不能为空')
      return
    }
    if (menu.sub_button && menu.sub_button.length > 0) {
      for (const sub of menu.sub_button) {
        if (!sub.name) {
          message.warning('子菜单名称不能为空')
          return
        }
        if (!sub.type) {
          message.warning(`子菜单"${sub.name}"请选择菜单类型`)
          return
        }
      }
    } else if (!menu.type) {
      message.warning(`菜单"${menu.name}"请选择菜单类型或添加子菜单`)
      return
    }
  }

  menuSyncing.value = true
  try {
    const menuData = JSON.stringify({ button: menuList.value.map(formatMenuForApi) })
    await wechatApi.syncMenu(menuData)
    // 保存到配置
    configs.wechatMp.menuConfig = menuData
    message.success('菜单同步成功')
  } catch (error: any) {
    message.error(error.message || '菜单同步失败')
  } finally {
    menuSyncing.value = false
  }
}

// 获取当前菜单
async function handleGetMenu() {
  menuLoading.value = true
  try {
    const result = await wechatApi.getMenu()
    if (result) {
      const data = JSON.parse(result)
      if (data.menu && data.menu.button) {
        menuList.value = data.menu.button
      } else if (data.button) {
        menuList.value = data.button
      }
      configs.wechatMp.menuConfig = JSON.stringify({ button: menuList.value }, null, 2)
      message.success('获取菜单成功')
    } else {
      message.info('当前没有配置菜单')
      menuList.value = []
    }
  } catch (error: any) {
    message.error(error.message || '获取菜单失败')
  } finally {
    menuLoading.value = false
  }
}

// 删除菜单
async function handleDeleteMenu() {
  try {
    await wechatApi.deleteMenu()
    menuList.value = []
    configs.wechatMp.menuConfig = ''
    selectedMenuIndex.value = -1
    selectedSubIndex.value = -1
    message.success('菜单删除成功')
  } catch (error: any) {
    message.error(error.message || '删除菜单失败')
  }
}

// 支付测试相关
const paymentTesting = ref(false)
const showPaymentModal = ref(false)
const paymentResult = ref<{
  type: string
  orderNo: string
  qrcode: string
  payUrl: string
}>({
  type: '',
  orderNo: '',
  qrcode: '',
  payUrl: ''
})

// 测试支付
async function testPayment(type: 'wechat' | 'alipay') {
  paymentTesting.value = true
  try {
    const result = await configGroupApi.testPayment(type)
    paymentResult.value = {
      type,
      orderNo: result.orderNo,
      qrcode: result.qrcode || '',
      payUrl: result.payUrl || ''
    }
    showPaymentModal.value = true
  } catch (error: any) {
    message.error(error.message || '创建测试订单失败')
  } finally {
    paymentTesting.value = false
  }
}

// 加载配置分组
async function loadGroups() {
  try {
    configGroups.value = await configGroupApi.list()
    if (configGroups.value.length > 0) {
      activeTab.value = configGroups.value[0].groupCode
      await loadConfig(activeTab.value)
    }
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 深度合并对象（保持响应性）
function deepMergeReactive(target: any, source: any): void {
  for (const key in source) {
    if (source[key] !== null && typeof source[key] === 'object' && !Array.isArray(source[key])) {
      if (!target[key] || typeof target[key] !== 'object') {
        target[key] = {}
      }
      deepMergeReactive(target[key], source[key])
    } else {
      target[key] = source[key]
    }
  }
}

// 加载指定分组配置
async function loadConfig(groupCode: string) {
  try {
    const group = await configGroupApi.getByCode(groupCode)
    if (group && group.configValue) {
      const value = JSON.parse(group.configValue)
      // 使用深度合并，保持响应性
      if (!configs[groupCode]) {
        configs[groupCode] = {}
      }
      deepMergeReactive(configs[groupCode], value)
    }

    // 如果是公众号配置，自动获取当前菜单
    if (groupCode === 'wechatMp') {
      await loadWechatMpMenu()
    }
  } catch (error) {
    console.error('加载配置失败:', error)
  }
}

// Tab 切换
async function handleTabChange(tab: string) {
  await loadConfig(tab)
}

// 保存配置
async function handleSave() {
  saving.value = true
  try {
    await configGroupApi.save(activeTab.value, configs[activeTab.value])
    message.success('保存成功')

    // 如果是系统配置，立即更新站点信息和水印配置
    if (activeTab.value === 'system') {
      // 直接更新 store 值，让水印立即生效
      siteStore.siteName = configs.system.siteName || 'MasterLife'
      siteStore.siteDescription = configs.system.siteDescription || ''
      siteStore.siteLogo = configs.system.siteLogo || ''
      siteStore.copyright = configs.system.copyright || '版权所有@朋诚科技'
      siteStore.icp = configs.system.icp || ''
      siteStore.watermarkEnabled = configs.system.watermarkEnabled !== false
      siteStore.watermarkType = configs.system.watermarkType || 'username'
      siteStore.watermarkOpacity = configs.system.watermarkOpacity || 0.1
    }
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    saving.value = false
  }
}

// 刷新缓存
async function handleRefresh() {
  try {
    await configGroupApi.refresh()
    await siteStore.loadConfig()
    // 重新加载当前 tab 的配置，保持页面显示最新值
    await loadConfig(activeTab.value)
    message.success('缓存刷新成功')
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// Logo 上传
async function handleLogoUpload(options: UploadCustomRequestOptions) {
  const { file, onFinish, onError } = options
  try {
    const result = await fileApi.upload(file.file as File)
    configs.system.siteLogo = result.url
    message.success('Logo上传成功')
    onFinish()
  } catch (error) {
    message.error('Logo上传失败')
    onError()
  }
}

onMounted(() => {
  loadGroups()
})

// 记录是否已加载过菜单
const menuLoaded = ref(false)

// 加载公众号菜单
async function loadWechatMpMenu() {
  if (menuLoaded.value) return
  menuLoaded.value = true

  // 先尝试从微信获取当前菜单
  try {
    await handleGetMenu()
  } catch (e) {
    // 获取失败则从本地配置解析
    parseMenuFromConfig()
  }
}
</script>

<style scoped>
.config-card {
  min-height: calc(100vh - 160px);
}

.config-content {
  padding: 20px 0;
  max-width: 800px;
}

.config-footer {
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
}

.form-hint {
  margin-left: 12px;
  color: #9ca3af;
  font-size: 13px;
}

:deep(.n-tabs-nav) {
  padding: 0 16px;
}

:deep(.n-tab-pane) {
  padding: 0 16px;
}

:deep(.n-collapse) {
  margin-bottom: 16px;
}

.logo-upload {
  width: 100%;
}

.logo-preview {
  position: relative;
  display: inline-block;

  img {
    max-width: 200px;
    max-height: 80px;
    object-fit: contain;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 8px;
    background: #f9fafb;
  }

  .logo-delete {
    position: absolute;
    top: -8px;
    right: -8px;
  }
}

.logo-upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 200px;
  height: 80px;
  border: 2px dashed #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  color: #9ca3af;

  &:hover {
    border-color: #111827;
    color: #111827;
  }

  span {
    margin-top: 8px;
    font-size: 13px;
  }
}

:deep(.n-collapse-item__header-main) {
  font-weight: 500;
}

/* 菜单编辑器样式 */
.menu-editor {
  display: flex;
  gap: 24px;
  margin-top: 16px;
}

.menu-preview {
  flex-shrink: 0;
}

.phone-frame {
  width: 320px;
  height: 500px;
  background: #f5f5f5;
  border-radius: 24px;
  padding: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.phone-screen {
  width: 100%;
  height: 100%;
  background: #ededed;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-area {
  flex: 1;
  background: linear-gradient(180deg, #f7f7f7 0%, #ececec 100%);
}

.menu-bar {
  display: flex;
  background: #fafafa;
  border-top: 1px solid #e0e0e0;
  min-height: 50px;
}

.menu-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-right: 1px solid #e0e0e0;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
  padding: 0 8px;

  &:last-child {
    border-right: none;
  }

  &:hover {
    background: #f0f0f0;
  }

  &.active {
    background: #e6f7ff;
    color: #1890ff;
  }

  &.add-menu {
    color: #999;
    &:hover {
      color: #1890ff;
    }
  }
}

.menu-name {
  font-size: 13px;
  text-align: center;
  word-break: break-all;
  line-height: 1.3;
}

.sub-menu-list {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  background: #fafafa;
  border: 1px solid #e0e0e0;
  border-bottom: none;
  display: none;
}

.menu-item:hover .sub-menu-list,
.menu-item.active .sub-menu-list {
  display: block;
}

.sub-menu-item {
  padding: 12px 8px;
  text-align: center;
  font-size: 12px;
  border-bottom: 1px solid #e0e0e0;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #f0f0f0;
  }

  &.active {
    background: #e6f7ff;
    color: #1890ff;
  }

  &.add-sub {
    color: #999;
    font-size: 16px;
    &:hover {
      color: #1890ff;
    }
  }
}

.menu-config {
  flex: 1;
  min-width: 300px;
}

.menu-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border-radius: 8px;
  min-height: 200px;
}

.payment-test-modal {
  text-align: center;

  .payment-info {
    margin-bottom: 20px;
    text-align: left;
    padding: 16px;
    background: #f9fafb;
    border-radius: 8px;

    p {
      margin: 8px 0;
      color: #374151;
    }

    .amount {
      font-size: 24px;
      font-weight: 600;
      color: #ef4444;
    }
  }

  .qrcode-container {
    padding: 20px;
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    display: inline-block;

    .qrcode-img {
      width: 200px;
      height: 200px;
    }

    .qrcode-tip {
      margin-top: 12px;
      color: #6b7280;
      font-size: 14px;
    }
  }

  .payment-link {
    margin-top: 20px;
  }
}
</style>
