"""Synthetic host for emulator integration tests. Run with Neyra's uv environment.

uv run python ../neyra-android/scripts/companion_fixture.py --state /tmp/neyra-fixture.json

Use only on a development machine. No actual notes, reminders, or commands run.
The state file contains temporary test credentials; delete it after testing.
"""
import argparse
import base64
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
reminders = [{'id': 'synthetic-reminder', 'text': 'Review launch notes', 'due_at': time.time() + 600, 'status': 'pending', 'repeat_seconds': 86400}]
jobs = {}
notes = [{'id': 'synthetic-note', 'text': 'Synthetic searchable note', 'time': '2026-10-28T10:00:00', 'title': 'Synthetic note'}]


def dispatch(path, body):
    if path in ('/v1/transcribe', '/v1/chat'):
        identifier = 'audio' if path == '/v1/transcribe' else 'chat'
        if identifier == 'audio':
            assert len(base64.b64decode(body['pcm'])) >= 3200
        jobs[identifier] = {'status': 'done', 'text': 'Synthetic transcript' if identifier == 'audio' else 'Synthetic phone answer', 'error': ''}
        return {'jobId': identifier}
    if path == '/v1/job':
        if body.get('action') == 'cancel':
            jobs[body['jobId']]['status'] = 'cancelled'
        return jobs[body['jobId']]
    if path == '/v1/forget':
        return {'message': 'Cleared'}
    if path == '/v1/capture':
        if body['mode'] == 'NOTE':
            notes.append({'id': body['requestId'], 'text': body['text'], 'time': time.strftime('%Y-%m-%dT%H:%M:%S'), 'title': ''})
        if body['mode'] == 'REMINDER':
            reminders[:] = [{'id': 'synthetic-reminder', 'text': 'Synthetic reminder', 'due_at': time.time() + 600, 'status': 'pending', 'repeat_seconds': body.get('repeatSeconds', 0)}]
        return {'message': 'Note saved'}
    if path == '/v1/reminder':
        if body['action'] == 'stop_repeat':
            reminders[0].pop('repeat_seconds', None)
        else:
            reminders[0]['status'] = 'done' if body['action'] == 'done' else 'pending'
    return {'machineId': 'fixture', 'name': 'Synthetic workstation', 'reminders': reminders, 'notes': notes}


with tempfile.TemporaryDirectory() as directory:
    server = CompanionServer(directory, dispatch)
    server.machine_id = 'fixture'
    server.name = 'Synthetic workstation'
    token = server.new_phone_token()
    server.start()
    fd = os.open(args.state, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, 'w') as output:
        json.dump({'port': server.port, 'pin': server.fingerprint, 'token': token, 'invite': server.new_invite()}, output)
    stop = threading.Event()
    signal.signal(signal.SIGTERM, lambda *_: stop.set())
    signal.signal(signal.SIGINT, lambda *_: stop.set())
    try:
        stop.wait()
    finally:
        server.close()
        args.state.unlink(missing_ok=True)
