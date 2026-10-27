"""Synthetic host for emulator integration tests. Run with Neyra's uv environment.

uv run python ../neyra-android/scripts/companion_fixture.py --state /tmp/neyra-fixture.json

Use only on a development machine. No actual notes, reminders, or commands run.
The state file contains temporary test credentials; delete it after testing.
"""
import argparse
import json
import os
from pathlib import Path
import signal
import tempfile
import threading
import time
from neyra.companion import CompanionServer

parser = argparse.ArgumentParser()
parser.add_argument('--state', required=True, type=Path)
args = parser.parse_args()
reminders = []


def dispatch(path, body):
    if path == '/v1/capture':
        if body['mode'] == 'REMINDER':
            reminders[:] = [{'id': 'synthetic-reminder', 'text': 'Synthetic reminder', 'due_at': time.time() + 600, 'status': 'pending'}]
        return {'message': 'Note saved'}
    if path == '/v1/reminder':
        reminders[0]['status'] = 'done' if body['action'] == 'done' else 'pending'
    return {'machineId': 'fixture', 'name': 'Synthetic workstation', 'reminders': reminders}


with tempfile.TemporaryDirectory() as directory:
    server = CompanionServer(directory, dispatch)
    server.machine_id = 'fixture'
    server.name = 'Synthetic workstation'
    token = server.new_phone_token()
    server.start()
    fd = os.open(args.state, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, 'w') as output:
        json.dump({'port': server.port, 'pin': server.fingerprint, 'token': token}, output)
    stop = threading.Event()
    signal.signal(signal.SIGTERM, lambda *_: stop.set())
    signal.signal(signal.SIGINT, lambda *_: stop.set())
    try:
        stop.wait()
    finally:
        server.close()
        args.state.unlink(missing_ok=True)
