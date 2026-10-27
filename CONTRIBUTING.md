# Contributing

Read README.md and docs/architecture.md first. Use JDK 17 and the checked-in
Gradle wrapper. Keep feature/domain models platform-free and network/credential
handling out of composables.

Run `./gradlew :app:assembleDebug :app:lintDebug :core:test`. With an emulator,
run `./gradlew :app:connectedDebugAndroidTest` for capture/navigation regressions. Integration tests
live under data/src/androidTest and require the synthetic desktop fixture:
run scripts/companion_fixture.py with Neyra's Python environment, then pass its
state file to `python scripts/run_emulator_tests.py /tmp/neyra-fixture.json`.
The helper supplies Android instrumentation arguments `port`, `pin`, `token`.
The emulator reaches the host at 10.0.2.2. These temporary test credentials must
never be committed. Test discovery, speech, Doze, reboot and notification delivery
on real hardware before claiming production readiness.

Use .github/pull_request_template.md; name the human creator and implementation
assistance. Never commit local.properties, keys, tokens, recordings or notes.
