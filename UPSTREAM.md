# Upstream provenance

BitNow Android is derived from `permissionlesstech/bitchat-android`. The exact imported revision is stored in `UPSTREAM_COMMIT`.

Transport, packet framing, cryptographic session, routing and peer-discovery code remain in their upstream namespaces to minimize compatibility risk. BitNow-specific product code is isolated under `com.bitchat.android.bitnow`.
