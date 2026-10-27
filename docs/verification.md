# Verification

Development environment: JDK 17, SDK 35, Android 15 x86_64 emulator.

- `:app:assembleDebug`: passed; installable development APK generated.
- `:app:lintDebug`: passed, no errors. Dependency-update and backup-rule advice
  remain warnings. Versions are intentionally pinned for reproducibility.
- `:core:test`: passed (2 JVM tests: scoped delivery identity and certificate pins).
- `:data:connectedDebugAndroidTest`: passed (1 integration scenario exercising
  real TLS transport against a synthetic server, wrong-pin rejection, capture,
  reminder completion, encrypted peer persistence, and cache removal).
- `:app:connectedDebugAndroidTest`: passed (spoken draft/mode retention across
  navigation, changed-target send guard, and edit-to-review recovery).
- NSD: the app discovered the synthetic workstation in the emulator.
- Native layouts inspected at phone and tablet sizes, with dark theme and 1.3x
  font scale. Screenshots in `.impeccable/review` contain synthetic data only.
- Desktop: 347 regression tests run: 346 passed, 1 skipped; the seven companion tests
  include real loopback TLS and controller boundary tests. Ruff, Voz/Piper
  environment checks, website JS syntax, and AUR generator checks passed.

Still unverified: physical phone speech recognition, real device/network firewall
variations, long-running battery/Doze behavior, reboot timing, audio playback on
a remote computer, Windows/macOS companion startup, and exact notification timing
(which is not promised). No production-readiness claim is made.
