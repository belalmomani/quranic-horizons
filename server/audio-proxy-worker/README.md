# خدمة التلاوة على Cloudflare Workers (بديل audio.php)
1. حساب مجاني على cloudflare.com ← Workers & Pages ← Create ← Create Worker ← Deploy (باسم مثل `qh-audio`) ← Edit code ← الصق محتوى `worker.js` ← Deploy.
2. Settings ← Variables and Secrets ← أضف:
   - `QF_CLIENT_ID` (Secret) · `QF_CLIENT_SECRET` (Secret) · `QF_ENV` (نص: `prelive` الآن، ثم `production` بعد الموافقة).
3. افتح: `https://qh-audio.<حسابك>.workers.dev/audio.php?surah=1&recitation=7` ويجب أن يظهر JSON فيه `reciterName` و`files`.
4. في التطبيق: `AUDIO_API_BASE_URL` = `https://qh-audio.<حسابك>.workers.dev` (بدون / في النهاية).
