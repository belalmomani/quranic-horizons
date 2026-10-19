# صيغة المحتوى (formatVersion 1)
- `surahs.json`: 114 سورة، `deliveryOrder` عكسي (114→1)، ومصدر النص (Tanzil Uthmani).
- `catalog.json`: لكل وحدة `version`, `status` (draft/approved), وملفاتها مع `sha256` و`bytes`.
- `modules/<n>/module.json`: الآيات (النص دون البسملة المضافة)، `basmala` للعرض، بصمة أسطر المصدر، `audio` لكل آية، `review.blockers`.
- `modules/<n>/tafsir.json`: سجلات حرفية: الكتاب، المؤلف، المصدر، `licenseStatus`, `attributionReview`, `sourceNotes` (اختلافات النسخ).
- `modules/<n>/scenes.json`: مشاهد إجرائية معرّفة بمعاملات، كل مشهد يرتبط بآيات ومقتبس حرفي من التفسير و`review.status`.
شرط الاعتماد: لا معوّقات + كل المشاهد approved + كل التلاوات verified + كل النسب approved.
