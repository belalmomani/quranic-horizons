#!/usr/bin/env python3
"""يبني وحدة سورة الفلق (113) من المصادر الأصلية دون تعديل نصوصها.

الاستعمال:
  python3 tools/build_falaq.py <QURAN_JSON_DIR> <TAFSIR_API_DIR>            # مسودة في content/drafts/falaq (لا تدخل التطبيق)
  python3 tools/build_falaq.py <QURAN_JSON_DIR> <TAFSIR_API_DIR> --publish  # تُدرج في content/bootstrap وتُحدّث الكتالوج

--publish يتطلب وجود كل صور اللقطات في content/bootstrap/modules/113/img/ بأسمائها (falaq_01_dawn ... بأي امتداد jpg/png/webp).
لا صور بديلة ولا مؤقتة: بلا صور حقيقية لا تُنشر الوحدة. وحدة 114 وملفاتها لا تُمَسّ.
"""
import hashlib, json, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from imgsize import image_size  # noqa: E402

SURAH = 113
args = [a for a in sys.argv[1:] if not a.startswith('--')]
PUBLISH = '--publish' in sys.argv
Q, T = args[0], args[1]
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'content')
BOOT = os.path.join(ROOT, 'bootstrap')
IMG_DIR = os.path.join(BOOT, 'modules', str(SURAH), 'img')
MIN_W = 1080


def sha(b):
    return hashlib.sha256(b).hexdigest()


def dump(obj):
    return json.dumps(obj, ensure_ascii=False, indent=1).encode('utf-8')


tz = json.load(open(f'{Q}/data/tanzil/uthmani.json', encoding='utf-8'))
chapters = json.load(open(f'{Q}/data/tanzil/chapters.json', encoding='utf-8'))['chapters']
ch = next(c for c in chapters if c['id'] == SURAH)
verses = tz[str(SURAH)]
N = ch['total_verses']
assert N == len(verses) == 5 and [v['verse'] for v in verses] == list(range(1, 6))
raw_lines = [v['text'] for v in verses]
BASMALA = tz['1'][0]['text']
pref = raw_lines[0].startswith(BASMALA + ' ')
text_lines = list(raw_lines)
if pref:
    text_lines[0] = raw_lines[0][len(BASMALA) + 1:]


def tf(slug, a):
    return json.load(open(f'{T}/tafsir/{slug}/{SURAH}/{a}.json', encoding='utf-8'))['text']


NOTE = 'النص منقول حرفيًا من النسخة الرقمية دون تعديل.'
records = []
for a in range(1, N + 1):
    records.append({'id': f'muyassar-{a}', 'bookAr': 'التفسير الميسّر', 'authorAr': 'نخبة من العلماء (مجمع الملك فهد لطباعة المصحف الشريف)',
                    'scope': 'ayah', 'ayahs': [a], 'text': tf('ar-tafsir-muyassar', a), 'dataset': 'spa5k/tafsir_api',
                    'origin': 'https://qul.tarteel.ai/resources/tafsir/38', 'attribution': NOTE + (' وتتضمن الآية 1 تمهيدًا عن اسم السورة ومقاصدها.' if a == 1 else ''),
                    'sourceNotes': [], 'licenseStatus': 'unverified', 'attributionReview': 'pending'})
for a in range(1, N + 1):
    records.append({'id': f'saadi-{a}', 'bookAr': 'تفسير السعدي (تيسير الكريم الرحمن)', 'authorAr': 'عبد الرحمن بن ناصر السعدي',
                    'scope': 'ayah', 'ayahs': [a], 'text': tf('ar-tafseer-al-saddi', a), 'dataset': 'spa5k/tafsir_api',
                    'origin': 'https://qul.tarteel.ai/resources/tafsir/24', 'attribution': NOTE,
                    'sourceNotes': [], 'licenseStatus': 'unverified', 'attributionReview': 'pending'})
for sid, slug, book, author, url in [
    ('ibn-kathir', 'ar-tafsir-ibn-kathir', 'تفسير القرآن العظيم', 'إسماعيل بن عمر بن كثير', 'https://qul.tarteel.ai/resources/tafsir/22'),
    ('tabari', 'ar-tafsir-al-tabari', 'جامع البيان عن تأويل آي القرآن', 'محمد بن جرير الطبري', 'https://qul.tarteel.ai/resources/tafsir/37'),
]:
    files = [tf(slug, a) for a in range(1, N + 1)]
    notes = [] if len(set(files)) == 1 else ['نسخ الآيات تختلف في بيانات المصدر؛ اعتُمدت نسخة الآية 1 كما وردت.']
    records.append({'id': sid, 'bookAr': book, 'authorAr': author, 'scope': 'surah', 'ayahs': list(range(1, N + 1)), 'text': files[0],
                    'dataset': 'spa5k/tafsir_api', 'origin': url, 'attribution': NOTE, 'sourceNotes': notes,
                    'licenseStatus': 'unverified', 'attributionReview': 'pending'})
rec = {r['id']: r for r in records}

# اقتباسات حرفية من الميسّر (تُتحقَّق أنها جزء من نص السجل)
Q_ = {
    1: ('muyassar-1', 'قل -أيها الرسول-: أعوذ وأعتصم برب الفلق، وهو الصبح.'),
    2: ('muyassar-2', 'من شر جميع المخلوقات وأذاها.'),
    3: ('muyassar-3', 'ومن شر ليل شديد الظلمة إذا دخل وتغلغل'),
    4: ('muyassar-4', 'ومن شر الساحرات اللاتي ينفخن فيما يعقدن من عُقَد بقصد السحر.'),
    5: ('muyassar-5', 'ومن شر حاسد مبغض للناس إذا حسدهم على ما وهبهم الله من نِعَم'),
}
for a, (rid, q) in Q_.items():
    assert q in rec[rid]['text'], (a, q)

# اللقطات: الاسم (بلا امتداد) -> الآية. الآية 4 لها لقطتان منفصلتان.
SHOTS = [('falaq_01_dawn', 1), ('falaq_02_creation', 2), ('falaq_03_night', 3),
         ('falaq_04_knots', 4), ('falaq_04_blowing', 4), ('falaq_05_envy', 5)]
PROPOSED = {'falaq_01_dawn', 'falaq_02_creation'}  # لم يقدّمها صاحب المشروع؛ مقترحة وتنتظر موافقته


def find_img(stem):
    for ext in ('webp', 'png', 'jpg', 'jpeg'):
        p = os.path.join(IMG_DIR, f'{stem}.{ext}')
        if os.path.isfile(p):
            return f'modules/{SURAH}/img/{stem}.{ext}'
    return None


found = {stem: find_img(stem) for stem, _ in SHOTS}
missing = [s for s, f in found.items() if not f]
ids = {stem: f'img{i + 1}' for i, (stem, _) in enumerate(SHOTS)}


def K(t, **sets):
    return {'t': t, 'set': sets}


def fade(prev_ids, cur, t=0.2, extra=None):
    s = {i: {'a': 0.0} for i in prev_ids}
    s[cur] = {'a': 1.0}
    return K(t, **s)


BEATS = {
 1: dict(meaning='الأمر بالاعتصام برب الفلق، وهو الصبح، الذي يفلق ظلمة الليل.',
         visualEvent='فجر ينشقّ فيه الليل عن خيط ضوء على الأفق، وشخص عام من الخلف متوجّه إلى الضوء رافع يديه بالدعاء.',
         rationale='«الفلق» هو الصبح؛ فالمشهد يُظهر انفلاق الظلام عن الضوء، ورفع اليدين صورة الاعتصام. لا يُصوَّر الرسول ولا الذات الإلهية.',
         symbolic=False, symbolicNote='', gap='اللقطة مقترحة من Claude ولم يقدّمها صاحب المشروع؛ تحتاج موافقته.'),
 2: dict(meaning='الاستعاذة من شر جميع المخلوقات وأذاها.',
         visualEvent='منظر طبيعي واسع تتجاور فيه مظاهر الخلق: بحر هائج وغابة وصحراء، بلا تصوير مخلوق مخيف.',
         rationale='الآية عامة في «جميع المخلوقات»؛ فيُعبَّر عنها بتنوّع الخلق ومظاهر الأذى الطبيعي دون تعيين مخلوق بعينه.',
         symbolic=True, symbolicNote='العموم لا يُصوَّر في مشهد واحد حرفيًا؛ المنظر الطبيعي المتنوع تمثيل رمزي للعموم.',
         gap='اللقطة مقترحة من Claude ولم يقدّمها صاحب المشروع؛ تحتاج موافقته.'),
 3: dict(meaning='الاستعاذة من شر الليل إذا اشتدّ ظلامه ودخل، وما فيه من الشرور والمؤذيات.',
         visualEvent='طريق ريفي ضيّق بين أشجار كثيفة في منطقة جبلية ليلًا، غيوم تحجب القمر وضباب خفيف، والطريق يمتدّ إلى عمق مظلم.',
         rationale='«ليل شديد الظلمة إذا دخل وتغلغل» يتجسّد في الطريق الذي يبتلعه الظلام. لا تُصوَّر مخلوقات خيالية ولا أرواح.',
         symbolic=False, symbolicNote='', gap='«الشرور والمؤذيات» تُترك للإحساس بالمخاطر المحتملة ولا تُصوَّر.'),
 4: dict(meaning='الاستعاذة من شر الساحرات اللاتي ينفخن في العقد التي يعقدنها بقصد السحر.',
         visualEvent='لقطة قريبة ليدَي امرأة مجهولة تربط عقدًا متتابعة في خيط، ثم لقطة بزاوية مختلفة لها وهي تنفث باتجاه العقد.',
         rationale='الآية والتفسير يذكران فعلين: عقد العقد والنفث فيها؛ فلكل فعل لقطة مستقلة. بلا مؤثرات سحرية ولا رموز غيبية.',
         symbolic=False, symbolicNote='', gap='السحر نفسه وأثره غيب لا يُصوَّر؛ المصوَّر الفعل المذكور فقط.'),
 5: dict(meaning='الاستعاذة من شر الحاسد الذي يبغض نعمة الله على غيره ويريد زوالها وإيقاع الأذى بصاحبها.',
         visualEvent='رجل في مقدمة الصورة أمام بيته أو حديقته راضٍ بنعمته، وفي الخلفية رجل آخر يراقبه بنظرات استياء وغيرة.',
         rationale='التباين بين صاحب النعمة والحاسد يُظهر معنى «حسدهم على ما وهبهم الله»، ويُعبَّر عن الحسد بالوجه والنظرة والسلوك فقط.',
         symbolic=False, symbolicNote='', gap='الإيذاء المراد لا يُصوَّر؛ بلا عين زرقاء ولا أشعة ولا دخان.'),
}
by_ayah = {}
for stem, a in SHOTS:
    by_ayah.setdefault(a, []).append(ids[stem])

beats = []
shown = []  # اللقطات الظاهرة حتى الآن
for a in range(1, N + 1):
    cur = by_ayah[a]
    keys = []
    if len(cur) == 1:
        keys.append(fade(shown, cur[0], 0.2))
        shown = [cur[0]]
    else:  # لقطتان لنفس الآية: الأولى مبكرًا والثانية عند منتصف الآية
        keys.append(fade(shown, cur[0], 0.12))
        keys.append(fade([cur[0]], cur[1], 0.55))
        shown = [cur[1]]
    keys.append(K(0.95, **{shown[0]: {'z': 1.0}}))
    d = BEATS[a]
    rid, q = Q_[a]
    beats.append({'ayah': a, 'tafsirRecord': rid, 'tafsirQuote': q, 'meaning': d['meaning'], 'visualEvent': d['visualEvent'],
                  'rationale': d['rationale'], 'symbolic': d['symbolic'], 'symbolicNote': d['symbolicNote'], 'gap': d['gap'], 'keys': keys})

scene = {'id': 'f-images', 'ayahs': list(range(1, N + 1)), 'kind': 'image', 'title': 'سورة الفلق — لقطة لكل آية',
         'env': {'skyTop': '#050B1F', 'skyBottom': '#050B1F', 'stars': 0, 'twinkle': 0.0, 'ridges': [], 'mist': 0.0, 'drift': 0.0},
         'actors': [{'id': ids[s], 'kind': 'image', 'file': f'modules/{SURAH}/img/{s}.webp', 'init': {'a': 0.0, 'z': 0.0}} for s, _ in SHOTS],
         'beats': beats,
         'review': {'status': 'draft', 'note': 'مسودة: لقطتا الآيتين 1 و2 مقترحتان، والصور النهائية لم تصل بعد.'},
         'transition': {'type': 'crossfade', 'ms': 1600}}
# إن وُجدت الصور فعلًا نعتمد امتداداتها الحقيقية
for act in scene['actors']:
    stem = next(s for s, _ in SHOTS if ids[s] == act['id'])
    if found[stem]:
        act['file'] = found[stem]

blockers = [
    'الصور النهائية لم تصل بعد (بلا صور بديلة): ' + '، '.join(s for s, _ in SHOTS) + ' — المواصفات في docs/IMAGE_SPEC.md وdocs/image-briefs',
    'لقطتا الآيتين 1 و2 (falaq_01_dawn وfalaq_02_creation) مقترحتان من Claude ولم يقدّمهما صاحب المشروع؛ تنتظران موافقته',
    'التلاوة: بثّ أونلاين من Quran.Foundation بانتظار موافقة الإنتاج، وتحديد ياسر الدوسري ورقم تلاوته وترخيصه؛ لا ملفات صوت في المشروع',
    'مراجعة نسبة نصوص التفاسير وحقوقها (attributionReview = pending)',
    'مراجعة صاحب المشروع البصرية للقطات (review.status = draft)',
    'مصدر الصور وحقوقها: توثيق الأداة وشروط الاستخدام التجاري قبل التوزيع',
    'مدد الآيات غير مقيسة (تُقاس من التلاوة المبثوثة)',
]
if PUBLISH and missing:
    sys.exit('لا نشر: صور ناقصة في %s: %s' % (IMG_DIR, '، '.join(missing)))
if PUBLISH:
    blockers = [b for b in blockers if not b.startswith('الصور النهائية')]
    for stem, rel in found.items():
        w, h = image_size(os.path.join(BOOT, rel))
        if w < MIN_W or not (0.55 <= w / h <= 0.58):
            blockers.append(f'دقة/نسبة صورة {stem} غير مطابقة ({w}x{h}): المطلوب عرض ≥ {MIN_W} px ونسبة 9:16')

ayahs = [{'number': v['verse'], 'text': text_lines[v['verse'] - 1], 'sceneId': 'f-images',
          'audio': {'url': '', 'durationMs': None, 'status': 'online_only'}} for v in verses]
module = {'formatVersion': 1, 'surah': SURAH, 'moduleVersion': '0.1.0', 'nameAr': ch['name'], 'ayahCount': N,
          'review': {'status': 'draft', 'blockers': blockers}, 'basmala': BASMALA if pref else None,
          'text': {'source': 'Tanzil Uthmani', 'sha256OfSourceLines': sha('\n'.join(raw_lines).encode('utf-8')),
                   'note': 'سطر الآية 1 في المصدر يبدأ بالبسملة وقد فُصلت عرضًا فقط'},
          'ayahs': ayahs}

out = {'module.json': module, 'tafsir.json': {'formatVersion': 1, 'surah': SURAH, 'records': records},
       'scenes.json': {'formatVersion': 2, 'surah': SURAH, 'scenes': [scene]}}

if not PUBLISH:
    d = os.path.join(ROOT, 'drafts', 'falaq')
    os.makedirs(d, exist_ok=True)
    for n, o in out.items():
        open(os.path.join(d, n), 'wb').write(dump(o))
    print('مسودة الفلق في content/drafts/falaq — لم تُنشر.', 'صور ناقصة:', len(missing), 'من', len(SHOTS))
    sys.exit(0)

if missing:
    sys.exit('لا نشر: صور ناقصة في %s: %s' % (IMG_DIR, '، '.join(missing)))
mdir = os.path.join(BOOT, 'modules', str(SURAH))
files = []
for n, o in out.items():
    data = dump(o)
    open(os.path.join(mdir, n), 'wb').write(data)
    files.append({'path': f'modules/{SURAH}/{n}', 'sha256': sha(data), 'bytes': len(data)})
for stem, rel in found.items():
    data = open(os.path.join(BOOT, rel), 'rb').read()
    files.append({'path': rel, 'sha256': sha(data), 'bytes': len(data)})
cat_path = os.path.join(BOOT, 'catalog.json')
cat = json.load(open(cat_path, encoding='utf-8'))
cat['modules'] = [m for m in cat['modules'] if m['surah'] != SURAH] + [
    {'surah': SURAH, 'version': '0.1.0', 'status': 'draft', 'dir': f'modules/{SURAH}', 'files': files,
     'totalBytes': sum(f['bytes'] for f in files)}]
open(cat_path, 'wb').write(dump(cat))
print('نُشرت وحدة الفلق:', json.dumps({f['path']: f['bytes'] for f in files}))
