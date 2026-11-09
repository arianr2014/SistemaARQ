# Plan de Trabajo - Fase 4: Integración Completa a través del ESB, Content Enricher y Trazabilidad

**Fecha:** 08/11/2026  
**Rama:** eature-integracion-esb  
**Objetivo:** Implementar la integración de punta a punta de la arquitectura SOA mediante el Enterprise Service Bus (
ovasalud-esb), habilitando el patrón *Content Enricher* para la consulta de recetas en Farmacia, la propagación homogénea del X-Correlation-ID en todos los saltos y el enrutamiento de los consumidores frontends y orquestador a través del ESB.

---

## 1. Contexto y Justificación Técnica

En las fases previas (1 a 3 y 5) se construyeron los microservicios, el contrato canónico de prescripción, el bus de integración Apache Camel (
ovasalud-esb) y la interfaz dinámica de recetas en el frontend de atención con resiliencia de reintentos.
Sin embargo, de acuerdo con la hoja de ruta del proyecto y la sección 10 de docs/GUIA_CONTINUIDAD_DESARROLLO.md:
1. **Los consumidores aún llaman directamente a las APIs:** En Docker, el Orquestador y los Frontends Web se comunican directamente con los puertos de los microservicios sin atravesar el ESB (puerto 8086).
2. **Falta de enriquecimiento en Farmacia (Decisión Opción 1 / Hallazgo 9.2):** Farmacia no almacena datos personales ni diagnóstico. Cuando Farmacia Web lista prescripciones, los campos de paciente, médico y diagnóstico quedan vacíos o con valores simulados porque no existe el intermediario que orqueste la consulta enriquecida.
3. **El CMP y DNI del médico se devuelven en 
ull (Hallazgo 9.3):** El endpoint GET /api/medicos/{id} de Citas API no realiza el cruce con la especialidad ni recupera el CMP o DNI del médico en MedicoDAOImpl.
4. **Discontinuidad en la Trazabilidad (Hallazgo 9.4):** El Orquestador genera un identificador propio (CORR-...) en lugar de reutilizar el encabezado HTTP X-Correlation-ID recibido del cliente o del ESB, impidiendo rastrear una transacción completa en un único log correlacionado.

---

## 2. Alcance Detallado

### 2.1 Módulo 
ovasalud-citas-api (Citas Backend)
- **DAO (MedicoDAOImpl.java):**
  - Actualizar obtenerPorId(int idMedico) con consulta SQL enriquecida (LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad).
  - Mapear atributos cmp, dni, correo, consultorio y 
ombreEspecialidad al modelo Medico.
  - Impacto: GET /api/medicos/{id} entregará la información completa requerida por el Content Enricher.

### 2.2 Módulo 
ovasalud-orchestrator (Orquestador Backend)
- **Controlador REST (AtencionOrchestrationResource.java):**
  - Capturar el encabezado HTTP X-Correlation-ID si está presente en la solicitud entrante.
  - Suministrar el ID al servicio de orquestación.
- **Servicio (AtencionOrchestrationService.java):**
  - Evaluar el incomingCorrelationId: si existe y no está vacío, reutilizarlo para toda la traza y clientes HTTP; de lo contrario, generar uno nuevo vía TraceService.
  - Garantizar que las llamadas salientes hacia Citas API, Atención API y Farmacia API propaguen exactamente ese mismo X-Correlation-ID.

### 2.3 Módulo 
ovasalud-citas-web (Frontend Citas)
- **Cliente HTTP (ApiClient.java):**
  - Generar o propagar el encabezado X-Correlation-ID en cada solicitud saliente hacia el Orquestador y las APIs.
  - Asegurar que el ciclo de vida de la petición web mantenga la misma correlación hasta el backend.

### 2.4 Módulo 
ovasalud-esb (Enterprise Service Bus con Apache Camel)
- **Dependencias (pom.xml):**
  - Incorporar soporte para manipulación JSON de alto rendimiento (gson o jackson-databind) en el procesamiento de mensajes enriquecidos.
- **Procesador de Enriquecimiento (PrescripcionContentEnricherProcessor.java):**
  - Diseñar el patrón *Content Enricher* para GET /esb/farmacia/prescripciones y GET /esb/farmacia/prescripciones/{id}.
  - Invocar Farmacia API para obtener la(s) prescripción(es) base.
  - Para cada prescripción:
    - Consultar Citas API GET /pacientes/{idPacienteExterno} -> Obtener nombre completo y DNI.
    - Consultar Citas API GET /medicos/{idMedicoExterno} -> Obtener nombre del médico y CMP.
    - Consultar Atención API GET /atencion-medica/{idAtencionExterna} -> Obtener diagnóstico clínico.
    - Formatear fechas de emisión y vencimiento (7 días).
    - Mecanismo de caché en memoria por ciclo de petición para evitar llamadas HTTP redundantes al mismo paciente o médico en lotes de recetas.
    - Tolerancia a fallos: Si un microservicio subordinado no responde o responde 404/500, capturar la excepción y devolver campos descriptivos o vacíos sin truncar la entrega de la receta.
- **Configuración de Rutas (EsbRoutes.java):**
  - Registrar la ruta enriquecida de prescripciones con prioridad sobre la ruta proxy genérica de Farmacia.
  - Asegurar la preservación y respuesta del encabezado X-Correlation-ID.

### 2.5 Configuración de Entorno e Integración Docker
- **docker-compose.yml:**
  - Actualizar variables de entorno del servicio orchestrator:
    - CITAS_API_URL: http://esb:8080/novasalud-esb/esb/citas
    - ATENCION_API_URL: http://esb:8080/novasalud-esb/esb/atencion
    - FARMACIA_API_URL: http://esb:8080/novasalud-esb/esb/farmacia
    - depends_on: [citas-api, atencion-api, farmacia-api, esb]
  - Asegurar dependencias de inicio en citas-web y armacia-web hacia esb.
- **Propiedades de Frontend (docker/config/.../api.properties):**
  - 
ovasalud-citas-web: Enrutar endpoints a través de /novasalud-esb/esb/*.
  - 
ovasalud-farmacia-web: Enrutar base URL a través de http://esb:8080/novasalud-esb/esb/farmacia.

---

## 3. Plan de Pruebas y Validación

1. **Compilación Maven Integral:**
   - Ejecutar mvn clean compile -DskipTests en la raíz garantizando BUILD SUCCESS en los 8 módulos (cero errores).
2. **Prueba Unitaria / Verificación CMP en Citas API:**
   - Comprobar que GET /api/medicos/1 devuelva cmp, dni y 
ombreEspecialidad no nulos.
3. **Prueba de Trazabilidad End-to-End:**
   - Enviar una petición con X-Correlation-ID: TEST-E2E-F4-001 y verificar su presencia inalterada en:
     - Log del ESB ([ESB-...] / TEST-E2E-F4-001).
     - Log del Orquestador.
     - Logs de Citas, Atención y Farmacia.
4. **Prueba del Content Enricher en ESB:**
   - Consultar GET http://localhost:8086/novasalud-esb/esb/farmacia/prescripciones.
   - Verificar en la respuesta JSON la presencia de 
ombrePaciente, dniPaciente, 
ombreMedico, 
umeroColegiaturaMedico y diagnostico.
5. **Prueba de Resiliencia en Content Enricher:**
   - Detener temporalmente tencion-api.
   - Reconsultar prescripciones en el ESB y verificar que la lista se entregue normalmente con el diagnóstico vacío o por defecto (sin error 500).

---

## 4. Riesgos y Medidas de Mitigación

- **Sobrecarga por consultas N+1 en Content Enricher:**
  - *Mitigación:* Implementar una tabla de dispersión (Map<Integer, ...>) local en el procesador durante la iteración del listado para reutilizar respuestas de pacientes o médicos ya consultados en la misma petición.
- **Ruta ESB colisionando con el proxy transparente:**
  - *Mitigación:* Registrar la ruta específica rom('servlet:/farmacia/prescripciones?matchOnUriPrefix=false') antes de la regla comodín /farmacia?matchOnUriPrefix=true.
