# Especificación Técnica BPMN y Matriz de Trazabilidad Proceso → Servicio SOA

**Proyecto:** NovaSalud Clinic - Sistema Integrado de Citas y Farmacia  
**Curso:** Arquitectura Orientada a Servicios (SOA) – Entregable APF3 (2026)  
**Fecha de Creación:** 08/11/2026  
**Versión:** 3.2.1  
**Estado:** Oficial / Implementado en `main`  
**Autores:** Equipo de Arquitectura NovaSalud  

---

## 1. Contexto y Objetivos de la Integración BPM + SOA

En el diagnóstico inicial del sistema de salud NovaSalud (APF1), se evidenció una desconexión crítica entre la atención clínica ambulatoria y la gestión farmacéutica. La prescripción médica se redactaba como texto libre o se entregaba físicamente en papel, generando:
1. Errores de interpretación de posología y medicamentos.
2. Inconsistencia entre diagnósticos médicos e inventario disponible en farmacia.
3. Tiempos muertos de espera para el paciente y transcripción manual duplicada por el químico farmacéutico.

Para resolver esta problemática bajo el paradigma **BPM (Business Process Management)** y **SOA (Service-Oriented Architecture)** (APF2 y APF3), se modeló y automatizó el flujo integral de **Atención Médica y Prescripción Digital**, coordinando personas, sistemas legados, microservicios independientes, un **Orquestador Central de Procesos Clínicos** y un **Enterprise Service Bus (ESB)** basado en Apache Camel.

---

## 2. Descripción de Carriles (Swimlanes) del Proceso

El proceso de negocio se estructura en cinco (5) carriles de responsabilidad funcional y técnica:

| Carril | Actor / Entidad | Responsabilidad Operativa en el Proceso |
| :--- | :--- | :--- |
| **Carril 1: Recepción** | Personal de Admisión | Identificación del paciente, búsqueda por DNI o registro demográfico, asignación de especialidad/médico y programación de la cita médica. |
| **Carril 2: Caja** | Cajero(a) | Cobro de la consulta ambulatoria, registro del medio de pago, emisión del comprobante y cambio del estado de la cita a `PAGADO`. |
| **Carril 3: Médico Tratante** | Médico Especialista | Admisión del paciente en consultorio, anamnesis, diagnóstico clínico, selección de medicamentos del catálogo real de Farmacia, dosificación estructurada y finalización de la atención. |
| **Carril 4: Sistema SOA** | Orquestador & ESB | **Orquestador (`novasalud-orchestrator`):** Coordina la validación de pago en Citas, el registro clínico en Atención y el despacho de receta a Farmacia.<br>**ESB (`novasalud-esb`):** Enrutamiento central, validación de JWT, correlación (`X-Correlation-ID`), auditoría, resiliencia y enriquecimiento (*Content Enricher*). |
| **Carril 5: Farmacia** | Químico Farmacéutico | Consulta enriquecida de recetas digitales pendientes, verificación de colegiatura médica y diagnóstico, validación de stock y dispensación al paciente. |

---

## 3. Diagrama BPMN del Proceso de Negocio (5 Carriles)

El siguiente diagrama formaliza el flujo de extremo a extremo, detallando eventos de inicio/fin, compuertas exclusivas de decisión (`XOR`), tareas de usuario, tareas automáticas de servicio (`Service Tasks`) y tareas de compensación/reintento asincrónico:

```mermaid
flowchart TD
    %% Definición de estilos
    classDef startEnd fill:#00703C,stroke:#0A5C36,stroke-width:2px,color:#fff;
    classDef userTask fill:#E8F5E9,stroke:#00703C,stroke-width:1.5px,color:#111827;
    classDef serviceTask fill:#E3F2FD,stroke:#1565C0,stroke-width:1.5px,color:#111827;
    classDef gateway fill:#FFF3E0,stroke:#E65100,stroke-width:1.5px,color:#111827;
    classDef errorTask fill:#FFEBEE,stroke:#C62828,stroke-width:1.5px,color:#111827;

    %% Carril 1: Recepción
    subgraph Lane1 ["🏊 Carril 1: Recepción (Admisión)"]
        node_start((Inicio: Paciente Solicita Cita)):::startEnd
        task_reg_paciente["1.1 Registrar / Validar Paciente"]:::userTask
        task_prog_cita["1.2 Programar Cita Médica\n(Estado: PENDIENTE)"]:::userTask
    end

    %% Carril 2: Caja
    subgraph Lane2 ["🏊 Carril 2: Caja (Facturación)"]
        task_cobro["2.1 Realizar Cobro de Consulta\n(Generar Pago)"]:::userTask
        task_actualiza_pago["2.2 Marcar Cita como PAGADA\n(Citas API)"]:::serviceTask
    end

    %% Carril 3: Médico
    subgraph Lane3 ["🏊 Carril 3: Médico Tratante"]
        task_iniciar_atencion["3.1 Llamar Paciente e Iniciar Consulta"]:::userTask
        task_diagnostico["3.2 Registrar Anamnesis y Diagnóstico"]:::userTask
        task_buscar_med["3.3 Consultar Catálogo Farmacia\n(Búsqueda en Grilla)"]:::userTask
        task_armar_receta["3.4 Estructurar Receta Médica\n(Dosis, Frecuencia, Cantidad)"]:::userTask
        task_finalizar["3.5 Guardar y Finalizar Atención"]:::userTask
        task_alerta_pago["Mostrar Alerta: Cita No Pagada\n(Atención Bloqueada)"]:::errorTask
        task_exito_medico["Visualizar Resumen y Estado Farmacia\n(ENVIADO / PENDIENTE)"]:::userTask
    end

    %% Carril 4: Sistema SOA (Orquestador + ESB)
    subgraph Lane4 ["🏊 Carril 4: Sistema SOA (Orquestador & ESB Camel)"]
        task_orq_inicio["4.1 Recibir Solicitud de Atención\n(POST /orquestacion/atencion)"]:::serviceTask
        gw_pago{¿Cita Pagada?}:::gateway
        task_reg_atencion["4.2 Registrar Atención Clínica\n(Atención API)"]:::serviceTask
        gw_receta{¿Incluye Receta?}:::gateway
        task_envio_farmacia["4.3 Transformar y Enviar Prescripción\n(Vía ESB -> Farmacia API)"]:::serviceTask
        gw_farmacia_resp{¿Farmacia Responde 201/200?}:::gateway
        task_marca_enviado["4.4 Marcar: ENVIADO_A_FARMACIA\n(atencion_medica)"]:::serviceTask
        task_marca_pendiente["4.5 Marcar: PENDIENTE_ENVIO_FARMACIA\n(atencion_medica)"]:::serviceTask
        task_scheduler["4.6 Tarea Programada de Reintentos\n(FarmaciaRetryListener cada 2 min)"]:::serviceTask
        task_content_enricher["4.7 Content Enricher ESB:\nCitas + Atención -> Receta Enriquecida"]:::serviceTask
    end

    %% Carril 5: Farmacia
    subgraph Lane5 ["🏊 Carril 5: Farmacia (Dispensación)"]
        task_recibe_receta["5.1 Almacenar Prescripción Canónica\n(Estado: PENDIENTE)"]:::serviceTask
        task_consultar_web["5.2 Listar Recetas en Farmacia Web\n(Vía ESB Enriquecido)"]:::userTask
        task_verificar_datos["5.3 Validar Paciente, CMP y Diagnóstico"]:::userTask
        task_dispensar["5.4 Dispensar Medicamentos y Cerrar\n(Estado: ATENDIDA)"]:::userTask
        node_fin((Fin: Proceso Completado)):::startEnd
    end

    %% Conexiones entre Carriles
    node_start --> task_reg_paciente
    task_reg_paciente --> task_prog_cita
    task_prog_cita --> task_cobro
    task_cobro --> task_actualiza_pago
    task_actualiza_pago --> task_iniciar_atencion

    task_iniciar_atencion --> task_diagnostico
    task_diagnostico --> task_buscar_med
    task_buscar_med --> task_armar_receta
    task_armar_receta --> task_finalizar
    task_finalizar --> task_orq_inicio

    %% Orquestación SOA
    task_orq_inicio --> gw_pago
    gw_pago -- "No (Pendiente)" --> task_alerta_pago
    gw_pago -- "Sí (PAGADO)" --> task_reg_atencion
    task_reg_atencion --> gw_receta

    gw_receta -- "No" --> task_exito_medico
    gw_receta -- "Sí" --> task_envio_farmacia

    task_envio_farmacia --> gw_farmacia_resp
    gw_farmacia_resp -- "201 / 409 Duplicada" --> task_marca_enviado
    gw_farmacia_resp -- "503 / Timeout / Caída" --> task_marca_pendiente

    task_marca_enviado --> task_recibe_receta
    task_recibe_receta --> task_exito_medico

    task_marca_pendiente --> task_exito_medico
    task_marca_pendiente -.-> task_scheduler
    task_scheduler -- "Reintento Exitoso" --> task_marca_enviado

    %% Flujo Farmacia
    task_recibe_receta --> task_consultar_web
    task_consultar_web --> task_content_enricher
    task_content_enricher --> task_verificar_datos
    task_verificar_datos --> task_dispensar
    task_dispensar --> node_fin
```

---

## 4. Diagrama de Secuencia SOA Ultra HD (EIP & Orquestación)

El siguiente diagrama de interacción ilustra la coreografía sincrónica y asincrónica entre los componentes distribuidos, destacando la propagación de seguridad (JWT Bearer), trazabilidad (`X-Correlation-ID`) y el patrón *Content Enricher*:

```mermaid
sequenceDiagram
    autonumber
    actor Medico as 👨‍⚕️ Médico Tratante
    participant Web as 🖥️ Frontend Citas
    participant ESB as 🚌 ESB (Camel 8086)
    participant Orq as ⚙️ Orquestador (8083)
    participant Citas as 🗄️ Citas API (8084)
    participant Atencion as 🗄️ Atención API (8082)
    participant Farmacia as 💊 Farmacia API (8085)
    participant FarmWeb as 🖥️ Farmacia Web (8081)

    %% 1. Invocación desde Frontend
    Medico->>Web: Presiona "Guardar y Finalizar Atención"
    Note over Web: Inyecta X-Correlation-ID: WEB-a1b2c3d4<br/>y Bearer JWT de sesión
    Web->>ESB: POST /esb/orquestacion/orquestacion/atencion
    ESB->>ESB: Valida JWT + Log Auditoría [ESB-...]
    ESB->>Orq: POST /api/orquestacion/atencion (propaga X-Correlation-ID)

    %% 2. Validación de Cita
    Orq->>ESB: GET /esb/citas/citas/{idCita}
    ESB->>Citas: GET /api/citas/{idCita}
    Citas-->>ESB: 200 OK { idPaciente: 3, estadoPago: "PAGADO" }
    ESB-->>Orq: 200 OK

    %% 3. Registro Clínico
    Orq->>ESB: POST /esb/atencion/atencion-medica
    ESB->>Atencion: POST /api/atencion-medica { diagnostico, estadoFarmacia: PENDIENTE }
    Atencion-->>ESB: 201 Created { idAtencion: 25 }
    ESB-->>Orq: 201 Created

    %% 4. Despacho a Farmacia
    Orq->>ESB: POST /esb/farmacia/prescripciones
    ESB->>Farmacia: POST /api/prescripciones { codigoPrescripcion: "RX-25", medicamentos: [...] }
    
    alt Farmacia Disponible (201 Created o 409 Duplicada)
        Farmacia-->>ESB: 201 Created { idPrescripcion: 101 }
        ESB-->>Orq: 201 Created
        Orq->>ESB: PUT /esb/atencion/atencion-medica/25/estado-farmacia
        ESB->>Atencion: PUT ... { estadoFarmacia: "ENVIADO_A_FARMACIA" }
        Atencion-->>Orq: 200 OK
        Orq-->>Web: 200 OK { estado: "ENVIADO_A_FARMACIA", correlationId: "WEB-a1b2c3d4" }
        Web-->>Medico: Muestra Confirmación y Estado ENVIADO_A_FARMACIA
    else Farmacia No Disponible (Timeout / 503)
        Farmacia-->>ESB: Conexión rechazada / 503
        ESB-->>Orq: 503 Service Unavailable
        Note over Orq: Atencion queda PENDIENTE_ENVIO_FARMACIA.<br/>FarmaciaRetryListener reintentará cada 2 min.
        Orq-->>Web: 200 OK { estado: "PENDIENTE_ENVIO_FARMACIA" }
        Web-->>Medico: Muestra Alerta Amarilla PENDIENTE_ENVIO_FARMACIA
    end

    %% 5. Consulta Enriquecida en Farmacia
    opt Químico Farmacéutico Consulta Prescripciones
        FarmWeb->>ESB: GET /esb/farmacia/prescripciones (Bearer JWT)
        ESB->>Farmacia: GET /api/prescripciones
        Farmacia-->>ESB: 200 OK [ { idAtencionExterna: 25, idPacienteExterno: 3, idMedicoExterno: 1, ... } ]
        
        Note over ESB: Content Enricher Processor<br/>(Caché en memoria por petición)
        ESB->>Citas: GET /api/pacientes/3 -> { nombrePaciente: "Lucía Morales", dni: "70123456" }
        ESB->>Citas: GET /api/medicos/1 -> { nombreMedico: "Dr. Valeria Rojas", cmp: "CMP-45892" }
        ESB->>Atencion: GET /api/atencion-medica/25 -> { diagnostico: "Faringitis Aguda" }
        
        ESB-->>FarmWeb: 200 OK [ Receta Enriquecida Completa ]
        FarmWeb-->>FarmWeb: Renderiza Paciente Real, CMP Real y Diagnóstico
    end
```

---

## 5. Matriz Maestra de Trazabilidad Proceso → Servicio SOA

Esta matriz vincula cada paso del proceso de negocio BPMN con los contratos, endpoints, métodos y componentes de la arquitectura orientada a servicios:

| # | Tarea del Proceso BPMN | Carril (Actor) | Tipo Tarea | Servicio SOA Invocado | Endpoint REST | Método | Mensaje Entrada (Payload) | Mensaje Salida / Retorno | Patrón EIP / Resiliencia |
| :---: | :--- | :--- | :---: | :--- | :--- | :---: | :--- | :--- | :--- |
| **T1** | Búsqueda / Registro de Paciente | Recepción | Usuario / Servicio | `novasalud-citas-api` | `/api/pacientes`<br>`/api/pacientes/buscar/dni/{dni}` | `GET`<br>`POST` | `{"dni":"70123456", "nombres":"Lucía", ...}` | `{"idPaciente":3, "estado":"ACTIVO"}` | Point-to-Point REST / Validación de unicidad |
| **T2** | Programación de Cita Médica | Recepción | Usuario / Servicio | `novasalud-citas-api` | `/api/citas` | `POST` | `{"idPaciente":3, "idMedico":1, "fecha":"2026-11-09", "hora":"10:00"}` | `{"idCita":40, "estado":"PENDIENTE", "estadoPago":"PENDIENTE"}` | Service Invocation / Bloqueo de solapamiento |
| **T3** | Cobro y Facturación de Consulta | Caja | Usuario / Servicio | `novasalud-citas-api` | `/api/pagos` | `POST` | `{"idCita":40, "monto":80.00, "metodoPago":"TARJETA"}` | `{"idPago":12, "estado":"COMPLETADO"}` | Transactional Update / Actualiza `cita.estado_pago = 'PAGADO'` |
| **T4** | Consulta de Catálogo de Medicamentos | Médico | Usuario / Servicio | `novasalud-farmacia-api` (vía Citas Web BFF) | `/api/medicamentos?q={criterio}` | `GET` | Parámetro `q=paracetamol` | `[{"idMedicamento":1, "nombre":"Paracetamol 500mg", "stock":150, "precio":1.50}]` | Content-Based Router / Filtro LIKE con límite |
| **T5** | Invocación de Orquestación Clínica | Médico | Servicio | `novasalud-orchestrator` (vía ESB) | `/esb/orquestacion/orquestacion/atencion` | `POST` | `AtencionProcessRequest`: `{idCita:40, idMedico:1, diagnostico:"...", prescripcion:{...}}` | `AtencionProcessResponse`: `{success:true, idAtencion:25, correlationId:"..."}` | **Process Orchestration (SOA)** / Propagación de `X-Correlation-ID` |
| **T6** | Validación de Regla de Pago | Sistema | Regla / Servicio | `novasalud-citas-api` (vía ESB) | `/esb/citas/citas/{idCita}` | `GET` | PathParam `idCita=40` | `{"idCita":40, "estadoPago":"PAGADO", ...}` | Guard Constraint / Si != `PAGADO` detiene flujo (400 `ERROR_CITA_NO_PAGADA`) |
| **T7** | Persistencia de Atención Clínica | Sistema | Servicio | `novasalud-atencion-api` (vía ESB) | `/esb/atencion/atencion-medica` | `POST` | `{"idCita":40, "idPaciente":3, "diagnostico":"...", "estadoFarmacia":"PENDIENTE_ENVIO_FARMACIA"}` | `{"idAtencion":25, "estadoRegistro":"ACTIVO"}` | Service Task / Persistencia en `bd_citasmedicas_v321` |
| **T8** | Enrutamiento de Prescripción Digital | Sistema | Servicio | `novasalud-farmacia-api` (vía ESB) | `/esb/farmacia/prescripciones` | `POST` | Mensaje Canónico: `{codigoPrescripcion:"RX-25", idAtencionExterna:25, medicamentos:[...]}` | `{"idPrescripcion":101, "codigo":"RX-25"}` | **Message Router + Idempotent Consumer** / 409 tratado como éxito idempotente |
| **T9** | Actualización de Estado de Prescripción | Sistema | Servicio | `novasalud-atencion-api` (vía ESB) | `/esb/atencion/atencion-medica/{id}/estado-farmacia` | `PUT` | `{"estadoFarmacia":"ENVIADO_A_FARMACIA"}` | `{"success":true}` | Service Task / Reflejo de confirmación en historia clínica |
| **T10** | Reintento Automático en Background | Sistema | Servicio / Timer | `novasalud-orchestrator` | Tarea en background (Scheduler 2 min) | Interno | Consulta `GET /pendientes-farmacia` | Reenvía a Farmacia API y actualiza a `ENVIADO_A_FARMACIA` | **Retry Pattern & Guaranteed Delivery** / Resiliencia ante caída de Farmacia |
| **T11** | Consulta Enriquecida de Recetas | Farmacia | Usuario / Servicio | `novasalud-esb` (Camel) | `/esb/farmacia/prescripciones`<br>`/esb/farmacia/prescripciones/{id}` | `GET` | Encabezado `Authorization: Bearer <token>` | `[{"codigoPrescripcion":"RX-25", "nombrePaciente":"Lucía Morales", "dniPaciente":"70123456", "nombreMedico":"Dr. Valeria Rojas", "numeroColegiaturaMedico":"CMP-45892", "diagnostico":"Faringitis", ...}]` | **Content Enricher (EIP)** / Agregación distribuida en paralelo con caché local |
| **T12** | Dispensación y Cierre de Receta | Farmacia | Usuario / Servicio | `novasalud-farmacia-api` (vía ESB) | `/esb/farmacia/prescripciones/{id}` | `POST` / `PUT` | `{"estado":"ATENDIDA", "usuario":"qf_martinez"}` | `{"success":true, "estado":"ATENDIDA"}` | State Transition / Descuento de stock en `bd_famacia` |

---

## 6. Principios de Diseño SOA Aplicados en la Solución

De acuerdo con los principios de arquitectura de Thomas Erl y los criterios de evaluación del APF3:

1. **Contratos Estandarizados (Standardized Service Contract):**
   - Definición formal en `/contratos/CONTRATO_PRESCRIPCION.md` y schemas JSON (`prescripcion-canonica.schema.json` y `prescripcion-farmacia.schema.json`).
   - Todos los servicios exponen documentación Swagger/OpenAPI 3.0 en contexto relativo.
2. **Bajo Acoplamiento (Service Loose Coupling):**
   - `novasalud-farmacia-api` no conoce la estructura ni la base de datos de Citas. Farmacia solo recibe identificadores externos y delega la composición al ESB (Decisión de Diseño "Opción 1").
3. **Abstracción del Servicio (Service Abstraction):**
   - Los frontends web no conocen la ubicación física de las APIs; se comunican únicamente con el ESB (`novasalud-esb:8080/novasalud-esb/esb/*`).
4. **Reusabilidad del Servicio (Service Reusability):**
   - Endpoints como `GET /pacientes/{id}` y `GET /medicos/{id}` son reutilizados tanto por la admisión, como por la agenda clínica y por el *Content Enricher* del ESB.
5. **Autonomía del Servicio (Service Autonomy):**
   - Cada servicio cuenta con su propio despliegue Docker y su propia persistencia (`bd_citasmedicas_v321` y `bd_famacia`), pudiendo operar de manera independiente.
6. **Sin Estado (Service Statelessness):**
   - Las solicitudes son independientes y portan su estado de seguridad mediante tokens criptográficos **JWT Bearer**, evitando sesiones compartidas en servidor.
7. **Descubribilidad (Service Discoverability):**
   - Registro unificado de endpoints a través del ESB (`/esb/health`, `/esb/citas`, `/esb/atencion`, `/esb/farmacia`, `/esb/orquestacion`).
8. **Composabilidad (Service Composability):**
   - El proceso de atención médica es un servicio compuesto orquestado por `novasalud-orchestrator`, coordinando transacciones entre Citas, Atención y Farmacia.
