# VidrierApp - Web de voto (WEB-01)

Pantalla unica, estatica y sin dependencias para que el vecino vote el rubro de un local al escanear el QR.

## Ejecutar

```
python3 -m http.server -d web 8080
```

- Voto real: `http://localhost:8080/index.html?localId=shop-mataderos-01` (la API se llama en el mismo origen).
- Demo sin backend: agregar `&mock=1`.
- Escenarios de demo: `&mock=duplicate` (409), `&mock=ratelimit` (429), `&mock=error` (falla de red la primera vez, funciona al reintentar).
- Sin `localId` o con un `localId` invalido se muestra un estado de error amigable.

## API base

Por defecto es el mismo origen. Para apuntar a otro host, definir antes de `app.js`:

```
<script>window.VIDRIERAPP_API_BASE = 'https://api.vidrierapp.app';</script>
```

El modo mock solo se activa con `?mock=...` o `window.VIDRIERAPP_MOCK = true`; nunca por fallback automatico.

## Contrato

`POST /api/v1/votes`

```
{
  "localId": "shop-mataderos-01",
  "category": "Kiosco",
  "frequency": "WEEKLY",
  "scannedAt": "2026-11-25T10:00:00.000Z",
  "clientToken": "uuid-v4",
  "fingerprintHash": "sha256-hex",
  "submittedAt": "2026-11-25T10:00:05.000Z"
}
```

- Con "Otro" se envia `otherText` en lugar de `category`.
- `frequency` es `DAILY`, `WEEKLY`, `OCCASIONAL` o `null`.
- Respuestas: 201 gracias, 409 duplicado, 429 limite, 400/404/422 local invalido, otro o falla de red: reintento.

`POST /api/v1/shops/{localId}/report-rented` con cuerpo `{}`: 200/201/202/204 gracias, 409 ya reportado, 429 limite, 404 local invalido.

El control anti-duplicados vive en el backend (BE-02).

## Tests

```
node --test web/test/*.test.mjs
```

## Archivos

- `index.html`, `styles.css`, `app.js`: pagina y UI (textos en el objeto `STRINGS`).
- `vote-logic.js`: logica pura (UMD, usable en navegador y Node).
- `test/vote-logic.test.mjs`: tests unitarios.
