package com.bitchat.android.mesh

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class BleMeshService(private val context: Context) {
    companion object {
        private const val TAG = "BleMeshService"
        
        // BitChat Protocol UUIDs (based on iOS version)
        val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val CHARACTERISTIC_TX_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val CHARACTERISTIC_RX_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        
        // Protocol constants
        private const val MAX_PACKET_SIZE = 512
        private const val MAX_TTL = 7
        private const val SCAN_PERIOD = 10000L
        private const val ADVERTISE_PERIOD = 30000L
        
        // Message types (1 byte)
        private const val MSG_TYPE_CHAT = 0x01
        private const val MSG_TYPE_PRIVATE = 0x02
        private const val MSG_TYPE_JOIN_ROOM = 0x03
        private const val MSG_TYPE_ANNOUNCE = 0x04
        private const val MSG_TYPE_PING = 0x05
        private const val MSG_TYPE_PONG = 0x06
        private const val MSG_TYPE_FRAGMENT = 0x07
    }
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    
    private val bleScanner = bluetoothAdapter.bluetoothLeScanner
    private val bleAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
    
    private val gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
    private val connectedDevices = ConcurrentHashMap<String, BluetoothGatt>()
    private val knownPeers = ConcurrentHashMap<String, PeerInfo>()
    private val messageCache = ConcurrentHashMap<String, Long>()
    
    private val _peers = MutableStateFlow<List<String>>(emptyList())
    val peers: StateFlow<List<String>> = _peers
    
    private val _messages = MutableStateFlow<List<MeshMessage>>(emptyList())
    val messages: StateFlow<List<MeshMessage>> = _messages
    
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var isScanning = false
    private var isAdvertising = false
    private var nickname = "Anonymous${Random.nextInt(1000, 9999)}"
    
    fun initialize() {
        Log.d(TAG, "Initializing BitChat BLE Mesh Service")
        setupGattServer()
        startAdvertising()
        startScanning()
        
        // Start periodic announce
        handler.postDelayed(announceRunnable, 5000)
    }
    
    fun setNickname(name: String) {
        nickname = name
    }
    
    fun sendMessage(content: String, room: String? = null, isPrivate: Boolean = false) {
        val message = MeshMessage(
            id = UUID.randomUUID().toString(),
            type = if (isPrivate) MSG_TYPE_PRIVATE else MSG_TYPE_CHAT,
            sender = nickname,
            content = content,
            room = room,
            ttl = MAX_TTL,
            timestamp = System.currentTimeMillis()
        )
        
        broadcastMessage(message)
        _messages.value = _messages.value + message
    }
    
    fun joinRoom(roomName: String) {
        val message = MeshMessage(
            id = UUID.randomUUID().toString(),
            type = MSG_TYPE_JOIN_ROOM,
            sender = nickname,
            content = roomName,
            ttl = MAX_TTL,
            timestamp = System.currentTimeMillis()
        )
        
        broadcastMessage(message)
    }
    
    private fun setupGattServer() {
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        
        val txCharacteristic = BluetoothGattCharacteristic(
            CHARACTERISTIC_TX_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        
        val rxCharacteristic = BluetoothGattCharacteristic(
            CHARACTERISTIC_RX_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        
        val descriptor = BluetoothGattDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        rxCharacteristic.addDescriptor(descriptor)
        
        service.addCharacteristic(txCharacteristic)
        service.addCharacteristic(rxCharacteristic)
        
        gattServer?.addService(service)
    }
    
    private fun startAdvertising() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()
        
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        
        bleAdvertiser?.startAdvertising(settings, data, advertiseCallback)
        isAdvertising = true
        Log.d(TAG, "Started advertising")
    }
    
    private fun startScanning() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        
        bleScanner?.startScan(listOf(filter), settings, scanCallback)
        isScanning = true
        Log.d(TAG, "Started scanning")
        
        // Stop scanning after period and restart
        handler.postDelayed({
            stopScanning()
            handler.postDelayed({ startScanning() }, 2000)
        }, SCAN_PERIOD)
    }
    
    private fun stopScanning() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        if (isScanning) {
            bleScanner?.stopScan(scanCallback)
            isScanning = false
            Log.d(TAG, "Stopped scanning")
        }
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (!connectedDevices.containsKey(device.address)) {
                connectToDevice(device)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
        }
    }
    
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(TAG, "Advertising started successfully")
        }
        
        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "Advertising failed with error: $errorCode")
        }
    }
    
    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        serviceScope.launch {
            try {
                val gatt = device.connectGatt(context, false, gattCallback)
                Log.d(TAG, "Connecting to device: ${device.address}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to device: ${e.message}")
            }
        }
    }
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to ${gatt.device.address}")
                    connectedDevices[gatt.device.address] = gatt
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from ${gatt.device.address}")
                    connectedDevices.remove(gatt.device.address)
                    gatt.close()
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                val rxCharacteristic = service?.getCharacteristic(CHARACTERISTIC_RX_UUID)
                
                if (rxCharacteristic != null) {
                    gatt.setCharacteristicNotification(rxCharacteristic, true)
                    val descriptor = rxCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                    
                    // Send announce message
                    sendAnnounce(gatt)
                }
            }
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == CHARACTERISTIC_RX_UUID) {
                val data = characteristic.value
                handleReceivedData(data, gatt.device.address)
            }
        }
    }
    
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Device connected to server: ${device.address}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Device disconnected from server: ${device.address}")
                }
            }
        }
        
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (characteristic.uuid == CHARACTERISTIC_TX_UUID) {
                handleReceivedData(value, device.address)
                
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
            }
        }
        
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
        }
    }
    
    private fun sendAnnounce(gatt: BluetoothGatt) {
        val message = MeshMessage(
            id = UUID.randomUUID().toString(),
            type = MSG_TYPE_ANNOUNCE,
            sender = nickname,
            content = "",
            ttl = 1,
            timestamp = System.currentTimeMillis()
        )
        
        sendMessageToGatt(gatt, message)
    }
    
    private fun sendMessageToGatt(gatt: BluetoothGatt, message: MeshMessage) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        val service = gatt.getService(SERVICE_UUID)
        val txCharacteristic = service?.getCharacteristic(CHARACTERISTIC_TX_UUID)
        
        if (txCharacteristic != null) {
            val data = serializeMessage(message)
            txCharacteristic.value = data
            gatt.writeCharacteristic(txCharacteristic)
        }
    }
    
    private fun broadcastMessage(message: MeshMessage) {
        // Add to cache to prevent loops
        messageCache[message.id] = System.currentTimeMillis()
        
        // Send to all connected devices
        connectedDevices.values.forEach { gatt ->
            sendMessageToGatt(gatt, message)
        }
        
        // Also send via GATT server to connected clients
        val data = serializeMessage(message)
        val service = gattServer?.getService(SERVICE_UUID)
        val rxCharacteristic = service?.getCharacteristic(CHARACTERISTIC_RX_UUID)
        
        if (rxCharacteristic != null && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            rxCharacteristic.value = data
            bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER).forEach { device ->
                gattServer?.notifyCharacteristicChanged(device, rxCharacteristic, false)
            }
        }
    }
    
    private fun handleReceivedData(data: ByteArray, senderAddress: String) {
        try {
            val message = deserializeMessage(data)
            
            // Check if we've seen this message before
            if (messageCache.containsKey(message.id)) {
                return
            }
            
            // Add to cache
            messageCache[message.id] = System.currentTimeMillis()
            
            when (message.type) {
                MSG_TYPE_ANNOUNCE -> {
                    handleAnnounce(message, senderAddress)
                }
                MSG_TYPE_CHAT -> {
                    handleChatMessage(message)
                }
                MSG_TYPE_PRIVATE -> {
                    handlePrivateMessage(message)
                }
                MSG_TYPE_JOIN_ROOM -> {
                    handleJoinRoom(message)
                }
                MSG_TYPE_PING -> {
                    handlePing(message, senderAddress)
                }
                MSG_TYPE_PONG -> {
                    handlePong(message, senderAddress)
                }
            }
            
            // Relay message if TTL > 0
            if (message.ttl > 0) {
                val relayMessage = message.copy(ttl = message.ttl - 1)
                relayMessage(relayMessage, senderAddress)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle received data: ${e.message}")
        }
    }
    
    private fun handleAnnounce(message: MeshMessage, senderAddress: String) {
        val peerInfo = PeerInfo(
            nickname = message.sender,
            address = senderAddress,
            lastSeen = System.currentTimeMillis()
        )
        
        knownPeers[message.sender] = peerInfo
        updatePeersList()
        
        Log.d(TAG, "Peer announced: ${message.sender}")
    }
    
    private fun handleChatMessage(message: MeshMessage) {
        _messages.value = _messages.value + message
        Log.d(TAG, "Chat message from ${message.sender}: ${message.content}")
    }
    
    private fun handlePrivateMessage(message: MeshMessage) {
        // TODO: Implement decryption for private messages
        _messages.value = _messages.value + message
        Log.d(TAG, "Private message from ${message.sender}")
    }
    
    private fun handleJoinRoom(message: MeshMessage) {
        Log.d(TAG, "${message.sender} joined room: ${message.content}")
    }
    
    private fun handlePing(message: MeshMessage, senderAddress: String) {
        val pong = MeshMessage(
            id = UUID.randomUUID().toString(),
            type = MSG_TYPE_PONG,
            sender = nickname,
            content = message.id, // Echo back the ping ID
            ttl = 1,
            timestamp = System.currentTimeMillis()
        )
        
        // Send pong only to sender
        connectedDevices[senderAddress]?.let { gatt ->
            sendMessageToGatt(gatt, pong)
        }
    }
    
    private fun handlePong(message: MeshMessage, senderAddress: String) {
        // Update peer info with fresh timestamp
        knownPeers[message.sender]?.let { peer ->
            knownPeers[message.sender] = peer.copy(lastSeen = System.currentTimeMillis())
        }
    }
    
    private fun relayMessage(message: MeshMessage, originalSender: String) {
        connectedDevices.values.forEach { gatt ->
            if (gatt.device.address != originalSender) {
                sendMessageToGatt(gatt, message)
            }
        }
    }
    
    private fun updatePeersList() {
        val currentTime = System.currentTimeMillis()
        val activePeers = knownPeers.values
            .filter { currentTime - it.lastSeen < 60000 } // 1 minute timeout
            .map { it.nickname }
        
        _peers.value = activePeers
    }
    
    private val announceRunnable = object : Runnable {
        override fun run() {
            if (connectedDevices.isNotEmpty()) {
                val announce = MeshMessage(
                    id = UUID.randomUUID().toString(),
                    type = MSG_TYPE_ANNOUNCE,
                    sender = nickname,
                    content = "",
                    ttl = MAX_TTL,
                    timestamp = System.currentTimeMillis()
                )
                
                broadcastMessage(announce)
            }
            
            // Clean up old message cache entries
            val currentTime = System.currentTimeMillis()
            messageCache.entries.removeAll { currentTime - it.value > 300000 } // 5 minutes
            
            // Update peers list
            updatePeersList()
            
            handler.postDelayed(this, 30000) // Announce every 30 seconds
        }
    }
    
    private fun serializeMessage(message: MeshMessage): ByteArray {
        val buffer = ByteBuffer.allocate(MAX_PACKET_SIZE)
        
        // Protocol header
        buffer.put(message.type.toByte())
        buffer.put(message.ttl.toByte())
        
        // Message ID (16 bytes)
        val idBytes = message.id.toByteArray()
        buffer.put(idBytes.size.toByte())
        buffer.put(idBytes)
        
        // Sender
        val senderBytes = message.sender.toByteArray()
        buffer.put(senderBytes.size.toByte())
        buffer.put(senderBytes)
        
        // Content
        val contentBytes = message.content.toByteArray()
        buffer.putShort(contentBytes.size.toShort())
        buffer.put(contentBytes)
        
        // Room (optional)
        val roomBytes = (message.room ?: "").toByteArray()
        buffer.put(roomBytes.size.toByte())
        buffer.put(roomBytes)
        
        // Timestamp
        buffer.putLong(message.timestamp)
        
        return buffer.array().sliceArray(0..buffer.position() - 1)
    }
    
    private fun deserializeMessage(data: ByteArray): MeshMessage {
        val buffer = ByteBuffer.wrap(data)
        
        val type = buffer.get().toInt()
        val ttl = buffer.get().toInt()
        
        val idLength = buffer.get().toInt()
        val idBytes = ByteArray(idLength)
        buffer.get(idBytes)
        val id = String(idBytes)
        
        val senderLength = buffer.get().toInt()
        val senderBytes = ByteArray(senderLength)
        buffer.get(senderBytes)
        val sender = String(senderBytes)
        
        val contentLength = buffer.short.toInt()
        val contentBytes = ByteArray(contentLength)
        buffer.get(contentBytes)
        val content = String(contentBytes)
        
        val roomLength = buffer.get().toInt()
        val room = if (roomLength > 0) {
            val roomBytes = ByteArray(roomLength)
            buffer.get(roomBytes)
            String(roomBytes)
        } else null
        
        val timestamp = buffer.long
        
        return MeshMessage(
            id = id,
            type = type,
            sender = sender,
            content = content,
            room = room,
            ttl = ttl,
            timestamp = timestamp
        )
    }
    
    fun cleanup() {
        handler.removeCallbacks(announceRunnable)
        stopScanning()
        
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            bleAdvertiser?.stopAdvertising(advertiseCallback)
        }
        
        connectedDevices.values.forEach { gatt ->
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gatt.disconnect()
                gatt.close()
            }
        }
        
        gattServer?.close()
        serviceScope.cancel()
    }
}

data class MeshMessage(
    val id: String,
    val type: Int,
    val sender: String,
    val content: String,
    val room: String? = null,
    val ttl: Int,
    val timestamp: Long
)

data class PeerInfo(
    val nickname: String,
    val address: String,
    val lastSeen: Long
)