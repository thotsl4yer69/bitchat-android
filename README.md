# BitNow Android

BitNow is a proximity-first decentralized Android client built on the production upstream BitChat Android transport.

## Architecture

BitNow deliberately retains upstream Bluetooth LE mesh discovery, packet framing, multi-hop routing, Noise private-message sessions, foreground connectivity service, Wi-Fi Aware support and Nostr transport. Product behavior is layered above those components instead of replacing them.

## Product

- 18+ local profiles
- nearby-first discovery
- local/on-device identity and profile state
- private direct messaging over upstream encrypted sessions
- no phone-number requirement
- no central account requirement
- offline BLE operation retained
- Nostr fallback retained

## Build

```bash
./gradlew test
./gradlew assembleDebug
```

GitHub Actions publishes the debug build as `bitnow-debug-apk`.

## Upstream and licensing

Based on `permissionlesstech/bitchat-android`. The upstream license is retained unchanged. See `UPSTREAM.md` and `UPSTREAM_COMMIT`.
