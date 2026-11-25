(function () {
  'use strict';

  var API_BASE = window.VIDRIERAPP_API_BASE || '';
  var REQUEST_TIMEOUT_MS = 10000;

  var STRINGS = {
    title: '¿Qué te gustaría ver en este local?',
    lead: 'Un toque y listo. Tu voto es anónimo.',
    category: 'Rubro',
    other: 'Otro',
    otherLabel: '¿Qué rubro?',
    otherPlaceholder: 'Escribí el rubro',
    frequency: '¿Con qué frecuencia pasarías?',
    optional: '(opcional)',
    freq: { DAILY: 'Diario', WEEKLY: 'Semanal', OCCASIONAL: 'Ocasional' },
    submit: 'Confirmar voto',
    sending: 'Enviando…',
    hintChoose: 'Elegí un rubro para continuar.',
    hintOtherShort: 'Escribí al menos 2 letras.',
    hintOtherLong: 'Máximo 60 caracteres.',
    privacy: 'No pedimos nombre, mail ni ubicación.',
    reportLink: '¿Este local ya se alquiló o no tiene cartel?',
    reportTitle: '¿Reportar este local?',
    reportBody: 'Vamos a avisar que el local ya se alquiló o no tiene cartel. No hace falta que dejes ningún dato.',
    reportConfirm: 'Sí, reportar',
    reportCancel: 'Cancelar',
    demo: 'Modo demo: no se envía nada al servidor.',
    states: {
      success: { icon: '✓', title: '¡Gracias por tu voto!', body: 'Tu opinión ayuda a elegir mejor qué abrir en este local.' },
      duplicate: { icon: 'i', title: 'Ya registramos tu voto para este local, gracias', body: 'No hace falta que votes de nuevo.' },
      ratelimit: { icon: '!', title: 'Demasiados intentos', body: 'Probá de nuevo en unos minutos.', bad: true },
      error: { icon: '!', title: 'No pudimos enviar tu voto', body: 'Revisá tu conexión y volvé a intentar.', bad: true, retry: 'Reintentar' },
      invalid: { icon: '!', title: 'No encontramos este local', body: 'Escaneá de nuevo el código QR del cartel.', bad: true },
      missing: { icon: '!', title: 'Este enlace no es válido', body: 'Escaneá el código QR del cartel del local para votar.', bad: true },
      reportSuccess: { icon: '✓', title: '¡Gracias por avisarnos!', body: 'Vamos a revisar este local.' },
      reportDuplicate: { icon: 'i', title: 'Ya recibimos tu aviso', body: 'Gracias, lo vamos a revisar.' },
      reportError: { icon: '!', title: 'No pudimos enviar el aviso', body: 'Revisá tu conexión y volvé a intentar.', bad: true, retry: 'Reintentar' }
    },
    offline: 'Parece que no tenés conexión. Tu voto no se guardó; podés reintentar cuando vuelva.'
  };

  var L = window.VoteLogic;
  var $ = function (id) { return document.getElementById(id); };
  var params = new URLSearchParams(location.search);
  var localId = L.parseLocalId(location.search);
  var scannedAt = new Date().toISOString();
  var selection = { category: null, otherText: '', frequency: null };
  var busy = false;
  var lastAction = null;
  var mockMode = params.has('mock') || window.VIDRIERAPP_MOCK === true;
  var mockScenario = params.get('mock') || 'ok';

  function enc(id) { return encodeURIComponent(id); }

  function mockFetch(url, opts) {
    var failedOnce = mockFetch.failedOnce;
    var isReport = /\/report-rented$/.test(url);
    var status = isReport ? 204 : 201;
    var scenario = mockScenario;
    if (scenario === 'duplicate') status = 409;
    else if (scenario === 'ratelimit') status = 429;
    else if (scenario === 'error' && !failedOnce) {
      mockFetch.failedOnce = true;
      return new Promise(function (_, reject) { setTimeout(function () { reject(new TypeError('mock network error')); }, 400); });
    }
    return new Promise(function (resolve) {
      setTimeout(function () { resolve({ status: status, ok: status < 300 }); }, 400);
    });
  }

  function http(path, body) {
    var doFetch = mockMode ? mockFetch : window.fetch.bind(window);
    var ctrl = typeof AbortController === 'function' ? new AbortController() : null;
    var timer = ctrl ? setTimeout(function () { ctrl.abort(); }, REQUEST_TIMEOUT_MS) : null;
    return doFetch(API_BASE + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify(body || {}),
      credentials: 'omit',
      signal: ctrl ? ctrl.signal : undefined
    }).then(function (r) { clearTimeout(timer); return r.status; },
            function (e) { clearTimeout(timer); throw e; });
  }

  function show(view) {
    $('view-form').hidden = view !== 'form';
    $('view-result').hidden = view !== 'result';
  }

  function showState(key, extra) {
    var s = STRINGS.states[key];
    $('r-icon').textContent = s.icon;
    $('r-icon').className = 'icon' + (s.bad ? ' bad' : '');
    $('r-title').textContent = s.title;
    $('r-body').textContent = s.body;
    var note = $('r-note');
    note.hidden = !extra;
    note.textContent = extra || '';
    var act = $('r-action');
    act.hidden = !s.retry;
    act.textContent = s.retry || '';
    if (document.activeElement && document.activeElement.blur) document.activeElement.blur();
    show('result');
    $('view-result').focus();
  }

  function hintFor(check) {
    if (check.valid) return '';
    if (check.reason === 'other-too-short') return STRINGS.hintOtherShort;
    if (check.reason === 'other-too-long') return STRINGS.hintOtherLong;
    return STRINGS.hintChoose;
  }

  function refresh() {
    var check = L.validateSelection(selection);
    $('submit').disabled = !check.valid || busy;
    $('hint').textContent = busy ? '' : hintFor(check);
    $('other-wrap').hidden = selection.category !== L.OTHER;
  }

  function buildChips() {
    var box = $('chips');
    L.RUBROS.concat([L.OTHER]).forEach(function (r) {
      var label = document.createElement('label');
      label.className = 'chip';
      var input = document.createElement('input');
      input.type = 'radio';
      input.name = 'category';
      input.value = r;
      var span = document.createElement('span');
      span.textContent = r === L.OTHER ? STRINGS.other : r;
      label.appendChild(input);
      label.appendChild(span);
      box.appendChild(label);
    });
    box.addEventListener('change', function (e) {
      selection.category = e.target.value;
      if (selection.category === L.OTHER) setTimeout(function () { $('other').focus(); }, 0);
      refresh();
    });
    var dl = $('rubros');
    L.KNOWN_RUBROS.forEach(function (r) {
      var o = document.createElement('option');
      o.value = r;
      dl.appendChild(o);
    });
  }

  function buildFrequency() {
    var box = $('freq');
    box.setAttribute('aria-labelledby', 't-frequency');
    L.FREQUENCIES.forEach(function (f) {
      var b = document.createElement('button');
      b.type = 'button';
      b.dataset.value = f;
      b.setAttribute('aria-pressed', 'false');
      b.textContent = STRINGS.freq[f];
      box.appendChild(b);
    });
    box.addEventListener('click', function (e) {
      var b = e.target.closest('button');
      if (!b) return;
      selection.frequency = selection.frequency === b.dataset.value ? null : b.dataset.value;
      Array.prototype.forEach.call(box.children, function (c) {
        c.setAttribute('aria-pressed', String(c.dataset.value === selection.frequency));
      });
    });
  }

  function fingerprintEnv() {
    var tz = '';
    try { tz = Intl.DateTimeFormat().resolvedOptions().timeZone || ''; } catch (e) {}
    return {
      userAgent: navigator.userAgent,
      language: navigator.language,
      screen: screen.width + 'x' + screen.height,
      timezone: tz
    };
  }

  function offlineNote() {
    return navigator.onLine === false ? STRINGS.offline : '';
  }

  function submitVote() {
    if (busy || !L.validateSelection(selection).valid) return;
    busy = true;
    $('submit').textContent = STRINGS.sending;
    refresh();
    var subtle = window.crypto && window.crypto.subtle;
    L.hashFingerprint(fingerprintEnv(), subtle).then(function (fp) {
      var payload = L.buildVotePayload({
        localId: localId,
        category: selection.category,
        otherText: selection.otherText,
        frequency: selection.frequency,
        scannedAt: scannedAt,
        clientToken: L.getOrCreateToken({
          storage: safeStorage(),
          cookies: cookieJar(),
          crypto: window.crypto
        }),
        fingerprintHash: fp,
        submittedAt: new Date().toISOString()
      });
      return http('/api/v1/votes', payload);
    }).then(function (status) {
      var kind = L.mapVoteResponse(status);
      if (kind === 'error') throw new Error('http');
      showState(kind);
    }).catch(function () {
      lastAction = submitVote;
      showState('error', offlineNote());
    }).then(function () {
      busy = false;
      $('submit').textContent = STRINGS.submit;
      refresh();
    });
  }

  function sendReport() {
    if (busy) return;
    busy = true;
    http('/api/v1/shops/' + enc(localId) + '/report-rented', {}).then(function (status) {
      var kind = L.mapReportResponse(status);
      if (kind === 'error') throw new Error('http');
      showState(kind === 'success' ? 'reportSuccess' : kind === 'duplicate' ? 'reportDuplicate' : kind);
    }).catch(function () {
      lastAction = sendReport;
      showState('reportError', offlineNote());
    }).then(function () { busy = false; });
  }

  function safeStorage() {
    try { return window.localStorage; } catch (e) { return null; }
  }

  function cookieJar() {
    return {
      get: function () { return document.cookie; },
      set: function (v) { document.cookie = v; }
    };
  }

  function bind() {
    $('t-title').textContent = STRINGS.title;
    $('t-lead').textContent = STRINGS.lead;
    $('t-category').textContent = STRINGS.category;
    $('t-other-label').textContent = STRINGS.otherLabel;
    $('other').placeholder = STRINGS.otherPlaceholder;
    $('t-frequency').textContent = STRINGS.frequency;
    $('t-optional').textContent = STRINGS.optional;
    $('submit').textContent = STRINGS.submit;
    $('t-privacy').textContent = STRINGS.privacy;
    $('open-report').textContent = STRINGS.reportLink;
    $('d-title').textContent = STRINGS.reportTitle;
    $('d-body').textContent = STRINGS.reportBody;
    $('d-confirm').textContent = STRINGS.reportConfirm;
    $('d-cancel').textContent = STRINGS.reportCancel;
    if (mockMode) {
      $('demo').hidden = false;
      $('demo').textContent = STRINGS.demo;
    }
    buildChips();
    buildFrequency();
    $('other').addEventListener('input', function (e) {
      selection.otherText = e.target.value;
      refresh();
    });
    $('form').addEventListener('submit', function (e) {
      e.preventDefault();
      submitVote();
    });
    $('r-action').addEventListener('click', function () {
      show('form');
      if (lastAction) lastAction();
    });
    var dlg = $('dlg');
    $('open-report').addEventListener('click', function () {
      if (dlg.showModal) dlg.showModal(); else dlg.setAttribute('open', '');
    });
    $('d-cancel').addEventListener('click', function () { dlg.close(); });
    $('d-confirm').addEventListener('click', function () {
      dlg.close();
      lastAction = sendReport;
      sendReport();
    });
    refresh();
  }

  document.documentElement.lang = 'es';
  bind();
  if (!localId) {
    showState('missing');
  } else {
    show('form');
  }
})();
