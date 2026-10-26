#!/usr/bin/env python3
"""يدرج مواقيت الآيات (content/timing-src/<سورة>.json) في وحدة السورة كـ timing_ajmi.json ويحدّث بصمات الكتالوج.
   python3 tools/add_timing.py            # كل الملفات الموجودة في timing-src
تُشغَّل بعد build_surah/build_content (فهما يعيدان كتابة مدخل الكتالوج)، وبعد add_translations."""
import hashlib, json, os, sys
H = os.path.dirname(os.path.abspath(__file__))
SRC, BOOT = os.path.join(H, '..', 'content', 'timing-src'), os.path.join(H, '..', 'content', 'bootstrap')
cat_path = os.path.join(BOOT, 'catalog.json')
cat = json.load(open(cat_path, encoding='utf-8'))
n = 0
for fn in sorted(os.listdir(SRC)) if os.path.isdir(SRC) else []:
    if not fn.endswith('.json'):
        continue
    S = int(fn[:-5])
    e = next((m for m in cat['modules'] if m['surah'] == S), None)
    if e is None:
        print('تخطّي', S, '(لا وحدة منشورة)'); continue
    t = json.load(open(os.path.join(SRC, fn), encoding='utf-8'))
    N = json.load(open(os.path.join(BOOT, e['dir'], 'module.json'), encoding='utf-8'))['ayahCount']
    assert len(t['endsMs']) == N and t['endsMs'] == sorted(t['endsMs']) and t['endsMs'][-1] <= t['durationMs'] + 50, f'{S}: مواقيت غير سليمة'
    data = json.dumps(t, ensure_ascii=False, indent=1).encode('utf-8')
    rel = f"{e['dir']}/timing_ajmi.json"
    open(os.path.join(BOOT, rel), 'wb').write(data)
    e['files'] = [f for f in e['files'] if f['path'] != rel] + [{'path': rel, 'sha256': hashlib.sha256(data).hexdigest(), 'bytes': len(data)}]
    e['totalBytes'] = sum(f['bytes'] for f in e['files'])
    n += 1
open(cat_path, 'wb').write(json.dumps(cat, ensure_ascii=False, indent=1).encode('utf-8'))
print('أُدرجت مواقيت', n, 'سورة')
