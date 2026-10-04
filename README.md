# Lumora

Lumora is an Android Telegram client project built with Kotlin and Jetpack Compose, with a dark translucent glass interface.

## Current status

**Prototype / UI foundation.** This repository currently contains a Compose UI shell and Android build workflow. It is not yet connected to Telegram: login, chat sync, messaging, calls, media transfer, push notifications, and secret chats are not implemented. Sample rows are placeholders, not real messages.

## Build

Open in Android Studio and run the `:app` configuration, or use **Actions → Android build** to produce a debug APK artifact.

- Minimum Android: 8.0 (API 26)
- Compile/target SDK: 35
- Kotlin: 2.0.21
- Jetpack Compose

## Telegram integration plan

Lumora is intended to use [TDLib](https://github.com/tdlib/td), rather than implementing MTProto itself. TDLib needs a compatible Android native build/package and a carefully managed authorization lifecycle. TDLib is **not bundled yet**; a phone-number form alone would not make login functional.

To finish live support:
1. Add a maintained Android TDLib distribution with required native ABIs, or build TDLib for Android.
2. Create an app at [my.telegram.org](https://my.telegram.org). Supply API ID/hash through local untracked configuration or GitHub Actions secrets. Never commit them publicly.
3. Implement authorization states (phone, code, optional 2-step verification), encrypted session storage, chat/message handlers, and lifecycle-safe updates.
4. Test sign-in and messaging on a physical device before calling the app functional.

## Security

Do not publish phone numbers, login codes, passwords, API hashes, or session databases. This prototype has no Telegram session storage or network client.

## License

No license has been selected yet.