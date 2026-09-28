package com.bitchat.android.bitnow

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitchat.android.ui.ChatScreen
import com.bitchat.android.ui.ChatViewModel
import kotlinx.coroutines.delay

private enum class BitNowTab { NEARBY, MATCHES, CHATS, ME }

@Composable
fun BitNowRootScreen(viewModel: ChatViewModel) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(BitNowTab.NEARBY) }
    var localProfile by remember { mutableStateOf(BitNowProfileStore.load(context)) }
    val availabilityUntil by viewModel.bitNowAvailabilityUntil.collectAsStateWithLifecycle()
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(availabilityUntil) {
        localProfile = BitNowProfileStore.load(context)
        while (availabilityUntil != null) {
            nowMs = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    BitNowTab.NEARBY to "Nearby",
                    BitNowTab.MATCHES to "Matches",
                    BitNowTab.CHATS to "Chats",
                    BitNowTab.ME to "Me"
                ).forEach { (item, label) ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Text(if (tab == item) "●" else "○") },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                BitNowTab.NEARBY -> BitNowNearbyScreen(
                    viewModel = viewModel,
                    localProfile = localProfile,
                    availabilityUntil = availabilityUntil,
                    nowMs = nowMs,
                    onProfileReload = { localProfile = BitNowProfileStore.load(context) },
                    onOpenChat = { peerId ->
                        viewModel.openBitNowChat(peerId)
                        tab = BitNowTab.CHATS
                    }
                )
                BitNowTab.MATCHES -> BitNowMatchesScreen(
                    viewModel = viewModel,
                    onOpenChat = { peerId ->
                        viewModel.openBitNowChat(peerId)
                        tab = BitNowTab.CHATS
                    }
                )
                BitNowTab.CHATS -> ChatScreen(viewModel = viewModel)
                BitNowTab.ME -> BitNowMeScreen(
                    profile = localProfile,
                    onSave = {
                        viewModel.saveBitNowProfile(it)
                        localProfile = BitNowProfileStore.load(context)
                    }
                )
            }
        }
    }
}

@Composable
fun BitNowProfileSetupScreen(
    initial: BitNowProfile? = null,
    onSave: (BitNowProfile) -> Unit
) {
    var displayName by remember(initial) { mutableStateOf(initial?.displayName.orEmpty()) }
    var ageText by remember(initial) { mutableStateOf(initial?.age?.toString().orEmpty()) }
    var headline by remember(initial) { mutableStateOf(initial?.headline.orEmpty()) }
    var about by remember(initial) { mutableStateOf(initial?.about.orEmpty()) }
    var pronouns by remember(initial) { mutableStateOf(initial?.pronouns.orEmpty()) }
    var intent by remember(initial) { mutableStateOf(initial?.primaryIntent ?: BitNowIntent.MEET_NOW) }
    var identity by remember(initial) { mutableStateOf(initial?.identity) }
    var interestedIn by remember(initial) { mutableStateOf(initial?.interestedIn ?: emptySet()) }
    var showAge by remember(initial) { mutableStateOf(initial?.showAge ?: true) }
    var adultConfirmed by remember(initial) { mutableStateOf(initial != null) }

    val age = ageText.toIntOrNull()
    val valid = adultConfirmed &&
        displayName.isNotBlank() &&
        age != null &&
        age in 18..99 &&
        headline.length <= BitNowSharedProfile.MAX_HEADLINE_LENGTH &&
        about.length <= BitNowSharedProfile.MAX_ABOUT_LENGTH &&
        pronouns.length <= BitNowSharedProfile.MAX_PRONOUNS_LENGTH &&
        BitNowProfileContentPolicy.allows(headline, about, pronouns)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("BitNow", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("Nearby dating without a central account.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Profiles start hidden. Going live is always explicit and time-limited. Interest is not consent.")
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = adultConfirmed, onCheckedChange = { adultConfirmed = it })
                Text("I confirm I am 18 or older.")
            }
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
                onValueChange = { ageText = it.filter(Char::isDigit).take(2) },
                label = { Text("Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showAge, onCheckedChange = { showAge = it })
                Text(if (showAge) "Show my age" else "Show 18+ instead")
            }
        }
        item {
            Text("I am", fontWeight = FontWeight.SemiBold)
            ChipRow(
                options = BitNowIdentity.entries,
                selected = { it == identity },
                label = { it.title },
                onClick = { identity = if (identity == it) null else it }
            )
        }
        item {
            Text("Open to meeting", fontWeight = FontWeight.SemiBold)
            ChipRow(
                options = BitNowIdentity.entries,
                selected = { it in interestedIn },
                label = { it.title },
                onClick = {
                    interestedIn = if (it in interestedIn) interestedIn - it else interestedIn + it
                }
            )
        }
        item {
            Text("What are you up for?", fontWeight = FontWeight.SemiBold)
            ChipRow(
                options = BitNowIntent.entries,
                selected = { it == intent },
                label = { it.title },
                onClick = { intent = it }
            )
        }
        item {
            OutlinedTextField(
                value = headline,
                onValueChange = { headline = it.take(BitNowSharedProfile.MAX_HEADLINE_LENGTH) },
                label = { Text("Headline") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("${headline.length}/${BitNowSharedProfile.MAX_HEADLINE_LENGTH}") }
            )
        }
        item {
            OutlinedTextField(
                value = about,
                onValueChange = { about = it.take(BitNowSharedProfile.MAX_ABOUT_LENGTH) },
                label = { Text("About / boundaries") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("${about.length}/${BitNowSharedProfile.MAX_ABOUT_LENGTH}") }
            )
        }
        item {
            OutlinedTextField(
                value = pronouns,
                onValueChange = { pronouns = it.take(BitNowSharedProfile.MAX_PRONOUNS_LENGTH) },
                label = { Text("Pronouns (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        BitNowProfile(
                            displayName = displayName.trim(),
                            age = age!!,
                            headline = headline.trim(),
                            about = about.trim(),
                            primaryIntent = intent,
                            visibleNearby = initial?.visibleNearby ?: false,
                            showAge = showAge,
                            identity = identity,
                            interestedIn = interestedIn,
                            pronouns = pronouns.trim().takeIf(String::isNotEmpty)
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (initial == null) "Save profile — stay hidden" else "Save changes")
            }
        }
    }
}

@Composable
private fun BitNowNearbyScreen(
    viewModel: ChatViewModel,
    localProfile: BitNowProfile?,
    availabilityUntil: Long?,
    nowMs: Long,
    onProfileReload: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val context = LocalContext.current
    val connected by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val profiles by viewModel.bitNowProfiles.collectAsStateWithLifecycle()
    val nicknames by viewModel.peerNicknames.collectAsStateWithLifecycle()
    val rssi by viewModel.peerRSSI.collectAsStateWithLifecycle()
    val mine by viewModel.bitNowMyInterests.collectAsStateWithLifecycle()
    val theirs by viewModel.bitNowTheirInterests.collectAsStateWithLifecycle()
    val filter by viewModel.bitNowDiscoveryFilter.collectAsStateWithLifecycle()
    val blocked by viewModel.bitNowBlockedPeers.collectAsStateWithLifecycle()
    var window by remember { mutableStateOf(BitNowAvailabilityWindow.ONE_HOUR) }

    val live = localProfile?.visibleNearby == true &&
        availabilityUntil != null &&
        availabilityUntil > nowMs

    val peers = remember(connected, profiles, filter, localProfile, blocked) {
        connected.filter { peerId ->
            val id = peerId.lowercase()
            val profile = profiles[id]
            peerId != viewModel.myPeerID &&
                id !in blocked &&
                profile != null &&
                filter.matches(profile) &&
                profile.appearsInterestedIn(localProfile?.identity)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Nearby now", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(if (live) availabilityLabel(availabilityUntil, nowMs) else "You are hidden")
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (live) "Visible to nearby BitNow users" else "Go live when you want to be discoverable",
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!live) {
                        ChipRow(
                            options = BitNowAvailabilityWindow.entries,
                            selected = { it == window },
                            label = { it.title },
                            onClick = { window = it }
                        )
                        Button(
                            onClick = {
                                viewModel.startBitNowAvailability(window)
                                onProfileReload()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Go live for ${window.title}") }
                    } else {
                        OutlinedButton(
                            onClick = {
                                viewModel.stopBitNowAvailability()
                                onProfileReload()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Hide me now") }
                    }
                }
            }
        }
        item {
            BitNowFilterCard(filter = filter) {
                BitNowDiscoveryFilterStore.save(context, it)
            }
        }

        if (peers.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No matching BitNow profiles in range", fontWeight = FontWeight.SemiBold)
                        Text("Only peers actively sharing a BitNow profile appear here. Exact distance is never shown.")
                    }
                }
            }
        }

        items(peers, key = { it }) { peerId ->
            val profile = profiles.getValue(peerId.lowercase())
            val id = peerId.lowercase()
            val liked = id in mine
            val match = liked && id in theirs
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${nicknames[peerId] ?: "Nearby person"}, ${profile.ageLabel}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(profile.primaryIntent.title, color = MaterialTheme.colorScheme.primary)
                            profile.identity?.let { Text(it.title, style = MaterialTheme.typography.labelMedium) }
                            profile.pronouns?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                        }
                        Text(signalLabel(rssi[peerId]), style = MaterialTheme.typography.labelMedium)
                    }
                    if (profile.headline.isNotBlank()) {
                        Text(profile.headline, fontWeight = FontWeight.SemiBold)
                    }
                    if (profile.about.isNotBlank()) Text(profile.about)
                    if (match) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "MATCH — interest is mutual. Consent still requires a conversation.",
                                Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = live,
                            onClick = { viewModel.setBitNowInterest(peerId, !liked) }
                        ) {
                            Text(if (liked) "Interested ✓" else "Interested")
                        }
                        OutlinedButton(onClick = { onOpenChat(peerId) }) {
                            Text("Message")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.blockBitNowPeer(peerId) }) { Text("Block") }
                        TextButton(onClick = {
                            viewModel.reportBitNowPeer(peerId, "Reported from nearby profile")
                        }) { Text("Report & block") }
                    }
                }
            }
        }
    }
}

@Composable
private fun BitNowMatchesScreen(
    viewModel: ChatViewModel,
    onOpenChat: (String) -> Unit
) {
    val profiles by viewModel.bitNowProfiles.collectAsStateWithLifecycle()
    val nicknames by viewModel.peerNicknames.collectAsStateWithLifecycle()
    val mine by viewModel.bitNowMyInterests.collectAsStateWithLifecycle()
    val theirs by viewModel.bitNowTheirInterests.collectAsStateWithLifecycle()
    val matches = remember(mine, theirs) { (mine intersect theirs).toList() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Matches", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Signals expire after 45 minutes.")
        }
        if (matches.isEmpty()) {
            item { Text("No active mutual matches yet.") }
        }
        items(matches, key = { it }) { id ->
            val profile = profiles[id]
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        nicknames.entries.firstOrNull { it.key.lowercase() == id }?.value
                            ?: profile?.headline?.takeIf { it.isNotBlank() }
                            ?: "Active match",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    profile?.let { Text("${it.ageLabel} · ${it.primaryIntent.title}") }
                    Button(onClick = { onOpenChat(id) }) { Text("Open chat") }
                }
            }
        }
    }
}

@Composable
private fun BitNowMeScreen(
    profile: BitNowProfile?,
    onSave: (BitNowProfile) -> Unit
) {
    if (profile == null) {
        BitNowProfileSetupScreen(onSave = onSave)
    } else {
        BitNowProfileSetupScreen(initial = profile, onSave = onSave)
    }
}

@Composable
private fun BitNowFilterCard(
    filter: BitNowDiscoveryFilter,
    onChange: (BitNowDiscoveryFilter) -> Unit
) {
    var minAge by remember(filter.minimumAge) { mutableStateOf(filter.minimumAge.toString()) }
    var maxAge by remember(filter.maximumAge) { mutableStateOf(filter.maximumAge.toString()) }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Discovery filters", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minAge,
                    onValueChange = {
                        minAge = it.filter(Char::isDigit).take(2)
                        val value = minAge.toIntOrNull()
                        if (value != null) onChange(filter.copy(minimumAge = value).normalized())
                    },
                    label = { Text("Min age") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxAge,
                    onValueChange = {
                        maxAge = it.filter(Char::isDigit).take(2)
                        val value = maxAge.toIntOrNull()
                        if (value != null) onChange(filter.copy(maximumAge = value).normalized())
                    },
                    label = { Text("Max age") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Text("Identity", style = MaterialTheme.typography.labelLarge)
            ChipRow(
                options = BitNowIdentity.entries,
                selected = { it in filter.identities },
                label = { it.title },
                onClick = {
                    val next = if (it in filter.identities) filter.identities - it
                    else filter.identities + it
                    onChange(filter.copy(identities = next))
                }
            )
            Text("Intent", style = MaterialTheme.typography.labelLarge)
            ChipRow(
                options = BitNowIntent.entries,
                selected = { it in filter.intents },
                label = { it.title },
                onClick = {
                    val next = if (it in filter.intents) filter.intents - it
                    else filter.intents + it
                    onChange(filter.copy(intents = next))
                }
            )
        }
    }
}

@Composable
private fun <T> ChipRow(
    options: Iterable<T>,
    selected: (T) -> Boolean,
    label: (T) -> String,
    onClick: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected(option),
                onClick = { onClick(option) },
                label = { Text(label(option)) }
            )
        }
    }
}

private fun availabilityLabel(untilMs: Long?, nowMs: Long): String {
    if (untilMs == null || untilMs <= nowMs) return "You are hidden"
    val minutes = ((untilMs - nowMs + 59_999L) / 60_000L).coerceAtLeast(1)
    return "Live · ${minutes}m remaining"
}

private fun signalLabel(rssi: Int?): String = when {
    rssi == null -> "nearby"
    rssi >= -60 -> "very close"
    rssi >= -75 -> "close"
    else -> "nearby"
}
