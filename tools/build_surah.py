#!/usr/bin/env python3
"""يبني وحدة سورة (غير الناس) من المصادر الأصلية دون تعديل نصوصها، بمواصفات في content/specs/<رقم>.json.

  python3 tools/build_surah.py <SURAH> <QURAN_JSON_DIR> <TAFSIR_API_DIR>              # مسودة في content/drafts/<رقم>
  python3 tools/build_surah.py <SURAH> <QURAN_JSON_DIR> <TAFSIR_API_DIR> --publish     # نشر كامل: كل الصور موجودة
  python3 tools/build_surah.py <SURAH> <QURAN_JSON_DIR> <TAFSIR_API_DIR> --publish --partial   # معاينة باللقطات المتوفرة (لقطة لكل آية على الأقل)

الصور في content/bootstrap/modules/<رقم>/img/<الاسم>.(webp|png|jpg). لا صور بديلة ولا مؤقتة.
وحدة 114 وملفاتها لا تُمَسّ. نص القرآن من Tanzil فقط؛ والبسملة تُفصل عرضًا دون تغيير حرف.
"""
import hashlib, json, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from imgsize import image_size  # noqa: E402

args = [a for a in sys.argv[1:] if not a.startswith('--')]
SURAH, Q, T = int(args[0]), args[1], args[2]
PUBLISH, PARTIAL = '--publish' in sys.argv, '--partial' in sys.argv
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'content')
BOOT = os.path.join(ROOT, 'bootstrap')
IMG_DIR = os.path.join(BOOT, 'modules', str(SURAH), 'img')
MIN_W = 1080
spec = json.load(open(os.path.join(ROOT, 'specs', f'{SURAH}.json'), encoding='utf-8'))
assert spec['surah'] == SURAH and SURAH != 114


def sha(b):
    return hashlib.sha256(b).hexdigest()


def dump(o):
    return json.dumps(o, ensure_ascii=False, indent=1).encode('utf-8')


tz = json.load(open(f'{Q}/data/tanzil/uthmani.json', encoding='utf-8'))
chapters = json.load(open(f'{Q}/data/tanzil/chapters.json', encoding='utf-8'))['chapters']
ch = next(c for c in chapters if c['id'] == SURAH)
verses = tz[str(SURAH)]
N = ch['total_verses']
assert N == len(verses) and [v['verse'] for v in verses] == list(range(1, N + 1))
raw_lines = [v['text'] for v in verses]
BASMALA = tz['1'][0]['text']
pref = raw_lines[0].startswith(BASMALA + ' ')
text_lines = list(raw_lines)
if pref:
    text_lines[0] = raw_lines[0][len(BASMALA) + 1:]


MISSING_SRC = []  # آيات لا نص لها في نسخة المصدر الرقمية: لا تُختلق ولا تُدمج


def tf(slug, a):
    path = f'{T}/tafsir/{slug}/{SURAH}/{a}.json'
    if not os.path.isfile(path):
        MISSING_SRC.append((slug, a))
        return None
    return json.load(open(path, encoding='utf-8'))['text']


NOTE = 'النص منقول حرفيًا من النسخة الرقمية دون تعديل.'
BOOKS = [
    ('muyassar', 'ar-tafsir-muyassar', 'التفسير الميسّر', 'نخبة من العلماء (مجمع الملك فهد لطباعة المصحف الشريف)', 'https://qul.tarteel.ai/resources/tafsir/38'),
    ('saadi', 'ar-tafseer-al-saddi', 'تفسير السعدي (تيسير الكريم الرحمن)', 'عبد الرحمن بن ناصر السعدي', 'https://qul.tarteel.ai/resources/tafsir/24'),
    ('ibn-kathir', 'ar-tafsir-ibn-kathir', 'تفسير القرآن العظيم', 'إسماعيل بن عمر بن كثير', 'https://qul.tarteel.ai/resources/tafsir/22'),
    ('tabari', 'ar-tafsir-al-tabari', 'جامع البيان عن تأويل آي القرآن', 'محمد بن جرير الطبري', 'https://qul.tarteel.ai/resources/tafsir/37'),
]
records = []
for sid, slug, book, author, url in BOOKS:
    texts = [tf(slug, a) for a in range(1, N + 1)]
    if sid == 'muyassar':
        assert None not in texts, 'الميسّر ناقص'
    nn = [t for t in texts if t is not None]
    base = {'bookAr': book, 'authorAr': author, 'dataset': 'spa5k/tafsir_api', 'origin': url,
            'licenseStatus': 'unverified', 'attributionReview': 'pending'}
    if len(set(nn)) == 1 and sid != 'muyassar':  # النسخة الرقمية تكرّر نص السورة في كل آية: سجل واحد للسورة
        records.append({**base, 'id': sid, 'scope': 'surah', 'ayahs': list(range(1, N + 1)), 'text': nn[0],
                        'attribution': NOTE, 'sourceNotes': []})
    else:
        for a in range(1, N + 1):
            if texts[a - 1] is None:
                continue
            extra = ' وتتضمن الآية 1 تمهيدًا عن اسم السورة ومقاصدها.' if (sid == 'muyassar' and a == 1) else ''
            records.append({**base, 'id': f'{sid}-{a}', 'scope': 'ayah', 'ayahs': [a], 'text': texts[a - 1],
                            'attribution': NOTE + extra, 'sourceNotes': []})
rec = {r['id']: r for r in records}
for a, (rid, q) in spec['quotes'].items():
    assert q in rec[rid]['text'], (a, q)

SHOTS_ALL = [(s, int(a)) for s, a in spec['stems']]


def find_img(stem):
    for ext in ('webp', 'png', 'jpg', 'jpeg'):
        if os.path.isfile(os.path.join(IMG_DIR, f'{stem}.{ext}')):
            return f'modules/{SURAH}/img/{stem}.{ext}'
    return None


found = {s: find_img(s) for s, _ in SHOTS_ALL}
missing = [s for s, f in found.items() if not f]
SHOTS = SHOTS_ALL
if PUBLISH and PARTIAL:
    SHOTS = [(s, a) for s, a in SHOTS_ALL if found[s]]
    if {a for _, a in SHOTS} != set(range(1, N + 1)):
        sys.exit('--partial يتطلب لقطة واحدة على الأقل لكل آية')
if PUBLISH and missing and not PARTIAL:
    sys.exit('لا نشر: صور ناقصة في %s: %s' % (IMG_DIR, '، '.join(missing)))
ids = {s: f'img{i + 1}' for i, (s, _) in enumerate(SHOTS)}


def K(t, **sets):
    return {'t': t, 'set': sets}


def fade(prev, cur, t):
    st = {i: {'a': 0.0} for i in prev}
    st[cur] = {'a': 1.0}
    return K(t, **st)


by_ayah = {}
for s, a in SHOTS:
    by_ayah.setdefault(a, []).append(ids[s])
beats, shown = [], []
for a in range(1, N + 1):
    cur, keys = by_ayah[a], []
    if len(cur) == 1:
        keys.append(fade(shown, cur[0], 0.2)); shown = [cur[0]]
    else:
        keys.append(fade(shown, cur[0], 0.12)); keys.append(fade([cur[0]], cur[1], 0.55)); shown = [cur[1]]
    keys.append(K(0.95, **{shown[0]: {'z': 1.0}}))
    d = spec['beats'][str(a)]
    rid, q = spec['quotes'][str(a)]
    beats.append({'ayah': a, 'tafsirRecord': rid, 'tafsirQuote': q, 'meaning': d['meaning'], 'visualEvent': d['visualEvent'],
                  'rationale': d['rationale'], 'symbolic': d['symbolic'], 'symbolicNote': d['symbolicNote'], 'gap': d['gap'], 'keys': keys})

sid_ = f"{spec['scenePrefix']}-images"
scene = {'id': sid_, 'ayahs': list(range(1, N + 1)), 'kind': 'image', 'title': f"سورة {ch['name']} — لقطة لكل آية",
         'env': {'skyTop': '#050B1F', 'skyBottom': '#050B1F', 'stars': 0, 'twinkle': 0.0, 'ridges': [], 'mist': 0.0, 'drift': 0.0},
         'actors': [{'id': ids[s], 'kind': 'image', 'file': found[s] or f'modules/{SURAH}/img/{s}.webp', 'init': {'a': 0.0, 'z': 0.0}} for s, _ in SHOTS],
         'beats': beats,
         'review': {'status': 'draft', 'note': spec.get('sceneNote', 'مسودة: الصور النهائية لم تصل بعد.')},
         'transition': {'type': 'crossfade', 'ms': 1600}}

blockers = [
    'الصور النهائية لم تصل بعد (بلا صور بديلة): ' + '، '.join(s for s, _ in SHOTS_ALL) + ' — المواصفات في docs/IMAGE_SPEC.md وdocs/image-briefs',
    *spec.get('extraBlockers', []),
    'التلاوة: بثّ أونلاين من Quran.Foundation بانتظار موافقة الإنتاج، وتحديد ياسر الدوسري ورقم تلاوته وترخيصه؛ لا ملفات صوت في المشروع',
    *[f'{slug}: لا نص للآية {a} في النسخة الرقمية المصدرية (لم يُختلق شيء)' for slug, a in MISSING_SRC],
    'مراجعة نسبة نصوص التفاسير وحقوقها (attributionReview = pending)',
    'مراجعة صاحب المشروع البصرية للقطات (review.status = draft)',
    'مصدر الصور وحقوقها: توثيق الأداة وشروط الاستخدام التجاري قبل التوزيع',
    'مدد الآيات غير مقيسة (تُقاس من التلاوة المبثوثة)',
]
if PUBLISH:
    blockers = [b for b in blockers if not b.startswith('الصور النهائية')]
    if PARTIAL:
        blockers.append('الصور النهائية لم تصل بعد: ما نُشر معاينة. ناقص: ' + ('، '.join(missing) or 'لا شيء') +
                        ((' — ' + spec['partialNote']) if spec.get('partialNote') else ''))
        if spec.get('previewImagesNote'):
            blockers.append(spec['previewImagesNote'])
    for stem, rel in [(k, v) for k, v in found.items() if v]:
        w, h = image_size(os.path.join(BOOT, rel))
        if w < MIN_W or not (0.55 <= w / h <= 0.58):
            blockers.append(f'دقة/نسبة صورة {stem} غير مطابقة ({w}x{h}): المطلوب عرض ≥ {MIN_W} px ونسبة 9:16')

ayahs = [{'number': v['verse'], 'text': text_lines[v['verse'] - 1], 'sceneId': sid_,
          'audio': {'url': '', 'durationMs': None, 'status': 'online_only'}} for v in verses]
module = {'formatVersion': 1, 'surah': SURAH, 'moduleVersion': '0.1.0', 'nameAr': ch['name'], 'ayahCount': N,
          'review': {'status': 'draft', 'blockers': blockers}, 'basmala': BASMALA if pref else None,
          'text': {'source': 'Tanzil Uthmani', 'sha256OfSourceLines': sha('\n'.join(raw_lines).encode('utf-8')),
                   'note': 'سطر الآية 1 في المصدر يبدأ بالبسملة وقد فُصلت عرضًا فقط'},
          'ayahs': ayahs}
out = {'module.json': module, 'tafsir.json': {'formatVersion': 1, 'surah': SURAH, 'records': records},
       'scenes.json': {'formatVersion': 2, 'surah': SURAH, 'scenes': [scene]}}

if not PUBLISH:
    d = os.path.join(ROOT, 'drafts', str(SURAH))
    os.makedirs(d, exist_ok=True)
    for n, o in out.items():
        open(os.path.join(d, n), 'wb').write(dump(o))
    print(f'مسودة سورة {SURAH} في content/drafts/{SURAH} — لم تُنشر. صور ناقصة: {len(missing)} من {len(SHOTS_ALL)}')
    sys.exit(0)

mdir = os.path.join(BOOT, 'modules', str(SURAH))
os.makedirs(mdir, exist_ok=True)
files = []
for n, o in out.items():
    data = dump(o)
    open(os.path.join(mdir, n), 'wb').write(data)
    files.append({'path': f'modules/{SURAH}/{n}', 'sha256': sha(data), 'bytes': len(data)})
for stem, rel in [(k, v) for k, v in found.items() if v]:
    data = open(os.path.join(BOOT, rel), 'rb').read()
    files.append({'path': rel, 'sha256': sha(data), 'bytes': len(data)})
cat_path = os.path.join(BOOT, 'catalog.json')
cat = json.load(open(cat_path, encoding='utf-8'))
cat['modules'] = [m for m in cat['modules'] if m['surah'] != SURAH] + [
    {'surah': SURAH, 'version': '0.1.0', 'status': 'draft', 'dir': f'modules/{SURAH}', 'files': files,
     'totalBytes': sum(f['bytes'] for f in files)}]
open(cat_path, 'wb').write(dump(cat))
print(f'نُشرت وحدة سورة {SURAH}:', json.dumps({f['path']: f['bytes'] for f in files}, ensure_ascii=False))
