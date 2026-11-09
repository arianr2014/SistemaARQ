# Control de Cambios - Fase 4: Integración Completa a través del ESB, Content Enricher y Trazabilidad

**Fecha:** 08/11/2026  
**Rama:** `feature-integracion-esb`  
**Estado:** Implementado y Compilado Satisfactoriamente (BUILD SUCCESS - 8 Módulos)  
**Responsable Técnico:** Agente Antigravity / Equipo NovaSalud SOA  

---

## 1. Ficha Técnica Resumen

| Metadato | Valor |
| :--- | :--- |
| **Proyecto / Solución** | Sistema NovaSalud SOA (APF3) |
| **Módulos Afectados** | `novasalud-esb`, `novasalud-citas-api`, `novasalud-orchestrator`, `novasalud-citas-web`, `novasalud-farmacia-web` |
| **Fecha de Ejecución** | 08/11/2026 |
| **Versión del Sistema** | V3.2.1 |
| **Tipo de Cambio** | Requerimiento de Integración / Arquitectura SOA EIP |
| **Impacto en BD** | Ninguno (Se aprovechan columnas preexistentes `dni`, `cmp` en tabla `medico`) |
| **Impacto en Backend** | Alto (Rutas ESB, Content Enricher, propagación `X-Correlation-ID`) |
| **Impacto en Frontend** | Medio (Enrutamiento hacia el ESB en `api.properties` y propagación de headers) |
| **Responsable Técnico** | Antigravity AI Pair Programmer & Desarrollador Principal |
| **Estado de QA / Compilación** | Aprobado (0 Errores en Maven Reactor - 8/8 módulos) |

---

## 2. Descripción del Cambio

Se completó la **Fase 4** del proyecto NovaSalud SOA, resolviendo los puntos de integración y desacoplamiento pendientes entre microservicios, el bus de integración Apache Camel y los frontends web:

1. **Resolución de Hallazgo 9.3 (CMP y DNI de médicos en Citas API):**
   - En `novasalud-citas-api/.../MedicoDAOImpl.java`, se actualizó `obtenerPorId` y `listar` ejecutando un `LEFT JOIN especialidad` y recuperando `dni`, `cmp`, `correo`, `consultorio` y `especialidad`.
   - Ahora `GET /api/medicos/{id}` entrega los datos completos del colegiado médico.

2. **Resolución de Hallazgo 9.4 y Trazabilidad Continua (`X-Correlation-ID`):**
   - En `novasalud-orchestrator`:
     - `AtencionOrchestrationResource` lee el encabezado HTTP `X-Correlation-ID` de la petición y lo retorna en la respuesta HTTP.
     - `AtencionOrchestrationService` reutiliza el ID entrante si está presente; de lo contrario genera uno nuevo. Se propaga homogéneamente hacia Citas API, Atención API y Farmacia API.
   - En `novasalud-citas-web` y `novasalud-farmacia-web`:
     - `ApiClient.java` inyecta automáticamente el encabezado `X-Correlation-ID` en todas las peticiones salientes (`GET`, `POST`, `PUT`, `DELETE`).

3. **Patrón Content Enricher en el ESB (`novasalud-esb` - Hallazgo 9.2):**
   - Se incorporó la dependencia `gson:2.10.1` en `novasalud-esb/pom.xml`.
   - Se implementó el procesador `PrescripcionContentEnricherProcessor.java` para interceptar las respuestas `GET` de `novasalud-farmacia-api` en `/esb/farmacia/prescripciones` y `/esb/farmacia/prescripciones/{id}`.
   - Enriquece cada prescripción con:
     - `nombrePaciente` y `dniPaciente` desde Citas API (`/api/pacientes/{id}`).
     - `nombreMedico` y `numeroColegiaturaMedico` desde Citas API (`/api/medicos/{id}`).
     - `diagnostico` desde Atención API (`/api/atencion-medica/{id}`).
     - `fechaEmision` y `fechaVencimiento` (7 días).
   - Incluye caché en memoria por petición (evitando sobrecarga N+1 en listados) y tolerancia ante indisponibilidad de microservicios externos.
   - Se configuró la ruta especializada en `EsbRoutes.java` preservando el enrutamiento transparente para el resto de endpoints.

4. **Enrutamiento de Consumidores a través del ESB:**
   - En `docker-compose.yml`, se configuró `orchestrator` apuntando a:
     - `CITAS_API_URL: http://esb:8080/novasalud-esb/esb/citas`
     - `ATENCION_API_URL: http://esb:8080/novasalud-esb/esb/atencion`
     - `FARMACIA_API_URL: http://esb:8080/novasalud-esb/esb/farmacia`
     - Dependencia `[citas-api, atencion-api, farmacia-api, esb]`.
   - En `docker/config/novasalud-citas-web/api.properties`:
     - Todas las URLs redirigidas a través de `http://esb:8080/novasalud-esb/esb/...`.
   - En `docker/config/novasalud-farmacia-web/api.properties`:
     - `api.base.url=http://esb:8080/novasalud-esb/esb/farmacia`.

---

## 3. Matriz de Archivos Intervenidos

| Archivo | Tipo de Operación | Descripción del Cambio |
| :--- | :---: | :--- |
| `novasalud-citas-api/.../dao/MedicoDAOImpl.java` | Modificación | `LEFT JOIN especialidad` y mapeo de `dni`, `cmp`, `correo` y especialidad en `obtenerPorId` y `listar`. |
| `novasalud-orchestrator/.../rest/AtencionOrchestrationResource.java` | Modificación | Captura y retorno del encabezado `X-Correlation-ID`. |
| `novasalud-orchestrator/.../service/AtencionOrchestrationService.java` | Modificación | Sobrecarga de método para recibir y reutilizar `incomingCorrelationId`. |
| `novasalud-citas-web/.../util/ApiClient.java` | Modificación | Inyección del encabezado `X-Correlation-ID` en peticiones REST. |
| `novasalud-farmacia-web/.../util/ApiClient.java` | Modificación | Inyección del encabezado `X-Correlation-ID` en peticiones REST. |
| `novasalud-esb/pom.xml` | Modificación | Adición de dependencia `gson:2.10.1`. |
| `novasalud-esb/.../processor/PrescripcionContentEnricherProcessor.java` | Creación | Patrón Content Enricher para prescripciones con caché local y tolerancia a fallos. |
| `novasalud-esb/.../routes/EsbRoutes.java` | Modificación | Registro de ruta especializada `/farmacia/prescripciones` con Content Enricher en GET. |
| `docker-compose.yml` | Modificación | Enrutamiento del Orquestador y dependencias hacia el ESB. |
| `docker/config/novasalud-citas-web/api.properties` | Modificación | URLs base de APIs dirigidas a través del ESB (`/esb/*`). |
| `docker/config/novasalud-farmacia-web/api.properties` | Modificación | URL base dirigida a través del ESB (`/esb/farmacia`). |
| `docs/plans/08112026_plan_fase_4_integracion_esb.md` | Creación | Plan de trabajo técnico previo a la implementación. |
| `docs/changes/08112026_control_cambios_fase_4_integracion_esb.md` | Creación | Control de cambios y documentación técnica de la Fase 4. |

---

## 4. Verificación y Resultados de Pruebas

- **Compilación Maven Reactor (0 Errores):**
  - `novasalud-citas-api`: SUCCESS
  - `novasalud-atencion-api`: SUCCESS
  - `novasalud-orchestrator`: SUCCESS
  - `novasalud-citas-web`: SUCCESS
  - `novasalud-farmacia-api`: SUCCESS
  - `novasalud-farmacia-web`: SUCCESS
  - `novasalud-esb`: SUCCESS
  - `novasalud-parent`: SUCCESS
  - **Resultado:** `BUILD SUCCESS` en 14.69 s.

