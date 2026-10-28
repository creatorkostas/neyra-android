# Neyra for Android

Native Kotlin / Jetpack Compose companion for [Neyra](https://github.com/creatorkostas/neyra).
Public, MIT licensed, local network first. Early development; native device validation
is still needed before daily use.

## What works in 0.2

- LAN discovery, manual pairing, and camera scanning of desktop pairing QR codes.
  QR invitations work once and expire after five minutes.
- Choose a computer, record up to 60 seconds, and transcribe through that
  computer's selected Whistle/Voz engine. Review the text before sending it.
- Conversation mode: ask and follow up using the computer's configured assistant
  provider; see partial answers, hear completed replies on your phone, and stop
  or clear the conversation. Conversations cannot dispatch desktop tools or
  implicitly read screen, clipboard, files or notes. Commands remain a separate mode.
- Durable offline note outbox, including recordings. Queued recordings transcribe
  and save automatically as notes on their original computer after reconnecting.
- Android share-sheet capture for text/links, plus a home-screen quick-note and
  voice-recording widget. Sharing opens a review draft; it never sends silently.
- Reminder sync, notification Done/Snooze actions, daily/weekly recurrence, and
  Stop repeating in Inbox. Daily/weekly use fixed 24-hour/7-day intervals anchored
  to the original deadline. Delivery or early completion creates the next occurrence;
  missed intervals are skipped. Stopping a pending series keeps that occurrence once.
- Offline search of cached notes from all paired computers. Sync brings up to 200
  recent dictation/meeting notes per computer within a bounded response budget;
  long notes are excerpts of up to 8,000 characters.
- Material 3 light/dark themes and phone/tablet navigation.

Recording does not use Android's speech recognition service. Audio stays in private
phone storage until you transcribe or queue it. Completed or discarded audio is
removed. Conversation text goes to the provider configured on your computer;
phone replies use an installed offline Android TTS voice. If no matching offline
voice is installed, the answer remains available as text.

## Connect your computer

Desktop support is currently in [Neyra PR #6](https://github.com/creatorkostas/neyra/pull/6)
on branch `feat/android-companion`; use that version until the bridge is merged.

1. Install the companion bridge version of Neyra and its optional dependencies:
   `uv sync --locked --extra companion`.
2. Start Neyra. Open **Android companion** in the tray, or under
   **Home** or **Voice settings**, and choose **Enable companion and add a phone**.
3. Keep the phone and computer on the same trusted Wi-Fi/network. Allow Neyra
   through the computer's firewall on private networks if prompted.
4. In the Android app, open **PCs → Scan pairing QR** and scan the code
   displayed on your computer. If it expires, generate another on the desktop.
   Manual fingerprint/token pairing remains available; verify those on the computer.
5. Select the computer in Capture’s dropdown and tap **Speak to Neyra**. Stopping
   opens **Compose** for review. For typing or other modes, open Compose. Choose **Transcribe on computer**, review, and send. **Queue as note** saves a
   recording for automatic transcription and note creation later.
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

## Outbox and conversation limits

Outbox storage is capped at 100 notes/recordings. Requests keep their original
machine and stable note ID; reconnecting or choosing another computer does not
retarget them. Foreground retry and periodic WorkManager sync drain small batches.
Notification actions wait for network availability and reject changed deadlines.
The desktop remembers the latest 2,000 note/reminder receipts across restarts;
note metadata also deduplicates retries while the note remains in desktop history.
Removing a paired computer deletes its local cache and queued notes. In-flight
network requests may already have reached the computer.

Chat and transcription jobs expire and are kept only in desktop memory. Conversation
context is phone/account scoped, bounded to four completed pairs and ten minutes.
No chat transcript is written to the phone database or desktop settings.
Speech and camera quality, installed TTS voices, widgets and long-running Doze
behavior still need physical-device validation. See [verification](docs/verification.md).

## Future work

Away-from-home relay/push delivery, general desktop notification mirroring,
full offline copies of long notes, and calendar-based recurrence across DST.

## License

[MIT](LICENSE), matching Neyra, copyright Konstantinos Mokas.
