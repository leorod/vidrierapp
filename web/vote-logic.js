(function (root, factory) {
  if (typeof module === 'object' && module.exports) {
    module.exports = factory();
  } else {
    root.VoteLogic = factory();
  }
})(typeof self !== 'undefined' ? self : this, function () {
  var RUBROS = [
    'Cafetería', 'Verdulería', 'Panadería', 'Kiosco',
    'Almacén', 'Carnicería', 'Ferretería', 'Farmacia'
  ];
  var KNOWN_RUBROS = RUBROS.concat([
    'Librería', 'Regalería', 'Peluquería', 'Gimnasio', 'Heladería',
    'Lavandería', 'Pizzería', 'Restaurante', 'Ropa', 'Zapatería',
    'Veterinaria', 'Pet shop', 'Papelería', 'Óptica', 'Florería',
    'Casa de comidas', 'Dietética', 'Fiambrería', 'Pollería', 'Pescadería'
  ]);
  var FREQUENCIES = ['DAILY', 'WEEKLY', 'OCCASIONAL'];
  var OTHER = '__other__';
  var MAX_OTHER = 60;
  var TOKEN_KEY = 'vidrierapp.clientToken';
  var LOCAL_ID_RE = /^[A-Za-z0-9_-]{1,64}$/;
  var UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

  function parseLocalId(search) {
    var m = /[?&]localId=([^&#]*)/.exec(search || '');
    if (!m) return null;
    var v;
    try { v = decodeURIComponent(m[1]).trim(); } catch (e) { return null; }
    return LOCAL_ID_RE.test(v) ? v : null;
  }

  function cleanText(text) {
    return String(text == null ? '' : text).replace(/[\u0000-\u001f\u007f]/g, ' ').replace(/\s+/g, ' ').trim();
  }

  function validateSelection(sel) {
    var s = sel || {};
    if (s.category === OTHER) {
      var t = cleanText(s.otherText);
      if (t.length < 2) return { valid: false, reason: 'other-too-short' };
      if (t.length > MAX_OTHER) return { valid: false, reason: 'other-too-long' };
      return { valid: true };
    }
    if (RUBROS.indexOf(s.category) === -1) return { valid: false, reason: 'no-category' };
    return { valid: true };
  }

  function buildVotePayload(input) {
    var check = validateSelection(input);
    if (!check.valid) throw new Error(check.reason);
    var payload = { localId: input.localId };
    if (input.category === OTHER) {
      payload.otherText = cleanText(input.otherText);
    } else {
      payload.category = input.category;
    }
    payload.frequency = FREQUENCIES.indexOf(input.frequency) === -1 ? null : input.frequency;
    payload.scannedAt = input.scannedAt;
    payload.clientToken = input.clientToken;
    payload.fingerprintHash = input.fingerprintHash;
    payload.submittedAt = input.submittedAt;
    return payload;
  }

  function fingerprintSource(env) {
    var e = env || {};
    return [e.userAgent || '', e.language || '', e.screen || '', e.timezone || ''].join('|');
  }

  function fallbackHash(str) {
    var h1 = 0x811c9dc5, h2 = 0x01000193;
    for (var i = 0; i < str.length; i++) {
      var c = str.charCodeAt(i);
      h1 = Math.imul(h1 ^ c, 16777619) >>> 0;
      h2 = Math.imul(h2 + c, 2246822519) >>> 0;
      h2 ^= h1 >>> 13;
    }
    var out = '';
    for (var k = 0; k < 4; k++) {
      h1 = Math.imul(h1 ^ (h2 >>> 15), 2654435761) >>> 0;
      h2 = Math.imul(h2 ^ (h1 >>> 13), 3266489917) >>> 0;
      out += ('00000000' + h1.toString(16)).slice(-8) + ('00000000' + h2.toString(16)).slice(-8);
    }
    return out;
  }

  function toHex(buf) {
    var bytes = new Uint8Array(buf);
    var s = '';
    for (var i = 0; i < bytes.length; i++) s += ('0' + bytes[i].toString(16)).slice(-2);
    return s;
  }

  function hashFingerprint(env, subtle) {
    var src = fingerprintSource(env);
    if (!subtle || typeof TextEncoder === 'undefined') {
      return Promise.resolve(fallbackHash(src));
    }
    return Promise.resolve()
      .then(function () { return subtle.digest('SHA-256', new TextEncoder().encode(src)); })
      .then(toHex)
      .catch(function () { return fallbackHash(src); });
  }

  function randomUuid(cryptoObj) {
    var c = cryptoObj;
    if (c && typeof c.randomUUID === 'function') return c.randomUUID();
    var b = new Uint8Array(16);
    if (c && typeof c.getRandomValues === 'function') {
      c.getRandomValues(b);
    } else {
      for (var i = 0; i < 16; i++) b[i] = Math.floor(Math.random() * 256);
    }
    b[6] = (b[6] & 0x0f) | 0x40;
    b[8] = (b[8] & 0x3f) | 0x80;
    var h = toHex(b.buffer);
    return h.slice(0, 8) + '-' + h.slice(8, 12) + '-' + h.slice(12, 16) + '-' + h.slice(16, 20) + '-' + h.slice(20);
  }

  function safeGet(storage, key) {
    try { return storage ? storage.getItem(key) : null; } catch (e) { return null; }
  }

  function safeSet(storage, key, value) {
    try { if (storage) { storage.setItem(key, value); return true; } } catch (e) {}
    return false;
  }

  function readCookie(jar, key) {
    if (!jar) return null;
    var raw = '';
    try { raw = jar.get(); } catch (e) { return null; }
    var parts = String(raw || '').split(';');
    for (var i = 0; i < parts.length; i++) {
      var p = parts[i].trim();
      if (p.indexOf(key + '=') === 0) return p.slice(key.length + 1);
    }
    return null;
  }

  function writeCookie(jar, key, value) {
    if (!jar) return;
    try { jar.set(key + '=' + value + '; max-age=31536000; path=/; SameSite=Lax'); } catch (e) {}
  }

  function getOrCreateToken(deps) {
    var d = deps || {};
    var found = safeGet(d.storage, TOKEN_KEY);
    if (!UUID_RE.test(found || '')) found = readCookie(d.cookies, TOKEN_KEY);
    if (!UUID_RE.test(found || '')) found = null;
    var token = found || randomUuid(d.crypto);
    if (safeGet(d.storage, TOKEN_KEY) !== token) safeSet(d.storage, TOKEN_KEY, token);
    if (readCookie(d.cookies, TOKEN_KEY) !== token) writeCookie(d.cookies, TOKEN_KEY, token);
    return token;
  }

  function mapVoteResponse(status) {
    if (status === 201 || status === 200) return 'success';
    if (status === 409) return 'duplicate';
    if (status === 429) return 'ratelimit';
    if (status === 400 || status === 404 || status === 422) return 'invalid';
    return 'error';
  }

  function mapReportResponse(status) {
    if (status === 200 || status === 201 || status === 202 || status === 204) return 'success';
    if (status === 409) return 'duplicate';
    if (status === 429) return 'ratelimit';
    if (status === 404) return 'invalid';
    return 'error';
  }

  return {
    RUBROS: RUBROS,
    KNOWN_RUBROS: KNOWN_RUBROS,
    FREQUENCIES: FREQUENCIES,
    OTHER: OTHER,
    MAX_OTHER: MAX_OTHER,
    TOKEN_KEY: TOKEN_KEY,
    parseLocalId: parseLocalId,
    cleanText: cleanText,
    validateSelection: validateSelection,
    buildVotePayload: buildVotePayload,
    fingerprintSource: fingerprintSource,
    fallbackHash: fallbackHash,
    hashFingerprint: hashFingerprint,
    randomUuid: randomUuid,
    getOrCreateToken: getOrCreateToken,
    mapVoteResponse: mapVoteResponse,
    mapReportResponse: mapReportResponse
  };
});
