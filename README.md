# TPO Desarrollo de Aplicaciones I
## VidrierApp
> *"Abrí donde te esperan"*

**VidrierApp** es una plataforma colaborativa y basada en datos diseñada para conectar la oferta inmobiliaria comercial con la demanda real de los barrios. Ayudamos a los emprendedores a decidir de forma inteligente dónde ubicar sus negocios, basándose en lo que los vecinos de la zona realmente necesitan.

## Documentos 

### Documento de Diseño y Análisis (Pre-entrega - Etapa 1)
El análisis completo, los requisitos detallados, el alcance, las justificaciones tecnológicas y las decisiones arquitectónicas del proyecto se irán documentando en el siguiente link:

**[(Google Docs) VidrierApp Pre-entrega](https://docs.google.com/document/d/1DLETnXIkVfcgO08Xo1XmYDy4gQZs8149/edit?usp=sharing&ouid=107418476718967049931&rtpof=true&sd=true)**

### Diagramas y documentos auxiliares

En [docs/](/docs/) se encuentra el código Mermaid de los diagramas + un ADR con las decisiones que se van tomando sobre la arquitectura. El ADR es un documento interno para usar como referencia a futuro a medida que avanzamos con el proyecto, el único documento *formal* es el Documento de Diseño.

Todos los documentos en esta carpeta son documentos vivos que se irán manteniendo conforme avance el proyecto.

---

## Propuesta de Valor y Problema

### El Problema 
Elegir dónde abrir un local comercial se decide, hoy en día, prácticamente a ciegas. Un emprendedor que busca alquilar un local comercial no cuenta con suficientes herramientas objetivas para saber si una zona o una cuadra en específico es un lugar viable para establecer su negocio, menos aún qué preferirían cuál es la preferencia de consumo de los vecinos si abriera allí. Actualmente, esta incertidumbre se intenta paliar caminando la zona, conjeturando informalmente y asumiendo un alto riesgo financiero.

### La Solución 
VidrierApp propone un modelo colaborativo de captación de demanda real mediante interacción digital combinada con señalética física:
1. **QR en la Vidriera:** Se coloca un código QR distintivo en el cartel de alquiler del local comercial. 
2. **Voto del Vecino (Web Liviana):** Los peatones y vecinos escanean el QR y acceden a una web liviana e interactiva de menos de 10 segundos donde, de forma anónima y sin instalar aplicaciones, votan qué rubro les gustaría ver en ese local y con qué frecuencia lo visitarían.
3. **App Móvil para Emprendedores:** Los emprendedores acceden a la aplicación Android para explorar un mapa interactivo con la consolidación de estos votos, analizando el comportamiento de la demanda real y la oferta del entorno antes de realizar su inversión.

---

## User Journey principal

1. **Registro:** El propietario o la inmobiliaria registra el local disponible en la plataforma y pega el QR en la vidriera.
2. **Captura de Demanda:** Los vecinos que pasan por la vereda escanean el QR y votan sus preferencias (ej. *Cafetería*, con frecuencia *Diaria*).
3. **Consolidación:** El backend procesa las respuestas en tiempo real, agrupando votos por local y zona.
4. **Toma de Decisión:** El emprendedor abre **VidrierApp**, filtra el mapa por el rubro de su interés (*"Cafetería"*), selecciona un local en el mapa, y accede a las métricas detalladas de demanda del inmueble (frecuencia, volumen de votos). Al validar el alto interés del barrio, contacta directamente a la inmobiliaria a través de la app.

---

## Equipo

* **Leonardo Rodriguez** (LU 1048084) - https://github.com/leorod/
  * **Rol Principal:** Arquitectura, Backend, Persistencia y Modelo de Datos.
* **Facundo Reyes** (LU 1111386) - https://github.com/FacundoReyes
  * **Rol Principal:** UX/UI y Flujo de Experiencia de Usuario.

---

## Repositorio y Metodología

* **Repositorio:** [https://github.com/leorod/vidrierapp](https://github.com/leorod/vidrierapp)
* **Branching Model:** *Trunk-based Development*. Tratamos el branch `main` como nuestro trunk principal. Trabajamos directamente sobre feature branches (con prefijo `feature/` o `feat/`) lo más atómicos posible, a mergear directamente a `main` a través de PRs. 
*Nice to have, a definir: CI en Github Actions para garantizar estabilidad.*
* **Estructura de Documentación:** Todos los recursos gráficos, diagramas de arquitectura y el PDF final consolidado se encuentran en la carpeta `docs/` de este repositorio.
