# Neyra Android — Quiet clarity

A ground-up visual reset requested by the user, guided by Apple Design principles
and implemented in native Jetpack Compose. This supersedes Forest connection.

## Foundation

Light mode uses a cool porcelain canvas (#F5F6F8), white content surfaces,
graphite text (#181B22), secondary gray (#5C6370), and blue actions (#245EDB).
Dark mode uses #101216 canvas, #1C1F26 groups, #F2F3F7 text, #ADB3C0 secondary
text, and #91B5FF actions. Connection and recording state always use words;
color alone never communicates readiness.

Android's system sans-serif replaces the bundled display face. Headlines are
32/38sp bold with -0.8sp tracking; section titles 22/28sp semibold; row titles
17/23sp semibold; body 16/24sp and 14/21sp; metadata 12/18sp. All type scales
with Android settings. No font downloads or rendering dependencies are added.

## Layout and components

Capture opens with a centered compact header: small neyra wordmark, then the
selected-computer menu. A single rounded-square microphone control follows.
Modes, editors, disclosures, and review belong to Compose, never the home screen.

A white/charcoal floating phone dock carries five labeled destinations. Blue
labels and a short marker identify selection. Tablet navigation becomes a rail
at 600dp; content is constrained to 720dp. System status icons adapt to the theme.

Content groups have 24dp corners, 20dp padding and 12dp internal gaps. Editors
are filled surfaces with a blue focus outline. Action buttons use 18dp corners,
52dp minimum height, and explicit primary/secondary/disabled treatments. Mode
choices are shaped selectable surfaces with at least 48dp touch height. Icon
tiles provide consistent grouping for devices, reminders and empty states.
Computer identity and connection status share one compact group in Compose.
Notes and reminders use individual content groups rather than unstructured text.

## Interaction

Press feedback starts immediately through Compose interaction sources. Custom
actions scale to .98; the microphone scales to .96. Critically damped springs
(stiffness 900 and 700 respectively) remain interruptible and respect Compose's
system animation duration scale. Actions commit on release using native clickable
semantics, preserving drag-away cancellation. No perpetual animation, backdrop
blur, artificial delay, remote asset, or additional network request is introduced.
Menus remain anchored to their selector, with native dismissal and focus behavior.

## Product invariants

- Computer selection is explicit; queued work retains its original target.
- Drafts, modes and speech targets survive navigation and recreation.
- Sharing opens an editable review; it never sends automatically.
- Recording, provider use, outbox behavior and notification limits remain visible.
- Destructive actions retain confirmation; permission recovery stays available.
- Native Android keyboard, scrolling, accessibility semantics and text scaling remain.

## Evidence

See docs/verification.md for checks and current captures. Earlier Forest/clean-home
review verdicts apply to their historical versions, not this reset. Emulator
verification does not establish physical-device performance, battery use or TalkBack
quality. No independent design verdict is claimed for this version.
