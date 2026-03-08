## YueTong VPN

A deeply customized Android VPN client based on [Clash Meta for Android](https://github.com/MetaCubeX/ClashMetaForAndroid), integrated with the [YueTong](https://yue.to) XBoard subscription panel.

### Features

- Native login / registration with one-tap WebView fallback for Cloudflare challenges
- Auto-sync subscriptions (background download after login, supports hysteria2 / vless / reality protocols)
- VPN dashboard: real-time traffic, speed, and connection duration
- Account center: plan expiry, data usage, balance, referral link
- Store: plan listing (Markdown descriptions), one-tap purchase
- My Orders: order history, cancel pending orders
- Announcements: panel notifications (Markdown rendering)
- GeoIP / GeoSite databases delivered via jsDelivr CDN (accessible in mainland China)
- Auto-update Geo databases every 24 hours

### Requirements

- Android 5.0+ (7.0+ recommended)
- Supported architectures: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`

### Build

1. Initialize submodules

   ```bash
   git submodule update --init --recursive
   ```

2. Install **OpenJDK 11**, **Android SDK**, **CMake**, **Golang**

3. Create `local.properties` in the project root

   ```properties
   sdk.dir=/path/to/android-sdk
   ```

4. Create `signing.properties` in the project root

   ```properties
   keystore.path=/path/to/keystore/file
   keystore.password=<keystore password>
   key.alias=<key alias>
   key.password=<key password>
   ```

5. Build release APK

   ```bash
   ./gradlew app:assembleAlphaRelease
   ```

### CI / CD

Push an `alpha` tag to trigger GitHub Actions build and release:

```bash
git tag alpha -f
git push origin alpha -f
```

### Architecture

| Layer | Description |
|-------|-------------|
| Core | [mihomo (Clash.Meta)](https://github.com/MetaCubeX/Clash.Meta) Go native library |
| Service | Android Foreground Service, manages VPN tunnel and profile downloads |
| Panel | [XBoard (cedar2025)](https://github.com/cedar2025/Xboard) API integration |
| UI | Material Design 3, DataBinding, Kotlin Coroutines |

### Subscription Download

After first login, subscriptions are downloaded in the background (non-blocking). The subscription includes 40+ rule-providers served from jsDelivr CDN. Initial download takes ~1-2 minutes; subsequent launches are instant.
