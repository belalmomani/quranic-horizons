#!/usr/bin/env python3
"""يتحقق من content/data: (1) كل سجل تفسير يطابق حرفيًا ملف المصدر لكل آية من آياته؛ (2) كل آية في كل كتاب إما مغطاة أو مسجّلة missing؛
(3) نص الآيات يطابق Tanzil بعد فصل البسملة؛ (4) عدد الآيات صحيح.   python3 tools/validate_data.py <QURAN_JSON_DIR> <TAFSIR_API_DIR>"""
import json, os, sys
Q, T = sys.argv[1], sys.argv[2]
D = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'content', 'data')
SLUG = {'muyassar': 'ar-tafsir-muyassar', 'saadi': 'ar-tafseer-al-saddi', 'ibn-kathir': 'ar-tafsir-ibn-kathir', 'tabari': 'ar-tafsir-al-tabari'}
tz = json.load(open(f'{Q}/data/tanzil/uthmani.json', encoding='utf-8'))
BAS = tz['1'][0]['text']
errs, n = [], 0
for S in sorted(int(x) for x in os.listdir(D) if x.isdigit()):
    d = os.path.join(D, str(S))
    m = json.load(open(f'{d}/module.json', encoding='utf-8'))
    recs = json.load(open(f'{d}/tafsir.json', encoding='utf-8'))['records']
    v = tz[str(S)]
    if m['ayahCount'] != len(v) or len(m['ayahs']) != len(v):
        errs.append(f'{S}: عدد الآيات')
    for a, src in zip(m['ayahs'], v):
        exp = src['text'][len(BAS) + 1:] if (a['number'] == 1 and m['basmala']) else src['text']
        if a['text'] != exp:
            errs.append(f'{S}:{a["number"]} نص الآية')
    cover = {}
    for r in recs:
        book = r['id'].split('-')[0] if r['id'].split('-')[0] in SLUG else ('ibn-kathir' if r['id'].startswith('ibn-kathir') else r['id'])
        for a in r['ayahs']:
            cover.setdefault(book, set()).add(a)
            src = json.load(open(f'{T}/tafsir/{SLUG[book]}/{S}/{a}.json', encoding='utf-8'))['text']
            if src != r['text']:
                errs.append(f'{S}:{a} {book} نص التفسير يخالف المصدر')
            n += 1
    miss = {(x['book'], x['ayah']) for x in m['missingTafsir']}
    for b in SLUG:
        for a in range(1, len(v) + 1):
            if a not in cover.get(b, set()) and (b, a) not in miss:
                errs.append(f'{S}:{a} {b} غير مغطّاة ولا مسجّلة')
print('أخطاء:', len(errs), '| فحص سجل-آية:', n)
for e in errs[:30]:
    print(e)
sys.exit(1 if errs else 0)
