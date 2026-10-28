# Architecture

Kotlin, Jetpack Compose, minSdk 26, target/compile SDK 35, JDK 17.

## Modules and ownership

- `core`: platform-free immutable models, discovery/transport/store interfaces,
  capture modes, reminder delivery identity. JVM tests run without Android.
- `data`: Android NSD adapter, certificate-pinned HTTPS transport, Android Keystore
  credential storage, reminder cache/alarms/notifications, boot receiver and
  WorkManager synchronization. No Compose dependency.
- `app`: dependency composition in NeyraApplication; lifecycle and permission
  integration in MainActivity; state and orchestration in CompanionViewModel;
  Material 3 screens in `ui/`. UI never creates a transport or stores credentials.

Add a feature by defining its models/contracts in core, implementing platform or
network behavior in data, and presenting state/actions in app. Split UI files by
feature as they grow; avoid adding all logic to MainActivity. Dependencies flow
app → core/data, data → core. There is no DI framework or code-generation layer.

## Runtime

NSD runs only while the activity is started. Candidates are untrusted until paired.
Resolve operations are serialized for Android compatibility. Reachability and paired
identity are separate. Discovered addresses update only after pinned authentication.
The certificate pin, not the mDNS name or IP, authenticates a desktop.

Coroutines run transport and persistence off the main thread. The ViewModel owns
foreground refresh and user operations; lifecycle cancellation stops polling.
Requests bind to a peer before suspension. Speech records bounded PCM16 locally and the desktop engine produces reviewed text; changing computers after speech requires reviewing/editing
or returning to that computer. Speech does not silently dispatch desktop actions.

WorkManager handles periodic background sync. ReminderCache has one process-wide
instance so UI, workers, and receivers serialize updates. Snapshot replacement
cancels stale alarms/notifications, then schedules pending reminders. Delivery keys
combine machine ID, reminder ID, and deadline. Phone delivery never marks desktop
reminders fired or done. Boot restores cached schedules. There are no exact alarms,
foreground services, always-on sockets, or cloud infrastructure in this version.

Credentials use AES-GCM with a non-exportable Android Keystore key. Backups are
disabled. Reminder task text is private app data and is visible in notifications
according to Android's notification settings; it is not separately encrypted.

## Desktop boundary

The bridge lives in Neyra's repository: companion.py owns TLS, authentication,
DNS-SD, bounded HTTP bodies and in-session request deduplication. companion_ui.py
marshals requests into a bounded queue serviced by a Qt timer. Only the Qt thread
mutates history, reminders, or assistant controllers. The service is disabled by
default; enabling and shutdown happen outside the GUI thread.

See protocol.md for wire contracts and verification.md for tested scope.


## Capture, outbox and conversations

PhoneRecorder owns foreground-only 16 kHz mono PCM16 capture, capped at 60 seconds.
The activity requests microphone permission and stops capture on backgrounding.
A pending recording's path and original target survive activity/process recreation.
CompanionDatabase owns SQLite note snapshots and a durable note/audio outbox.
OutboxSync serializes sends, binds every entry to its original peer, persists
transcription before note submission, and reuses the stable note request ID.
Batches contain at most ten entries and one transcription. Failed peers are
skipped for the rest of that batch. Pair removal is checked before submission.
WorkManager also runs bounded discovery to recover changed desktop addresses.

TLS transport uses cancellable OkHttp calls with connect/read/write/overall
limits. Audio is base64 PCM in a bounded JSON request. Long transcription and
provider requests return job IDs; status polling exposes partial text, completion,
failure, and cancellation. Cancelling a job never dispatches its late result.
PhoneSpeech owns offline Android TTS playback; Stop clears pending playback.
MainActivity owns QR scanner, share intents, widget intents and permissions;
NotesScreen presents cached notes and queued entries.

The desktop's companion_jobs.py owns remote transcription and conversation jobs.
It reuses the desktop-owned Transcriber while guarding recording state; native
inference may finish after cancellation but its result is discarded. Provider
clients and settings are selected on Qt's thread. Workers stream plain text only;
results and conversation-memory commits return through Qt signals. Phone chat
never enters tool routing. Per-phone ConversationMemory retains existing account,
TTL and size limits. Authenticated request scope is injected by the transport,
never accepted from client JSON.

Reminder notification actions enqueue deadline-checked WorkManager jobs, not
network work inside broadcast receivers. The desktop owns recurring schedules
and creates one next occurrence on delivery/early completion; series IDs prevent
duplicate successors and support Stop repeating. Cached phone alerts do not
acknowledge delivery or independently create recurring desktop records.

Shared STT results route explicitly to the phone job until the worker finishes,
including after cancellation/revocation. They never enter desktop paste/history
completion handlers. The desktop alone closes the engine, avoiding duplicate
loaded models and preserving native runtime ownership.

Capture form state is hoisted above destination composition; a saveable holder
preserves per-destination scroll/search. Shared-input navigation is consumed once
so configuration changes preserve the user's current destination. Finished desktop
transcripts are persisted locally before recording deletion, and pending audio
metadata is saved at recording start to survive process shutdown.
