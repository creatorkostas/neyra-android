"""Run synthetic companion tests without echoing temporary test credentials."""
import argparse
import json
import os
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('state', type=Path, help='Temporary state JSON from companion_fixture.py')
args = parser.parse_args()
state = json.loads(args.state.read_text())
root = Path(__file__).resolve().parents[1]
wrapper = root / ('gradlew.bat' if os.name == 'nt' else 'gradlew')
command = [str(wrapper), '-p', str(root), ':data:connectedDebugAndroidTest']
for key in ('port', 'pin', 'token'):
    command.append(f'-Pandroid.testInstrumentationRunnerArguments.{key}={state[key]}')
raise SystemExit(subprocess.call(command))
