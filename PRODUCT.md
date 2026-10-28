# Neyra Android

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Confirmed: native Kotlin and Jetpack Compose. Keep the app easy to maintain and
extend through the existing core, data and app module boundaries.

## Users

Neyra users moving between their computers and phone, including when they are
away from their desk but remain on the same trusted local network.

## Product Purpose

Discover computers running Neyra, choose the machine to address, capture thoughts
and speak to its assistant, and receive reminders on both phone and desktop.
Success means moving between devices without losing a thought or confusing which
computer receives it.

## Positioning

A companion to the user's existing Neyra computers: phone recordings can use the
selected computer's speech engine, while notes and reminders stay connected to
that machine. Local-network operation is the confirmed initial scope.

## Operating Context

- The phone and computer share a trusted local network; the desktop must run its
  opt-in companion bridge. Discovery does not authorize pairing.
- Pair explicitly with a single-use, expiring desktop QR invitation or manual
  fingerprint/token verification, then choose the destination computer.
- Record or type, review the text, and send. Shared Android text/links also open
  an editable review draft rather than sending automatically.
- Save notes or recordings to an outbox when disconnected; queued items retain
  their original destination and retry after reconnecting.
- Use phone conversations with follow-ups, reply playback and Stop, cached note
  search, reminder actions, and the home-screen capture widget.

## Capabilities and Constraints

- LAN discovery, QR/manual pairing and explicit computer selection.
- Phone recordings up to 60 seconds, transcribed by the desktop's selected engine.
- Durable offline note/audio outbox, Android text/link sharing and capture widget.
- Cached note search; synced content is bounded and long notes may be excerpts.
- Reminder synchronization, Done/Snooze actions, daily/weekly recurrence and Stop
  repeating. Recurrence uses fixed elapsed intervals rather than calendar/DST rules.
- Cached reminders can notify offline. New reminders, changes and desktop actions
  require connectivity. Android background restrictions can delay synchronization
  and alerts; exact or simultaneous delivery is not promised.
- No cloud relay, away-from-network delivery or arbitrary PC notification mirroring
  in this version.

## Privacy Boundaries

Privacy is an explicit user priority, not a claim that every feature is offline.

- Keep capture and processing local where supported. Recordings remain in private
  phone storage until the user chooses transcription or queueing; completed or
  discarded audio is removed.
- Authenticate paired computers with pinned encrypted connections and protect
  stored credentials with Android Keystore-backed encryption.
- Conversation text goes to the assistant provider configured on the computer.
  Disclose that boundary; do not describe provider conversations as fully local.
- Phone conversations do not implicitly read the desktop screen, clipboard, files
  or notes and cannot dispatch desktop tools. Commands are a separate workflow.
- Phone reply playback uses an installed offline TTS voice, with text available
  when a suitable voice is absent.
- Notes and reminder text are private app data, not separately encrypted storage.
  Notification visibility follows Android settings. Do not claim end-to-end
  encryption or comprehensive at-rest encryption.

## Brand Commitments

Keep the Neyra name, public repository and MIT license matching the desktop app.
The user explicitly wants a stunning appearance: exceptional visual quality is a
product requirement alongside speed and privacy. This records the ambition;
palettes, typography and other visual-system decisions belong in DESIGN.md and
surface briefs.

## Product Principles

1. **Make it stunning.** Treat visual quality and interaction polish as part of the
   product, while keeping frequent tasks clear and native to Android.
2. **Make it fast.** Prioritize responsive capture, navigation, search and feedback.
   Keep network, disk and audio work off the UI thread and bound background work.
   These are performance goals, not measured latency claims.
3. **Make it private.** Minimize exposure of personal content, preserve local
   processing where available and explain every provider/network boundary honestly.
4. **Keep ownership explicit.** Show the destination, preserve editable drafts,
   and never silently retarget queued work or send imported content.
5. **Make it maintainable.** Keep feature/platform boundaries clear so new
   capabilities can be added without concentrating logic in screens or activities.

## Evidence on Hand

- README.md, docs/architecture.md and docs/protocol.md describe implemented flows
  and boundaries; docs/verification.md records the tested scope.
- .impeccable/review/expanded/ contains native emulator and desktop captures using
  synthetic data, plus Android instrumentation results.
- Version 0.2 checks include build/lint, core tests, two app instrumentation
  scenarios and three data scenarios; desktop regression results are documented
  separately. These establish tested behavior, not real-world performance metrics.
- Physical camera/microphone quality, installed TTS voices, real-launcher widgets,
  battery/Doze and reboot behavior remain device-validation work.

## Open Decisions

No numeric startup, interaction-latency or battery-use budgets have been agreed.
Remote delivery beyond the local network is outside the current scope.

## Confirmed opening workflow

The user explicitly requires a clean opening screen: a dropdown at the top to
select a computer, then a Speak button below. All secondary capture controls and
information belong on other screens. Preserve feature access through navigation;
recording review and shared text belong in Compose.

The latest confirmed refinement places a smaller Neyra wordmark beside the PC
selector in a centered header. Keep the Speak-first workflow while giving controls
custom styling and responsive feedback inspired by Apple Design principles.

## Current appearance direction

The user requested a from-scratch Apple Design reset. Quiet clarity supersedes the
forest palette: neutral surfaces, system typography, blue actions and custom-shaped
controls throughout. Preserve the centered small wordmark/PC selector and single
Speak action on home, all features, explicit destinations, speed and privacy.
