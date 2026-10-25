#!/usr/bin/env python3
"""يُدرج ترجمات التفسير (لا الآيات) في الوحدات ويحدّث بصمات الكتالوج.

المصدر: content/translations-src/<رقم السورة>/<lang>.json  (lang ∈ en, zh, hi, es, fr, bn؛ العربية هي الأصل فلا ملف لها)
الشكل: {"lang","status","source","translator","scopeNote","tafsir": {"<معرّف سجل التفسير>": "نص مترجم"}}
القواعد: المفاتيح معرّفات سجلات موجودة في tafsir.json للوحدة؛ لا نصوص فارغة؛ الحالة لا تكون approved إلا بمراجعة بشرية موثّقة.
تُشغَّل بعد build_content/build_falaq (لأنهما يعيدان كتابة مدخل الكتالوج):  python3 tools/add_translations.py
"""
import hashlib, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, '..', 'content', 'translations-src')
BOOT = os.path.join(HERE, '..', 'content', 'bootstrap')
LANGS = {'en', 'zh', 'hi', 'es', 'fr', 'bn'}


def sha(b):
    return hashlib.sha256(b).hexdigest()


cat_path = os.path.join(BOOT, 'catalog.json')
cat = json.load(open(cat_path, encoding='utf-8'))
count = 0
for sdir in sorted(os.listdir(SRC)):
    if not sdir.isdigit():
        continue
    surah = int(sdir)
    entry = next((m for m in cat['modules'] if m['surah'] == surah), None)
    if entry is None:
        sys.exit(f'سورة {surah}: لا وحدة في الكتالوج (ابنِها أولًا)')
    tafsir = json.load(open(os.path.join(BOOT, entry['dir'], 'tafsir.json'), encoding='utf-8'))['records']
    ids = {r['id'] for r in tafsir}
    prefix = f"{entry['dir']}/translations/"
    entry['files'] = [f for f in entry['files'] if not f['path'].startswith(prefix)]
    os.makedirs(os.path.join(BOOT, entry['dir'], 'translations'), exist_ok=True)
    for fn in sorted(os.listdir(os.path.join(SRC, sdir))):
        if not fn.endswith('.json'):
            continue
        obj = json.load(open(os.path.join(SRC, sdir, fn), encoding='utf-8'))
        lang = obj['lang']
        if lang not in LANGS or fn != lang + '.json':
            sys.exit(f'{sdir}/{fn}: لغة غير مدعومة أو اسم الملف لا يطابق اللغة')
        if obj.get('status') == 'approved' and not obj.get('reviewer'):
            sys.exit(f'{sdir}/{fn}: لا اعتماد بلا مراجع بشري (reviewer)')
        bad = [k for k in obj['tafsir'] if k not in ids]
        if bad:
            sys.exit(f'{sdir}/{fn}: معرّفات تفسير غير موجودة: {bad}')
        if any(not str(v).strip() for v in obj['tafsir'].values()):
            sys.exit(f'{sdir}/{fn}: ترجمة فارغة')
        data = (json.dumps(obj, ensure_ascii=False, indent=1)).encode('utf-8')
        rel = f"{prefix}{lang}.json"
        open(os.path.join(BOOT, rel), 'wb').write(data)
        entry['files'].append({'path': rel, 'sha256': sha(data), 'bytes': len(data)})
        count += 1
    entry['totalBytes'] = sum(f['bytes'] for f in entry['files'])
open(cat_path, 'wb').write(json.dumps(cat, ensure_ascii=False, indent=1).encode('utf-8'))
print(f'أُدرجت {count} ملف ترجمة')
