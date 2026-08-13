# BitChat Android

**Android BLE peer-to-peer messaging experiment inspired by the BitChat protocol.**

[![Status](https://img.shields.io/badge/status-software%20prototype-blue)](PROJECT_STATUS.md)
![Android](https://img.shields.io/badge/Android-Kotlin%20%2B%20Compose-green)
![BLE](https://img.shields.io/badge/transport-Bluetooth%20LE-blue)

> **Maturity: Software prototype.** This repository demonstrates native Android, BLE, asynchronous state and cryptographic-library integration. It is **not** represented as an audited secure messenger or as proven protocol-compatible with every upstream BitChat implementation. See [PROJECT_STATUS.md](PROJECT_STATUS.md).

## What it explores

The project investigates how an Android application can exchange messages directly over Bluetooth Low Energy without depending on a conventional central messaging server.

Key areas include:

- Kotlin and Jetpack Compose;
- BLE GATT discovery/communication;
- peer/relay and store-and-forward concepts;
- coroutines and asynchronous application state;
- room/direct-message UI patterns;
- cryptographic primitives through established libraries;
- Android Bluetooth/location permission handling.

## Architecture

```text
Compose UI / commands
        │
        ▼
  application state
        │
   ┌────┴─────┐
   │          │
   ▼          ▼
message     crypto
logic       library use
   │          │
   └────┬─────┘
        ▼
 BLE transport / peer relay
```

Representative source structure:

```text
app/src/main/java/com/bitchat/android/
├── MainActivity.kt
├── ChatViewModel.kt
├── mesh/
│   └── BleMeshService.kt
├── crypto/
│   └── EncryptionService.kt
└── ui/theme/
```

## Cryptography boundary

The code experiments with standard primitives/libraries such as X25519-style key agreement, AES-GCM, Ed25519-style signatures and password/KDF concepts depending on the repository revision.

That does **not** establish that the complete protocol is secure. Security depends on much more than choosing strong primitives: identity, key lifecycle, replay handling, metadata leakage, protocol state, randomness, implementation mistakes and peer authentication all matter.

No claim is made that this project has undergone professional cryptographic review, penetration testing or formal verification.

## Upstream relationship

The project was inspired by the original BitChat work and attempts to follow parts of its protocol/interface model. The separate `thotsl4yer69/bitchat` repository is an upstream **research fork** and is not represented as authored MAZLABZ work.

Any cross-platform compatibility claim should be tied to an actual tested pair of builds/devices rather than inferred from matching UUIDs or packet definitions.

## Build

Requirements depend on the repository's Gradle configuration and Android target at the selected revision. Typical development flow:

```bash
./gradlew assembleDebug
```

Install the generated debug APK on BLE-capable Android hardware and test with real peer devices.

## Testing priorities

For meaningful validation, test:

- permission denial/recovery;
- discovery with multiple devices;
- connection loss and reconnection;
- duplicate/replayed packets;
- store-and-forward expiry;
- app background/foreground transitions;
- malformed packets;
- cryptographic failure states;
- battery/resource behaviour;
- actual cross-platform interoperability where claimed.

## Evidence boundary

“Peer-to-peer” describes the architecture under test; it should not be read as anonymity. Bluetooth identifiers, radio observations, traffic timing and device/application metadata may still expose information.

Likewise, relay/store-and-forward logic is an application experiment rather than proof of a large-scale resilient mesh network.

## Development provenance

This is an authored Android experiment developed with AI-assisted engineering workflows and informed by an upstream BitChat protocol/project. Upstream protocol/code authorship remains with the original project. MAZLABZ portfolio value here is the Android implementation work, BLE integration, application architecture, debugging and evaluation.

## Portfolio significance

**Kotlin · Jetpack Compose · BLE · coroutines · peer-to-peer architecture · crypto-library integration · Android permissions**
