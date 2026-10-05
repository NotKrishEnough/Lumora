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
    var senderNames: Map<String, String> = emptyMap()
        private set
    var mediaPaths: Map<Long, String> = emptyMap()
        private set
    var stickers: List<TdApi.Sticker> = emptyList()
        private set
    var animations: List<TdApi.Animation> = emptyList()
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
                is TdApi.UpdateNewMessage -> {
                    val m = update.message
                    if (selectedChat?.id == m.chatId && messages.none { it.id == m.id }) {
                        messages = messages + m
                        resolveSender(m)
                        prepareMedia(m)
                        notifyChanged()
                    }
                }
                is TdApi.UpdateFile -> {
                    val file = update.file
                    if (file.local.isDownloadingCompleted && file.local.path.isNotBlank()) {
                        val match = chats.firstOrNull { it.photo?.small?.id == file.id }
                        if (match != null) avatarPaths = avatarPaths + (match.id to file.local.path)
                        val mediaMessage = messages.firstOrNull { messageFileId(it) == file.id }
                        if (mediaMessage != null) mediaPaths = mediaPaths + (mediaMessage.id to file.local.path)
                        notifyChanged()
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
                messages.forEach { resolveSender(it); prepareMedia(it) }
                notifyChanged()
            }
        }
    }

    fun sendMessage(text: String, replyToMessageId: Long = 0L) {
        val chat = selectedChat ?: return
        if (text.isBlank()) return
        val formatted = TdApi.FormattedText(text.trim(), null)
        val content = TdApi.InputMessageText(formatted, null, false)
        val replyTo: TdApi.InputMessageReplyTo? = if (replyToMessageId != 0L) TdApi.InputMessageReplyToMessage(replyToMessageId, null, 0, "") else null
        client?.send(TdApi.SendMessage(chat.id, null, replyTo, null, null, content)) { result ->
            if (result is TdApi.Message) {
                messages = messages + result
                notifyChanged()
            } else if (result is TdApi.Error) {
                error = result.message
                notifyChanged()
            }
        }
    }

    fun forwardMessage(message: TdApi.Message, targetChatId: Long) {
        client?.send(TdApi.ForwardMessages(targetChatId, null, message.chatId, longArrayOf(message.id), null, false, false)) { result ->
            if (result is TdApi.Error) { error = result.message; notifyChanged() }
        }
    }

    fun loadStickerAndAnimationPicks() {
        val chatId = selectedChat?.id ?: 0L
        client?.send(TdApi.GetStickers(null, "", 32, chatId)) { result ->
            if (result is TdApi.Stickers) {
                stickers = result.stickers?.toList() ?: emptyList()
                stickers.forEach { client?.send(TdApi.DownloadFile(it.sticker.id, 1, 0, 0, false)) { } }
                notifyChanged()
            }
        }
        client?.send(TdApi.GetSavedAnimations()) { result ->
            if (result is TdApi.Animations) {
                animations = result.animations?.toList() ?: emptyList()
                animations.forEach { client?.send(TdApi.DownloadFile(it.animation.id, 1, 0, 0, false)) { } }
                notifyChanged()
            }
        }
    }

    fun sendSticker(sticker: TdApi.Sticker) {
        val chat = selectedChat ?: return
        val input = TdApi.InputSticker(TdApi.InputFileId(sticker.sticker.id), null, sticker.width, sticker.height)
        val content = TdApi.InputMessageSticker(input, sticker.emoji ?: "")
        client?.send(TdApi.SendMessage(chat.id, null, null, null, null, content)) { result ->
            if (result is TdApi.Message) { messages = messages + result; notifyChanged() }
        }
    }

    fun sendAnimation(animation: TdApi.Animation) {
        val chat = selectedChat ?: return
        val input = TdApi.InputAnimation(TdApi.InputFileId(animation.animation.id), null, intArrayOf(), animation.duration, animation.width, animation.height)
        val content = TdApi.InputMessageAnimation(input, null, false, false)
        client?.send(TdApi.SendMessage(chat.id, null, null, null, null, content)) { result ->
            if (result is TdApi.Message) { messages = messages + result; notifyChanged() }
        }
    }

    private fun resolveSender(message: TdApi.Message) {
        when (val sender = message.senderId) {
            is TdApi.MessageSenderUser -> client?.send(TdApi.GetUser(sender.userId)) { result ->
                if (result is TdApi.User) {
                    val name = listOf(result.firstName, result.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Telegram user" }
                    senderNames = senderNames + ("u:" + sender.userId to name)
                    notifyChanged()
                }
            }
            is TdApi.MessageSenderChat -> client?.send(TdApi.GetChat(sender.chatId)) { result ->
                if (result is TdApi.Chat) { senderNames = senderNames + ("c:" + sender.chatId to result.title); notifyChanged() }
            }
        }
    }

    private fun messageFileId(message: TdApi.Message): Int? = when (val c = message.content) {
        is TdApi.MessageSticker -> c.sticker?.sticker?.id
        is TdApi.MessageAnimation -> c.animation?.animation?.id
        else -> null
    }

    private fun prepareMedia(message: TdApi.Message) {
        messageFileId(message)?.let { client?.send(TdApi.DownloadFile(it, 1, 0, 0, false)) { } }
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