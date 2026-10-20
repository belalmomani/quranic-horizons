<?php
// انسخ هذا الملف إلى config.php على الاستضافة فقط (config.php مُتجاهَل في git). لا تضع الأسرار في المستودع.
return [
    'env'           => 'production',          // 'prelive' للاختبار (سورتا 1 و2 فقط)
    'client_id'     => '97505d14-4ec6-4f32-94a9-0d5264aee448',
    'client_secret' => 'qfcs_3b69b082d4c44e8bb0dff50c017a755c890d07bb3bd740528c129b7b0a9227b4',
    'cache_dir'     => __DIR__ . '/cache',   // يخزّن الرمز وقوائم الروابط فقط (JSON)، لا ملفات صوت
    'manifest_ttl'  => 86400,                 // أقل بكثير من حدّ الأسبوع في شروط Quran.Foundation
    'allowed_recitations' => [7],              // أرقام التلاوات المعتمدة فقط (بعد التحقق من القارئ)
    'rate_per_minute' => 30,
];
