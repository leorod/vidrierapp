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

* **Decisión:** Implementar la base de datos SQLite/Room en el dispositivo móvil para la gestión de la bitácora de visitas (notas, fotografías capturadas con CameraX y conteos peatonales).
* **Motivación:** Garantizar la operatividad en zonas con cobertura de red deficiente y asegurar la privacidad del usuario comerciante, manteniendo sus anotaciones estratégicas almacenadas exclusivamente en su dispositivo (RNF-PRI-01).

---

## 5. Alternativas Evaluadas y Descartadas

### ❌ Alternativa Descartada 1: Uso de MongoDB / NoSQL como Base Principal

* **Razón del descarte:** Aunque MongoDB ofrece esquemas flexibles para los datos de los locales, carece del nivel de rendimiento, soporte de polígonos avanzados y optimizaciones geoespaciales que proporciona PostGIS en PostgreSQL. Asimismo, la gestión transaccional relacional requerida para la activación de pases y usuarios resulta más consistente en un motor SQL tradicional. La necesidad de flexibilidad en atributos se resolvió mediante campos `JSONB` en PostgreSQL.

### ❌ Alternativa Descartada 2: Procesamiento de Texto Libre 100% mediante LLM en Tiempo Real

* **Razón del descarte:** Introducir llamadas a API de modelos de lenguaje por cada voto enviado desde la Web App genera latencia innecesaria en la experiencia del vecino, dependencia de servicios externos y costos de infraestructura no escalables. Se optó por el enfoque determinístico por regla.

### ❌ Alternativa Descartada 3: Aplicación Web Progresiva (PWA) Unificada para Ambos Perfiles

* **Razón del descarte:** Una PWA no proporciona el nivel de integración necesario con el hardware del dispositivo (gestión eficiente de cámara para bitácora, almacenamiento seguro en base de datos local y rendimiento fluido en renderizado de mapas interactivos) que requiere el usuario emprendedor en sus jornadas de trabajo de campo.