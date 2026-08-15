# BitNow Android

BitNow is a proximity-first decentralized Android client built on the production upstream BitChat Android transport.

## Architecture

BitNow deliberately retains upstream Bluetooth LE mesh discovery, packet framing, multi-hop routing, Noise private-message sessions, foreground connectivity service, Wi-Fi Aware support and Nostr transport. Product behavior is layered above those components instead of replacing them.

BitNow product metadata does not introduce a second radio protocol. Profiles and interest state are encoded as invisible application control messages and sent through the upstream private-message router, so the existing Noise session, routing, queueing and retry behavior remains the transport boundary.

## Product

- 18+ local profiles
- nearby-first discovery with signal bands rather than false distance estimates
- local/on-device identity and profile state
- encrypted profile exchange between BitNow peers
- private mutual-interest state and match indication
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

The authoritative workflow is `.github/workflows/build.yml`. It assembles first and publishes the debug APK as `bitnow-debug-apk`; upstream JVM tests run separately so a pre-existing test-suite failure cannot hide the APK build result.

## Upstream and licensing

Based on `permissionlesstech/bitchat-android`. The upstream license is retained unchanged. See `UPSTREAM.md` and `UPSTREAM_COMMIT` for provenance and the exact imported revision.
