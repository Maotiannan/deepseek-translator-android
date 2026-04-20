## DeepSeek Translator (Android)

一个基于 Jetpack Compose 的三语（简体中文 / English / Français）翻译应用，界面风格参考谷歌翻译。用户在任意一个输入框录入内容后，会自动调用 DeepSeek Chat API，以流式方式把译文实时填充到另外两个输入框。

### 功能亮点
1. **三输入框对等交互**：任意语言输入都会成为源语言，另外两种语言由“开始翻译”按钮触发并即时刷新。
2. **DeepSeek 流式翻译**：基于 SSE 逐字接收内容，界面实时滚动更新，接近网页端体验。
3. **Jetpack Compose UI**：Material 3 + 响应式设计，状态管理集中在 `TranslatorViewModel`。
4. **设置面板**：通过右上角“设置”按钮可覆盖 API Key / Base URL，并选择 AI “碎碎念”的偏好语言。
5. **对话碎碎念模式**：底部按钮可进入对话记录面板，自动保存每次翻译并生成 300 字内的语境理解，可随时新建/退出。
6. **错误提示与取消**：网络/接口异常会在界面底部提示，重新输入或重新请求即可重试。

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
- 默认从根目录 `local.properties` 读取 `deepseek.apiKey` / `deepseek.baseUrl` / `deepseek.model`。
- 也支持通过 `DEEPSEEK_API_KEY` / `DEEPSEEK_BASE_URL` / `DEEPSEEK_MODEL` 环境变量注入构建默认值。
- App 内仍可点击右上角“设置”按钮，动态覆盖 API Key / Base URL。

> **安全提示**：公共仓库默认不提交任何密钥。请把密钥保留在本地 `local.properties`、环境变量或 CI/CD 私密变量中。

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
2. 首次运行前，在 `local.properties` 或环境变量中配置 DeepSeek Key。
3. 编译：`./gradlew assembleDebug`（首次运行需联网下载依赖）。
4. 安装：`./gradlew installDebug` 或使用 Android Studio 直接运行。
5. 连接真机或 Emulator（Android 8.0 / API 26 及以上）。

### 已知限制
- DeepSeek API 需要公网访问；离线环境下无法完成翻译。
- 公共仓库默认不内置 API Key，首次运行前需要自行配置。
- 未实现翻译历史、语音输入等扩展功能。

### 相关文件
- 产品/使用说明：`README.md`（当前文件）
- 后续计划：`update.txt`
