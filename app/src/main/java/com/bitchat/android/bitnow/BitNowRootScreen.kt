package com.bitchat.android.bitnow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitchat.android.ui.ChatScreen
import com.bitchat.android.ui.ChatViewModel

private enum class BitNowTab { NEARBY, MESSAGES }

@Composable
fun BitNowRootScreen(viewModel: ChatViewModel) {
    var tab by remember { mutableStateOf(BitNowTab.NEARBY) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == BitNowTab.NEARBY,
                    onClick = { tab = BitNowTab.NEARBY },
                    icon = { Text("●") },
                    label = { Text("Nearby") }
                )
                NavigationBarItem(
                    selected = tab == BitNowTab.MESSAGES,
                    onClick = { tab = BitNowTab.MESSAGES },
                    icon = { Text("✦") },
                    label = { Text("Messages") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                BitNowTab.NEARBY -> BitNowNearbyScreen(viewModel)
                BitNowTab.MESSAGES -> ChatScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BitNowProfileSetupScreen(
    initial: BitNowProfile? = null,
    onSave: (BitNowProfile) -> Unit
) {
    var displayName by remember { mutableStateOf(initial?.displayName.orEmpty()) }
    var ageText by remember { mutableStateOf(initial?.age?.toString().orEmpty()) }
    var bio by remember { mutableStateOf(initial?.bio.orEmpty()) }
    var intent by remember { mutableStateOf(initial?.intent ?: "Meet now") }
    val age = ageText.toIntOrNull()
    val valid = displayName.isNotBlank() && age != null && age >= 18 && bio.length <= 280

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("BitNow", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("Meet nearby. Message privately.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("18+ only. Your profile stays on this device and is shared directly with nearby BitNow peers.")
        }
        item {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it.take(40) },
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = ageText,
                onValueChange = { ageText = it.filter(Char::isDigit).take(3) },
                label = { Text("Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { if (age != null && age < 18) Text("BitNow is 18+ only") }
            )
        }
        item {
            Text("What are you up for?", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Meet now", "Chat now", "Open to plans").forEach { option ->
                    FilterChip(
                        selected = intent == option,
                        onClick = { intent = option },
                        label = { Text(option) }
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(280) },
                label = { Text("Bio") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("${bio.length}/280") }
            )
        }
        item {
            Button(
                enabled = valid,
                onClick = { onSave(BitNowProfile(displayName.trim(), age!!, bio.trim(), intent, true)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go nearby")
            }
        }
    }
}

@Composable
private fun BitNowNearbyScreen(viewModel: ChatViewModel) {
    val connected by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val profiles by viewModel.bitNowProfiles.collectAsStateWithLifecycle()
    val nicknames by viewModel.peerNicknames.collectAsStateWithLifecycle()
    val rssi by viewModel.peerRSSI.collectAsStateWithLifecycle()
    val mine by viewModel.bitNowMyInterests.collectAsStateWithLifecycle()
    val theirs by viewModel.bitNowTheirInterests.collectAsStateWithLifecycle()

    val peers = remember(connected, profiles) {
        connected.filter { it != viewModel.myPeerID && profiles.containsKey(it.lowercase()) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("BitNow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(if (peers.isEmpty()) "Looking nearby…" else "${peers.size} nearby now")
                }
                AssistChip(onClick = {}, label = { Text("LIVE") })
            }
        }

        if (peers.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No BitNow profiles in range yet", fontWeight = FontWeight.SemiBold)
                        Text("Bluetooth mesh is active. Nearby BitChat peers can still appear in Messages; BitNow cards appear when another BitNow client shares its profile.")
                    }
                }
            }
        }

        items(peers, key = { it }) { peerId ->
            val profile = profiles.getValue(peerId.lowercase())
            val id = peerId.lowercase()
            val liked = id in mine
            val match = liked && id in theirs
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("${profile.displayName}, ${profile.age}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(profile.intent, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(signalLabel(rssi[peerId]), style = MaterialTheme.typography.labelMedium)
                    }
                    if (profile.bio.isNotBlank()) Text(profile.bio)
                    Text("Mesh identity: ${nicknames[peerId] ?: peerId.take(8)}", style = MaterialTheme.typography.labelSmall)
                    if (match) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                            Text("MATCH — interest is mutual", Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.setBitNowInterest(peerId, !liked) }) {
                            Text(if (liked) "Interested ✓" else "Interested")
                        }
                        OutlinedButton(onClick = { viewModel.openBitNowChat(peerId) }) {
                            Text("Message")
                        }
                    }
                }
            }
        }
    }
}

private fun signalLabel(rssi: Int?): String = when {
    rssi == null -> "nearby"
    rssi >= -60 -> "very close"
    rssi >= -75 -> "close"
    else -> "nearby"
}
