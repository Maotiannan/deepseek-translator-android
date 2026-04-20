## DeepSeek Translator (Android)

一个基于 Jetpack Compose 的三语（简体中文 / English / Français）翻译应用，界面风格参考谷歌翻译。用户在任意一个输入框录入内容后，会自动调用 DeepSeek Chat API，以流式方式把译文实时填充到另外两个输入框。

### 功能亮点
1. **三输入框对等交互**：任意语言输入都会成为源语言，另外两种语言由“开始翻译”按钮触发并即时刷新。
2. **DeepSeek 流式翻译**：基于 SSE 逐字接收内容，界面实时滚动更新，接近网页端体验。
3. **Jetpack Compose UI**：Material 3 + 响应式设计，状态管理集中在 `TranslatorViewModel`。
4. **首次配置引导**：首次打开若未配置 API Key，会自动弹出设置；保存后仅保存在当前设备，后续直接打开即可使用。
5. **设置面板**：通过右上角“设置”按钮可维护 API Key / Base URL，并选择 AI “碎碎念”的偏好语言。
6. **对话碎碎念模式**：底部按钮可进入对话记录面板，自动保存每次翻译并生成 300 字内的语境理解，可随时新建/退出。
7. **错误提示与取消**：网络/接口异常会在界面底部提示，重新输入或重新请求即可重试。

### 目录结构
```
app/
 └─ src/main/java/com/example/translator
     ├─ MainActivity.kt                # Compose 界面
     ├─ data/
     │   ├─ DeepSeekApi.kt             # 封装 DeepSeek SSE 调用
     │   └─ TranslationRepository.kt   # 多语言翻译协程流
     ├─ model/Language.kt
     └─ ui/
         ├─ TranslatorUiState.kt
         ├─ TranslatorViewModel.kt
         └─ theme/…                    # 主题定义
```

### DeepSeek API 使用方式
- 接口：`POST https://api.deepseek.com/chat/completions`
- 模型：`deepseek-chat`
- 请求体包含 system + user message（见 `DeepSeekApi`），并设置 `stream=true`。
- SSE 监听 `data:` 行，直至 `[DONE]`。
- 运行时优先使用 App 内“设置”里保存的 API Key / Base URL。
- 若安装包内带了默认配置，可作为开发或内部测试兜底；正式使用仍建议在 App 内保存自己的 API Key。
- `local.properties` / 环境变量仍可用于开发阶段生成默认构建值，但不再是用户使用 App 的前提。

> **安全提示**：公共仓库默认不提交任何密钥。最终用户应在 App 内完成本机配置；开发者可在本地构建配置或 CI/CD 私密变量中注入默认值。

### 本地配置
Android Studio 生成的 `local.properties` 通常已经包含 `sdk.dir`。在保留该行的前提下，可追加：

```properties
sdk.dir=/path/to/android-sdk
deepseek.apiKey=your_api_key
deepseek.baseUrl=https://api.deepseek.com/chat/completions
deepseek.model=deepseek-chat
```

### 开发 & 运行
1. 确认已安装 **Android Studio Iguana+**（或 `./gradlew`，JDK 17）。
2. 编译：`./gradlew assembleDebug`（首次运行需联网下载依赖）。
3. 安装：`./gradlew installDebug` 或使用 Android Studio 直接运行。
4. 首次打开若未配置 API Key，App 会自动弹出设置面板；保存后本机后续可直接使用。
5. 连接真机或 Emulator（Android 8.0 / API 26 及以上）。

### 已知限制
- DeepSeek API 需要公网访问；离线环境下无法完成翻译。
- 公共仓库默认不内置 API Key，但可在 App 内完成首次配置，无需重新打包。
- 未实现翻译历史、语音输入等扩展功能。

### 相关文件
- 产品/使用说明：`README.md`（当前文件）
- 后续计划：`update.txt`
