package com.nke.lumora

import android.content.Context
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File

class TdLibManager(context: Context) {
    private val appContext = context.applicationContext
    private val databaseDir = File(appContext.filesDir, "tdlib").apply { mkdirs() }
    private val filesDir = File(appContext.filesDir, "tdlib-files").apply { mkdirs() }
    private var client: Client? = null
    private var apiId: Int = 0
    var state: AuthState = AuthState.Starting
        private set
    var error: String? = null
        private set
    private var listener: (() -> Unit)? = null
    var chats: List<TdApi.Chat> = emptyList()
        private set
    var selectedChat: TdApi.Chat? = null
        private set
    var messages: List<TdApi.Message> = emptyList()
        private set
    var avatarPaths: Map<Long, String> = emptyMap()
        private set

    fun observe(listener: () -> Unit) {
        this.listener = listener
        listener()
    }

    fun start() {
        if (client != null) return
        apiId = BuildConfig.TELEGRAM_API_ID.toIntOrNull() ?: 0
        val apiHash = BuildConfig.TELEGRAM_API_HASH
        if (apiId <= 0 || apiHash.isBlank()) {
            state = AuthState.Error("Telegram API credentials are not configured in this build.")
            notifyChanged()
            return
        }

        client = Client.create({ update ->
            when (update) {
                is TdApi.UpdateAuthorizationState -> handleAuthState(update.authorizationState)
                is TdApi.UpdateFile -> {
                    val file = update.file
                    if (file.local.isDownloadingCompleted && file.local.path.isNotBlank()) {
                        val match = chats.firstOrNull { it.photo?.small?.id == file.id }
                        if (match != null) {
                            avatarPaths = avatarPaths + (match.id to file.local.path)
                            notifyChanged()
                        }
                    }
                }
            }
        }, { throwable ->
            error = throwable.message ?: throwable.javaClass.simpleName
            notifyChanged()
        }, { throwable ->
            error = throwable.message ?: throwable.javaClass.simpleName
            notifyChanged()
        })

        client?.send(TdApi.SetLogVerbosityLevel(1), { })
    }

    private fun handleAuthState(authState: TdApi.AuthorizationState) {
        when (authState) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                state = AuthState.Initializing
                notifyChanged()
                client?.send(TdApi.SetTdlibParameters(
                    false,
                    databaseDir.absolutePath,
                    filesDir.absolutePath,
                    ByteArray(0),
                    false,
                    true,
                    true,
                    true,
                    apiId,
                    BuildConfig.TELEGRAM_API_HASH,
                    "en",
                    "Lumora",
                    android.os.Build.VERSION.RELEASE,
                    BuildConfig.VERSION_NAME
                )) { result ->
                    if (result is TdApi.Error) {
                        error = result.message
                        state = AuthState.Error(result.message)
                        notifyChanged()
                    }
                }
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                state = AuthState.WaitingForPhone
                notifyChanged()
            }
            is TdApi.AuthorizationStateWaitCode -> {
                state = AuthState.WaitingForCode
                notifyChanged()
            }
            is TdApi.AuthorizationStateWaitPassword -> {
                state = AuthState.WaitingForPassword
                notifyChanged()
            }
            is TdApi.AuthorizationStateReady -> {
                state = AuthState.Ready
                error = null
                notifyChanged()
                loadChats()
            }
            is TdApi.AuthorizationStateLoggingOut,
            is TdApi.AuthorizationStateClosing -> {
                state = AuthState.Initializing
                notifyChanged()
            }
            is TdApi.AuthorizationStateClosed -> {
                state = AuthState.Closed
                notifyChanged()
            }
        }
    }

    fun loadChats() {
        client?.send(TdApi.GetChats(TdApi.ChatListMain(), 100)) { result ->
            if (result is TdApi.Chats) {
                val ids = result.chatIds
                if (ids.isEmpty()) {
                    chats = emptyList()
                    notifyChanged()
                    return@send
                }
                val loaded = java.util.Collections.synchronizedList(mutableListOf<TdApi.Chat>())
                var remaining = ids.size
                ids.forEach { id ->
                    client?.send(TdApi.GetChat(id)) { chatResult ->
                        if (chatResult is TdApi.Chat) loaded.add(chatResult)
                        remaining--
                        if (remaining == 0) {
                            chats = loaded.sortedByDescending { it.lastMessage?.date ?: 0 }
                            chats.forEach { chat ->
                                chat.photo?.small?.let { client?.send(TdApi.DownloadFile(it.id, 1, 0, 0, false)) { } }
                            }
                            notifyChanged()
                        }
                    }
                }
            } else if (result is TdApi.Error) {
                error = result.message
                notifyChanged()
            }
        }
    }

    fun openChat(chat: TdApi.Chat) {
        selectedChat = chat
        messages = emptyList()
        notifyChanged()
        client?.send(TdApi.GetChatHistory(chat.id, 0, 0, 50, false)) { result ->
            if (result is TdApi.Messages) {
                messages = result.messages?.toList()?.reversed() ?: emptyList()
                notifyChanged()
            }
        }
    }

    fun closeChat() {
        selectedChat = null
        messages = emptyList()
        notifyChanged()
    }

    fun setPhone(phone: String) {
        error = null
        client?.send(TdApi.SetAuthenticationPhoneNumber(phone.trim(), null)) { result ->
            if (result is TdApi.Error) {
                error = result.message
                notifyChanged()
            }
        }
    }

    fun setCode(code: String) {
        error = null
        client?.send(TdApi.CheckAuthenticationCode(code.trim())) { result ->
            if (result is TdApi.Error) {
                error = result.message
                notifyChanged()
            }
        }
    }

    fun setPassword(password: String) {
        error = null
        client?.send(TdApi.CheckAuthenticationPassword(password)) { result ->
            if (result is TdApi.Error) {
                error = result.message
                notifyChanged()
            }
        }
    }

    fun close() {
        client?.send(TdApi.Close()) { }
        client = null
    }

    private fun notifyChanged() {
        listener?.invoke()
    }

    sealed interface AuthState {
        data object Starting : AuthState
        data object Initializing : AuthState
        data object WaitingForPhone : AuthState
        data object WaitingForCode : AuthState
        data object WaitingForPassword : AuthState
        data object Ready : AuthState
        data object Closed : AuthState
        data class Error(val message: String) : AuthState
    }
}