# Architecture plan

This is a separate Android companion for Neyra. Stack selection is pending.

## Ownership

- App shell: dependency composition, navigation, permissions, lifecycle.
- Core model: machine identity, connection state, notes, reminder states.
- Discovery: Android NSD adapter; emits discovered candidates, never trusted peers.
- Pairing: desktop approval, identity verification, credential storage/revocation.
- Transport: authenticated versioned protocol, request IDs, bounded retries.
- Devices feature: discovery, pairing, machine selection, connection health.
- Capture feature: explicit voice recording and typed fallback, target selection.
- Inbox feature: reminder/event sync, read state, snooze and completion.
- Platform notifications: notification permission, channels, local scheduling.

Features depend on repository interfaces and immutable models. Platform adapters
implement those interfaces. Compose dependencies at the app boundary; UI code
must not construct network clients or access credentials directly.

## Runtime boundaries

The selected machine is explicit for every command. Discovery is lifecycle-bound;
network loss removes reachability but preserves paired identity. Requests retain
their original target while in flight. Background sync must respect Android's
execution limits; local reminder scheduling must survive app/process restart.
Never advertise reliable instant background delivery without a tested transport.

The desktop bridge belongs in Neyra's repository and must reuse existing domain
logic through the Qt controller thread. The Android repository does not fork
Neyra's reminder parser, assistant execution, or speech inference runtime.

See protocol.md for the draft connection contract and security boundaries.
