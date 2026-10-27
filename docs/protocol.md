# Companion protocol v1

DNS-SD type: `_neyra._tcp.local.`. TXT: `id`, `name`, `version=1`. Advertised
addresses, names, IDs and ports are untrusted. No credentials are advertised.

Transport: HTTPS with a persistent self-signed desktop certificate. The phone
verifies the exact SHA-256 certificate fingerprint copied from desktop settings,
including certificate validity. Only after verification does it send
`Authorization: Bearer <per-phone 256-bit token>`. Redirects and cleartext are
forbidden. Credentials are encrypted with Android Keystore. Pairing is explicit
copy-based; short codes and QR enrollment are future work.

Endpoints:

- `GET /v1/snapshot`: `{machineId, name, reminders: [{id, text, due_at, status}]}`.
  Deadlines are epoch seconds. States are `pending`, `fired`, `done`.
- `POST /v1/capture`: `{requestId, mode, text}`. Modes: `NOTE`, `REMINDER`,
  `ASSISTANT`; 1–8,000 characters. Returns `{message}`. Note capture respects
  desktop history settings and never inserts text into the focused application.
- `POST /v1/reminder`: `{requestId, id, due_ms, action}`. Action `done` or `snooze`
  (10 minutes). `due_ms` must match the existing deadline truncated to epoch
  milliseconds. Returns the full snapshot. A stale deadline is rejected.

JSON errors use `{error}` and HTTP 400/401/404/409/500/503. Bodies are capped at
32 KiB; phone response reads at 2 MB. Mutation request IDs are deduplicated in a
bounded 512-entry in-memory cache scoped to the token for the server session.
Do not claim exactly-once delivery across desktop restarts or cache eviction.
The phone makes no automatic mutation retry; after uncertain delivery, check the
desktop before resending. Read-only refresh can retry safely.

Remote assistant commands are restricted to the existing local parser's app,
media, reminder, and briefing actions. Screen, clipboard, selected text, note
search, arbitrary files, and AI-routed commands are not remotely dispatched.
Responses do not trigger shell execution. Assistant acceptance is not a report
that an external app successfully opened or played audio.

Snapshots are authoritative per computer. Notification identity combines machine,
reminder and deadline. Completing from a phone is explicit; receiving a phone
notification does not acknowledge desktop delivery. Full snapshots replace local
state; there is no event cursor/replay protocol yet.
