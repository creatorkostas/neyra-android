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
Requests bind to a peer before suspension. Speech produces reviewed text through
Android's speech service; changing computers after speech requires reviewing/editing
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
