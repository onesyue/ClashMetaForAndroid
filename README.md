## YueTong

A global acceleration network built for AI and streaming — Android VPN client based on [Clash Meta](https://github.com/MetaCubeX/ClashMetaForAndroid), integrated with [XBoard](https://github.com/cedar2025/Xboard) subscription management.

### Features

- **Account System** — Native login/registration with WebView fallback for Cloudflare challenges; automatic session recovery on token expiry
- **Subscription Sync** — Background download after login with progress dialog; supports hysteria2, vless, reality protocols
- **VPN Dashboard** — Real-time upload/download speed, traffic usage, connection duration
- **Account Center** — Plan details, expiry date, data usage, balance, referral link with one-tap copy
- **Store** — Browse plans with Markdown descriptions, multiple billing periods, coupon code support at checkout
- **Orders** — Full order history with status tracking, discount/surplus details, cancel pending orders
- **Announcements** — Panel notifications with Markdown rendering
- **Cyberpunk Dark Theme** — Glass-effect cards, gradient accents, consistent dark UI across all screens
- **Geo Databases** — GeoIP/GeoSite delivered via jsDelivr CDN with auto-update every 24 hours

### Requirements

- Android 5.0+ (7.0+ recommended)
- Architectures: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`

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
| Core | [mihomo (Clash.Meta)](https://github.com/MetaCubeX/Clash.Meta) — Go native tunnel library |
| Service | Android Foreground Service — VPN tunnel and profile management |
| Panel | [XBoard](https://github.com/cedar2025/Xboard) — Subscription, store, orders, announcements API |
| UI | Material Design 3, DataBinding, Kotlin Coroutines, dark theme |

### License

Licensed under [GPLv3](LICENSE).
