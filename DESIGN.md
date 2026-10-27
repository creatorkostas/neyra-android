---
name: Neyra Android
description: Native local-network companion with calm ink and warm surfaces.
colors:
  light-primary: "#394A75"
  light-on-primary: "#FFFFFF"
  light-primary-container: "#DCE2F9"
  light-on-primary-container: "#162447"
  light-background: "#FAF9F6"
  light-surface: "#FAF9F6"
  light-on-surface: "#1B2335"
  light-secondary: "#565F71"
  light-secondary-container: "#DCE2F0"
  light-on-secondary-container: "#253454"
  dark-primary: "#BAC7EF"
  dark-on-primary: "#223259"
  dark-primary-container: "#394A75"
  dark-on-primary-container: "#DCE2F9"
  dark-background: "#151922"
  dark-surface: "#151922"
  dark-on-surface: "#E9EDF5"
  dark-secondary: "#BDC8DC"
  dark-secondary-container: "#35425E"
  dark-on-secondary-container: "#DCE2F0"
typography:
  headline-large:
    fontFamily: "Android default sans-serif"
    fontSize: "32sp"
    fontWeight: 400
    lineHeight: "40sp"
    letterSpacing: "0sp"
  title-large:
    fontFamily: "Android default sans-serif"
    fontSize: "22sp"
    fontWeight: 400
    lineHeight: "28sp"
    letterSpacing: "0sp"
  title-medium:
    fontFamily: "Android default sans-serif"
    fontSize: "16sp"
    fontWeight: 500
    lineHeight: "24sp"
    letterSpacing: "0.15sp"
  body-large:
    fontFamily: "Android default sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    letterSpacing: "0.5sp"
  body-medium:
    fontFamily: "Android default sans-serif"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
    letterSpacing: "0.25sp"
  body-small:
    fontFamily: "Android default sans-serif"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: "16sp"
    letterSpacing: "0.4sp"
  label-large:
    fontFamily: "Android default sans-serif"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
    letterSpacing: "0.1sp"
rounded:
  field: "4dp"
  chip: "8dp"
  medium: "12dp"
spacing:
  compact: "8dp"
  icon-gap: "12dp"
  group: "16dp"
  panel: "20dp"
  page: "24dp"
components:
  destination-panel:
    backgroundColor: "{colors.light-primary-container}"
    textColor: "{colors.light-on-primary-container}"
    rounded: "{rounded.medium}"
    padding: "20dp"
  speak:
    backgroundColor: "{colors.light-secondary-container}"
    textColor: "{colors.light-on-secondary-container}"
    typography: "{typography.title-medium}"
    height: "64dp"
  send:
    backgroundColor: "{colors.light-primary}"
    textColor: "{colors.light-on-primary}"
    height: "48dp"
---
# Design System: Neyra Android

## Overview

**Creative North Star: "Your voice, your computer"**

Neyra’s calm ink and warm-surface identity becomes a native Material 3 remote for the user’s own computers. System typography, quiet tonal grouping and familiar controls keep capture and connection state readable.

The selected computer is visible before capture. Speech produces editable text rather than silently dispatching an action; local-network and notification limits remain explicit.

**Key Characteristics:**
- Ink accents on warm neutral surfaces.
- Native Material 3 controls and system typography.
- Visible destination and honest connection state.

Source of truth: `app/src/main/kotlin/dev/neyra/companion/ui/CompanionApp.kt`; identity asset: `app/src/main/res/drawable/ic_neyra.xml` (ink square with white N). This is the current native implementation, not a proposed redesign.

## Colors

### Primary
Quiet ink becomes pale periwinkle in dark mode. The `light-*` and `dark-*` primary/on-primary pairs serve filled actions; primary-container pairs group the selected computer. The launcher asset retains the light primary ink and white mark.

### Secondary
Secondary-container pairs color the tonal Speak action and selected controls; secondary roles supply the supporting native palette.

### Neutral
Background and surface share warm paper in light mode and deep ink in dark mode; on-surface supplies readable foregrounds. Frontmatter records every explicit color override in the app, including distinct role names with identical values.

Unspecified roles—including onSurfaceVariant, outline, error, surfaceContainer, inverse roles and disabled-state compositing—remain Material 3 `lightColorScheme` / `darkColorScheme` defaults. They are not aliases of Neyra's custom surface. Dynamic wallpaper color is not enabled. Do not infer a complete custom palette from these overrides; native component defaults still contribute visible lavender navigation tones and neutral borders.

**The Visible Destination Rule.** Keep the selected computer and connection state visible before capture; name the destination in the send action.

## Typography

**Display / Body / Label Font:** Android default sans-serif through unmodified Material 3 `Typography`; no bundled custom font. Frontmatter uses native sp units, not CSS pixels. Defaults are inherited from resolved Material 3 1.3.2 (Compose BOM 2025.04.01).

### Hierarchy
- **Headline large:** destination headings, including deliberate two-line Capture and Inbox headings.
- **Title large:** computer identity and empty-state titles.
- **Title medium:** Speak label, reminder text and list section titles.
- **Body large:** introductory copy; **body medium:** connection details and explanatory text; **body small:** privacy, sync and reminder metadata.
- **Label large:** sync timestamp and standard Material control labels. TopAppBar uses the native title role; fields, chips and navigation inherit their native role mappings.

**The Native Type Rule.** Use Material typography roles and Android font scaling rather than fixed pixel text or a decorative display face.

## Layout

One vertically scrollable content column sits beneath a native TopAppBar. At available width below 600dp, three labeled destinations use NavigationBar; at 600dp and above, they use NavigationRail. The content column is centered and capped at 720dp including its horizontal 24dp padding. This is a width-based switch, not device-name detection.

Groups use 16dp gaps; chips and inner panel/reminder groups use 8dp. The computer panel has 20dp padding; icon gaps use 8dp or 12dp; the rail starts with a 20dp spacer. Pairing fields use 12dp gaps; content ends with a 24dp spacer. FlowRow wraps chips and reminder actions.

Scaffold content padding clears bars; the rail applies safeDrawing vertical insets, and the content column applies imePadding. Preserve native bar inset handling rather than adding duplicate system padding. Scroll to reach the send action on short screens. Speak has a 64dp minimum height; send has a 48dp minimum. Other controls retain Material interactive minimum targets (48dp), including chips whose painted bounds can be smaller. Do not disable minimum interactive sizing.

Evidence inspected: `.impeccable/review/phone-paired.png` shows the light phone Capture screen, labeled synthetic host, wrapped headline and bottom navigation; `tablet.png` shows the left rail and centered bounded column. These are Android emulator captures, not physical-device proof. The direction contract additionally records dark and 1.3 font-scale captures; those images were not visually inspected for this documentation pass.

## Elevation & Depth

Depth comes primarily from tonal grouping and native component states. The computer panel is a plain Surface with no explicit elevation or shadow; no custom shadow, blur, spring, easing or animation token exists. Navigation, dialogs, press/ripple, focus and disabled treatments inherit Material 3 behavior. No custom entrance animation is introduced.

**The Tonal Grouping Rule.** Use native tonal surfaces for grouping; do not add decorative hard shadows to the destination panel.

## Shapes

Material defaults are unmodified: the computer panel uses medium corners; outlined fields use small corners and filter chips use small rounded corners as recorded in frontmatter. Buttons use native fully rounded silhouettes, rather than a fixed custom radius. AlertDialog retains the native extra-large shape. Material outlined vector icons communicate actions; decorative glyph substitutes are not part of this system.

## Components

### Buttons
Filled Button sends; FilledTonalButton speaks; OutlinedButton scans or opens notification settings; TextButton changes computer, pairs and acts on reminders. Preserve native padding, rounded shapes, focus, ripple and disabled opacity. Busy state disables relevant actions; the refresh icon has an accessible action description.

### Chips
FilterChip selects Note, Reminder or Assistant. Selected state uses the native secondary-container treatment; inactive chips retain native outlines. FlowRow accommodates width and font-scale changes.

### Cards / Containers
The selected-computer Surface uses primaryContainer with its native content color, medium shape, 20dp padding and 8dp inner spacing. It is a state-and-destination panel, not a decorative card repeated around every section.

### Inputs / Fields
OutlinedTextField holds the editable draft with four minimum and ten maximum lines, up to 8000 characters. Pairing uses full-width outlined fields with ASCII fingerprint and masked password token. Native focus, label and error colors remain authoritative; errors are visible text rather than color alone. Pairing content scrolls.

### Navigation
Capture, Computers and Inbox always retain text labels with outlined microphone, computer and notification icons. Phone bar and tablet rail share selection state. Parent SaveableStateHolder and destination SaveableStateProvider preserve capture mode, draft and speech target while navigating. This is UI state retention, not an offline outbox guarantee.

### Feedback and recovery
Linear progress indicates busy work; SnackbarHost presents operation messages. Send clears the matching draft only after success. A changed speech target blocks sending until review/edit or restoration. Inbox exposes Notification settings recovery; pairing requires verified fingerprint and token. Native AlertDialog owns pairing dismissal and confirmation states.

## Do's and Don'ts

### Do:
- Do preserve system font scaling, scrolling, IME clearance and native touch targets.
- Do retain draft text, mode and speech target across destination changes through SaveableStateHolder.
- Do keep the speech-provider privacy disclosure and notification-delay limits readable.

### Don't:
- Don't treat discovery as pairing or an offline computer as connected.
- Don't send a spoken draft to a changed computer until it is reviewed/edited or the original destination is restored.
- Don't replace native controls with web styling or introduce custom entrance animation.

Documentation limits: default Material role colors and component metrics are inherited rather than exhaustively extracted; sidecar web snippets are illustrative translations, not Android implementation specifications. Review verdict resolved draft retention and notification recovery; permanent-denial notification routing has code inspection only, and physical-device/production validation remains open. No defect was redefined as a design token or repaired in this pass.
