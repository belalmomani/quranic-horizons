#!/usr/bin/env python3
"""طبقة البيانات النصية لكل السور (1..108): نص الآيات من Tanzil + التفاسير الأربعة نقلًا حرفيًا، بلا مشاهد ولا صور.

  python3 tools/build_data.py <QURAN_JSON_DIR> <TAFSIR_API_DIR> [سورة ...]     # الكل إن لم تُذكر سور

المخرجات في content/data/<رقم>/ (خارج bootstrap فلا تدخل الـAPK): module.json وtafsir.json. وحدة بلا مشاهد = status "text_only".
القواعد: لا نص يُختلق ولا يُعدَّل؛ نصوص التفسير المتطابقة بين آيات تُجمَع في سجل واحد لآياتها؛ الآية الفارغة في المصدر تُسجَّل في missing ولا تُملأ.
"""
import hashlib, json, os, sys

args = [a for a in sys.argv[1:]]
Q, T = args[0], args[1]
only = [int(a) for a in args[2:]]
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'content', 'data')
tz = json.load(open(f'{Q}/data/tanzil/uthmani.json', encoding='utf-8'))
chapters = json.load(open(f'{Q}/data/tanzil/chapters.json', encoding='utf-8'))['chapters']
BASMALA = tz['1'][0]['text']
BOOKS = [
    ('muyassar', 'ar-tafsir-muyassar', 'التفسير الميسّر', 'نخبة من العلماء (مجمع الملك فهد لطباعة المصحف الشريف)', 'https://qul.tarteel.ai/resources/tafsir/38'),
    ('saadi', 'ar-tafseer-al-saddi', 'تفسير السعدي (تيسير الكريم الرحمن)', 'عبد الرحمن بن ناصر السعدي', 'https://qul.tarteel.ai/resources/tafsir/24'),
    ('ibn-kathir', 'ar-tafsir-ibn-kathir', 'تفسير القرآن العظيم', 'إسماعيل بن عمر بن كثير', 'https://qul.tarteel.ai/resources/tafsir/22'),
    ('tabari', 'ar-tafsir-al-tabari', 'جامع البيان عن تأويل آي القرآن', 'محمد بن جرير الطبري', 'https://qul.tarteel.ai/resources/tafsir/37'),
]
NOTE = 'النص منقول حرفيًا من النسخة الرقمية دون تعديل.'


def sha(b):
    return hashlib.sha256(b).hexdigest()


def dump(o):
    return json.dumps(o, ensure_ascii=False, indent=1).encode('utf-8')


total = 0
for ch in chapters:
    S = ch['id']
    if S > 108 or (only and S not in only):
        continue
    verses = tz[str(S)]
    N = ch['total_verses']
    assert N == len(verses) and [v['verse'] for v in verses] == list(range(1, N + 1))
    raw = [v['text'] for v in verses]
    pref = S != 1 and raw[0].startswith(BASMALA + ' ')
    text = list(raw)
    if pref:
        text[0] = raw[0][len(BASMALA) + 1:]
    records, missing = [], []
    for sid, slug, book, author, url in BOOKS:
        groups = {}
        for a in range(1, N + 1):
            p = f'{T}/tafsir/{slug}/{S}/{a}.json'
            if not os.path.isfile(p):
                missing.append({'book': sid, 'ayah': a})
                continue
            groups.setdefault(json.load(open(p, encoding='utf-8'))['text'], []).append(a)
        base = {'bookAr': book, 'authorAr': author, 'dataset': 'spa5k/tafsir_api', 'origin': url,
                'licenseStatus': 'unverified', 'attributionReview': 'pending', 'attribution': NOTE, 'sourceNotes': []}
        for i, (t, ays) in enumerate(sorted(groups.items(), key=lambda kv: kv[1][0]), 1):
            whole = len(ays) == N and N > 1
            records.append({**base, 'id': f'{sid}-{ays[0]}' if not whole else sid, 'scope': 'surah' if whole else ('ayah' if len(ays) == 1 else 'group'),
                            'ayahs': ays, 'text': t})
    ids = [r['id'] for r in records]
    assert len(ids) == len(set(ids)), (S, 'معرّفات مكررة')
    mod = {'formatVersion': 1, 'surah': S, 'moduleVersion': '0.0.1', 'nameAr': ch['name'], 'ayahCount': N,
           'review': {'status': 'text_only', 'blockers': ['لا مشاهد ولا صور بعد: وحدة نصية فقط', 'مراجعة نسبة نصوص التفاسير وحقوقها (attributionReview = pending)',
                      'التلاوة: بثّ أونلاين من Quran.Foundation بانتظار موافقة الإنتاج']},
           'basmala': BASMALA if pref else None,
           'text': {'source': 'Tanzil Uthmani', 'sha256OfSourceLines': sha('\n'.join(raw).encode('utf-8')),
                    'note': 'سطر الآية 1 في المصدر يبدأ بالبسملة وقد فُصلت عرضًا فقط' if pref else ''},
           'missingTafsir': missing,
           'ayahs': [{'number': v['verse'], 'text': text[v['verse'] - 1]} for v in verses]}
    d = os.path.join(ROOT, str(S))
    os.makedirs(d, exist_ok=True)
    a, b = dump(mod), dump({'formatVersion': 1, 'surah': S, 'records': records})
    open(os.path.join(d, 'module.json'), 'wb').write(a)
    open(os.path.join(d, 'tafsir.json'), 'wb').write(b)
    total += len(a) + len(b)
    print(S, ch['name'], N, 'سجلات', len(records), 'ناقص', len(missing), round((len(a) + len(b)) / 1e6, 2), 'MB')
print('المجموع MB', round(total / 1e6, 1))
