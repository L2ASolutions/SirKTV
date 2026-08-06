# SirKTV

A premium Android TV / Fire TV Stick IPTV player. Client-only: no IPTV
services, streams, or channels are bundled — you supply your own Xtream
Codes credentials.

## Status: Phase 1 — Login

This phase implements login only:

- Server URL / username / password form, D-pad optimized, dark theme
- Validates credentials against the Xtream Codes `player_api.php` endpoint
- Handles invalid credentials, expired/disabled/banned subscriptions,
  malformed server URLs, and network failures with distinct, user-facing
  messages
- Saves credentials locally, encrypted at rest, and silently reconnects on
  next launch
- Lands on a placeholder "connected" screen after a successful login

Live TV browsing, EPG, favorites, and playback are not implemented yet —
that's the next phase. The full Clean Architecture module layout
(`domain` / `data` / `network` / `storage` / `di` / `presentation`) is in
place so those phases slot in without restructuring.

## Architecture

- **Kotlin, MVVM, Clean Architecture** — `domain` (models, use cases,
  repository interfaces) is Android-agnostic; `data` implements the
  repositories; `network` and `storage` are the two data sources;
  `presentation` is Compose UI + ViewModels; `di` wires it together with Hilt.
- **Retrofit + OkHttp** for the Xtream Codes API, with the server URL
  supplied per-request (`@Url`) since it's user-entered rather than fixed.
- **kotlinx.serialization** for JSON, configured leniently
  (`ignoreUnknownKeys`, `coerceInputValues`) since Xtream panel
  implementations vary widely in what fields they return.
- **Hilt** for DI, **Coroutines + Flow** for async and state.
- **Jetpack Compose for TV** (`androidx.tv.material3`) for D-pad-native
  components (buttons, switches, focus/click animations), layered with
  standard Compose Material3 for form inputs, which don't yet have a
  TV-native equivalent.
- **Encrypted credential storage**: `EncryptedSharedPreferences`
  (AndroidX Security, AES-256-GCM via Keystore) on API 23+. On API 21-22
  — older Fire OS devices where that library's Keystore-backed AES isn't
  available — falls back to AES-256-GCM with the key wrapped by an
  RSA keypair held in AndroidKeyStore (supported since API 18). See
  `storage/EncryptedCredentialStore.kt` and `storage/LegacyCredentialCipher.kt`.

## Building

Requires JDK 17 and the Android SDK (`compileSdk 35`). Point
`ANDROID_HOME`/`local.properties` at your SDK, then:

```
./gradlew assembleDebug
```

Dependency versions in `gradle/libs.versions.toml` were current at time of
writing; run a Gradle sync and bump anything Android Studio flags as
outdated before your first build — this scaffold was written without a
live Android SDK/emulator available to verify the build.

## Package layout

```
com.sirktv.app
├── domain/          # models, use cases, repository interfaces, in-memory session
├── network/         # Retrofit service, DTOs, Xtream URL building
├── storage/          # encrypted credential persistence
├── data/            # repository implementations, DTO -> domain mapping
├── di/              # Hilt modules
└── presentation/    # Compose screens, ViewModels, theme, navigation
```
