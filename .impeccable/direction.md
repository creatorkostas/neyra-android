# Companion direction contract

THESIS: An Android remote for the user's own Neyra computers. Selecting the
computer is the first meaningful act; speaking captures reviewed text for it.

OWN-WORLD: Expand the existing Neyra ink / warm-surface visual identity through
native Material 3 color roles and system type. This is an Operate surface;
Android navigation, accessibility and honest state take precedence over visual
novelty. User confirmed Kotlin/Compose, local-network first, direct UI building.

STORY: First pair in Computers, then select the destination, then capture a note,
reminder or supported command. Inbox brings synced reminders to the phone.
No demonstration computers are bundled. Screenshots use a labeled synthetic host.

FIRST VIEWPORT: Capture heading, explicit selected/unpaired computer panel,
mode chips, primary Speak button, recognition privacy copy and editable text.
The send action is below the draft; errors preserve the draft, success clears it.
No invented statistics, oversized marketing artwork, or animation gates.

FORM: Three destinations. Phone bottom navigation changes to a tablet rail.
Reading/form width is capped at 720dp and centered on wide screens. Material
headline/title/body/label roles, 24dp horizontal space, 16dp group spacing,
48dp touch targets. Light and dark themes, system font scale, IME/system insets.

SIGNATURE INTERACTION: Select computer → speak → review → send to that computer.
A target change after voice input blocks sending until the text is reviewed/edited
or the original computer is selected. Native state/press transitions honor system
motion settings; no custom entrance animation.

Workflow: code-led by explicit user choice. No approved image comp. No web
detector ran because this is native Compose. Evidence is Android 15 emulator
screenshots, including tablet-sized configuration, dark theme and 1.3 font scale.

## Confirmed 0.2 extension

Keep the existing world. Four destinations now include Notes for cached search
and the durable outbox. Conversation answers precede the follow-up composer;
phone offline TTS reads completed answers. Recording remains local until explicit
Transcribe or Queue, with a visible original destination and persisted draft.
Computers adds single-use QR scanning; Inbox adds recurrence and Stop repeating.
Sharing opens an editable draft. Capture draft ownership is hoisted so asynchronous
results survive navigation, and each destination keeps its own scroll state.
Native captures in review/expanded are authoritative for this extension. They
use synthetic service responses; hardware speech/camera/TTS/widget checks remain
explicitly unverified. Desktop Home also exposes the companion action at 1000px.
