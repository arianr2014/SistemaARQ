# Catálogo de Evidencias de Ejecución e Integración SOA (APF3)

Este directorio contiene las capturas de pantalla y evidencias visuales en tiempo real del ecosistema distribuido **NovaSalud SOA**, tomadas directamente contra los contenedores Docker en ejecución (`novasalud-net`).

---

## 1. Inventario de Evidencias Capturadas

| Archivo | Componente Evaluado | URL / Puerto | Descripción del Hallazgo / Prueba |
| :--- | :--- | :--- | :--- |
| **`01_esb_health.png`** | `novasalud-esb` | `:8086/novasalud-esb/esb/health` | Estado del bus de integración ESB (`UP`, 0 ms latency, microservicios conectados). |
| **`02_swagger_citas_api.png`** | `novasalud-citas-api` | `:8084/novasalud-citas-api/swagger/` | Documentación OpenAPI 3.0 / Swagger UI del dominio Citas (Auth JWT, Médicos, Especialidades, Pacientes). |
| **`03_swagger_atencion_api.png`** | `novasalud-atencion-api` | `:8082/novasalud-atencion-api/swagger/` | Documentación OpenAPI 3.0 / Swagger UI del dominio Atención Médica e Historias Clínicas. |
| **`04_swagger_orchestrator.png`** | `novasalud-orchestrator` | `:8083/novasalud-orchestrator/swagger/` | Documentación Swagger UI del motor de Orquestación de Procesos Médicos Síncronos y Resiliencia. |
| **`05_swagger_farmacia_api.png`** | `novasalud-farmacia-api` | `:8085/novasalud-farmacia-api/swagger/` | Documentación OpenAPI 3.0 / Swagger UI del dominio Farmacia (Dispensación, Catálogo y Prescripciones). |
| **`06_frontend_citas_login.png`** | `novasalud-citas-web` | `:8080/novasalud-citas-web/` | Portal web para médicos y recepcionistas (Bootstrap 5, consumo autenticado vía ESB). |
| **`07_frontend_farmacia_login.png`** | `novasalud-farmacia-web` | `:8081/novasalud-farmacia-web/` | Portal web para químicos farmacéuticos y despacho de medicamentos. |
| **`08_reporte_integracion_esb_trazabilidad.png`** | Ecosistema SOA Completo | Local Report | Panel de verificación con trazabilidad EIP (`X-Correlation-ID`) en logs y patrón *Content Enricher* ejecutado en vuelo. |

---

## 2. Comprobación de Patrones Enterprise Integration Patterns (EIP)

### A. Patrón Correlation Identifier (`X-Correlation-ID`)
El bus `novasalud-esb` intercepta cada petición y asegura la inyección o propagación del encabezado de correlación hacia todos los microservicios aguas abajo:
```text
2026-11-08 23:05:33 INFO NOVASALUD-ESB - [APF3-TEST-TRACE-888] CITAS-API    -> GET /citas/medicos/1    | HTTP 200 | 6 ms
2026-11-08 23:05:33 INFO NOVASALUD-ESB - [APF3-TEST-TRACE-888] CITAS-API    -> GET /citas/pacientes/1  | HTTP 200 | 4 ms
2026-11-08 23:05:33 INFO NOVASALUD-ESB - [APF3-TEST-TRACE-888] FARMACIA-API -> GET /farmacia/medicam...| HTTP 200 | 11 ms
2026-11-08 23:05:33 INFO NOVASALUD-ESB - [APF3-TEST-TRACE-888] FARMACIA-API -> GET /farmacia/prescr...| HTTP 200 | 41 ms
```

### B. Patrón Content Enricher (Enriquecimiento en Vuelo)
Al consultar `GET /esb/farmacia/prescripciones`, el ESB consulta concurrentemente el catálogo de farmacia y los servicios de Citas y Atención para entregar al frontend un objeto unificado sin exponer la complejidad interna:
```json
{
  "idPrescripcion": 2,
  "codigoPrescripcion": "PRES-2026-0002",
  "idPacienteExterno": 3,
  "idMedicoExterno": 5,
  "idAtencionExterna": 102,
  "nombreMedico": "Dr. Ana Beltrán Ruiz",
  "numeroColegiaturaMedico": "CMP-10105",
  "nombrePaciente": "Lucía Chávez Paredes",
  "dniPaciente": "70000003",
  "diagnostico": "Evaluación médica satisfactoria."
}
```

### C. Patrón Resilient Polling / Retry Listener
El orquestador (`novasalud-orchestrator`) cuenta con un hilo de fondo (`FarmaciaRetryListener`) programado cada 2 minutos que monitorea atenciones que quedaron con receta pendiente de transmisión debido a contingencias de red o fallas transitorias en Farmacia, ejecutando la retransmisión idempotente automática.
