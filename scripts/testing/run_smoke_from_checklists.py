#!/usr/bin/env python3
import re
import subprocess
from pathlib import Path
from datetime import datetime

ROOT = Path(__file__).resolve().parents[2]
checklists = [
    'c1-c2-api-checklist.md',
    'c3-c4-api-checklist.md',
    'c5-c6-api-checklist.md',
    'c7-c8-api-checklist.md',
    'c9-c10-d-e-checklist.md',
]

replacements = {
    '${PATIENT_TOKEN}': 'DUMMY_PATIENT_TOKEN',
    '${DOCTOR_TOKEN}': 'DUMMY_DOCTOR_TOKEN',
    '${ADMIN_TOKEN}': 'DUMMY_ADMIN_TOKEN',
    '${APPOINT_TOKEN}': 'DUMMY_APPOINT_TOKEN',
    '{roomId}': '189',
}


def extract_curl_commands(md_text: str):
    cmds = []
    in_block = False
    block_lines = []
    for line in md_text.splitlines():
        if line.strip().startswith('```'):
            if in_block:
                full = []
                cur = ''
                for ln in block_lines:
                    s = ln.strip()
                    if not s:
                        continue
                    if cur:
                        cur += ' ' + s
                    else:
                        cur = s
                    if cur.endswith('\\'):
                        cur = cur[:-1].strip()
                        continue
                    full.append(cur)
                    cur = ''
                if cur:
                    full.append(cur)
                cmds.extend([c for c in full if c.startswith('curl ')])
                block_lines = []
            in_block = not in_block
            continue
        if in_block:
            block_lines.append(line)
    return cmds


rows = []
for name in checklists:
    p = ROOT / 'scripts' / 'testing' / name
    text = p.read_text(encoding='utf-8')
    cmds = extract_curl_commands(text)
    for cmd in cmds:
        c = cmd
        for k, v in replacements.items():
            c = c.replace(k, v)
        shell_cmd = f"{c} -o /tmp/smoke_body.txt -w 'HTTP_STATUS:%{{http_code}}' -m 5"
        try:
            proc = subprocess.run(shell_cmd, shell=True, capture_output=True, text=True, timeout=12)
            out = (proc.stdout or '').strip()
            m = re.search(r'HTTP_STATUS:(\d+)$', out)
            code = m.group(1) if m else '000'
            status = 'PASS' if code.startswith('2') else 'WARN'
            err = (proc.stderr or '').strip()
        except Exception as e:
            code = '000'
            status = 'WARN'
            err = str(e)
        rows.append((name, c, code, status, err[:120]))

report = [
    '# Smoke Replay Report',
    '',
    f'- Generated at: {datetime.utcnow().isoformat()}Z',
    f'- Total requests: **{len(rows)}**',
    '',
    '| Checklist | Command | HTTP | Result | Notes |',
    '|---|---|---:|---|---|',
]
for r in rows:
    cmd_short = r[1].replace('|', '\\|')
    if len(cmd_short) > 90:
        cmd_short = cmd_short[:87] + '...'
    notes = r[4].replace('|', '\\|') if r[4] else ''
    report.append(f"| {r[0]} | `{cmd_short}` | {r[2]} | {r[3]} | {notes} |")

out = ROOT / 'scripts' / 'testing' / 'smoke-replay-report.md'
out.write_text('\n'.join(report), encoding='utf-8')
print(out)
