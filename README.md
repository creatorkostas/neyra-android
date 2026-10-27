# Neyra for Android

Native Kotlin / Jetpack Compose companion for [Neyra](https://github.com/creatorkostas/neyra).
Public, MIT licensed, local network first. Early development; native device validation
is still needed before daily use.

## What works in this version

- Discover enabled Neyra computers on the same LAN using Android NSD.
- Explicitly pair using a desktop certificate fingerprint and per-phone token.
- Select a computer and save a typed or spoken note to its Neyra history.
- Create relative-time reminders using Neyra's existing desktop parser.
- Send supported local assistant commands: apps, media, timers, daily briefing.
- Sync reminders from multiple paired computers; complete or snooze from Inbox.
- Cache reminders, schedule local Android alerts, and restore schedules after boot.
- Material 3 light/dark themes, phone navigation bar and tablet navigation rail.

Speech uses your installed Android speech service. Offline recognition is requested
but not guaranteed; your speech provider may use the internet. Review text before
sending it. Audio is not uploaded to the desktop in this version. Assistant replies
play on the computer; conversational answers are not streamed to the phone.

## Connect your computer

Desktop support is currently in [Neyra PR #6](https://github.com/creatorkostas/neyra/pull/6)
on branch `feat/android-companion`; use that version until the bridge is merged.

1. Install the companion bridge version of Neyra and its optional dependencies:
   `uv sync --locked --extra companion`.
2. Start Neyra. Open **Android companion** in the tray, or under
   **Voice settings → Data & backup**, and choose **Enable companion and add a phone**.
3. Keep the phone and computer on the same trusted Wi-Fi/network. Allow Neyra
   through the computer's firewall on private networks if prompted.
4. In the Android app, open **Computers**, select **Pair**, and copy the full
   fingerprint and phone token from that computer's companion window. Verify
   the fingerprint directly on the computer, never from discovery metadata.
5. Select the computer, return to Capture, speak or type, then send.
6. In Inbox, enable Android notifications. Keep Neyra running for synchronization.

Discovery never pairs automatically. Each generated token is independent; desktop
**Disconnect all phones and stop companion** revokes them all. Forgetting a computer
on the phone clears local credentials and reminders but does not revoke its token
on the desktop. The desktop service is opt-in for each Neyra session.

## Notification limits

Foreground refresh runs about every five seconds; background WorkManager sync is
periodic (at least 15 minutes, potentially delayed by Android). Cached reminders
use inexact local alarms and may be delayed by battery restrictions. No promise
of exact delivery is made. Reconnecting refreshes cancellations and changed times;
a disconnected phone can still show a reminder cancelled on the computer after
its last sync. Opening Inbox shows the selected computer's last successful sync.
New reminders and changes require a local-network connection. There is no relay,
remote push service, or arbitrary Windows/Linux notification mirroring.

## Build

Install Android Studio, JDK 17, Android SDK 35, and Build Tools 35.0.0. Set
`ANDROID_HOME` or an untracked `local.properties` with `sdk.dir`.

```sh
./gradlew :app:assembleDebug :app:lintDebug :core:test
```

The installable development APK is `app/build/outputs/apk/debug/app-debug.apk`.
Open this directory in Android Studio to run on a phone. Debug builds are signed
with your development key; release signing keys are deliberately excluded.
CI uploads a debug APK for each successful build.

Read [architecture](docs/architecture.md), [protocol](docs/protocol.md), and
[contribution guidance](CONTRIBUTING.md) before extending a feature.

## Next features

QR-based pairing, desktop-engine audio transcription, Android share-sheet capture,
a quick-note widget, offline note search, and remote delivery are future work.

## License

[MIT](LICENSE), matching Neyra, copyright Konstantinos Mokas.
