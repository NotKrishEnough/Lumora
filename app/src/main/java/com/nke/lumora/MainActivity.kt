package com.nke.lumora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
        onSurface = Color.White
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
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text("LUMORA", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.height(10.dp))
        Text("Chats", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        if (tdLib.chats.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Cyan, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Loading your chats…", color = Color.White.copy(.55f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(tdLib.chats, key = { it.id }) { chat -> ChatRow(chat) }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: TdApi.Chat) {
    Row(
        Modifier.fillMaxWidth()
            .background(Glass, RoundedCornerShape(22.dp))
            .border(1.dp, Color.White.copy(.10f), RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).background(Cyan.copy(.18f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(chat.title.take(1).uppercase(), color = Cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(chat.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                if (chat.lastMessage != null) "Telegram chat" else "No messages yet",
                color = Color.White.copy(.50f),
                fontSize = 12.sp
            )
        }
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
        Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold)
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