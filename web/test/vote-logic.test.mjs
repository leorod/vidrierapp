import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import { webcrypto } from 'node:crypto';

const L = createRequire(import.meta.url)('../vote-logic.js');

const base = {
  localId: 'shop-1',
  category: 'Kiosco',
  frequency: 'WEEKLY',
  scannedAt: '2026-11-25T10:00:00.000Z',
  clientToken: '11111111-1111-4111-8111-111111111111',
  fingerprintHash: 'abc',
  submittedAt: '2026-11-25T10:00:05.000Z'
};

function memStorage(initial = {}) {
  const m = { ...initial };
  return { getItem: (k) => (k in m ? m[k] : null), setItem: (k, v) => { m[k] = v; }, dump: () => m };
}
function memJar() {
  let raw = '';
  return { get: () => raw, set: (v) => { raw = v.split(';')[0]; } };
}
function brokenStorage() {
  return { getItem() { throw new Error('denied'); }, setItem() { throw new Error('denied'); } };
}

test('parseLocalId reads valid ids', () => {
  assert.equal(L.parseLocalId('?localId=shop-mataderos-01'), 'shop-mataderos-01');
  assert.equal(L.parseLocalId('?mock=1&localId=abc_9'), 'abc_9');
});

test('parseLocalId rejects missing, empty and malformed ids', () => {
  assert.equal(L.parseLocalId(''), null);
  assert.equal(L.parseLocalId('?localId='), null);
  assert.equal(L.parseLocalId('?other=1'), null);
  assert.equal(L.parseLocalId('?localId=a%20b'), null);
  assert.equal(L.parseLocalId('?localId=%E0%A4%A'), null);
  assert.equal(L.parseLocalId('?localId=' + 'a'.repeat(65)), null);
  assert.equal(L.parseLocalId('?localId=<script>'), null);
});

test('validateSelection requires a known category', () => {
  assert.deepEqual(L.validateSelection({}), { valid: false, reason: 'no-category' });
  assert.deepEqual(L.validateSelection({ category: 'Nada' }), { valid: false, reason: 'no-category' });
  assert.equal(L.validateSelection({ category: 'Farmacia' }).valid, true);
});

test('validateSelection checks free text for Otro', () => {
  assert.equal(L.validateSelection({ category: L.OTHER, otherText: '' }).reason, 'other-too-short');
  assert.equal(L.validateSelection({ category: L.OTHER, otherText: ' a ' }).reason, 'other-too-short');
  assert.equal(L.validateSelection({ category: L.OTHER, otherText: 'x'.repeat(61) }).reason, 'other-too-long');
  assert.equal(L.validateSelection({ category: L.OTHER, otherText: 'Gimnasio' }).valid, true);
});

test('buildVotePayload with a chip category', () => {
  const p = L.buildVotePayload(base);
  assert.deepEqual(p, {
    localId: 'shop-1', category: 'Kiosco', frequency: 'WEEKLY',
    scannedAt: base.scannedAt, clientToken: base.clientToken,
    fingerprintHash: 'abc', submittedAt: base.submittedAt
  });
  assert.ok(!('otherText' in p));
});

test('buildVotePayload with Otro uses otherText only and cleans it', () => {
  const p = L.buildVotePayload({ ...base, category: L.OTHER, otherText: '  Gim\tnasio \n ' });
  assert.equal(p.otherText, 'Gim nasio');
  assert.ok(!('category' in p));
});

test('buildVotePayload sends null when frequency is omitted or unknown', () => {
  assert.equal(L.buildVotePayload({ ...base, frequency: undefined }).frequency, null);
  assert.equal(L.buildVotePayload({ ...base, frequency: 'HOURLY' }).frequency, null);
});

test('buildVotePayload throws on invalid selection', () => {
  assert.throws(() => L.buildVotePayload({ ...base, category: null }), /no-category/);
});

test('payload carries no personal-data fields', () => {
  const keys = Object.keys(L.buildVotePayload(base)).sort();
  assert.deepEqual(keys, ['category', 'clientToken', 'fingerprintHash', 'frequency', 'localId', 'scannedAt', 'submittedAt']);
});

const env = { userAgent: 'UA', language: 'es-AR', screen: '390x844', timezone: 'America/Argentina/Buenos_Aires' };

test('fingerprint SHA-256 is deterministic and matches reference', async () => {
  const a = await L.hashFingerprint(env, webcrypto.subtle);
  const b = await L.hashFingerprint({ ...env }, webcrypto.subtle);
  assert.equal(a, b);
  assert.match(a, /^[0-9a-f]{64}$/);
  const ref = Buffer.from(await webcrypto.subtle.digest('SHA-256', Buffer.from('UA|es-AR|390x844|America/Argentina/Buenos_Aires'))).toString('hex');
  assert.equal(a, ref);
});

test('fingerprint changes with any input', async () => {
  const a = await L.hashFingerprint(env, webcrypto.subtle);
  assert.notEqual(a, await L.hashFingerprint({ ...env, language: 'en' }, webcrypto.subtle));
  assert.notEqual(a, await L.hashFingerprint({ ...env, screen: '1x1' }, webcrypto.subtle));
});

test('fingerprint falls back when subtle is missing or fails', async () => {
  const fb = await L.hashFingerprint(env, null);
  assert.match(fb, /^[0-9a-f]{64}$/);
  assert.equal(fb, await L.hashFingerprint(env, null));
  const failing = { digest: () => Promise.reject(new Error('x')) };
  assert.equal(await L.hashFingerprint(env, failing), fb);
  assert.notEqual(fb, L.fallbackHash('other'));
});

test('token is generated once and persisted in storage and cookie', () => {
  const storage = memStorage();
  const cookies = memJar();
  const t1 = L.getOrCreateToken({ storage, cookies, crypto: webcrypto });
  assert.match(t1, /^[0-9a-f-]{36}$/);
  assert.equal(storage.dump()[L.TOKEN_KEY], t1);
  const t2 = L.getOrCreateToken({ storage, cookies, crypto: webcrypto });
  assert.equal(t1, t2);
});

test('token restored from cookie when localStorage is empty', () => {
  const cookies = memJar();
  const t1 = L.getOrCreateToken({ storage: memStorage(), cookies, crypto: webcrypto });
  const storage = memStorage();
  assert.equal(L.getOrCreateToken({ storage, cookies, crypto: webcrypto }), t1);
  assert.equal(storage.dump()[L.TOKEN_KEY], t1);
});

test('token works when storage throws, using cookie only', () => {
  const cookies = memJar();
  const t1 = L.getOrCreateToken({ storage: brokenStorage(), cookies, crypto: webcrypto });
  assert.equal(L.getOrCreateToken({ storage: brokenStorage(), cookies, crypto: webcrypto }), t1);
});

test('corrupt stored token is replaced', () => {
  const storage = memStorage({ [L.TOKEN_KEY]: 'garbage' });
  const t = L.getOrCreateToken({ storage, cookies: memJar(), crypto: webcrypto });
  assert.notEqual(t, 'garbage');
  assert.equal(storage.dump()[L.TOKEN_KEY], t);
});

test('randomUuid fallback without randomUUID is v4-shaped', () => {
  const u = L.randomUuid({ getRandomValues: (a) => webcrypto.getRandomValues(a) });
  assert.match(u, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
  assert.match(L.randomUuid(null), /^[0-9a-f]{8}-[0-9a-f]{4}-4/);
});

test('mapVoteResponse', () => {
  assert.equal(L.mapVoteResponse(201), 'success');
  assert.equal(L.mapVoteResponse(409), 'duplicate');
  assert.equal(L.mapVoteResponse(429), 'ratelimit');
  assert.equal(L.mapVoteResponse(404), 'invalid');
  assert.equal(L.mapVoteResponse(422), 'invalid');
  assert.equal(L.mapVoteResponse(500), 'error');
  assert.equal(L.mapVoteResponse(503), 'error');
  assert.equal(L.mapVoteResponse(0), 'error');
});

test('mapReportResponse', () => {
  assert.equal(L.mapReportResponse(204), 'success');
  assert.equal(L.mapReportResponse(200), 'success');
  assert.equal(L.mapReportResponse(409), 'duplicate');
  assert.equal(L.mapReportResponse(429), 'ratelimit');
  assert.equal(L.mapReportResponse(404), 'invalid');
  assert.equal(L.mapReportResponse(502), 'error');
});
