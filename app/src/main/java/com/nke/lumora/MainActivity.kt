package com.nke.lumora

import android.os.Bundle
import androidx.activity.ComponentActivity
import org.drinkless.tdlib.TdApi
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Security
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import android.graphics.BitmapFactory
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF101522)
private val Cyan = Color(0xFF9CEBFF)
private val Glass = Color.White.copy(alpha = .10f)

class MainActivity : ComponentActivity() {
    private lateinit var tdLib: TdLibManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tdLib = TdLibManager(this)
        setContent { LumoraApp(tdLib) }
        tdLib.start()
    }

    override fun onDestroy() {
        tdLib.close()
        super.onDestroy()
    }
}

@Composable
private fun LumoraApp(tdLib: TdLibManager) {
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(tdLib) {
        tdLib.observe { version++ }
        onDispose { }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink,
        surface = Color(0xFF202B3B),
        primary = Cyan,
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color.White.copy(alpha = .72f)
    )) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF17263D), Ink, Color(0xFF28213C)))
            )
        ) {
            when (tdLib.state) {
                TdLibManager.AuthState.Starting,
                TdLibManager.AuthState.Initializing -> LoadingScreen()
                TdLibManager.AuthState.WaitingForPhone -> PhoneScreen(tdLib)
                TdLibManager.AuthState.WaitingForCode -> CodeScreen(tdLib)
                TdLibManager.AuthState.WaitingForPassword -> PasswordScreen(tdLib)
                TdLibManager.AuthState.Ready -> ConnectedScreen(tdLib)
                TdLibManager.AuthState.Closed -> ErrorScreen("Telegram connection closed.")
                is TdLibManager.AuthState.Error -> ErrorScreen(tdLib.error ?: "Telegram setup failed.")
            }
        }
    }
    @Suppress("UNUSED_VARIABLE")
    val keepRecompositionAlive = version
}

@Composable
private fun LoadingScreen() {
    CenterColumn {
        CircularProgressIndicator(color = Cyan)
        Spacer(Modifier.height(18.dp))
        Text("Connecting to Telegram", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Starting Lumora's Telegram engine…", color = Color.White.copy(.60f), fontSize = 13.sp)
    }
}

@Composable
private fun PhoneScreen(tdLib: TdLibManager) {
    var phone by remember { mutableStateOf("") }
    AuthCard("Welcome to Lumora", "Enter your Telegram phone number, including the country code.") {
        OutlinedTextField(
            phone, { phone = it },
            label = { Text("Phone number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton("Continue", phone.isNotBlank()) { tdLib.setPhone(phone) }
        ErrorText(tdLib.error)
    }
}

@Composable
private fun CodeScreen(tdLib: TdLibManager) {
    var code by remember { mutableStateOf("") }
    AuthCard("Check your Telegram", "Telegram sent a login code to your account. Enter it here.") {
        OutlinedTextField(
            code, { code = it },
            label = { Text("Login code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton("Verify", code.isNotBlank()) { tdLib.setCode(code) }
        ErrorText(tdLib.error)
    }
}

@Composable
private fun PasswordScreen(tdLib: TdLibManager) {
    var password by remember { mutableStateOf("") }
    AuthCard("Two-step verification", "Enter your Telegram 2FA password if your account has one.") {
        OutlinedTextField(
            password, { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton("Sign in", password.isNotBlank()) { tdLib.setPassword(password) }
        ErrorText(tdLib.error)
    }
}

@Composable
private fun ConnectedScreen(tdLib: TdLibManager) {
    var tab by remember { mutableIntStateOf(0) }
    val selected = tdLib.selectedChat

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (selected != null) {
                ChatScreen(tdLib, selected)
            } else {
                when (tab) {
                    0 -> ChatsScreen(tdLib)
                    1 -> SimpleSection("Contacts", Icons.Default.Contacts)
                    2 -> SimpleSection("Calls", Icons.Default.Call)
                    else -> SettingsScreen()
                }
            }
        }
        if (selected == null) {
            NavigationBar(
                containerColor = Color.Black.copy(.28f),
                tonalElevation = 0.dp
            ) {
                val items = listOf(
                    "Chats" to Icons.Default.Chat,
                    "Contacts" to Icons.Default.Contacts,
                    "Calls" to Icons.Default.Call,
                    "Settings" to Icons.Default.Settings
                )
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.second, null) },
                        label = { Text(item.first) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Cyan,
                            selectedTextColor = Cyan,
                            unselectedIconColor = Color.White.copy(.55f),
                            unselectedTextColor = Color.White.copy(.55f),
                            indicatorColor = Cyan.copy(.14f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatsScreen(tdLib: TdLibManager) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text("LUMORA", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        Text("Chats", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        if (tdLib.chats.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading your chats…", color = Color.White.copy(.55f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(tdLib.chats, key = { it.id }) { chat -> ChatRow(chat, tdLib) }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: TdApi.Chat, tdLib: TdLibManager) {
    val path = tdLib.avatarPaths[chat.id]
    Row(
        Modifier.fillMaxWidth()
            .clickable { tdLib.openChat(chat) }
            .background(Glass, RoundedCornerShape(22.dp))
            .border(1.dp, Color.White.copy(.10f), RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(chat.title, path)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(chat.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                if (chat.lastMessage != null) "Telegram chat" else "No messages yet",
                color = Color.White.copy(.50f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun Avatar(title: String, path: String?) {
    val bitmap = remember(path) { path?.let { BitmapFactory.decodeFile(it) } }
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    } else {
        Box(
            Modifier.size(52.dp).background(Cyan.copy(.18f), RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(title.take(1).uppercase(), color = Cyan, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        }
    }
}

@Composable
private fun ChatScreen(tdLib: TdLibManager, chat: TdApi.Chat) {
    var draft by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<TdApi.Message?>(null) }
    var actionMessage by remember { mutableStateOf<TdApi.Message?>(null) }
    var forwardingMessage by remember { mutableStateOf<TdApi.Message?>(null) }
    var showMediaPicker by remember { mutableStateOf(false) }
    var mediaTab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { tdLib.closeChat() }) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Avatar(chat.title, tdLib.avatarPaths[chat.id])
            Spacer(Modifier.width(12.dp))
            Text(chat.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        HorizontalDivider(color = Color.White.copy(.08f))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (tdLib.messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No messages yet", color = Color.White.copy(.55f))
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    reverseLayout = true,
                    contentPadding = PaddingValues(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(tdLib.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            senderName = tdLib.senderNames[senderKey(message.senderId)],
                            mediaPath = tdLib.mediaPaths[message.id],
                            onLongPress = { actionMessage = message }
                        )
                    }
                }
            }
        }

        replyTo?.let { message ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    .background(Cyan.copy(.10f), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Replying to " + (tdLib.senderNames[senderKey(message.senderId)] ?: "message"), color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(messagePreview(message), color = Color.White.copy(.65f), fontSize = 12.sp, maxLines = 1)
                }
                TextButton(onClick = { replyTo = null }) { Text("Cancel", color = Color.White) }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                mediaTab = 0
                showMediaPicker = true
                tdLib.loadStickerAndAnimationPicks()
            }) {
                Text("＋", color = Cyan, fontSize = 26.sp)
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message", color = Color.White.copy(.45f)) },
                maxLines = 4,
                shape = RoundedCornerShape(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    tdLib.sendMessage(draft, replyTo?.id ?: 0L)
                    draft = ""
                    replyTo = null
                },
                enabled = draft.isNotBlank()
            ) {
                Icon(Icons.Default.Send, "Send", tint = if (draft.isNotBlank()) Cyan else Color.White.copy(.3f))
            }
        }
    }

    actionMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { actionMessage = null },
            title = { Text("Message actions", color = Color.White) },
            text = { Text(messagePreview(message), color = Color.White.copy(.65f), maxLines = 3) },
            confirmButton = {
                Row {
                    TextButton(onClick = { replyTo = message; actionMessage = null }) { Text("Reply", color = Cyan) }
                    TextButton(onClick = { forwardingMessage = message; actionMessage = null }) { Text("Forward", color = Cyan) }
                }
            },
            containerColor = Color(0xFF202B3B)
        )
    }

    forwardingMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { forwardingMessage = null },
            title = { Text("Forward to…", color = Color.White) },
            text = {
                Column {
                    tdLib.chats.filter { it.id != chat.id }.take(12).forEach { target ->
                        TextButton(
                            onClick = {
                                tdLib.forwardMessage(message, target.id)
                                forwardingMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(target.title, color = Color.White, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { forwardingMessage = null }) { Text("Cancel", color = Cyan) } },
            containerColor = Color(0xFF202B3B)
        )
    }

    if (showMediaPicker) {
        AlertDialog(
            onDismissRequest = { showMediaPicker = false },
            title = { Text("Stickers & GIFs", color = Color.White) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth()) {
                        TextButton(onClick = { mediaTab = 0 }) { Text("Stickers", color = if (mediaTab == 0) Cyan else Color.White.copy(.6f)) }
                        TextButton(onClick = { mediaTab = 1 }) { Text("GIFs", color = if (mediaTab == 1) Cyan else Color.White.copy(.6f)) }
                    }
                    LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (mediaTab == 0) {
                            items(tdLib.stickers, key = { it.id }) { sticker ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { tdLib.sendSticker(sticker); showMediaPicker = false }.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val path = tdLib.filePaths[sticker.sticker.id]
                                    if (path != null) {
                                        val bitmap = remember(path) { BitmapFactory.decodeFile(path) }
                                        if (bitmap != null) androidx.compose.foundation.Image(bitmap.asImageBitmap(), null, Modifier.size(54.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                                    } else {
                                        Text(sticker.emoji ?: "🙂", fontSize = 30.sp, modifier = Modifier.width(54.dp))
                                    }
                                    Text(sticker.emoji ?: "Sticker", color = Color.White, modifier = Modifier.padding(start = 10.dp))
                                }
                            }
                        } else {
                            items(tdLib.animations, key = { it.animation.id }) { animation ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { tdLib.sendAnimation(animation); showMediaPicker = false }.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val path = tdLib.filePaths[animation.animation.id]
                                    if (path != null) {
                                        val bitmap = remember(path) { BitmapFactory.decodeFile(path) }
                                        if (bitmap != null) androidx.compose.foundation.Image(bitmap.asImageBitmap(), null, Modifier.size(80.dp, 54.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                        else Text("GIF", color = Cyan, modifier = Modifier.width(54.dp))
                                    } else {
                                        Text("GIF", color = Cyan, modifier = Modifier.width(54.dp))
                                    }
                                    Text(animation.fileName, color = Color.White, maxLines = 1, modifier = Modifier.padding(start = 10.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMediaPicker = false }) { Text("Close", color = Cyan) } },
            containerColor = Color(0xFF202B3B)
        )
    }
}

@Composable
private fun MessageBubble(
    message: TdApi.Message,
    senderName: String?,
    mediaPath: String?,
    onLongPress: () -> Unit
) {
    val incoming = !message.isOutgoing
    val forwardLabel = message.forwardInfo?.let { "Forwarded message" }
    val replyLabel = if (message.replyTo != null || message.replyToMessageId != 0L) "↩ Reply" else null
    val content = messagePreview(message)

    Column(
        Modifier.fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = if (incoming) 0.dp else 24.dp, vertical = 2.dp),
        horizontalAlignment = if (incoming) Alignment.Start else Alignment.End
    ) {
        if (incoming && !senderName.isNullOrBlank()) {
            Text(senderName, color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
        }
        if (forwardLabel != null) Text(forwardLabel, color = Color.White.copy(.45f), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp))
        if (replyLabel != null) Text(replyLabel, color = Cyan.copy(.75f), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp))
        Row(
            Modifier.background(
                if (incoming) Glass else Cyan.copy(.18f),
                RoundedCornerShape(18.dp)
            ).padding(11.dp)
        ) {
            when {
                mediaPath != null && (message.content is TdApi.MessageSticker) -> {
                    val bitmap = remember(mediaPath) { BitmapFactory.decodeFile(mediaPath) }
                    if (bitmap != null) androidx.compose.foundation.Image(bitmap.asImageBitmap(), null, Modifier.sizeIn(maxWidth = 180.dp, maxHeight = 180.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                    else Text("Sticker", color = Color.White)
                }
                message.content is TdApi.MessageAnimation -> {
                    Text("GIF • " + ((message.content as TdApi.MessageAnimation).animation?.fileName ?: "animation"), color = Color.White)
                }
                else -> Text(content, color = Color.White)
            }
        }
    }
}

private fun senderKey(sender: TdApi.MessageSender): String = when (sender) {
    is TdApi.MessageSenderUser -> "u:" + sender.userId
    is TdApi.MessageSenderChat -> "c:" + sender.chatId
    else -> "x:0"
}

private fun messagePreview(message: TdApi.Message): String = when (val content = message.content) {
    is TdApi.MessageText -> content.text.text
    is TdApi.MessageSticker -> "Sticker " + (content.sticker?.emoji ?: "")
    is TdApi.MessageAnimation -> "GIF"
    else -> content.javaClass.simpleName.removePrefix("Message")
}

@Composable
private fun SettingsScreen() {
    var notifications by remember { mutableStateOf(true) }
    var animations by remember { mutableStateOf(true) }
    var glass by remember { mutableStateOf(true) }
    var compact by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text("LUMORA", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        Text("Settings", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            item { SettingsHeader("Appearance") }
            item { SettingToggle("Glass interface", "Use Lumora's glass surfaces", Icons.Default.DarkMode, glass) { glass = it } }
            item { SettingToggle("Animations", "Smooth transitions and motion", Icons.Default.Animation, animations) { animations = it } }
            item { SettingToggle("Compact chats", "Reduce spacing in the chat list", Icons.Default.Security, compact) { compact = it } }
            item { SettingsHeader("Notifications") }
            item { SettingToggle("Message notifications", "Show notifications for new messages", Icons.Default.Notifications, notifications) { notifications = it } }
            item {
                Row(Modifier.fillMaxWidth().background(Glass, RoundedCornerShape(20.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Telegram account", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Connected via TDLib", color = Color.White.copy(.5f), fontSize = 12.sp)
                    }
                    Text("CONNECTED", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            item { Text("Lumora • Telegram client", color = Color.White.copy(.35f), fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
        }
    }
}

@Composable
private fun SettingsHeader(text: String) {
    Text(text, color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp))
}

@Composable
private fun SettingToggle(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Glass, RoundedCornerShape(20.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.White.copy(.5f), fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SimpleSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Coming next", color = Color.White.copy(.50f), fontSize = 13.sp)
    }
}

@Composable
private fun AuthCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().safeDrawingPadding().padding(horizontal = 22.dp)
            .background(Glass, RoundedCornerShape(28.dp))
            .border(1.dp, Color.White.copy(.16f), RoundedCornerShape(28.dp))
            .padding(22.dp)
    ) {
        Text("LUMORA", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Color.White.copy(.62f), fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))
        content()
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ErrorText(message: String?) {
    if (!message.isNullOrBlank()) {
        Spacer(Modifier.height(12.dp))
        Text(message, color = Color(0xFFFF9A9A), fontSize = 12.sp)
    }
}

@Composable
private fun CenterColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
private fun ErrorScreen(message: String) {
    CenterColumn {
        Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFFF9A9A), modifier = Modifier.size(50.dp))
        Spacer(Modifier.height(14.dp))
        Text("Telegram setup error", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Color.White.copy(.65f), fontSize = 13.sp)
    }
}