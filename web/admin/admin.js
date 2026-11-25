(function () {
  'use strict';

  var API = '/api/v1';
  var state = { resolutionId: null };

  function $(id) { return document.getElementById(id); }

  function say(id, text, isError) {
    var el = $(id);
    el.textContent = text;
    el.className = 'hint' + (isError ? ' error' : '');
  }

  function call(path, method, body) {
    var headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    var token = $('token').value.trim();
    if (token) headers.Authorization = 'Bearer ' + token;
    return fetch(API + path, {
      method: method,
      headers: headers,
      body: body ? JSON.stringify(body) : undefined
    }).then(function (res) {
      if (res.status === 204) return null;
      return res.json().catch(function () { return {}; }).then(function (data) {
        if (!res.ok) throw new Error(data.message || ('HTTP ' + res.status));
        return data;
      });
    });
  }

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
    if (f.placeId.value.trim()) shop.placeId = f.placeId.value.trim();
    if (state.resolutionId) shop.geoResolutionId = state.resolutionId;
    return shop;
  }

  function resolveLocation() {
    var placeId = $('shop-form').elements.placeId.value.trim();
    if (!placeId) { say('resolve-out', 'Ingresá un Place ID.', true); return; }
    call('/admin/geo/resolve', 'POST', { placeId: placeId }).then(function (r) {
      state.resolutionId = r.resolutionId;
      var f = $('shop-form').elements;
      f.latitude.value = r.latitude;
      f.longitude.value = r.longitude;
      say('resolve-out', 'Centro resuelto. Ajustá el pin a menos de ' + r.radiusMeters + ' m.');
    }).catch(function (e) { say('resolve-out', e.message, true); });
  }

  function createShop(ev) {
    ev.preventDefault();
    call('/admin/shops', 'POST', formData()).then(function (shop) {
      say('form-out', 'Local creado: ' + shop.address);
      state.resolutionId = null;
      loadShops();
      showPoster(shop.id, shop.address);
    }).catch(function (e) { say('form-out', e.message, true); });
  }

  function loadShops() {
    fetch(API + '/shops').then(function (r) { return r.json(); }).then(function (shops) {
      var body = $('shops');
      body.textContent = '';
      shops.forEach(function (s) { body.appendChild(row(s)); });
      say('list-out', shops.length + ' locales disponibles.');
    }).catch(function (e) { say('list-out', e.message, true); });
  }

  function actionButton(label, secondary, handler) {
    var b = document.createElement('button');
    b.type = 'button';
    b.textContent = label;
    if (secondary) b.className = 'secondary';
    b.addEventListener('click', handler);
    return b;
  }

  function row(shop) {
    var tr = document.createElement('tr');
    [shop.address, shop.neighborhood, String(shop.totalVotes || 0)].forEach(function (text) {
      var td = document.createElement('td');
      td.textContent = text;
      tr.appendChild(td);
    });
    var cell = document.createElement('td');
    var box = document.createElement('div');
    box.className = 'actions';
    box.appendChild(actionButton('QR', false, function () { showPoster(shop.id, shop.address); }));
    box.appendChild(actionButton('Revalidar 60 días', true, function () { setStatus(shop.id, 'AVAILABLE'); }));
    box.appendChild(actionButton('En revisión', true, function () { setStatus(shop.id, 'UNDER_REVIEW'); }));
    box.appendChild(actionButton('Alquilado', true, function () { setStatus(shop.id, 'RENTED'); }));
    cell.appendChild(box);
    tr.appendChild(cell);
    return tr;
  }

  function setStatus(id, status) {
    call('/admin/shops/' + id + '/status', 'POST', { status: status })
      .then(loadShops)
      .catch(function (e) { say('list-out', e.message, true); });
  }

  function runMaintenance() {
    call('/admin/maintenance/run', 'POST').then(function (r) {
      say('list-out', 'Locales caducados: ' + r.expiredShops + '.');
      loadShops();
    }).catch(function (e) { say('list-out', e.message, true); });
  }

  function voteUrl(id) {
    return location.origin + '/?localId=' + encodeURIComponent(id);
  }

  function showPoster(id, address) {
    if (typeof qrcode !== 'function') {
      say('list-out', 'Falta web/admin/qrcode.js (librería de QR).', true);
      return;
    }
    var qr = qrcode(0, 'M');
    qr.addData(voteUrl(id));
    qr.make();
    var img = document.createElement('img');
    img.alt = 'Código QR para votar en ' + address;
    img.src = qr.createDataURL(6, 8);
    var holder = $('qr');
    holder.textContent = '';
    holder.appendChild(img);
    $('poster-url').textContent = address + ' · ' + voteUrl(id);
    $('poster-card').hidden = false;
    $('poster-card').scrollIntoView({ behavior: 'smooth' });
  }

  $('shop-form').addEventListener('submit', createShop);
  $('resolve').addEventListener('click', resolveLocation);
  $('reload').addEventListener('click', loadShops);
  $('maintenance').addEventListener('click', runMaintenance);
  $('print').addEventListener('click', function () { window.print(); });
  loadShops();
}());
