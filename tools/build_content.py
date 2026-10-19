#!/usr/bin/env python3
"""يبني محتوى التطبيق من المصادر الأصلية دون تعديل نصوصها، ويحسب بصمات الملفات.

المصادر (تُستنسخ محليًا ولا تُضمَّن في المستودع):
  QURAN_JSON  = مستودع risan/quran-json  (يحمل نسخة Tanzil حرفيًا + بيانات السور)
  TAFSIR_API  = مستودع spa5k/tafsir_api  (نصوص التفاسير)
الاستعمال:  python3 tools/build_content.py <QURAN_JSON_DIR> <TAFSIR_API_DIR>
"""
import hashlib, json, os, sys

SURAH = 114
Q, T = sys.argv[1], sys.argv[2]
OUT = os.path.join(os.path.dirname(__file__), '..', 'content', 'bootstrap')


def sha(b: bytes) -> str:
    return hashlib.sha256(b).hexdigest()


def write(path, obj):
    p = os.path.join(OUT, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    data = json.dumps(obj, ensure_ascii=False, indent=1, sort_keys=False).encode('utf-8')
    open(p, 'wb').write(data)
    return {'path': path, 'sha256': sha(data), 'bytes': len(data)}


tz = json.load(open(f'{Q}/data/tanzil/uthmani.json', encoding='utf-8'))
chapters = json.load(open(f'{Q}/data/tanzil/chapters.json', encoding='utf-8'))['chapters']
src = {s['path']: s for s in json.load(open(f'{Q}/data/meta/sources.json', encoding='utf-8'))}
tz_src = src['data/tanzil/uthmani.json']

# ترتيب التسليم المطلوب: عكس المصحف
DELIVERY = list(range(114, 0, -1))
surahs = [{'number': c['id'], 'nameAr': c['name'], 'nameEn': c['transliteration'],
           'meaning': c['translation'], 'revelation': c['type'], 'ayahCount': c['total_verses']} for c in chapters]
assert len(surahs) == 114 and sum(s['ayahCount'] for s in surahs) == 6236
write('surahs.json', {'formatVersion': 1, 'textSource': {'name': 'Tanzil Uthmani', 'url': tz_src['url'],
     'license': tz_src['license'], 'licenseUrl': tz_src['license_url'], 'sourceSha256': tz_src['sha256']},
     'deliveryOrder': DELIVERY, 'surahs': surahs})

verses = tz[str(SURAH)]
ch = next(c for c in chapters if c['id'] == SURAH)
assert len(verses) == ch['total_verses'] == 6
assert [v['verse'] for v in verses] == list(range(1, 7))
raw_lines = [v['text'] for v in verses]
# نسخة Tanzil تبدأ الآية الأولى من كل سورة (عدا الفاتحة) بالبسملة؛ نفصلها عرضًا فقط دون تغيير حرف
BASMALA = tz['1'][0]['text']
basmala_prefixed = raw_lines[0].startswith(BASMALA + ' ')
text_lines = list(raw_lines)
if basmala_prefixed:
    text_lines[0] = raw_lines[0][len(BASMALA) + 1:]
assert BASMALA + ' ' + text_lines[0] == raw_lines[0] or not basmala_prefixed

TAFSIRS = [
    ('saadi', 'ar-tafseer-al-saddi', 'تفسير السعدي (تيسير الكريم الرحمن)', 'عبد الرحمن بن ناصر السعدي',
     'https://qul.tarteel.ai/resources/tafsir/24'),
    ('ibn-kathir', 'ar-tafsir-ibn-kathir', 'تفسير القرآن العظيم', 'إسماعيل بن عمر بن كثير',
     'https://qul.tarteel.ai/resources/tafsir/22'),
    ('tabari', 'ar-tafsir-al-tabari', 'جامع البيان عن تأويل آي القرآن', 'محمد بن جرير الطبري',
     'https://qul.tarteel.ai/resources/tafsir/37'),
]
records = []
for sid, slug, book, author, url in TAFSIRS:
    files = [json.load(open(f'{T}/tafsir/{slug}/{SURAH}/{a}.json', encoding='utf-8')) for a in range(1, 7)]
    texts = {f['text'] for f in files}
    # هذه الطبعات الرقمية تخزّن نص السورة كاملًا لكل آية؛ نحتفظ به كما هو دون تقطيعه يدويًا
    notes = []
    if len(texts) != 1:
        # مصدر البيانات نفسه غير متسق بين ملفات الآيات: نعتمد نص الآية الأولى ونسجّل الاختلاف ليراجَع
        diff = [a for a in range(2, 7) if files[a - 1]['text'] != files[0]['text']]
        notes.append(f'نسخ الآيات {diff} تختلف عن نسخة الآية 1 في بيانات المصدر (حذف أقواس المحقق أو تكرار النص)؛ اعتُمدت نسخة الآية 1 كما وردت.')
    records.append({'id': sid, 'bookAr': book, 'authorAr': author, 'scope': 'surah', 'ayahs': [1, 2, 3, 4, 5, 6],
                    'text': files[0]['text'], 'dataset': 'spa5k/tafsir_api', 'origin': url,
                    'attribution': 'النص منقول حرفيًا من النسخة الرقمية دون تعديل، وتبقى فيه علامات المحقّق إن وُجدت.',
                    'sourceNotes': notes, 'licenseStatus': 'unverified', 'attributionReview': 'pending'})
tafsir_meta = write('modules/114/tafsir.json', {'formatVersion': 1, 'surah': SURAH, 'records': records})

# أوصاف المشاهد: مسودة تحريرية، كل مشهد رمزي مجرّد (لا تجسيد لله ولا لشيطان ولا لنبي)
# note = ما يستند إليه المشهد من نص تفسير السعدي (اقتباس حرفي) ويحتاج مراجعتك قبل الاعتماد.
def quote(sub):
    full = records[0]['text']
    assert sub in full, sub
    return sub

scenes = [
 {'id': 'n1', 'ayahs': [1], 'kind': 'procedural',
  'title': 'الالتجاء', 'tafsirQuote': quote('الاستعاذة برب الناس'),
  'spec': {'skyTop': '#050B1F', 'skyBottom': '#0F2A5C', 'stars': 70, 'twinkle': 0.35, 'glow': {'x': 0.5, 'y': 0.62, 'r': 0.55, 'color': '#D9A84F', 'alpha': 0.34},
           'ridges': [{'h': 0.2, 'color': '#07122B'}], 'mist': 0.0, 'shadowEdge': 0.0, 'drift': 0.2}},
 {'id': 'n2', 'ayahs': [2], 'kind': 'procedural',
  'title': 'الملك', 'tafsirQuote': quote('ومالكهم'),
  'spec': {'skyTop': '#040918', 'skyBottom': '#14316B', 'stars': 150, 'twinkle': 0.4, 'glow': {'x': 0.5, 'y': 0.5, 'r': 0.8, 'color': '#E3BE6C', 'alpha': 0.22},
           'ridges': [{'h': 0.16, 'color': '#07122B'}], 'mist': 0.0, 'shadowEdge': 0.0, 'drift': 0.15}},
 {'id': 'n3', 'ayahs': [3], 'kind': 'procedural',
  'title': 'الإله', 'tafsirQuote': quote('وإلههم'),
  'spec': {'skyTop': '#060D25', 'skyBottom': '#1B3A7A', 'stars': 110, 'twinkle': 0.3, 'glow': {'x': 0.5, 'y': 0.35, 'r': 0.7, 'color': '#F0D28A', 'alpha': 0.4},
           'ridges': [{'h': 0.14, 'color': '#08142F'}], 'mist': 0.0, 'shadowEdge': 0.0, 'drift': 0.1}},
 {'id': 'n4', 'ayahs': [4], 'kind': 'procedural',
  'title': 'الاستعاذة من الوسواس الخنّاس', 'tafsirQuote': quote('يخنس أي: يتأخر إذا ذكر العبد ربه'),
  'spec': {'skyTop': '#04081A', 'skyBottom': '#10224D', 'stars': 60, 'twinkle': 0.25, 'glow': {'x': 0.5, 'y': 0.6, 'r': 0.5, 'color': '#D9A84F', 'alpha': 0.3},
           'ridges': [{'h': 0.18, 'color': '#050E24'}], 'mist': 0.2, 'shadowEdge': 0.55, 'drift': 0.3, 'shadowRetreat': True}},
 {'id': 'n5', 'ayahs': [5], 'kind': 'procedural',
  'title': 'يوسوس في الصدور', 'tafsirQuote': quote('يوسوس في صدور الناس'),
  'spec': {'skyTop': '#050A1C', 'skyBottom': '#0D1C45', 'stars': 45, 'twinkle': 0.5, 'glow': {'x': 0.5, 'y': 0.7, 'r': 0.4, 'color': '#C9A24B', 'alpha': 0.22},
           'ridges': [{'h': 0.2, 'color': '#040C20'}], 'mist': 0.55, 'shadowEdge': 0.3, 'drift': 0.4}},
 {'id': 'n6', 'ayahs': [6], 'kind': 'procedural',
  'title': 'من الجنّة والناس', 'tafsirQuote': quote('والوسواس كما يكون من الجن يكون من الإنس'),
  'spec': {'skyTop': '#050B1F', 'skyBottom': '#14306A', 'stars': 90, 'twinkle': 0.3, 'glow': {'x': 0.5, 'y': 0.58, 'r': 0.65, 'color': '#E3BE6C', 'alpha': 0.36},
           'ridges': [{'h': 0.22, 'color': '#07122B'}, {'h': 0.12, 'color': '#040C20'}], 'mist': 0.25, 'shadowEdge': 0.0, 'drift': 0.2}},
]
for s in scenes:
    s['review'] = {'status': 'draft', 'note': 'مشهد رمزي مجرّد مرسوم بالكود؛ ينتظر موافقتك قبل الاعتماد.'}
    s['transition'] = {'type': 'crossfade', 'ms': 1600}
scene_meta = write('modules/114/scenes.json', {'formatVersion': 1, 'surah': SURAH, 'scenes': scenes})

# التلاوة: رابط ملف كل آية؛ لا تُضمَّن الملفات. الترخيص غير موثّق فتبقى معطّلة افتراضيًا.
AUDIO_BASE = 'https://everyayah.com/data/Yasser_Ad-Dussary_128kbps'
ayahs = [{'number': v['verse'], 'text': text_lines[v['verse'] - 1],
          'sceneId': next(s['id'] for s in scenes if v['verse'] in s['ayahs']),
          'audio': {'url': f"{AUDIO_BASE}/{SURAH:03d}{v['verse']:03d}.mp3", 'durationMs': None, 'status': 'license_unverified'}}
         for v in verses]
module = {'formatVersion': 1, 'surah': SURAH, 'moduleVersion': '0.1.0',
          'nameAr': ch['name'], 'ayahCount': ch['total_verses'],
          'review': {'status': 'draft', 'blockers': [
              'ترخيص تسجيلات الشيخ ياسر الدوسري (EveryAyah لا يذكر ترخيصًا) غير موثّق',
              'مراجعة نسبة نصوص التفاسير وحقوقها (attributionReview = pending)',
              'مراجعتك للمشاهد الستة (review.status = draft)',
              'هل ملف الآية الأولى في التسجيل يتضمن البسملة؟ يلزم الاستماع والتحقق قبل ضبط البداية',
              'التوقيت: يعتمد على حدود ملف كل آية، ولم تُقَس مدد الملفات بعد (durationMs = null)']},
          'basmala': BASMALA if basmala_prefixed else None,
          'text': {'source': 'Tanzil Uthmani', 'sha256OfSourceLines': sha('\n'.join(raw_lines).encode('utf-8')), 'note': 'سطر الآية 1 في المصدر يبدأ بالبسملة وقد فُصلت عرضًا فقط'},
          'ayahs': ayahs}
mod_meta = write('modules/114/module.json', module)

files = [mod_meta, tafsir_meta, scene_meta]
catalog = {'formatVersion': 1, 'appMinVersion': 1, 'modules': [
    {'surah': SURAH, 'version': '0.1.0', 'status': 'draft', 'dir': 'modules/114', 'files': files,
     'totalBytes': sum(f['bytes'] for f in files)}]}
write('catalog.json', catalog)
print('OK', json.dumps({f['path']: f['bytes'] for f in files}))
