(function () {
  'use strict';

  var API = '/api/v1';
  var SESSION_KEY = 'vidrierapp.admin.session';
  var DEFAULT_CENTER = [-34.6555, -58.5035];
  var state = { resolutionId: null, session: readSession(), map: null, pin: null, zone: null };

  function $(id) { return document.getElementById(id); }

  function readSession() {
    try { return JSON.parse(sessionStorage.getItem(SESSION_KEY)); } catch (_) { return null; }
  }

  function setSession(session) {
    state.session = session;
    if (session) sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
    else sessionStorage.removeItem(SESSION_KEY);
  }

  function say(id, text, isError) {
    var el = $(id);
    el.textContent = text;
    el.className = 'hint' + (isError ? ' error' : '');
  }

  function call(path, method, body) {
    var headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    if (state.session) headers.Authorization = 'Bearer ' + state.session.token;
    return fetch(API + path, { method: method, headers: headers, body: body ? JSON.stringify(body) : undefined })
      .then(function (res) {
        if (res.status === 204) return null;
        return res.json().catch(function () { return {}; }).then(function (data) {
          if (!res.ok) throw new Error(data.message || ('HTTP ' + res.status));
          return data;
        });
      });
  }

  function showLogin(message) {
    $('login-view').hidden = false;
    $('admin-view').hidden = true;
    if (message) say('login-out', message, true);
  }

  function showAdmin() {
    if (!state.session || state.session.role !== 'ADMIN') { showLogin('Esta consola requiere un perfil Administrador.'); return; }
    $('login-view').hidden = true;
    $('admin-view').hidden = false;
    $('session-user').textContent = state.session.username + ' · Administrador';
    if (state.map) state.map.invalidateSize(); else initMap();
    loadShops();
  }

  function login(event) {
    event.preventDefault();
    var form = $('login-form').elements;
    say('login-out', '');
    call('/auth/login', 'POST', { username: form.username.value.trim(), password: form.password.value })
      .then(function (session) {
        if (session.role !== 'ADMIN') throw new Error('Esta consola requiere un perfil Administrador.');
        setSession(session);
        form.password.value = '';
        showAdmin();
      })
      .catch(function (error) { say('login-out', error.message, true); });
  }

  function logout() { setSession(null); state.resolutionId = null; showLogin(); }

  function formData() {
    var f = $('shop-form').elements;
    var shop = {
      address: f.address.value.trim(),
      latitude: parseFloat(f.latitude.value),
      longitude: parseFloat(f.longitude.value),
      neighborhood: f.neighborhood.value.trim(),
      surfaceAreaM2: parseInt(f.surfaceAreaM2.value || '0', 10),
      rentalPrice: f.rentalPrice.value.trim(),
      agency: f.agency.value.trim(),
      contactPhone: f.contactPhone.value.trim(),
      contactEmail: f.contactEmail.value.trim(),
      confirmFarLocation: f.confirmFarLocation.checked
    };
    if (state.resolutionId) shop.geoResolutionId = state.resolutionId;
    return shop;
  }

  function initMap() {
    if (typeof L === 'undefined') {
      say('resolve-out', 'No se pudo cargar el mapa. Revisá la conexión a internet.', true);
      return;
    }
    state.map = L.map('map').setView(DEFAULT_CENTER, 14);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap'
    }).addTo(state.map);
    state.map.on('click', function (ev) { placePin(ev.latlng); });
  }

  function placePin(latlng) {
    if (!state.pin) {
      state.pin = L.marker(latlng, { draggable: true }).addTo(state.map);
      state.pin.on('dragend', function () { placePin(state.pin.getLatLng()); });
    } else {
      state.pin.setLatLng(latlng);
    }
    var f = $('shop-form').elements;
    f.latitude.value = latlng.lat.toFixed(6);
    f.longitude.value = latlng.lng.toFixed(6);
    var text = 'Pin en ' + f.latitude.value + ', ' + f.longitude.value + '.';
    if (state.zone) {
      var meters = Math.round(state.map.distance(latlng, state.zone.getLatLng()));
      text += ' Está a ' + meters + ' m de la dirección.';
      if (meters > state.zone.getRadius()) text += ' Para guardarlo, marcá la confirmación de abajo.';
    }
    say('pin-out', text);
    $('create').disabled = false;
  }

  function clearPin() {
    if (state.pin) { state.map.removeLayer(state.pin); state.pin = null; }
    var f = $('shop-form').elements;
    f.latitude.value = '';
    f.longitude.value = '';
    say('pin-out', 'Sin pin todavía.');
    $('create').disabled = true;
  }

  function resolveLocation() {
    var f = $('shop-form').elements;
    var address = f.address.value.trim();
    if (!address) { say('resolve-out', 'Escribí una dirección.', true); return; }
    if (!state.map) return;
    say('resolve-out', 'Buscando…');
    call('/admin/geo/resolve', 'POST', { address: address }).then(function (r) {
      state.resolutionId = r.resolutionId;
      var center = [r.latitude, r.longitude];
      if (state.zone) state.map.removeLayer(state.zone);
      state.zone = L.circle(center, { radius: r.radiusMeters || 150, color: '#3b2f6b', weight: 2, dashArray: '6 6', fillOpacity: 0.08 }).addTo(state.map);
      state.map.fitBounds(state.zone.getBounds(), { padding: [20, 20] });
      clearPin();
      say('resolve-out', (r.formattedAddress || address) + '. Tocá el mapa dentro del círculo, en el punto exacto del local.');
    }).catch(function (e) {
      state.resolutionId = null;
      say('resolve-out', 'No se pudo ubicar la dirección (' + e.message + '). Podés marcar el pin a mano y confirmar la ubicación.', true);
    });
  }

  function createShop(ev) {
    ev.preventDefault();
    call('/admin/shops', 'POST', formData()).then(function (shop) {
      say('form-out', 'Local creado: ' + shop.address);
      state.resolutionId = null;
      if (state.zone) { state.map.removeLayer(state.zone); state.zone = null; }
      clearPin();
      $('shop-form').reset();
      loadShops();
      showPoster(shop.id, shop.address);
    }).catch(function (e) { say('form-out', e.message, true); });
  }

  function loadShops() {
    call('/shops', 'GET').then(function (shops) {
      var body = $('shops');
      body.textContent = '';
      shops.forEach(function (s) { body.appendChild(row(s)); });
      say('list-out', shops.length + ' locales disponibles.');
    }).catch(function (e) { say('list-out', e.message, true); if (/Unauthorized|token/i.test(e.message)) logout(); });
  }

  function actionButton(label, secondary, handler) {
    var b = document.createElement('button');
    b.type = 'button'; b.textContent = label;
    if (secondary) b.className = 'secondary';
    b.addEventListener('click', handler);
    return b;
  }

  function row(shop) {
    var tr = document.createElement('tr');
    [shop.address, shop.neighborhood, String(shop.totalVotes || 0)].forEach(function (text) {
      var td = document.createElement('td'); td.textContent = text; tr.appendChild(td);
    });
    var cell = document.createElement('td'); var box = document.createElement('div'); box.className = 'actions';
    box.appendChild(actionButton('QR', false, function () { showPoster(shop.id, shop.address); }));
    box.appendChild(actionButton('Revalidar 60 días', true, function () { setStatus(shop.id, 'AVAILABLE'); }));
    box.appendChild(actionButton('En revisión', true, function () { setStatus(shop.id, 'UNDER_REVIEW'); }));
    box.appendChild(actionButton('Alquilado', true, function () { setStatus(shop.id, 'RENTED'); }));
    cell.appendChild(box); tr.appendChild(cell); return tr;
  }

  function setStatus(id, status) { call('/admin/shops/' + id + '/status', 'POST', { status: status }).then(loadShops).catch(function (e) { say('list-out', e.message, true); }); }
  function runMaintenance() { call('/admin/maintenance/run', 'POST').then(function (r) { say('list-out', 'Locales caducados: ' + r.expiredShops + '.'); loadShops(); }).catch(function (e) { say('list-out', e.message, true); }); }
  function voteUrl(id) { return location.origin + '/?localId=' + encodeURIComponent(id); }
  function showPoster(id, address) {
    if (typeof qrcode !== 'function') { say('list-out', 'Falta web/admin/qrcode.js (librería de QR).', true); return; }
    var qr = qrcode(0, 'M'); qr.addData(voteUrl(id)); qr.make();
    var img = document.createElement('img'); img.alt = 'Código QR para votar en ' + address; img.src = qr.createDataURL(6, 8);
    var holder = $('qr'); holder.textContent = ''; holder.appendChild(img);
    $('poster-url').textContent = address + ' · ' + voteUrl(id); $('poster-card').hidden = false; $('poster-card').scrollIntoView({ behavior: 'smooth' });
  }

  $('login-form').addEventListener('submit', login);
  $('logout').addEventListener('click', logout);
  $('shop-form').addEventListener('submit', createShop);
  $('shop-form').elements.address.addEventListener('keydown', function (ev) {
    if (ev.key === 'Enter') { ev.preventDefault(); resolveLocation(); }
  });
  $('resolve').addEventListener('click', resolveLocation);
  $('reload').addEventListener('click', loadShops);
  $('maintenance').addEventListener('click', runMaintenance);
  $('print').addEventListener('click', function () { window.print(); });
  if (state.session) showAdmin(); else showLogin();
}());
