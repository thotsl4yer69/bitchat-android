package com.bitchat.android

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _currentRoom = MutableStateFlow<String?>("general")
    val currentRoom: StateFlow<String?> = _currentRoom.asStateFlow()
    
    private val _nickname = MutableStateFlow(generateRandomNickname())
    val nickname: StateFlow<String> = _nickname.asStateFlow()
    
    private val _onlineUsers = MutableStateFlow<List<String>>(emptyList())
    val onlineUsers: StateFlow<List<String>> = _onlineUsers.asStateFlow()
    
    private val _rooms = MutableStateFlow<List<String>>(listOf("general"))
    val rooms: StateFlow<List<String>> = _rooms.asStateFlow()
    
    private val dateFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    init {
        // Add welcome message
        addSystemMessage("Welcome to BitChat! Type /help for commands.")
        addSystemMessage("Scanning for nearby peers...")
    }
    
    fun sendMessage(content: String) {
        if (content.startsWith("/")) {
            handleCommand(content)
        } else {
            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = _nickname.value,
                content = content,
                timestamp = dateFormatter.format(Date()),
                room = _currentRoom.value
            )
            
            _messages.value = _messages.value + message
            
            // TODO: Send via BLE mesh network
            // bleMeshService.sendMessage(message)
        }
    }
    
    private fun handleCommand(command: String) {
        val parts = command.split(" ")
        val cmd = parts[0].lowercase()
        
        when (cmd) {
            "/help", "/h" -> {
                addSystemMessage("""
                    Available commands:
                    /j #room - Join or create a room
                    /m @user message - Send private message
                    /w - List online users
                    /rooms - Show all rooms
                    /nick newname - Change nickname
                    /clear - Clear chat messages
                    /help - Show this help
                """.trimIndent())
            }
            
            "/j", "/join" -> {
                if (parts.size > 1) {
                    val roomName = parts[1].removePrefix("#")
                    joinRoom(roomName)
                } else {
                    addSystemMessage("Usage: /j #roomname")
                }
            }
            
            "/nick", "/nickname" -> {
                if (parts.size > 1) {
                    val newNick = parts.drop(1).joinToString(" ")
                    _nickname.value = newNick
                    addSystemMessage("Nickname changed to: $newNick")
                } else {
                    addSystemMessage("Usage: /nick newname")
                }
            }
            
            "/m", "/msg" -> {
                if (parts.size > 2) {
                    val target = parts[1].removePrefix("@")
                    val message = parts.drop(2).joinToString(" ")
                    sendPrivateMessage(target, message)
                } else {
                    addSystemMessage("Usage: /m @username message")
                }
            }
            
            "/w", "/who" -> {
                val users = _onlineUsers.value
                if (users.isNotEmpty()) {
                    addSystemMessage("Online users: ${users.joinToString(", ")}")
                } else {
                    addSystemMessage("No other users online")
                }
            }
            
            "/rooms" -> {
                val rooms = _rooms.value
                addSystemMessage("Available rooms: ${rooms.joinToString(", ")}")
            }
            
            "/clear" -> {
                _messages.value = emptyList()
                addSystemMessage("Chat cleared")
            }
            
            else -> {
                addSystemMessage("Unknown command: $cmd. Type /help for available commands.")
            }
        }
    }
    
    private fun joinRoom(roomName: String) {
        _currentRoom.value = roomName
        if (!_rooms.value.contains(roomName)) {
            _rooms.value = _rooms.value + roomName
        }
        addSystemMessage("Joined room: #$roomName")
        
        // TODO: Send room join message via BLE
        // bleMeshService.joinRoom(roomName)
    }
    
    private fun sendPrivateMessage(target: String, message: String) {
        val privateMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = _nickname.value,
            content = "→ $target: $message",
            timestamp = dateFormatter.format(Date()),
            isPrivate = true
        )
        
        _messages.value = _messages.value + privateMsg
        
        // TODO: Send encrypted private message via BLE
        // bleMeshService.sendPrivateMessage(target, message)
    }
    
    fun receiveMessage(message: ChatMessage) {
        // Only add if not duplicate
        if (_messages.value.none { it.id == message.id }) {
            _messages.value = _messages.value + message
        }
    }
    
    fun updateOnlineUsers(users: List<String>) {
        _onlineUsers.value = users
    }
    
    fun addPeer(nickname: String) {
        val current = _onlineUsers.value.toMutableList()
        if (!current.contains(nickname)) {
            current.add(nickname)
            _onlineUsers.value = current
            addSystemMessage("$nickname joined the mesh")
        }
    }
    
    fun removePeer(nickname: String) {
        val current = _onlineUsers.value.toMutableList()
        if (current.remove(nickname)) {
            _onlineUsers.value = current
            addSystemMessage("$nickname left the mesh")
        }
    }
    
    private fun addSystemMessage(content: String) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = "System",
            content = content,
            timestamp = dateFormatter.format(Date())
        )
        _messages.value = _messages.value + message
    }
    
    private fun generateRandomNickname(): String {
        val adjectives = listOf(
            "Anonymous", "Silent", "Quick", "Bright", "Swift", "Keen", "Bold", "Wise",
            "Calm", "Sharp", "Clear", "Fast", "Smart", "Cool", "Free", "True"
        )
        val nouns = listOf(
            "User", "Node", "Peer", "Guest", "Agent", "Ghost", "Wave", "Signal",
            "Voice", "Echo", "Pulse", "Link", "Path", "Code", "Hash", "Key"
        )
        
        val adj = adjectives.random()
        val noun = nouns.random()
        val number = (1000..9999).random()
        
        return "$adj$noun$number"
    }
}