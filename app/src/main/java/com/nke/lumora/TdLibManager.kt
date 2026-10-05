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
    var state: AuthState = AuthState.Starting
        private set
    var error: String? = null
        private set
    private var listener: (() -> Unit)? = null

    fun observe(listener: () -> Unit) {
        this.listener = listener
        listener()
    }

    fun start() {
        if (client != null) return
        val apiId = BuildConfig.TELEGRAM_API_ID.toIntOrNull() ?: 0
        val apiHash = BuildConfig.TELEGRAM_API_HASH
        if (apiId <= 0 || apiHash.isBlank()) {
            state = AuthState.Error("Telegram API credentials are not configured in this build.")
            notifyChanged()
            return
        }

        client = Client.create({ update ->
            when (update) {
                is TdApi.UpdateAuthorizationState -> handleAuthState(update.authorizationState)
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
            is TdApi.AuthorizationStateWaitEncryptionKey -> {
                state = AuthState.Initializing
                client?.send(TdApi.CheckDatabaseEncryptionKey(ByteArray(0))) { result ->
                    if (result is TdApi.Error) {
                        error = result.message
                        state = AuthState.Error(result.message)
                    }
                    notifyChanged()
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