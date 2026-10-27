# Companion protocol draft

Status: design draft; no desktop endpoint currently exists.

Discovery advertises `_neyra._tcp.local.` with a stable, randomly generated
machine ID, protocol version, display name, and TLS certificate fingerprint.
Discovery metadata is untrusted. Never automatically pair or execute an action
based on a discovered name or address.

Pair each phone separately with desktop approval and a short-lived code. Verify
the desktop certificate fingerprint using a QR code or a matching code on both
devices. Store per-device credentials in Android Keystore-backed storage; allow
revocation on the desktop. Do not expose the companion service on the Internet.

Versioned API responsibilities:

- Machine identity and supported capabilities.
- Notes: save text with a client-generated request ID for retry deduplication.
- Voice: bounded audio upload to the chosen desktop's transcription worker;
  distinct note, reminder, and assistant modes. Never paste remote notes into
  whichever desktop application happens to have focus.
- Reminder snapshots and revisioned events: stable reminder ID, deadline,
  task, pending/fired/done state, and originating machine ID.
- Reminder actions: complete and snooze with expected revision/deadline.
- Event replay: durable cursor and full resync when a cursor expires.

The desktop owns its records. A phone notification does not mark a reminder done
or acknowledge desktop notification delivery. Notification identity combines
machine ID, reminder ID, and deadline, so reconnecting does not show duplicates
and snoozing can notify again. Sync pending reminders to local Android scheduling
so the phone can notify while disconnected. Surface last-sync time and staleness.

Keep networking off Qt's GUI thread and marshal all mutations to that thread.
No direct QSettings reads/writes from HTTP workers. Stop discovery, request
workers, and streams on shutdown. Do not allow remote shell commands, implicit
screen capture, arbitrary file access, or unreviewed pairing.

Away-from-home delivery remains an open product decision. A relay/push service
or user-managed private network is needed; LAN discovery alone cannot provide it.
