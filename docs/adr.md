# VidrierApp — Bitácora de Decisiones de Arquitectura y Producto (ADR)

**Proyecto:** VidrierApp

**Estado:** Documento de Trabajo / Arquitectura Aprobada

**Última actualización:** Octubre 2026

---

## 1. Visión de Producto y Modelo de Negocio

### 1.1. Arquitectura Dual de Experiencia (App Nativa vs. Web Lite)

* **Decisión:** Dividir el producto en dos interfaces independientes según el perfil de usuario.
* **Vecinos (Captura de demanda):** Web App *responsive* liviana accesible exclusivamente vía escaneo de código QR físico, sin requerir descarga ni registro previo.
* **Emprendedores (Prospección y análisis):** Aplicación móvil nativa Android con capacidades de mapa interactivo, persistencia local y procesamiento de datos.


* **Motivación:** Eliminar la fricción de adopción en el transeúnte casual. La descarga de una aplicación para realizar una acción de 10 segundos genera una tasa de abandono superior al 90%. Para el emprendedor, la aplicación nativa justifica su instalación al ofrecer herramientas avanzadas de trabajo de campo (cámara, notas, mapas y persistencia offline).

### 1.2. Esquema de Monetización por Pase Temporal

* **Decisión:** Implementar un periodo de prueba completo de 3 días (*Free Trial*) por dispositivo/cuenta, seguido de la compra de un **Pase de 30 días** de pago único (sin renovación automática).
* **Motivación:** Evitar el riesgo de *churn* inmediato donde el usuario extrae la información del mapa en una única sesión y desinstala la aplicación. Un periodo de 3 días permite validar el valor del servicio durante las etapas iniciales de prospección. La compra de un pase de 30 días se alinea con el ciclo medio real de búsqueda y evaluación de inmuebles comerciales.

### 1.3. Estructuración de Métricas de Alta Conversión

* **Decisión:** Acompañar el volumen total de votos por rubro con indicadores de frecuencia estimada de compra (diaria, semanal, mensual).
* **Motivación:** Los datos puramente cuantitativos de preferencia no aseguran viabilidad financiera. Medir la intencionalidad de consumo recurrente (ej. *21 de 34 votantes afirman que asistirían diariamente*) proporciona una métrica de conversión real para la evaluación de riesgo del inversor.

---

## 2. Arquitectura de Software y Patrones de Diseño

### 2.1. Adopción de Clean Architecture + MVVM + UDF

* **Decisión:** Estructurar el código de la aplicación Android nativa bajo Clean Architecture con el patrón de diseño MVVM y Flujo Unidireccional de Datos (UDF).
* **Motivación:** Garantizar la separación de responsabilidades, testabilidad y mantenibilidad a largo plazo.
* **Capa de Presentación:** UI puramente declarativa en Jetpack Compose impulsada por un `UiState` inmutable expuesto vía `StateFlow` desde el `ViewModel`.
* **Capa de Dominio:** Módulo de Kotlin puro sin dependencias del SDK de Android, que concentra la lógica central del negocio (`UseCases`) y los modelos de dominio.
* **Capa de Datos:** Implementación del patrón *Repository*, aislando las fuentes de datos remotas (`Retrofit`) y locales (`Room`).



### 2.2. Inversión de Dependencias y Abstracción de Repositorios

* **Decisión:** La capa de Dominio define las interfaces de los repositorios (`ShopRepository`, `VisitRepository`), mientras que la capa de Datos se encarga de sus implementaciones concretas.
* **Motivación:** Desacoplar la lógica de negocio de la infraestructura tecnológica. Esto permite conmutar o simular (*mockear*) las fuentes de datos durante las pruebas unitarias e integraciones sin alterar las reglas del negocio.

### 2.3. Mapeo Aislado de Datos (DTO / Entity $\rightarrow$ Domain)

* **Decisión:** Implementar transformadores explícitos (*Mappers*) entre las entidades de red (`ShopDto`), almacenamiento local (`VisitEntity`) y los modelos puros de Dominio (`Shop`, `Visit`).
* **Motivación:** Prevenir la propagación de cambios en las respuestas JSON de las API o esquemas de base de datos hacia la interfaz de usuario y la lógica de negocio.

### 2.4. Creación de ViewModels y Navegación Inferior

* **Decisión:** (a) Todos los ViewModels se crean con una única factory, `AppViewModelProvider.Factory` (`viewModelFactory { initializer { ... } }`), que obtiene los UseCases de `AppContainer` vía `VidrierAppApplication.container`; las pantallas la usan con `viewModel(factory = AppViewModelProvider.Factory)`. (b) La barra inferior tiene dos pestañas de nivel superior, "Explorar" (`MapDestination`) y "Bitácora" (`VisitsHubDestination`), visibles solo cuando el tope del back stack es una de ellas; al cambiar de pestaña se reemplaza el back stack por ese único destino. (c) El FAB "Nueva visita" abre `LogVisitDestination` con `shopId` vacío (`NO_SHOP_ID`) hasta que VIS-03 agregue `shopId` nulo y dirección manual.
* **Motivación:** (a) Definir el patrón una sola vez, sin Hilt ni Koin, para que todas las pantallas lo reutilicen. (b) Es lo más simple: un único back stack sin estado por pestaña, y el detalle o el registro de visita ocultan la barra. (c) Permite entregar el flujo de la bitácora sin bloquear NAV-01 por VIS-03.

### 2.5. Marcadores del Mapa y Suficiencia de Muestra

* **Decisión:** (a) La suficiencia de la muestra se calcula en el dominio: `Shop.isSampleSufficient` es `totalVotes >= MIN_VOTES_FOR_METRICS` (15), sin leer el campo `status` del mock. (b) El color del marcador es un matiz determinístico derivado del `id` del rubro líder (`categoryMarkerHue`); si la muestra no alcanza, el marcador es neutro (matiz fijo y opacidad reducida) y su título indica "En recolección (votos/15)". (c) `MapViewModel` carga los locales en su `init` y expone `retry()` para recargar con el filtro actual.
* **Motivación:** (a) `status` mezcla suficiencia de muestra con disponibilidad del local; el umbral por votos es una regla de negocio y vive en el dominio. (b) El mismo rubro siempre tiene el mismo color sin mantener una tabla de colores. (c) La pantalla no necesita disparar la carga y el estado sobrevive a la recomposición.

### 2.6. Contrato de Datos de la Ficha y Tarjeta de Muestra Insuficiente

* **Decisión:** (a) `Shop` reemplaza los textos de visualización por datos crudos: `firstVoteAt` y `lastVoteAt` (epoch en milisegundos), `frequencyCounts` (`ConsumptionFrequency` a cantidad), `votesByWeekday` (`Weekday` a cantidad) y `votesByTimeSlot` (`TimeSlot` a cantidad). El DTO usa claves de texto (`DIARIA`, `MONDAY`, `MORNING`) y el mapper descarta las claves desconocidas. `Weekday` y `TimeSlot` son enums propios, sin `java.time` por el minSdk 24. (b) Los textos ("hace 2 días", "oct 2026", "21 de 34 visitarían a diario") se calculan en la UI con `DateUtils` y `strings.xml`. La ficha muestra solo Diario, Semanal y Ocasional, como el documento. (c) Con N < 15 la ficha muestra la tarjeta "Muestra insuficiente en recolección (X/15 votos mínimos)" y ningún porcentaje. (d) Hasta que DET-03 entregue el pase real, `ShopDetailViewModel` fija `hasActivePass = true` mediante la constante `PASS_ACTIVE_UNTIL_PAYWALL`.
* **Motivación:** (a) Los datos crudos no dependen del idioma ni del momento de la consulta y son lo que el backend puede entregar de forma estable. (b) La presentación vive en la capa de UI y se adapta al idioma y a la escala de fuente del sistema. (c) Un porcentaje sobre pocos votos induce a decidir mal. (d) Permite entregar y probar la ficha completa sin bloquear DET-01 por DET-03.

### 2.7. Tarjeta de Vista Previa del Local Seleccionado

* **Decisión:** (a) Al tocar un pin se muestra `ShopPreviewCard` fijada abajo del mapa y se suprime la ventana de información por defecto del marcador (el `onClick` devuelve `true`); el título y el snippet del marcador se mantienen para accesibilidad. (b) La tarjeta se cierra tocando el mapa fuera de ella, deslizándola hacia abajo (`Modifier.draggable` vertical con umbral de 72 dp, decidido por la función pura `shouldDismissOnDrag`) o con el botón de cierre de 48 dp; todos terminan en `MapViewModel.selectShop(null)`. (c) Tanto el botón "Ver ficha completa" (mínimo 48 dp) como la tarjeta entera llaman a `onShopClick(shop.id)`. (d) La tarjeta aparece con `AnimatedVisibility` y conserva el último local seleccionado mientras dura la animación de salida.
* **Motivación:** (a) La tarjeta reemplaza la ventana por defecto y muestra más datos (barrio, rubro líder o aviso de recolección, votos) sin superponer dos elementos. (b) Hay tres formas de cerrar para que ninguna dependa de un solo gesto. (c) Cumple RNF-02: pin y botón son dos toques hasta la ficha. (d) Evita que el contenido desaparezca de golpe al cerrar. No se agregaron librerías.


### 2.8. Formulario de Visita, Dirección Manual y Persistencia del Borrador

* **Decisión:** (a) `VisitEntity` y `Visit` pasan a `shopId: String?` y `manualAddress: String?`; la versión de Room sube de 1 a 2 con `MIGRATION_1_2` (recrea la tabla, convierte `shopId` vacío en `NULL` y conserva las filas) y se elimina `fallbackToDestructiveMigration`. (b) `VisitViewModel` recibe un `SavedStateHandle` (creado con `createSavedStateHandle()` en `AppViewModelProvider`) donde escribe en cada cambio las notas y la dirección manual; el estado se reconstruye desde ese handle. (c) Con `NO_SHOP_ID` el formulario muestra el campo de dirección manual, obligatorio al guardar; con un local conocido se guarda `shopId` y `manualAddress = null`. (d) `NavDisplay` usa `rememberSaveableStateHolderNavEntryDecorator` y `rememberViewModelStoreNavEntryDecorator` para que cada entrada tenga su propio ViewModel, y la fábrica cae en `VidrierAppApplication.instance` cuando el entry no trae `APPLICATION_KEY`. (e) Al guardar con éxito se borra el borrador y se vuelve atrás; si falla, el borrador se conserva.
* **Motivación:** (a) Una visita sin ficha previa no tiene `shopId`; `NULL` expresa la ausencia mejor que `""` y una migración real evita perder la bitácora del usuario. (b) `SavedStateHandle` sobrevive a la muerte del proceso (RNF-12) sin escribir en Room cada tecla ni agregar tablas o librerías, y el volumen (texto corto) está muy por debajo del límite del Bundle. Se descartó un borrador en Room (más escrituras, tabla extra, limpieza manual) y `rememberSaveable` (la lógica quedaría en la UI, fuera del ViewModel testeable). (d) Sin decoradores el ViewModel quedaba atado a la Activity y el borrador de una visita se arrastraba a la siguiente.

---

## 3. Decisiones de Backend y Procesamiento de Datos

### 3.1. Normalización Híbrida de Respuestas Libres ("Opción Otros")

* **Decisión:** Diseñar un pipeline de procesamiento en dos etapas para la categorización de texto libre ingresado por los usuarios.
1. **Pipeline Determinístico Inmediato (Tiempo Real):** Sanitización, eliminación de diacríticos, mapeo por tabla de alias y algoritmos de distancia de cadenas (*Fuzzy Matching* / Levenshtein). Resuelve el ~95% de las entradas en milisegundos y a costo cero.
2. **Worker de Excepciones (Diferido / Batch):** Procesamiento en segundo plano reservado exclusivamente para el subconjunto de entradas no clasificadas (`unclassified`).


* **Motivación:** Evitar el impacto operativo, la latencia y los costos recurrentes de invocar servicios de Lenguaje Natural (NLP/LLM) por cada voto emitido en tiempo real.

### 3.2. Autorización y Filtrado de Datos en el Servidor

* **Decisión:** Delegar al backend (Ktor API) la responsabilidad de validar el estado del pase activo y filtrar el payload devuelto.
* **Motivación:** Principio de defensibilidad de datos. Ocultar métricas en el cliente móvil sobre respuestas JSON completas expone la información a ingeniería inversa o intercepción de tráfico. Si el usuario no cuenta con un pase activo, la API elimina las métricas cuantitativas del cuerpo de la respuesta antes de enviarla.

---

## 4. Estrategia de Almacenamiento y Persistencia

### 4.1. Base de Datos Central: PostgreSQL + PostGIS + JSONB

* **Decisión:** Utilizar PostgreSQL como única base de datos relacional y geoespacial en la infraestructura Cloud.
* **Motivación:**
* **PostGIS:** Extensión estándar de la industria para consultas geoespaciales complejas, mapas de calor y filtrado por radios de distancia.
* **JSONB:** Permite almacenar atributos dinámicos y esquemas variables de locales comerciales o notas sin perder la integridad relacional de la base de datos.



### 4.2. Persistencia Local Offline-First para Trabajo de Campo

* **Decisión:** Implementar la base de datos SQLite/Room en el dispositivo móvil para la gestión de la bitácora de visitas (notas, fotografías capturadas con CameraX, conteos peatonales) y la **caché local de locales consultados con TTL de 7 días** (DATA-01). Asimismo, almacenar localmente identificadores de dispositivo (Device ID / UUID) para la validación de la prueba gratuita de 3 días (DET-03).
* **Motivación:** Garantizar la operatividad en zonas con cobertura de red deficiente, permitir el acceso offline a fichas previamente consultadas y asegurar la privacidad del usuario comerciante (RNF-PRI-01).

### 4.3. Geolocalización de Locales: Alta por Administrador con Pin Manual Acotado y Validado

* **Decisión:** El alta de locales es una operación exclusivamente administrativa. La dirección se resuelve con servicios de Google para **ubicar la zona**, pero la coordenada que se persiste es **la que el administrador marca manualmente** sobre un mapa acotado, validada por el backend contra la dirección ingresada.

**Flujo de alta:**

1. **Búsqueda de dirección (cliente):** el administrador escribe la dirección en un buscador con autocompletado (Places SDK, variante por sesión, con token de sesión). El autocompletado se resuelve en el cliente para evitar latencia por tecla.
2. **Resolución de zona (backend Ktor):** al elegir una sugerencia, la app envía el `place_id` al backend (`POST /admin/geo/resolve`). El backend lo geocodifica contra el Geocoding API y devuelve **solo** un punto central y un recuadro (*bounding box*) de ~150 m alrededor. La clave de API vive únicamente en el servidor, restringida por IP.
3. **Mapa acotado (cliente):** la app muestra el mapa con la cámara limitada a ese recuadro y un zoom mínimo, para que el administrador ubique el local con precisión. No se muestra ningún pin preexistente.
4. **Ubicación manual:** el administrador **toca el mapa** en el punto exacto. Aceptar o confirmar la sugerencia sin tocar el mapa no está permitido: sin toque no se habilita el botón de guardar.
5. **Vista previa:** antes de guardar, la app muestra el pin sobre el mapa junto con la dirección ingresada.
6. **Validación (backend):** al recibir `POST /admin/shops`, el backend calcula la distancia entre el punto enviado y el punto resuelto en el paso 2 (recuperado de una caché en memoria de 15 minutos, para no volver a geocodificar):
   * **≤ 150 m:** se acepta.
   * **> 150 m:** se rechaza salvo que la solicitud incluya una confirmación explícita (`confirm_far_location = true`), en cuyo caso se guarda marcando `location_override_used = true`.
7. **Persistencia:** se guarda el registro en PostgreSQL (ver modelo abajo). El punto resuelto por Google **no se persiste**.
8. **Visualización:** los clientes (Android y Web Lite) reciben las coordenadas desde la API propia, que las lee de PostGIS. Mostrar un local no genera ninguna llamada a Google; el mapa se renderiza con el Maps SDK for Android (`maps-compose`).

**Salida de emergencia:** si Google ubica mal la dirección (numeración irregular, altura inexistente), el administrador puede activar "La ubicación sugerida es incorrecta", que libera el mapa. El alta queda marcada con `location_override_used = true` para su revisión posterior.

**Modelo de datos (tabla `shops`, campos relevantes):**

| Campo | Descripción |
|---|---|
| `id` | UUID propio. Clave primaria del local. |
| `address_text` | Dirección ingresada, como texto. |
| `place_id` | Identificador de Google. Opcional, no único. |
| `location` | `geography(Point, 4326)` en PostGIS, con el punto elegido por el administrador. |
| `location_override_used` | Booleano. `true` si se usó la salida de emergencia o se confirmó una ubicación lejana. |
| `created_by`, `created_at` | Auditoría de alta. |
| `updated_by`, `updated_at` | Auditoría de modificaciones. |

* **Reglas derivadas:**
  * El `place_id` **no identifica al local**: varios locales pueden compartir dirección y, por lo tanto, `place_id`. La clave primaria es el UUID propio.
  * El `place_id` puede almacenarse sin límite de tiempo, pero no contiene coordenadas. No se usa para renderizar el mapa.
  * Las coordenadas devueltas por el Geocoding API se usan **solo en memoria del servidor y de forma transitoria** (centrado del mapa y validación). Nunca se escriben en la base de datos.
  * Reportes de ubicación incorrecta enviados desde la app llegan al administrador, que corrige el punto y deja trazabilidad en `updated_by` / `updated_at`.

* **Motivación:**
  * **Costo desacoplado del uso:** el Maps SDK for Android tiene uso móvil ilimitado sin cargo. Las únicas llamadas facturables ocurren en el alta (una de Geocoding por local, más el autocompletado). El gasto escala con los locales cargados, no con la cantidad de usuarios. Ambos servicios son SKUs Essentials con 10.000 solicitudes gratuitas por mes cada uno, volumen que el alta manual no se acerca a consumir.
  * **Cumplimiento de términos:** los Service Specific Terms de Google Maps Platform limitan el cacheo de lat/lng del Geocoding API a 30 días corridos, mientras que los valores de ID (`place_id`) pueden guardarse. Persistir solo coordenadas elegidas manualmente evita almacenar contenido de Google fuera de lo permitido.
  * **Precisión:** el geocoding puede ubicar una dirección en el medio de la manzana o con altura aproximada. Quien conoce el local lo ubica con más exactitud. Es además un patrón de UX establecido ("elegí el punto exacto en el mapa").
  * **Integridad de datos:** como el punto depende de una persona, el backend lo verifica contra la dirección, el mapa acotado previene el error de interfaz, y la auditoría permite rastrear y corregir.
  * **Consultas geoespaciales:** al ser datos propios en PostGIS, las búsquedas por radio y mapas de calor (4.1) no dependen

---

## 5. Alternativas Evaluadas y Descartadas

### ❌ Alternativa Descartada 1: Uso de MongoDB / NoSQL como Base Principal

* **Razón del descarte:** Aunque MongoDB ofrece esquemas flexibles para los datos de los locales, carece del nivel de rendimiento, soporte de polígonos avanzados y optimizaciones geoespaciales que proporciona PostGIS en PostgreSQL. Asimismo, la gestión transaccional relacional requerida para la activación de pases y usuarios resulta más consistente en un motor SQL tradicional. La necesidad de flexibilidad en atributos se resolvió mediante campos `JSONB` en PostgreSQL.

### ❌ Alternativa Descartada 2: Procesamiento de Texto Libre 100% mediante LLM en Tiempo Real

* **Razón del descarte:** Introducir llamadas a API de modelos de lenguaje por cada voto enviado desde la Web App genera latencia innecesaria en la experiencia del vecino, dependencia de servicios externos y costos de infraestructura no escalables. Se optó por el enfoque determinístico por regla.

### ❌ Alternativa Descartada 3: Aplicación Web Progresiva (PWA) Unificada para Ambos Perfiles

* **Razón del descarte:** Una PWA no proporciona el nivel de integración necesario con el hardware del dispositivo (gestión eficiente de cámara para bitácora, almacenamiento seguro en base de datos local y rendimiento fluido en renderizado de mapas interactivos) que requiere el usuario emprendedor en sus jornadas de trabajo de campo.

### ❌ Alternativa Descartada 4: Guardar solo el `place_id` y resolverlo en cada visualización

* **Razón del descarte:** Cada apertura del mapa generaría una llamada facturable por local mostrado, haciendo que el costo escale con los usuarios activos en lugar de con los locales cargados. Además, acopla la disponibilidad de la app a un servicio externo.

### ❌ Alternativa Descartada 5: Persistir las coordenadas del Geocoding API como dato definitivo

* **Razón del descarte:** Los términos de Google limitan el almacenamiento de lat/lng obtenidos por geocoding a 30 días corridos. Hacerlos definitivos sin confirmación del usuario nos expondría a incumplir las condiciones de uso.

### ❌ Alternativa Descartada 6: OpenStreetMap (MapLibre / osmdroid) como proveedor de mapas

* **Razón del descarte:** Los servidores públicos de teselas de OSM no están pensados para aplicaciones comerciales en producción y Nominatim tiene límites estrictos de peticiones. Contratar un proveedor de teselas agregaría un costo que con el Maps SDK for Android es cero, y las librerías de Compose para MapLibre son menos maduras que `maps-compose`. Se reevaluará si Google modifica sus condiciones comerciales, como ya ocurrió con la eliminación del crédito mensual de US$200 en marzo de 2025.