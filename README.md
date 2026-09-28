# BitNow Android

BitNow is a proximity-first decentralized Android client built on the production upstream BitChat Android transport.

## Architecture

BitNow deliberately retains upstream Bluetooth LE mesh discovery, packet framing, multi-hop routing, Noise private-message sessions, foreground connectivity service, Wi-Fi Aware support and Nostr transport. Product behavior is layered above those components instead of replacing them.

BitNow product metadata does not introduce a second radio protocol. Profiles, profile requests and dating signals use the versioned BitNow v1 wire envelope and are sent through the upstream private-message router, so the existing Noise session, routing, queueing and retry behavior remains the transport boundary.

## Product

- 18+ local profiles, hidden by default
- explicit 30-minute, 1-hour, 2-hour or 4-hour nearby availability windows
- capability bit 11 advertised only while the availability window is active
- nearby-first discovery with coarse signal bands rather than false distance estimates
- age, identity and intent discovery filters
- encrypted cross-platform profile exchange between active BitNow peers
- 45-minute interest signals and mutual match indication
- block, report-and-block and privacy-reset controls
- private direct messaging over upstream encrypted sessions
- no phone-number requirement
- no central account requirement
- offline BLE operation retained
- Nostr fallback retained
- full upstream Messages experience retained behind the BitNow app shell

## Build

```bash
./gradlew assembleDebug
./gradlew test
```

The authoritative workflow is `.github/workflows/build.yml`. It assembles the debug APK and publishes it as `bitnow-debug-apk`, then runs the JVM test suite. BitNow model/capability regression tests live under `app/src/test/kotlin/com/bitchat/android/bitnow` and `.../model`.

## Upstream and licensing

Based on `permissionlesstech/bitchat-android`. The upstream license is retained unchanged. See `UPSTREAM.md` and `UPSTREAM_COMMIT` for provenance and the exact imported revision.
