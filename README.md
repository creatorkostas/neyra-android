# Neyra for Android

Android companion for [Neyra](https://github.com/creatorkostas/neyra).

**Status: repository setup and architecture draft. No runnable app or desktop
companion service has been implemented yet.**

The intended app discovers Neyra computers on the same network, lets you choose
a target computer, captures spoken notes and requests, and brings your reminders
to your phone alongside the desktop.

## Planned first workflows

- Discover nearby Neyra computers and explicitly pair with each computer.
- Select the computer to address before speaking or typing.
- Save a note without triggering desktop clipboard insertion.
- Create reminders and sync their pending, delivered, and completed states.
- Schedule synced reminders on the phone for delivery while disconnected.
- Show connection health and when each computer last synced.

Useful follow-on features include Android share-sheet capture, a quick-note
widget, searchable offline notes, and notification actions for snooze and done.
These are planned features, not current capabilities.

## Development

Read [the architecture plan](docs/architecture.md) and
[the protocol draft](docs/protocol.md) before implementation. Native Android stack
selection and away-from-home notification requirements are awaiting confirmation.
The Android application will live here; desktop integration belongs in Neyra's
repository. A local-network connection alone cannot deliver new notifications
when the phone is away from that network.

## License

MIT, with the same license text and copyright holder as Neyra. See [LICENSE](LICENSE).
