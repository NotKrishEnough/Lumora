# Android TDLib integration

Lumora does not yet contain TDLib. This document records the reproducible next step; it is not a claim that Telegram support is already working.

## Why a Maven dependency alone is not enough

TDLib's Android Java API is backed by JNI native libraries. The official Android example builds TDLib and its Java interface together; it requires the Android SDK/NDK, CMake, a C++ toolchain, and OpenSSL. The generated Java classes and matching native libraries must come from the same TDLib build.

Official references:
- https://github.com/tdlib/td/tree/master/example/android
- https://core.telegram.org/tdlib/docs/

## Required integration deliverables

- [ ] Pin a TDLib commit/version and build its Android JNI interface.
- [ ] Include matching generated Java sources and native libraries for supported ABIs (at minimum arm64-v8a; add others only when built).
- [ ] Load/init TDLib once in an application-scoped component, not from a composable.
- [ ] Route TDLib updates to a repository using a lifecycle-safe coroutine/Flow bridge.
- [ ] Implement authorization states: parameters, encryption key, phone number, code, optional password, ready, closed/error.
- [ ] Persist the TDLib database in app-private storage; never log authorization codes, passwords, API hash, or database contents.
- [ ] Implement chat list, history, send message, and updates before exposing live chat UI.
- [ ] Add device tests for fresh login, relaunch/session restore, logout, offline errors, and message send/receive.

## Credentials

Create an app at https://my.telegram.org. Keep api_id and api_hash out of source control. For local builds use an untracked local configuration file; for CI use repository Actions secrets. Do not paste login codes or account passwords into issues, commits, or chat.

## Build note

The official Android instructions build native dependencies from source. A successful Kotlin/Compose build by itself does not verify TDLib availability or Telegram login. Do not show a working sign-in button until the native library and authorization flow are present and tested.
