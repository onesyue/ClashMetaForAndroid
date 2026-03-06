## 悦通 VPN

基于 [Clash Meta for Android](https://github.com/MetaCubeX/ClashMetaForAndroid) 深度定制，对接 [悦通](https://yue.to) XBoard 订阅面板的 Android VPN 客户端。

### 功能特性

- 原生登录 / 注册，遇到 CF 验证可一键切换网页登录
- 自动同步订阅（登录后后台下载，支持 hysteria2 / vless / reality 等协议）
- VPN 仪表盘：流量、速率、连接时长实时显示
- 账号中心：套餐到期、流量用量、余额、邀请链接
- 商店：套餐列表（Markdown 格式说明）、一键购买
- 我的订单：订单历史，待支付订单可直接取消
- 公告：面板通知（Markdown 渲染）
- GeoIP / GeoSite 数据库通过 jsDelivr CDN 分发（国内可直接访问）
- 每 24 小时自动更新 Geo 数据

### 系统要求

- Android 5.0+（推荐 7.0+）
- 支持架构：`armeabi-v7a`、`arm64-v8a`、`x86`、`x86_64`

### 编译

1. 初始化子模块

   ```bash
   git submodule update --init --recursive
   ```

2. 安装 **OpenJDK 11**、**Android SDK**、**CMake**、**Golang**

3. 在项目根目录创建 `local.properties`

   ```properties
   sdk.dir=/path/to/android-sdk
   ```

4. 在项目根目录创建 `signing.properties`

   ```properties
   keystore.path=/path/to/keystore/file
   keystore.password=<keystore 密码>
   key.alias=<key alias>
   key.password=<key 密码>
   ```

5. 编译 Release 包

   ```bash
   ./gradlew app:assembleAlphaRelease
   ```

### CI / CD

推送 `alpha` tag 自动触发 GitHub Actions 构建并发布：

```bash
git tag alpha -f
git push origin alpha -f
```

### 技术架构

| 层级 | 说明 |
|------|------|
| 内核 | [mihomo (Clash.Meta)](https://github.com/MetaCubeX/Clash.Meta) Go 原生库 |
| 服务 | Android Foreground Service，管理 VPN 隧道和 Profile 下载 |
| 面板 | [XBoard (cedar2025)](https://github.com/cedar2025/Xboard) API 对接 |
| UI | Material Design 3，DataBinding，Kotlin Coroutines |

### 订阅下载说明

首次登录后，订阅下载在后台自动进行（不阻塞界面）。订阅包含 40+ rule-providers（规则集），
全部来自 jsDelivr CDN，首次下载约需 1~2 分钟，后续秒开。
