#!/usr/bin/env python3
"""يتحقق من وحدات المحتوى: البصمات، عدد الآيات وترتيبها، تغطية المشاهد، وحالة الاعتماد.
اختياري: تمرير مجلد quran-json ليطابق النص حرفيًا مع نسخة Tanzil:
    python3 tools/validate_content.py [QURAN_JSON_DIR]
"""
import hashlib, json, os, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'content', 'bootstrap')
errors, warnings = [], []


def load(rel):
    return json.load(open(os.path.join(ROOT, rel), encoding='utf-8'))


def err(m):
    errors.append(m)


surahs = load('surahs.json')
by_num = {s['number']: s for s in surahs['surahs']}
if len(by_num) != 114 or sum(s['ayahCount'] for s in by_num.values()) != 6236:
    err('surahs.json: يجب أن يضم 114 سورة و6236 آية')
if surahs['deliveryOrder'] != list(range(114, 0, -1)):
    err('deliveryOrder ليس عكس ترتيب المصحف')

catalog = load('catalog.json')
tz = None
if len(sys.argv) > 1:
    tz = json.load(open(os.path.join(sys.argv[1], 'data', 'tanzil', 'uthmani.json'), encoding='utf-8'))

for m in catalog['modules']:
    n = m['surah']
    for f in m['files']:
        data = open(os.path.join(ROOT, f['path']), 'rb').read()
        if hashlib.sha256(data).hexdigest() != f['sha256'] or len(data) != f['bytes']:
            err(f'{f["path"]}: البصمة أو الحجم لا يطابق الكتالوج')
    mod = load(f'{m["dir"]}/module.json')
    scenes = {s['id']: s for s in load(f'{m["dir"]}/scenes.json')['scenes']}
    tafsir = load(f'{m["dir"]}/tafsir.json')['records']
    want = by_num[n]['ayahCount']
    nums = [a['number'] for a in mod['ayahs']]
    if nums != list(range(1, want + 1)):
        err(f'سورة {n}: ترتيب/عدد الآيات غير صحيح')
    for a in mod['ayahs']:
        if a['sceneId'] not in scenes:
            err(f'سورة {n} آية {a["number"]}: مشهد غير موجود')
        if not a['text'].strip():
            err(f'سورة {n} آية {a["number"]}: نص فارغ')
    covered = {x for s in scenes.values() for x in s['ayahs']}
    if covered != set(nums):
        err(f'سورة {n}: المشاهد لا تغطي كل الآيات بالضبط')
    if not tafsir:
        err(f'سورة {n}: لا تفسير')
    for t in tafsir:
        if not t.get('text') or not t.get('origin') or not t.get('authorAr'):
            err(f'سورة {n}: سجل تفسير ناقص ({t.get("id")})')
    # بوابة الاعتماد: لا تُعلن السورة مكتملة وفيها معوّقات
    status = m['status']
    pending = (mod['review'].get('blockers') or []) or any(s['review']['status'] != 'approved' for s in scenes.values()) \
        or any(a['audio']['status'] != 'verified' for a in mod['ayahs']) or any(t['attributionReview'] != 'approved' for t in tafsir)
    if status == 'approved' and pending:
        err(f'سورة {n}: معلنة approved وفيها معوّقات')
    if status != 'approved' and not pending:
        warnings.append(f'سورة {n}: لا معوّقات؛ يمكن اعتمادها')
    # مطابقة النص مع المصدر
    raw_ok = mod['text']['sha256OfSourceLines']
    lines = [a['text'] for a in mod['ayahs']]
    if mod.get('basmala'):
        lines[0] = mod['basmala'] + ' ' + lines[0]
    if hashlib.sha256('\n'.join(lines).encode('utf-8')).hexdigest() != raw_ok:
        err(f'سورة {n}: النص لا يطابق بصمة المصدر')
    if tz is not None and lines != [v['text'] for v in tz[str(n)]]:
        err(f'سورة {n}: النص لا يطابق Tanzil حرفيًا')

for w in warnings:
    print('تنبيه:', w)
if errors:
    print('فشل التحقق:')
    for e in errors:
        print(' -', e)
    sys.exit(1)
print('تحقق المحتوى: سليم (%d وحدة)' % len(catalog['modules']))
