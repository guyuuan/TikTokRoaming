<p align="center">
  <img src="design/app-icon-concept.png" width="128" alt="TikTokRoaming 图标">
</p>

<h1 align="center">TikTokRoaming</h1>

<p align="center">
  一个基于 libxposed API 102 的 TikTok 区域环境配置模块。
</p>

TikTokRoaming 可以在指定的 TikTok 进程内模拟目标国家或地区的 SIM、运营商、语言与时区信息，并可选择隐藏 Android 标准 API 暴露的 VPN 状态。模块采用固定作用域，不会 Hook System Framework。

> [!IMPORTANT]
> 本模块不会修改公网 IP，也不能替代代理或 VPN。TikTok 的内容分发还可能参考账号、IP、设备和服务端风控等信息，因此模块不保证一定能够切换内容区域。

## 功能

- 内置 26 个常用国家和地区档案，可在应用内搜索并切换。
- 模拟 SIM/网络国家代码、MCC/MNC、系统语言和时区。
- 可隐藏标准网络 API 中的 VPN 标记及常见隧道网卡。
- 可按需隐藏桌面启动器中的模块图标。
- 通过 libxposed 远程偏好设置同步配置，无需修改 TikTok 数据。
- 固定作用域仅包含以下 TikTok 国际版包名：
  - `com.zhiliaoapp.musically`
  - `com.ss.android.ugc.trill`
- 提供基于 Jetpack Compose Material 3 的配置界面。

支持的档案包括：美国、加拿大、墨西哥、巴西、英国、德国、法国、西班牙、意大利、荷兰、波兰、土耳其、日本、韩国、新加坡、马来西亚、印度尼西亚、泰国、越南、菲律宾、中国台湾、中国香港、印度、澳大利亚、阿联酋和沙特阿拉伯。

## 使用要求

- Android 8.0（API 26）或更高版本。
- 支持 libxposed API 102 的 Xposed 框架及管理器。
- 已安装上述包名之一的 TikTok 国际版。

## 安装与使用

1. 安装构建好的 APK。
2. 在兼容的 Xposed 管理器中启用 TikTokRoaming，并确认作用域包含设备上安装的 TikTok 包名。
3. 打开 TikTokRoaming；页面显示“模块服务已连接”后，开启环境伪装并选择目标国家或地区。
4. 按需开启“隐藏 VPN 状态”。
5. 如需隐藏模块入口，可开启“隐藏桌面图标”；支持模块配置入口的管理器仍可打开设置页。
6. 强制停止 TikTok，再重新启动应用使配置完整生效。

切换档案或开关后，建议每次都彻底结束并重新启动 TikTok。所有修改只对固定作用域内的 TikTok 进程生效。

## 从源码构建

构建环境需要 JDK 17 和 Android SDK 37。克隆项目后执行：

```bash
git clone https://github.com/guyuuan/TikTokRoaming.git
cd TikTokRoaming
./gradlew :app:assembleDebug
```

调试 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

### 构建 Release

Release 构建默认启用 R8 代码压缩、混淆和资源压缩，并保留及重写 libxposed 的模块入口。没有签名配置时可以生成仅供检查的 unsigned APK：

```bash
./gradlew :app:assembleRelease
```

用于分发时，先在仓库外生成并妥善备份签名文件：

```bash
keytool -genkeypair -v \
  -keystore /path/to/secure/tiktok-roaming-release.jks \
  -alias tiktok-roaming \
  -keyalg RSA -keysize 4096 -validity 10000
```

复制 `keystore.properties.example` 为仓库根目录下的 `keystore.properties`，填写签名文件的绝对路径、别名和密码。真实的 `keystore.properties`、`*.jks` 和 `*.keystore` 已被 Git 忽略：

```properties
storeFile=/path/to/secure/tiktok-roaming-release.jks
storePassword=你的密钥库密码
keyAlias=tiktok-roaming
keyPassword=你的密钥密码
```

`keystore.properties` 按 UTF-8 读取；Windows 路径建议使用 `/`，如使用反斜杠则必须写成 `\\`。限制本地配置文件权限后再构建签名版本：

```bash
chmod 600 keystore.properties
./gradlew :app:assembleRelease --no-configuration-cache
```

CI 环境无需创建 `keystore.properties`，可以通过密钥管理服务注入 `TIKTOKROAMING_STORE_FILE`、`TIKTOKROAMING_STORE_PASSWORD`、`TIKTOKROAMING_KEY_ALIAS` 和 `TIKTOKROAMING_KEY_PASSWORD` 四个环境变量。

已签名 APK 位于 `app/build/outputs/apk/release/`，R8 映射文件位于 `app/build/outputs/mapping/release/mapping.txt`。请将签名文件做加密离线备份，将密码保存在密码管理器中；后续版本必须继续使用同一签名。签名文件、密码和映射文件都不应提交到仓库，但应按发布版本单独安全归档对应的 `mapping.txt`，用于反混淆崩溃信息。

版本信息由 Git 自动生成：`versionCode` 是当前提交总数，`versionName` 是“版本号 + 7 位 commit id”。当前提交有 tag 时优先使用该 tag，否则按版本排序使用最新 tag；仓库没有 tag 时使用 `v1.0.0`。本地及 tag 构建的正式产物命名为 `TikTokRoaming-版本号.apk`，分支 push 触发的 daily 产物命名为 `TikTokRoaming-版本号-commit id.apk`。

### GitHub Actions

push 任意分支时会构建 daily APK，push 任意 tag 时会构建正式 APK。构建完成后会分别直接上传 APK 和 R8 `mapping.txt`，不会将它们打包成 ZIP。请在仓库的 Actions secrets 中配置：

- `TIKTOKROAMING_KEYSTORE_BASE64`：JKS 文件的 Base64 内容。
- `TIKTOKROAMING_STORE_PASSWORD`：密钥库密码。
- `TIKTOKROAMING_KEY_ALIAS`：签名别名。
- `TIKTOKROAMING_KEY_PASSWORD`：签名私钥密码。

在 macOS 上可以用以下命令生成单行 Base64 内容，再将输出完整保存到 `TIKTOKROAMING_KEYSTORE_BASE64`：

```bash
base64 -i /path/to/tiktok-roaming-release.jks
```

运行本地单元测试：

```bash
./gradlew :app:testDebugUnitTest
```

连接 Android 设备或启动模拟器后，可以运行仪器测试：

```bash
./gradlew :app:connectedDebugAndroidTest
```

## 实现概览

- `xposed/`：安装电话、语言、时区、系统属性和 VPN 相关 Hook。
- `settings/`：通过 libxposed service 读写模块远程配置。
- `model/`：维护国家与地区档案，包括 ISO、MCC/MNC、语言标签和时区。
- `ui/`：基于 Jetpack Compose 的配置界面。

模块只拦截 TikTok 进程对相关 Android/Java API 的读取。VPN 隐藏功能仅覆盖 Android 标准网络标记和常见隧道接口名称，无法覆盖应用或服务端的所有检测方式。

## 常见问题

### 页面提示“模块服务未连接”

确认模块已在兼容 libxposed API 102 的管理器中启用，然后重新打开 TikTokRoaming。若仍未连接，请检查当前框架是否实现了所需的 libxposed service。

### 切换档案后没有效果

确认 TikTok 包名位于模块作用域内，并强制停止后重新启动 TikTok。部分结果可能由账号或服务端策略决定，不受本模块控制。

### 模块能修改 IP 地址吗？

不能。模块仅修改 TikTok 进程读取到的本地环境信息；网络出口仍由系统当前的网络、代理或 VPN 决定。

## 免责声明

本项目仅供学习、研究和个人测试。使用前请了解并遵守所在地法律法规及相关平台条款。使用模块可能导致功能异常、账号限制或其他风险，使用者需自行承担相应责任。

## 许可证

本项目采用 [MIT License](LICENSE)。
