---
name: Neyra Android
description: Forest connection — a native companion for your chosen computer.
colors:
  day-primary: "#225542"
  day-on-primary: "#F5F8ED"
  day-primary-container: "#153D31"
  day-on-primary-container: "#EAF2DA"
  day-secondary: "#526348"
  day-secondary-container: "#DCEC98"
  day-on-secondary-container: "#243313"
  day-tertiary: "#845137"
  day-background: "#F4F5EC"
  day-surface: "#F4F5EC"
  day-on-surface: "#192E25"
  day-surface-variant: "#E4E8DB"
  day-on-surface-variant: "#465649"
  day-outline: "#737F6D"
  day-outline-variant: "#C5CEBB"
  day-surface-container: "#E9EDDF"
  day-surface-container-low: "#EEF1E5"
  day-surface-container-high: "#E1E7D6"
  day-surface-container-highest: "#DCE3D1"
  day-on-secondary: "#FFFFFF"
  night-primary: "#DCEC98"
  night-on-primary: "#243313"
  night-primary-container: "#193F32"
  night-on-primary-container: "#EAF2DA"
  night-secondary: "#BCCBA7"
  night-on-secondary: "#26361D"
  night-secondary-container: "#3B4F2C"
  night-on-secondary-container: "#E0ECAF"
  night-background: "#101F19"
  night-surface: "#101F19"
  night-on-surface: "#EEF2E3"
  night-surface-variant: "#2E4034"
  night-on-surface-variant: "#C1CCB8"
  night-outline: "#8A9B83"
  night-outline-variant: "#435442"
  night-surface-container: "#1C3025"
  night-surface-container-low: "#16271E"
  night-surface-container-high: "#263B2D"
  night-surface-container-highest: "#304535"
typography:
  headline-large:
    fontFamily: "Manrope"
    fontSize: "36sp"
    fontWeight: 450
    lineHeight: "40sp"
    letterSpacing: "-1sp"
  title-large:
    fontFamily: "Manrope"
    fontSize: "24sp"
    fontWeight: 650
    lineHeight: "30sp"
  title-medium:
    fontFamily: "Manrope"
    fontSize: "16sp"
    fontWeight: 650
    lineHeight: "24sp"
  body-large:
    fontFamily: "Manrope"
    fontSize: "16sp"
    fontWeight: 450
    lineHeight: "24sp"
  body-medium:
    fontFamily: "Manrope"
    fontSize: "14sp"
    fontWeight: 450
    lineHeight: "20sp"
  body-small:
    fontFamily: "Manrope"
    fontSize: "12sp"
    fontWeight: 450
    lineHeight: "16sp"
  label-large:
    fontFamily: "Manrope"
    fontSize: "14sp"
    fontWeight: 650
    lineHeight: "20sp"
rounded:
  small: "8dp"
  medium: "16dp"
  large: "20dp"
spacing:
  compact: "8dp"
  related: "12dp"
  section: "16dp"
  panel: "20dp"
  gutter: "24dp"
components:
  connection-panel:
    backgroundColor: "{colors.day-primary-container}"
    textColor: "{colors.day-on-primary-container}"
    rounded: "{rounded.medium}"
    padding: "20dp"
  send:
    backgroundColor: "{colors.day-primary}"
    textColor: "{colors.day-on-primary}"
  speak:
    backgroundColor: "{colors.day-primary}"
    textColor: "{colors.day-on-primary}"
---

# Design System: Neyra Android

## Overview

**Creative North Star: "Forest connection"**

A private thought travels from this phone to one chosen computer. Deep forest chrome frames lichen working surfaces; citron gives selection a distinct voice. The Nordic cultural identity direction is expressed through palette, bundled type and one geometric connection signature.

This is an Android task interface: native Material 3 controls, explicit destinations and editable drafts carry the work. Speed and privacy guide the design; neither physical-device performance nor hardware behavior is established by this documentation.

**Key Characteristics:**
- Forest chrome, lichen surfaces and citron emphasis.
- Bundled Manrope with native Android controls.
- Visible destination, a two-control home and text-backed connection geometry in Compose.

## Colors

### Primary
Forest primary drives light-mode actions; dark mode uses citron primary. Forest primary-container and pale on-container text anchor the app bar, phone navigation and connection panel in both modes.

### Secondary
Citron secondary-container emphasizes selected modes; dark mode uses the muted green secondary-container. Keep assignments semantic rather than forcing citron onto every selected element. The light tertiary is earthy brown; unoverridden Material roles remain inherited.

### Neutral
Lichen background/surface, container steps, on-surface text and green-gray outlines form the working area. Night mode moves to near-black forest with pale text. Frontmatter records explicit source overrides only; it is not a complete replacement for Material defaults.

**The Visible Destination Rule.** Show the selected computer before capture and name it in the send action; never silently retarget queued work.

## Typography

**Display Font:** Bundled Manrope.
**Body Font:** Bundled Manrope; roles not overridden retain Material defaults.

The variable font is registered as Normal with an explicit axis weight of 450 and SemiBold with an explicit axis weight of 650. Use the source role mapping, not synthetic 400/600 font files. Headline large supplies the Compose and other screen headings; Capture has no content headline; title large names computers and major groups. Body and label roles preserve native hierarchy. Sizes and line heights are in scalable Android sp, not CSS pixels; base-derived roles retain Material tracking.

**The Native Interaction Rule.** Keep Material controls, Android font scaling, scrolling, IME clearance and accessible action labels.

## Layout

Center a scrolling working column with a maximum width of 720dp, horizontal gutters of 24dp, vertical padding of 20dp and 16dp section gaps. Related controls wrap with 8dp gaps. Preserve scaffold insets, IME padding and bring-into-view handling for imported/reviewed drafts.

Capture opens with only a full-width computer dropdown at the top and a full-width Speak button below, within the shared app shell. No content headline, connection panel, modes, disclosures, editor, refresh action or busy-operation controls appear on this home. Compose contains the secondary capture and review workflow.

Below 600dp, use the five labeled native bottom destinations in source order: Capture, Compose, PCs, Inbox and Notes. At 600dp and above, use the native rail with safe vertical insets. The root Row backdrop, tablet rail and phone bar use primaryContainer forest chrome. The root backdrop continues behind the rail’s safe vertical insets. Both navigation variants use onPrimaryContainer labels and unselected icons, primary selection indicators and onPrimary selected icons. Do not force a desktop grid onto tablet content.

## Elevation & Depth

The custom identity is flat: tonal surfaces, outlines and spacing provide grouping. The connection panel adds no custom shadow or animation. Native Material dialogs, ripples, focus and component elevation remain platform behavior; this is not a global zero-elevation override.

## Shapes

Theme shapes are softly squared: small, medium and large tokens above. The connection panel uses medium corners. Buttons, chips, outlined fields, lists and dialogs retain native component geometry; the theme does not turn every control into a rectangular card.

## Components

### Buttons
Native filled Send and Speak retain enabled, disabled, pressed and focus behavior. Speak has a minimum height of 76dp and a 28dp microphone/stop icon; Send has a 48dp minimum. Speak becomes Stop recording while recording and requires a selected computer; busy state disables it. Progress and Stop request appear outside Capture. Secondary actions use native outlined or text buttons.

### Chips
Native FilterChips in Compose choose capture intent and reminder recurrence. Keep wrapping, selected semantics and busy-state disabling.

### Cards / Containers
The forest connection panel in Compose has 20dp padding and 12dp internal spacing. Other destinations use native lists, dividers and open text groups rather than a universal card wrapper.

### Inputs / Fields
The Capture computer selector is a native read-only outlined exposed dropdown labeled Computer. It lists paired computers and a Manage computers action leading to Computers; selection is disabled during recording or busy operations. Without a selection it reads Select a computer.

Native outlined Compose draft and Notes search fields retain labels, focus and disabled treatment. Drafts allow 3–10 visible lines and keep review warnings visible. Search is single-line. Pairing uses explicit fingerprint verification and a password-masked token field.

### Navigation
Keep outlined Material icons with visible destination labels. The forest app bar uses the lowercase Neyra wordmark and a sync icon with the accessible label Sync computers outside Capture; Capture omits sync. MainActivity uses edge-to-edge layout with a transparent status-bar scrim and light status icons. The primaryContainer root backdrop supplies forest behind the tablet rail’s status inset, while the app bar supplies forest behind the content-side status inset. Navigation-bar system styling remains the Android edge-to-edge default; no custom navigation-bar icon policy is specified.

### Home and Compose workflow
Capture starts recording against the selected computer. Stopping recording, receiving transcription/review text or importing shared text opens Compose. Compose retains the connection panel, capture modes, recording controls, transcription/queue/discard actions, editable review draft, target-change warning, send action, reminder recurrence and conversation playback/Stop. Draft state is hoisted above destination composition; per-destination state survives navigation. Computers owns discovery and pairing, Inbox owns reminders, and Notes owns cached search and the outbox.

### Connection path
In Compose, phone and computer icons bracket a short geometric path. Online uses a solid stroke; offline uses separated segments with explicit state copy. No selected computer produces pairing guidance.

**The Honest Connection Rule.** Use a solid path online and a broken path offline, always with explicit connection text. Geometry does not imply pairing or live audio.

## Do's and Don'ts

### Do:
- Do keep Capture to the computer dropdown followed by Speak; keep secondary capture and review controls in Compose.
- Do preserve draft text, mode and original speech target across navigation and destination changes.
- Do keep recording/provider disclosures, queue consent and notification-delay limits readable.
- Do retain search, outbox retry/deletion confirmation, reminder actions, pairing and conversation playback/Stop.

### Don't:
- Don't send imported text automatically or send a speech draft to a changed computer without editing/review or restoring its original target.
- Don't treat discovery as pairing or disconnected computers as connected.
- Don't add fetched decorative media, blur, perpetual animation or claims of fully local provider conversations.

Documentation evidence: SpeakHome.kt and CompanionApp.kt establish the current home, five destinations and workflow; NeyraTheme.kt establishes the preserved palette and type. Existing NotesScreen.kt and MainActivity.kt documentation remains unchanged in scope. Clean-home captures in .impeccable/review/clean-home are being generated; no clean-home visual verdict is available. Prior forest review results do not establish approval of this composition. Physical camera/audio, offline TTS, launcher widgets, battery/Doze and reboot behavior require device validation; no latency or hardware measurements are claimed. No defect or unverified review result is canonized as a system rule. Sidecar web samples illustrate native roles; they do not replace Compose specifications.
