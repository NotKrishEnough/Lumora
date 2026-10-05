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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF101522)
private val Cyan = Color(0xFF9CEBFF)
private val Glass = Color.White.copy(alpha = .10f)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LumoraApp() }
    }
}

private data class DemoChat(val title: String, val subtitle: String, val avatar: String, val time: String)

@Composable
private fun LumoraApp() {
    var tab by remember { mutableStateOf("Chats") }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(true) }
    var animations by remember { mutableStateOf(true) }
    var blur by remember { mutableStateOf(true) }
    val chats = listOf(
        DemoChat("Welcome to Lumora", "A calm, glassy space for your chats.", "✦", "Now"),
        DemoChat("Telegram setup", "Connect a Telegram engine to get started.", "T", "—"),
        DemoChat("Your space", "Your conversations will appear here.", "L", "—")
    )
    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Color(0xFF202B3B), primary = Cyan, onBackground = Color.White, onSurface = Color.White)) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF17263D), Ink, Color(0xFF28213C))))) {
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("LUMORA", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text(if (searchOpen) "Search" else tab, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    GlassButton(if (searchOpen) Icons.Default.Close else Icons.Default.Search) { searchOpen = !searchOpen; query = "" }
                    Spacer(Modifier.width(10.dp))
                    GlassButton(Icons.Default.Edit) { tab = "Chats"; searchOpen = true }
                }
                if (searchOpen) {
                    GlassPanel(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = Cyan)
                            Spacer(Modifier.width(10.dp))
                            BasicTextField(value = query, onValueChange = { query = it }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> if (query.isEmpty()) Text("Search chats", color = Color.White.copy(.45f)); inner() })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                when (tab) {
                    "Chats" -> {
                        GlassPanel(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, null, tint = Cyan)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Private by design", fontWeight = FontWeight.SemiBold)
                                    Text("Telegram connection is not active yet.", color = Color.White.copy(.65f), fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text("MESSAGES", color = Color.White.copy(.55f), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        val shown = chats.filter { it.title.contains(query, true) || it.subtitle.contains(query, true) }
                        if (shown.isEmpty()) EmptyState(Icons.Default.Search, "No matching chats", "Try another search.")
                        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(shown) { ChatRow(it) } }
                    }
                    "Contacts" -> EmptyState(Icons.Default.People, "Your contacts", "Contacts will be available after Telegram is connected.")
                    "Calls" -> EmptyState(Icons.Default.Call, "No calls yet", "Your call history will appear here after setup.")
                    else -> {
                        Text("PERSONALIZE", color = Cyan, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        GlassPanel(Modifier.fillMaxWidth()) {
                            SettingRow("Notifications", "Enable message alerts", notifications) { notifications = it }
                            Divider(color = Color.White.copy(.10f), modifier = Modifier.padding(vertical = 8.dp))
                            SettingRow("Smooth animations", "Use motion throughout Lumora", animations) { animations = it }
                            Divider(color = Color.White.copy(.10f), modifier = Modifier.padding(vertical = 8.dp))
                            SettingRow("Glass surfaces", "Show translucent panels", blur) { blur = it }
                        }
                        Spacer(Modifier.height(14.dp))
                        GlassPanel(Modifier.fillMaxWidth()) {
                            Text("ABOUT LUMORA", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Version 0.1.0 · UI preview", fontWeight = FontWeight.SemiBold)
                            Text("This build is a visual prototype. Telegram sign-in, sync and messaging are not implemented.", color = Color.White.copy(.65f), fontSize = 12.sp)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Chats" to Icons.Default.ChatBubble, "Contacts" to Icons.Default.People, "Calls" to Icons.Default.Call, "Settings" to Icons.Default.Settings).forEach { (name, icon) ->
                        Surface(onClick = { tab = name; searchOpen = false }, shape = RoundedCornerShape(22.dp), color = if (tab == name) Cyan.copy(.20f) else Glass, modifier = Modifier.weight(1f).border(1.dp, Color.White.copy(.10f), RoundedCornerShape(22.dp))) {
                            Column(Modifier.padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, null, tint = if (tab == name) Cyan else Color.White.copy(.65f))
                                Text(name, fontSize = 10.sp, color = Color.White.copy(.85f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.White.copy(.58f), fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = Cyan, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(12.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(5.dp))
                Text(subtitle, color = Color.White.copy(.62f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(Glass, RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(.16f), RoundedCornerShape(24.dp)).padding(16.dp), content = content)
}

@Composable
private fun GlassButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = Glass, modifier = Modifier.size(46.dp).border(1.dp, Color.White.copy(.14f), RoundedCornerShape(16.dp))) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White) }
    }
}

@Composable
private fun ChatRow(chat: DemoChat) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(Brush.linearGradient(listOf(Color(0xFF78DDF5), Color(0xFF8D82D9))), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Text(chat.avatar, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(chat.title, fontWeight = FontWeight.SemiBold)
                Text(chat.subtitle, color = Color.White.copy(.62f), fontSize = 12.sp)
            }
            Text(chat.time, color = Color.White.copy(.45f), fontSize = 11.sp)
        }
    }
}
