# BitChat Android

A decentralized, peer-to-peer messaging app for Android that operates over Bluetooth Low Energy mesh networks. Based on the original BitChat protocol by jackjackbits.

## Features

- 🔐 **End-to-End Encryption**: X25519 key exchange + AES-256-GCM
- 📡 **Bluetooth LE Mesh**: No internet required, works completely offline
- 🚫 **No Servers**: Pure peer-to-peer communication
- 🏠 **Room-Based Chat**: Join or create topic-based chat rooms
- 💬 **Private Messages**: Encrypted direct messaging between users
- 🔄 **Store & Forward**: Messages cached for offline peers
- 🎭 **Anonymous**: No accounts, phone numbers, or persistent identifiers
- ⚡ **IRC-Style Commands**: Familiar `/join`, `/msg`, `/who` interface
- 🎨 **Material Design 3**: Modern Android UI with dynamic theming
- 🔋 **Battery Optimized**: Adaptive scanning and power management

## Quick Start

### Prerequisites

- Android Studio Hedgehog or newer
- Android device with Bluetooth LE support
- Minimum SDK: Android 8.0 (API 26)
- Target SDK: Android 14 (API 34)

### Installation

1. Clone this repository to your Projects folder
2. Open in Android Studio
3. Sync Gradle files
4. Run on your Android device

### Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## Usage

### Basic Commands

- `/j #room` - Join or create a room
- `/m @user message` - Send private message
- `/w` - List online users  
- `/rooms` - Show all rooms
- `/nick newname` - Change nickname
- `/clear` - Clear chat messages
- `/help` - Show all commands

### Getting Started

1. Launch the app - it will auto-generate a random nickname
2. Grant Bluetooth and location permissions when prompted
3. The app automatically scans for nearby BitChat peers
4. Join the default `#general` room or create your own
5. Start chatting with nearby users!

## Architecture

### Protocol Stack

```
┌─────────────────────────────────────┐
│           Application Layer         │
│     (Chat UI, Commands, Rooms)      │
├─────────────────────────────────────┤
│            Service Layer            │
│  (Encryption, Retry, Compression)   │
├─────────────────────────────────────┤
│         Mesh Network Layer          │
│    (Routing, Relay, Store & Forward) │
├─────────────────────────────────────┤
│          Transport Layer            │
│   (Binary Protocol, BLE GATT)      │
└─────────────────────────────────────┘
```

### Key Components

- **MainActivity**: Main UI using Jetpack Compose
- **ChatViewModel**: Manages chat state and user interactions
- **BleMeshService**: Bluetooth LE mesh networking
- **EncryptionService**: End-to-end encryption and key management
- **MeshMessage**: Protocol data structures

### Security

- **Private Messages**: X25519 ECDH + AES-256-GCM
- **Room Messages**: Argon2id password derivation + AES-256-GCM  
- **Digital Signatures**: Ed25519 for message authenticity
- **Forward Secrecy**: New key pairs generated each session
- **Cover Traffic**: Random dummy messages prevent traffic analysis

## Protocol Compatibility

This Android implementation is compatible with the original iOS BitChat:

- Same Bluetooth service UUIDs
- Identical binary packet format
- Compatible encryption algorithms
- Cross-platform messaging support

## Development

### Project Structure

```
app/src/main/java/com/bitchat/android/
├── MainActivity.kt              # Main activity
├── ChatViewModel.kt             # Chat state management
├── mesh/
│   └── BleMeshService.kt       # Bluetooth LE mesh networking
├── crypto/
│   └── EncryptionService.kt    # Encryption and security
└── ui/theme/
    ├── Theme.kt                # Material Design 3 theme
    └── Typography.kt           # Typography scale
```

### Key Technologies

- **Kotlin**: Primary development language
- **Jetpack Compose**: Modern Android UI toolkit
- **Material Design 3**: Google's latest design system
- **Bluetooth LE**: Low energy mesh networking
- **BouncyCastle**: Cryptographic operations
- **Coroutines**: Asynchronous programming

### Contributing

This project follows the same open principles as the original BitChat:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test on real devices
5. Submit a pull request

## License

Released into the public domain, same as the original BitChat.

## Acknowledgments

- Original BitChat protocol by [jackjackbits](https://github.com/jackjackbits/bitchat)
- Material Design 3 by Google
- Bluetooth LE mesh networking concepts

## Troubleshooting

### Common Issues

**App crashes on startup**
- Ensure Bluetooth permissions are granted
- Check that device supports Bluetooth LE

**No peers discovered**
- Verify location permissions (required for BLE scanning)
- Try moving closer to other BitChat users
- Check Bluetooth is enabled

**Messages not sending**
- Ensure connected to at least one peer
- Check message format (avoid special characters)
- Try rejoining the room

### Debug Build

For development, use the debug build which includes:
- Extended logging
- Debug manifest with `.debug` suffix
- Additional debugging tools

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

**BitChat Android** - Bringing decentralized mesh communication to Android devices.
