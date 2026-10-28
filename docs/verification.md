# Verification — 0.2 companion extension

Environment: JDK 17, SDK 35, Android 15 x86_64 emulator; Qt offscreen on Linux.

- Android debug build, lint and the 2 core JVM tests pass.
- Android capture/navigation regression passes. Sharing/recreation also passes, including destination continuity after an import.
- Three data instrumentation scenarios pass against a synthetic TLS service:
  certificate pinning/rejection, encrypted credentials, capture/actions/cache;
  single-use QR enrollment, audio/chat job exchange, SQLite outbox persistence,
  stable retry IDs and searchable note cache; broadcast-to-WorkManager reminder action.
- Desktop full regression suite: 357 tests run, 356 passed, 1 skipped.
- Companion regressions: 17 passed as part of the final full run, including
  durable-note metadata, shared-engine late-result routing, QR expiry/single use, receipts across restart,
  recurrence/stop-series, phone/account scope, cancellation and mocked STT chunks.
- Ruff, Voz/Piper environment checks, website JS syntax and AUR generator pass.
- Desktop smoke: companion is visible on Home at 1000px and in Voice settings;
  enabling renders the pairing QR. Native models were mocked for this UI smoke.

Tests use synthetic notes/audio/provider responses. They do not sign into an
assistant account, consume provider usage, or prove real speech recognition quality.
Physical camera scanning, microphone/audio quality, offline TTS voice installation,
widget interaction on a real launcher, platform firewall differences, battery/Doze
and reboot timing remain manual device checks. Exact notification timing is not
promised; recurring cadence is fixed elapsed time rather than calendar/DST based.

Latest native screenshots are under `.impeccable/review/expanded/`; they use a
synthetic desktop, notes and provider responses. Earlier screenshots document 0.1.

Impeccable finish review: `ship`. All eleven expanded captures were inspected,
including corrected Capture/share views and a scrolled capture-controls view.
No material UI fixes remain from that review.

## Forest connection appearance redesign

The replacement visual system uses bundled Manrope, forest/citron role colors,
a state-bearing phone/computer path, and native adaptive navigation. Debug build,
lint, core tests and both app instrumentation regressions pass after the change.
The share test's expected Notes heading was updated for the new copy. No network,
audio, persistence or provider protocol behavior was changed. No latency or battery
benchmark was performed. Current emulator captures are in
`.impeccable/review/forest/` (phone, scrolled controls, destinations, dark theme at
1.3 font scale and tablet).

Impeccable full review found one tablet status-bar contrast issue. A forest
inset/rail backdrop corrected it; the exact tablet recapture was scored resolved
with disposition `ship` for that fix. The final inset change also passes build/lint.

## Minimal Capture home

The first screen contains the computer dropdown and Speak action. Compose holds
the full editor/modes/review. Build, lint, core tests and both app instrumentation
scenarios pass. The capture regression checks the uncluttered home, dropdown
selection, Speak-to-editor flow, retained text and changed-target guard. Share
recreation preserves the draft in Compose. Native captures are recorded under
`.impeccable/review/clean-home/`, including dark mode at 1.3 scale and tablet.

Clean-home Impeccable review: `ship`, all five current native captures valid,
no material fixes. Hardware, TalkBack and performance remain unmeasured.

## Compact header / Apple Design refinement

Build, lint, core tests and both UI instrumentation scenarios pass. Header selector,
circular Speak and navigation dock retain Android button/tab semantics. Current
phone/menu, dark 1.3-scale and tablet captures: `.impeccable/review/compact-header/`.
Press animation uses Compose spring state and system animation scaling; physical
frame pacing, TalkBack and hardware microphone behavior were not measured.

## Quiet clarity / Apple Design reset

Rebuilt theme, typography, home control, navigation treatment, computer rows,
mode choices, editors, note/reminder groups and empty states. Shared controls live
in `ui/Controls.kt`; networking, storage and recording contracts are unchanged.

`./gradlew :app:assembleDebug :app:lintDebug :core:test :app:connectedDebugAndroidTest`
passes. Both UI tests pass: spoken draft/mode/target guard across navigation and
shared draft edits across activity recreation. The Notes heading assertion now
matches “Your notes”. No new dependencies were added.

Native emulator captures: `.impeccable/review/quiet-clarity/`, covering home, anchored
menu, Compose, computers, Inbox, Notes, dark home at 1.3 font scale and tablet home.
These are unpaired empty states, not proof of physical-device operation. Speak is
intentionally disabled until a computer is selected. Status icons follow the system
theme and remain legible on both canvases. Hardware performance, TalkBack and battery
behavior remain unmeasured; no independent visual-review verdict is claimed.

Capture caveat: the SwiftShader emulator intermittently retained the prior screen's
pixels after navigation while its accessibility tree exposed the new screen. The
Compose image was recaptured after a configuration redraw. Navigation semantics
pass automated tests; physical-device rendering still needs confirmation.
