#!/usr/bin/env python3
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
roots = [
    ROOT / 'medical-back' / 'src' / 'main' / 'java' / 'cn' / 'edu' / 'ncu' / 'medical' / 'controller',
    ROOT / 'medical-back' / 'src' / 'main' / 'java' / 'cn' / 'edu' / 'ncu' / 'medical' / 'websocket',
]
checklists = sorted((ROOT / 'scripts' / 'testing').glob('*.md'))

base_pat = re.compile(r'@RequestMapping\("([^"]+)"\)')
map_pat = re.compile(r'@(GetMapping|PostMapping)\("([^"]+)"\)')
url_pat = re.compile(r"http://127\.0\.0\.1:8080/treat([^'\"\s]+)")

routes = set()
for root in roots:
    for f in root.rglob('*.java'):
        t = f.read_text(encoding='utf-8')
        bases = base_pat.findall(t)
        base = bases[0] if bases else ''
        for _, sub in map_pat.findall(t):
            routes.add((base + sub).replace('//', '/'))

lines = [
    '# Checklist Endpoint Validation Report',
    '',
    f'- Total backend routes indexed: **{len(routes)}**',
    ''
]

for chk in checklists:
    if chk.name == 'checklist-endpoint-validation.md':
        continue
    text = chk.read_text(encoding='utf-8')
    urls = url_pat.findall(text)
    lines.append(f'## {chk.name}')
    if not urls:
        lines.append('- No endpoint URLs found.')
        lines.append('')
        continue
    for u in urls:
        path = u.split('?')[0]
        ok = path in routes
        lines.append(f"- {'✅' if ok else '❌'} `{path}`")
    lines.append('')

out = ROOT / 'scripts' / 'testing' / 'checklist-endpoint-validation.md'
out.write_text('\n'.join(lines), encoding='utf-8')
print(out)
