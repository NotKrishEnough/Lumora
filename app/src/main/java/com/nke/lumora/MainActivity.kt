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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.graphics.asImageBitmap
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
                    else -> SimpleSection("Settings", Icons.Default.Settings)
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
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { tdLib.closeChat() }) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }
            Avatar(chat.title, tdLib.avatarPaths[chat.id])
            Spacer(Modifier.width(12.dp))
            Text(chat.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        HorizontalDivider(color = Color.White.copy(.08f))
        if (tdLib.messages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading messages…", color = Color.White.copy(.55f))
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tdLib.messages, key = { it.id }) { message ->
                    val text = if (message.content is TdApi.MessageText) {
                        (message.content as TdApi.MessageText).text.text
                    } else {
                        message.content.javaClass.simpleName.removePrefix("Message")
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            text,
                            color = Color.White,
                            modifier = Modifier.background(Glass, RoundedCornerShape(18.dp)).padding(12.dp)
                        )
                    }
                }
            }
        }
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