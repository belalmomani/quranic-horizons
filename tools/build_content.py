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
# «التفسير الميسّر» (مجمع الملك فهد): سجل لكل آية، نص حرفي من النسخة الرقمية؛ هو الأساس الأول لربط المعنى بالمشهد
MUY_SRC = 'https://qul.tarteel.ai/resources/tafsir/38'
muy_records = []
for a in range(1, 7):
    f = json.load(open(f'{T}/tafsir/ar-tafsir-muyassar/{SURAH}/{a}.json', encoding='utf-8'))
    muy_records.append({'id': f'muyassar-{a}', 'bookAr': 'التفسير الميسّر', 'authorAr': 'نخبة من العلماء (مجمع الملك فهد لطباعة المصحف الشريف)',
                        'scope': 'ayah', 'ayahs': [a], 'text': f['text'], 'dataset': 'spa5k/tafsir_api', 'origin': MUY_SRC,
                        'attribution': 'النص منقول حرفيًا من النسخة الرقمية دون تعديل (وتتضمن آية 1 تمهيدًا عن اسم السورة ومقاصدها).',
                        'sourceNotes': [], 'licenseStatus': 'unverified', 'attributionReview': 'pending'})
records = muy_records + records
def muy_quote(a):
    t = muy_records[a - 1]['text']
    return t.split('[التفسير]')[1].strip() if '[التفسير]' in t else t.strip()
tafsir_meta = write('modules/114/tafsir.json', {'formatVersion': 1, 'surah': SURAH, 'records': records})

# ---------------------------------------------------------------------------
# المشاهد (formatVersion 2): المعنى هو الذي يحدّد ما يحدث في المشهد.
# لكل آية "نبضة" (beat) تحوي: المعنى المستخلص من التفسير (اقتباس حرفي + خلاصة تحريرية)،
# الحدث البصري الذي سيراه المستخدم، وسبب الملاءمة. والحدث يغيّر عناصر المشهد نفسها
# (ظهور/اختفاء/حركة/تشكّل)، لا الإضاءة ولا الكاميرا.
# كل مشهد رمزي: لا تجسيد لله ولا لنبي ولا لشيطان/جنّ؛ الوسوسة تُمثَّل بخيوط دخانية بلا صورة.
# ---------------------------------------------------------------------------
def quote(sub):
    full = next(r for r in records if r['id'] == 'saadi')['text']
    assert sub in full, sub
    return sub

def K(t, **sets):
    return {'t': t, 'set': sets}

REFUGE = {
 'id': 'n-refuge', 'ayahs': [1, 2, 3], 'kind': 'procedural', 'title': 'الناس تحت ربوبية الله وملكه وألوهيته',
 'env': {'skyTop': '#050B1F', 'skyBottom': '#12306A', 'stars': 70, 'twinkle': 0.3,
         'glow': {'x': 0.5, 'y': 0.58, 'r': 0.6, 'color': '#D9A84F', 'alpha': 0.26},
         'ridges': [{'h': 0.46, 'color': '#07122B'}, {'h': 0.40, 'color': '#050E24'}], 'mist': 0.12, 'drift': 0.2},
 'actors': [
   {'id': 'stars', 'kind': 'starOrder', 'n': 40, 'seed': 5, 'init': {'a': 1.0, 'order': 0.0}},
   {'id': 'crowd', 'kind': 'crowd', 'n': 21, 'seed': 7, 'init': {'a': 0.0, 'gather': 0.0, 'rows': 0.0}},
   {'id': 'dome', 'kind': 'dome', 'init': {'a': 1.0, 'draw': 0.0}},
   {'id': 'beam', 'kind': 'beam', 'x': 0.5, 'init': {'a': 0.0, 'h': 0.0}},
 ],
 'beats': [
  {'ayah': 1,
   'tafsirRecord': 'muyassar-1',
   'tafsirQuote': muy_quote(1),
   'meaning': 'الاستعاذة والاعتصام برب الناس، القادر وحده على ردّ شر الوسواس.',
   'visualEvent': 'يظهر أناس متفرقون في العتمة، فيتجهون إلى مركز واحد ويجتمعون، ثم تُرسَم فوقهم قبّة نور حامية.',
   'rationale': 'الاعتصام والالتجاء فعلٌ يظهر في اجتماع الضعفاء إلى حِمى واحد؛ والقبّة رمز لرد الشر والحماية، لا صورة لشيء بعينه.',
   'symbolic': True, 'symbolicNote': 'القبّة رمز للاعتصام والحماية، ولا تُصوّر ذات الله.',
   'keys': [K(0.18, crowd={'a': 1.0}), K(0.68, crowd={'gather': 1.0}), K(0.92, dome={'draw': 1.0})]},
  {'ayah': 2,
   'tafsirRecord': 'muyassar-2',
   'tafsirQuote': muy_quote(2),
   'meaning': 'ملك الناس المتصرّف في كل شؤونهم.',
   'visualEvent': 'تتحرك النجوم المتناثرة في السماء فتنتظم في حلقات متّسقة فوق الناس المجتمعين، وتظهر خطوط حلقاتها.',
   'rationale': 'التصرف في كل الشؤون تدبيرٌ شامل؛ فانتظام ما كان متناثرًا تحت نظام واحد هو الصورة الرمزية له. الحدث مختلف عن الآية 1 (من الالتجاء إلى التصرف في الشؤون).',
   'symbolic': True, 'symbolicNote': 'انتظام النجوم رمز لشمول الملك والتدبير.',
   'keys': [K(0.85, stars={'order': 1.0})]},
  {'ayah': 3,
   'tafsirRecord': 'muyassar-3',
   'tafsirQuote': muy_quote(3),
   'meaning': 'إله الناس، الذي لا معبود بحق سواه.',
   'visualEvent': 'يرتفع شعاع نور واحد وحيد من الأفق، فيعيد الناس ترتيب أنفسهم صفوفًا منتظمة متجهة نحوه وحده.',
   'rationale': '«لا معبود بحق سواه» تُمثَّل بجهة واحدة فقط يتجه إليها الجميع: شعاع واحد لا ثاني له. الحدث مختلف عن الآيتين السابقتين (ظهور الشعاع وتغيّر تشكيل الناس).',
   'symbolic': True, 'symbolicNote': 'الشعاع جهةٌ رمزية للتوجّه، وليس صورة لله تعالى.',
   'keys': [K(0.5, beam={'a': 1.0, 'h': 0.0}), K(0.62, beam={'h': 1.0}), K(0.9, crowd={'rows': 1.0})]},
 ]}

WHISPER = {
 'id': 'n-whisper', 'ayahs': [4, 5, 6], 'kind': 'procedural', 'title': 'الوسواس الخنّاس',
 'env': {'skyTop': '#04081A', 'skyBottom': '#0C1B44', 'stars': 45, 'twinkle': 0.25,
         'glow': {'x': 0.5, 'y': 0.62, 'r': 0.45, 'color': '#C9A24B', 'alpha': 0.16},
         'ridges': [{'h': 0.46, 'color': '#050E24'}, {'h': 0.40, 'color': '#040A1C'}], 'mist': 0.3, 'drift': 0.3},
 'actors': [
   {'id': 'man', 'kind': 'figure', 'x': 0.5, 'y': 0.97, 's': 1.2, 'init': {'a': 1.0, 'chest': 0.0}},
   {'id': 'lantern', 'kind': 'lantern', 'x': 0.6, 'y': 0.80, 'init': {'a': 1.0, 'g': 0.3}},
   {'id': 'w1', 'kind': 'wisps', 'n': 7, 'sx': 1.05, 'sy': 0.50, 'tx': 0.60, 'ty': 0.80, 'init': {'a': 0.0, 'r': 0.0}},
   {'id': 'w2', 'kind': 'wisps', 'n': 7, 'sx': 0.95, 'sy': 0.40, 'tx': 0.50, 'ty': 0.74, 'init': {'a': 0.0, 'r': 0.0}},
   {'id': 'smoke', 'kind': 'smoke', 'x': 0.13, 'y': 0.50, 'init': {'a': 0.0}},
   {'id': 'other', 'kind': 'figure', 'x': 0.88, 'y': 0.97, 's': 1.1, 'init': {'a': 0.0, 'chest': 0.0}},
   {'id': 'wU', 'kind': 'wisps', 'n': 7, 'sx': 0.18, 'sy': 0.55, 'tx': 0.47, 'ty': 0.74, 'init': {'a': 0.0, 'r': 0.0}},
   {'id': 'wP', 'kind': 'wisps', 'n': 7, 'sx': 0.84, 'sy': 0.74, 'tx': 0.53, 'ty': 0.74, 'init': {'a': 0.0, 'r': 0.0}},
 ],
 'beats': [
  {'ayah': 4,
   'tafsirRecord': 'muyassar-4',
   'tafsirQuote': muy_quote(4),
   'meaning': 'من أذى الشيطان الذي يوسوس عند الغفلة ويختفي عند ذكر الله.',
   'visualEvent': 'والقنديل خافت (حال الغفلة) تقترب خيوط دخانية داكنة من إنسان؛ فإذا اشتدّ نور القنديل (ذكر الله) انكمشت الخيوط واختفت.',
   'rationale': 'الآية تذكر حالين: وسوسة عند الغفلة، واختفاء عند الذكر؛ فالحدث يُظهر الاقتراب مع خفوت النور ثم الاختفاء مع اشتداده، لا مجرد تغيّر لون.',
   'symbolic': True, 'symbolicNote': 'الخيوط الدخانية رمز للوسوسة غير المرئية، وليست صورة لشيطان. نور القنديل رمز للذكر.',
   'keys': [K(0.0, w1={'a': 0.9}), K(0.42, w1={'r': 0.9}), K(0.5, lantern={'g': 1.0}), K(0.92, w1={'r': 0.0, 'a': 0.0})]},
  {'ayah': 5,
   'tafsirRecord': 'muyassar-5',
   'tafsirQuote': muy_quote(5),
   'meaning': 'الذي يبثّ الشر والشكوك في صدور الناس.',
   'visualEvent': 'تتجه الخيوط الداكنة إلى صدر الإنسان نفسه فتدخل منطقة الصدر وتظهر فيها ذرّات داكنة مترددة (الشكوك)، ويخفت القنديل.',
   'rationale': 'الآية تحدّد موضع البثّ (الصدور) وما يُبَثّ (الشر والشكوك)؛ فالحدث يصل إلى الصدر نفسه لا إلى محيط الإنسان كما في الآية 4.',
   'symbolic': True, 'symbolicNote': 'ما يجري في الصدر رمز لحديث النفس الخفي ولا يُصوَّر حرفيًا.',
   'keys': [K(0.0, w2={'a': 0.9}), K(0.6, w2={'r': 1.0}), K(0.72, man={'chest': 1.0}, lantern={'g': 0.3})]},
  {'ayah': 6,
   'tafsirRecord': 'muyassar-6',
   'tafsirQuote': muy_quote(6),
   'meaning': 'من شياطين الجن والإنس.',
   'visualEvent': 'يظهر مصدران للوسوسة: دخان بلا شكل من جهة المجهول، وإنسان آخر من جهة أخرى؛ ويرسل كلٌّ منهما خيطه إلى الإنسان.',
   'rationale': 'الآية تُثبت مصدرين مختلفين؛ فتظهر عناصر جديدة (مصدر خفي بلا صورة ومصدر بشري) لم تكن في الآيتين السابقتين.',
   'symbolic': True, 'symbolicNote': 'الدخان بلا ملامح عمدًا: الجنّة غيب لا يُصوَّر. الإنسان الآخر شخص عام بلا هوية.',
   'keys': [K(0.0, w2={'a': 0.0, 'r': 0.0}), K(0.25, smoke={'a': 1.0}, other={'a': 1.0}), K(0.5, wU={'a': 0.9, 'r': 0.5}, wP={'a': 0.9, 'r': 0.5}), K(0.88, wU={'r': 1.0}, wP={'r': 1.0})]},
 ]}

scenes = [REFUGE, WHISPER]
for s in scenes:
    s['review'] = {'status': 'draft', 'note': 'مشهد رمزي مرسوم بالكود؛ ربط المعنى بالحدث موثّق لكل آية، وينتظر مراجعتك البصرية قبل الاعتماد.'}
    s['transition'] = {'type': 'crossfade', 'ms': 1600}
scene_meta = write('modules/114/scenes.json', {'formatVersion': 2, 'surah': SURAH, 'scenes': scenes})

# التلاوة: رابط ملف كل آية؛ لا تُضمَّن الملفات. الترخيص غير موثّق فتبقى معطّلة افتراضيًا.
AUDIO_BASE = 'https://everyayah.com/data/Yasser_Ad-Dussary_128kbps'
ayahs = [{'number': v['verse'], 'text': text_lines[v['verse'] - 1],
          'sceneId': next(s['id'] for s in scenes if v['verse'] in s['ayahs']),
          'audio': {'url': f"{AUDIO_BASE}/{SURAH:03d}{v['verse']:03d}.mp3", 'durationMs': None, 'status': 'license_unverified'}}
         for v in verses]
module = {'formatVersion': 1, 'surah': SURAH, 'moduleVersion': '0.1.0',
          'nameAr': ch['name'], 'ayahCount': ch['total_verses'],
          'review': {'status': 'draft', 'blockers': [
              'ترخيص التلاوة غير موثّق: روابط EveryAyah بلا ترخيص معلن، وملف m4a الذي أرسلته بلا بيانات قارئ أو ترخيص (وسوم الملف تدل على تنزيل من منصة بث)؛ لا يُعاد توزيعه قبل التحقق كتابيًا',
              'مراجعة نسبة نصوص التفاسير وحقوقها (attributionReview = pending)',
              'مراجعتك البصرية للمشهدين وأحداثهما الستة (review.status = draft)',
              'هل ملف الآية الأولى في التسجيل يتضمن البسملة؟ يلزم الاستماع والتحقق قبل ضبط البداية',
              'التوقيت: اقتُرحت نقاط تقسيم آلية لملف m4a (7 مقاطع) في docs/audio/an-nas-cues.draft.json، لكن لا يُعرف أي مقطع هو أي آية (وهل الأول بسملة) قبل استماعك؛ ومدد الملفات في module.json ما زالت null']},
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
