package com.nke.lumora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
private fun LumoraApp() {
    var tab by remember { mutableStateOf("Chats") }
    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Color(0xFF202B3B), primary = Cyan,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF17263D), Ink, Color(0xFF28213C))))) {
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("LUMORA", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text(tab, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    GlassButton(Icons.Default.Search) {}
                    Spacer(Modifier.width(10.dp))
                    GlassButton(Icons.Default.Edit) {}
                }
                GlassPanel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = Cyan)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Private by design", fontWeight = FontWeight.SemiBold)
                            Text("Your Telegram connection will live here.", color = Color.White.copy(.65f), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("MESSAGES", color = Color.White.copy(.55f), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { ChatRow("Welcome to Lumora", "A calm, glassy space for your chats.", "✦", "Now") }
                    item { ChatRow("Telegram setup", "MTProto client integration is the next step.", "T", "—") }
                    item { ChatRow("Your space", "Chats will appear here after you sign in.", "L", "—") }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Chats" to Icons.Default.ChatBubble, "Contacts" to Icons.Default.People, "Calls" to Icons.Default.Call, "Settings" to Icons.Default.Settings).forEach { (name, icon) ->
                        Surface(onClick = { tab = name }, shape = RoundedCornerShape(22.dp), color = if (tab == name) Cyan.copy(.20f) else Glass, modifier = Modifier.weight(1f).border(1.dp, Color.White.copy(.10f), RoundedCornerShape(22.dp))) {
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
private fun ChatRow(title: String, subtitle: String, avatar: String, time: String) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(Brush.linearGradient(listOf(Color(0xFF78DDF5), Color(0xFF8D82D9))), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Text(avatar, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.White.copy(.62f), fontSize = 12.sp)
            }
            Text(time, color = Color.White.copy(.45f), fontSize = 11.sp)
        }
    }
}
