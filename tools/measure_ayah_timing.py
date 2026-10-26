#!/usr/bin/env python3
"""يقيس نهايات الآيات في ملف تلاوة سورة كاملة (mp3/m4a) من فجوات الصمت، ويكتب timing_ajmi.json لوحدة السورة.

  python3 tools/measure_ayah_timing.py SURAH FILE.mp3 [--no-basmala]

يقرأ عدد الآيات من content/bootstrap/modules/<SURAH>/module.json (أو content/data/<SURAH>). يتطلب ffmpeg وnumpy.
الطريقة: يُقطَّع الصوت إلى مغلّف طاقة (50ms)، تُستخرج فجوات الصمت، وتُختار أطولها بعدد الحدود المطلوبة
(آيات-1، +1 إن كان الملف يبدأ ببسملة منفصلة). تُطبع هوامش الثقة (أقصر فجوة مختارة مقابل أطول فجوة مرفوضة): هامش ضعيف = راجع بالاستماع.
المخرج مقيس آليًا ويحتاج مطابقة بالاستماع قبل الاعتماد (status = measured-unverified).
"""
import json, os, subprocess, sys, tempfile, wave
import numpy as np

args = [a for a in sys.argv[1:] if not a.startswith('--')]
SURAH, SRC = int(args[0]), args[1]
BAS = '--no-basmala' not in sys.argv and SURAH not in (1, 9)
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'content')
for cand in (os.path.join(ROOT, 'bootstrap', 'modules', str(SURAH), 'module.json'), os.path.join(ROOT, 'data', str(SURAH), 'module.json')):
    if os.path.isfile(cand):
        N = json.load(open(cand, encoding='utf-8'))['ayahCount']
        break
else:
    sys.exit('لا module.json لهذه السورة')

tmp = os.path.join(tempfile.mkdtemp(), 'a.wav')
subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', SRC, '-ac', '1', '-ar', '16000', tmp], check=True)
w = wave.open(tmp)
x = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(float) / 32768
sr, hop = 16000, 800
env = np.array([np.sqrt(np.mean(x[i:i + hop] ** 2) + 1e-12) for i in range(0, len(x) - hop, hop)])
db = 20 * np.log10(env + 1e-9)
dur = len(x) / sr
ref = float(np.median(db[db > -35]))
th = ref - 12
gaps, i = [], 0
while i < len(db):
    if db[i] < th:
        j = i
        while j < len(db) and db[j] < th:
            j += 1
        a, b = i * 0.05, j * 0.05
        if a > 0.3 and b < dur - 0.15 and (b - a) >= 0.12:
            gaps.append((a, b))
        i = j
    else:
        i += 1
need = (N - 1) + (1 if BAS else 0)
if len(gaps) < need:
    sys.exit(f'فجوات غير كافية: وُجد {len(gaps)} والمطلوب {need}. جرّب ملفًا آخر أو راجع يدويًا.')
by_len = sorted(range(len(gaps)), key=lambda k: -(gaps[k][1] - gaps[k][0]))
chosen = sorted(by_len[:need])
rejected = by_len[need:]
margin = (gaps[by_len[need - 1]][1] - gaps[by_len[need - 1]][0]) - (gaps[rejected[0]][1] - gaps[rejected[0]][0]) if rejected else 9.9
cuts = [(gaps[k][0] + gaps[k][1]) / 2 for k in chosen]
if BAS:
    cuts = cuts[1:]
ends = [int(c * 1000) for c in cuts] + [int(dur * 1000)]
assert len(ends) == N
out = {'reciter': 'ahmad-ajmi', 'source': 'mp3quran.net', 'durationMs': int(dur * 1000), 'endsMs': ends, 'status': 'measured-unverified'}
d = os.path.join(ROOT, 'timing-src')
os.makedirs(d, exist_ok=True)
json.dump(out, open(os.path.join(d, f'{SURAH}.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print(f'سورة {SURAH}: آيات={N} المدة={dur:.1f}s فجوات={len(gaps)} مختارة={need} هامش الثقة={margin:.2f}s', '(ضعيف: راجع بالاستماع)' if margin < 0.1 else '')
print('نهايات الآيات (ثانية):', [round(e / 1000, 1) for e in ends])
