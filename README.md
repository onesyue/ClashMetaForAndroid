## YueTong

A premium VPN client for Android — built on [Clash Meta](https://github.com/MetaCubeX/ClashMetaForAndroid) with native [XBoard](https://github.com/cedar2025/Xboard) panel integration. Designed for AI tools, streaming, and global acceleration.

### Features

**Core VPN**
- One-tap connect with real-time speed chart and traffic ring
- Smart node selection with delay-based sorting and URL testing
- Supports hysteria2, vless, reality, and all Clash Meta protocols

**Account & Subscription**
- Native login/registration with WebView fallback for Cloudflare challenges
- Background subscription sync with automatic 24-hour refresh
- Session recovery on token expiry

**Dashboard**
- Real-time upload/download speed display
- Traffic usage ring with percentage and expiry date
- Connection duration and encryption status indicators

**Store & Orders**
- Browse plans with Markdown descriptions and multiple billing periods
- Coupon code support at checkout
- Full order history with status tracking and cancellation

**Widgets**
- Small (2×1): Status toggle
- Medium (3×2): Status, speed, traffic progress
- Large (4×3): Status, speed, traffic, security info
- All widgets support light/dark themes and live state updates

**Design**
- iOS 26-inspired color system (Apple Blue primary, system status colors)
- Liquid Glass card effects with adaptive light/dark themes
- Pure black OLED dark mode
- Smooth page transitions and card press animations
- Pill-style onboarding with glass icon containers

**More**
- Panel announcements with Markdown rendering
- Developer mode (hidden settings via 5-tap brand name)
- GeoIP/GeoSite databases via jsDelivr CDN with auto-update
- 3-tab navigation: Home, Store, Profile

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

### Architecture

| Layer | Description |
|-------|-------------|
| Core | [mihomo (Clash.Meta)](https://github.com/MetaCubeX/Clash.Meta) — Go native tunnel library |
| Service | Android Foreground Service — VPN tunnel and profile management |
| Panel | [XBoard](https://github.com/cedar2025/Xboard) — Subscription, store, orders, announcements API |
| UI | iOS 26 Liquid Glass design, DataBinding, Kotlin Coroutines |

### License

Licensed under [GPLv3](LICENSE).
