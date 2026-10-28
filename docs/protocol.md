# Companion protocol v1

DNS-SD type: `_neyra._tcp.local.`. TXT: `id`, `name`, `version=1`. Advertised
addresses, names, IDs and ports are untrusted. No credentials are advertised.

Transport: HTTPS with a persistent self-signed desktop certificate. The phone
verifies the exact SHA-256 certificate fingerprint copied from desktop settings,
including certificate validity. Only after verification does it send
`Authorization: Bearer <per-phone 256-bit token>`. Redirects and cleartext are
forbidden. Credentials are encrypted with Android Keystore. Pairing is explicit, using a scanned invitation or copied fingerprint/token.
Short-code enrollment is not included.

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
2.6 MB; phone response reads at 2 MB. Mutation request IDs use a bounded 512-entry in-memory cache scoped to the token;
NOTE/REMINDER captures additionally use the persistent receipts described below.
Do not claim exactly-once delivery across desktop restarts or cache eviction.
The outbox and notification workers retry bounded stable operations as described
below; ordinary Commands are not automatically retried. Read-only refresh is safe.

Remote assistant commands are restricted to the existing local parser's app,
media, reminder, and briefing actions. Screen, clipboard, selected text, note
search, arbitrary files, and AI-routed commands are not remotely dispatched.
Responses do not trigger shell execution. Assistant acceptance is not a report
that an external app successfully opened or played audio.

Snapshots are authoritative per computer. Notification identity combines machine,
reminder and deadline. Completing from a phone is explicit; receiving a phone
notification does not acknowledge desktop delivery. Full snapshots replace local
state; there is no event cursor/replay protocol yet.


## Extended contracts (0.2)

- QR JSON: `{type:"neyra-pair-v1", id, name, host, port, fingerprint, invite}`.
  Scan only from the intended computer. `POST /v1/enroll` authenticates with the
  single-use five-minute invite through pinned TLS, consumes it, and returns
  `{machineId, token}`. Invitations cannot read data or create jobs.
- Snapshot adds `notes:[{id,text,time,title}]` and optional reminder
  `repeat_seconds` (86400/604800). Notes are bounded recent excerpts, not all data.
- `POST /v1/transcribe`: `{requestId, pcm}`; PCM is base64 little-endian mono
  16 kHz signed 16-bit samples, 0.1–60 seconds. Returns `{jobId}`.
- `POST /v1/chat`: `{requestId,text}`; returns `{jobId}`. The selected desktop
  provider answers with only the phone's bounded conversation context.
- `POST /v1/job`: `{jobId,action:"status"|"cancel"}` returns
  `{status:"running"|"done"|"failed"|"cancelled",text,error}`. Phone-scoped,
  memory-only jobs are bounded to 32 retained entries with ten-minute cleanup.
- `POST /v1/forget`: clears that phone's conversation and cancels its jobs.
- Capture REMINDER accepts `repeatSeconds:0|86400|604800`.
- Reminder actions additionally accept `stop_repeat` and keep deadline checks.

Capture NOTE/REMINDER receipts are persisted in a bounded SQLite journal (latest
2,000). Notes additionally store a hashed companion request key before syncing
QSettings, closing the gap before receipt creation. Audio job retries use fresh
job request IDs; saving the transcribed note retains the outbox entry's stable ID.
There is no unbounded exactly-once guarantee after receipt/history eviction.
Chat/audio job data, model input and conversational answers are never journaled.

The earlier local-command restriction still applies to Commands mode. Conversation
mode is a separate provider-answer path and never executes tools or attaches
screen/clipboard/notes. Audio is always sent only to a paired computer chosen by
the user. Transcript review or explicit Queue as note authorizes note creation.
