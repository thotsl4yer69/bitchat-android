# BitNow Android

This branch prepares the old Android repository for BitNow, but it is **not a release candidate** yet.

## What is ready

- BitNow package/application identity (`app.bitnow.mesh`, version `0.1.0`).
- Shared BitNow v1 encounter model:
  - adult profile fields
  - identity/preferences
  - encounter intent
  - shared-profile privacy rules
- Cross-platform BitNow v1 control envelope:
  - `signal`
  - `profileRequest`
  - `profile`
- The same visible fallback + U+2063 separator + Base64 JSON framing used by the Swift client.
- Swift-compatible `Date` encoding using seconds since Apple's 2001 reference date.
- Unit tests for signal/profile encoding, hidden age and validation.
- Release builds no longer silently use Android's debug signing key.

## Hard transport gate

The existing Android repository is a July 2025 implementation and is **not** the current BitChat Android core. It has no common Git ancestry with `permissionlesstech/bitchat-android` and it is not wire-compatible with the current Swift core.

More importantly, its current `BleMeshService` handles `MSG_TYPE_PRIVATE` with:

```kotlin
// TODO: Implement decryption for private messages
```

BitNow must not transmit encounter profiles, sexual/dating preferences or interest signals over that plaintext path.

For that reason this branch intentionally provides the interoperable BitNow codec but does **not** connect BitNow profile/signal transmission to the legacy BLE service.

## Required next transport step

Replace this repository's legacy mesh layer with the current `permissionlesstech/bitchat-android` transport/security architecture (or create a proper fork of that repository), then port the BitNow product layer onto that encrypted core.

The shared `BitNowWire.kt` file is designed to survive that migration and match the Swift BitNow v1 contract.

## Release rule

Do not publish this Android branch until:

1. current BitChat Android BLE packet framing is in place;
2. private messaging uses the modern encrypted path;
3. BitNow capability/availability negotiation matches the Swift client;
4. cross-platform fixtures prove Swift ↔ Kotlin signal/profile decoding;
5. unit + instrumentation builds are green.
