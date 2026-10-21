// خدمة وسيطة للتلاوة على Cloudflare Workers (بديل audio.php). تحمل بيانات اعتماد Quran.Foundation كأسرار،
// وتُرجع JSON بروابط الآيات فقط؛ لا تخزّن ملفات صوت. الشكل: /audio.php?surah=1&recitation=7
const ALLOWED_RECITATIONS = [7];          // أرقام التلاوات المعتمدة فقط
const MANIFEST_TTL = 86400;               // 24 ساعة (أقل من حدّ الأسبوع في الشروط)

const json = (o, status = 200, extra = {}) =>
  new Response(JSON.stringify(o), { status, headers: { 'Content-Type': 'application/json; charset=utf-8', ...extra } });

let tokenCache = { t: '', exp: 0 };

async function getToken(env, oauth) {
  if (tokenCache.t && tokenCache.exp > Date.now() + 60000) return tokenCache.t;
  const r = await fetch(oauth, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', Authorization: 'Basic ' + btoa(`${env.QF_CLIENT_ID}:${env.QF_CLIENT_SECRET}`) },
    body: 'grant_type=client_credentials&scope=content',
  });
  const j = await r.json().catch(() => ({}));
  if (!r.ok || !j.access_token) throw { error: 'auth_failed', http: r.status, oauth: j.error || null };
  tokenCache = { t: j.access_token, exp: Date.now() + (j.expires_in || 3600) * 1000 };
  return tokenCache.t;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (!env.QF_CLIENT_ID || !env.QF_CLIENT_SECRET) return json({ error: 'not_configured' }, 503);
    const surah = parseInt(url.searchParams.get('surah'), 10);
    const rec = parseInt(url.searchParams.get('recitation'), 10);
    if (!(surah >= 1 && surah <= 114) || !ALLOWED_RECITATIONS.includes(rec)) return json({ error: 'bad_request' }, 400);

    const prod = (env.QF_ENV || 'prelive') === 'production';
    const api = prod ? 'https://apis.quran.foundation' : 'https://apis-prelive.quran.foundation';
    const oauth = prod ? 'https://oauth2.quran.foundation/oauth2/token' : 'https://prelive-oauth2.quran.foundation/oauth2/token';

    const cache = caches.default;
    const cacheKey = new Request(`https://cache.local/${prod ? 'p' : 't'}/${rec}/${surah}`);
    const hit = await cache.match(cacheKey);
    if (hit) return hit;

    try {
      const token = await getToken(env, oauth);
      const H = { 'x-auth-token': token, 'x-client-id': env.QF_CLIENT_ID };
      const rl = await fetch(`${api}/content/api/v4/resources/recitations`, { headers: H });
      const list = (await rl.json().catch(() => ({}))).recitations || [];
      const info = list.find((r) => r.id === rec);
      const name = info ? [info.reciter_name, info.style].filter(Boolean).join(' — ') : '';

      const files = [];
      for (let page = 1; page <= 10; page++) {
        const r = await fetch(`${api}/content/api/v4/recitations/${rec}/by_chapter/${surah}?per_page=50&page=${page}`, { headers: H });
        if (!r.ok) return json({ error: 'upstream', by_chapter_http: r.status, surah_requested: surah }, 502);
        const j = await r.json();
        files.push(...(j.audio_files || []));
        if (!j.pagination || !j.pagination.next_page) break;
      }
      if (!name || !files.length) return json({ error: 'upstream', name_found: !!name, files: files.length }, 502);

      const out = {
        source: 'Quran.Foundation', reciterName: name, recitationId: rec,
        files: files.map((f) => ({ verseKey: f.verse_key, url: /^https?:/.test(f.url) ? f.url : 'https://audio.qurancdn.com/' + String(f.url).replace(/^\/+/, '') })),
      };
      const res = json(out, 200, { 'Cache-Control': `public, max-age=${MANIFEST_TTL}` });
      await cache.put(cacheKey, res.clone());
      return res;
    } catch (e) {
      return json(e && e.error ? e : { error: 'upstream', message: String(e) }, 502);
    }
  },
};
