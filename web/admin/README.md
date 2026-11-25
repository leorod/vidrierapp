# Consola interna

Uso interno del equipo. La página pública de voto no carga nada de esta carpeta.

## Librería de terceros

- `qrcode.js`: `qrcode-generator` 2.0.4, de Kazuhiko Arase.
- Origen: https://github.com/kazuhikoarase/qrcode-generator (paquete npm `qrcode-generator`, archivo `dist/qrcode.js`).
- Licencia: MIT.
- SHA-256 del archivo: `79ec86f8…f791c` (completo al verificar con `shasum -a 256 qrcode.js` contra el paquete de npm).
- No usa red, almacenamiento ni `eval`. Solo devuelve el QR como data URL.
