package pe.edu.utp.novasalud.orchestrator.service;

import com.google.gson.JsonObject;
import pe.edu.utp.novasalud.orchestrator.audit.TraceService;
import pe.edu.utp.novasalud.orchestrator.client.AtencionApiClient;
import pe.edu.utp.novasalud.orchestrator.client.CitasApiClient;
import pe.edu.utp.novasalud.orchestrator.client.FarmaciaApiClient;
import pe.edu.utp.novasalud.orchestrator.dto.AtencionProcessRequest;
import pe.edu.utp.novasalud.orchestrator.dto.AtencionProcessResponse;
import pe.edu.utp.novasalud.orchestrator.util.AppLogger;

public class AtencionOrchestrationService {

    private final CitasApiClient citasApiClient = new CitasApiClient();
    private final AtencionApiClient atencionApiClient = new AtencionApiClient();
    private final FarmaciaApiClient farmaciaApiClient = new FarmaciaApiClient();

    public AtencionProcessResponse ejecutarProcesoAtencion(AtencionProcessRequest req, String jwtToken) {
        return ejecutarProcesoAtencion(req, jwtToken, null);
    }

    public AtencionProcessResponse ejecutarProcesoAtencion(AtencionProcessRequest req, String jwtToken, String incomingCorrelationId) {
        String correlationId = (incomingCorrelationId != null && !incomingCorrelationId.trim().isEmpty())
                ? incomingCorrelationId.trim()
                : TraceService.generateCorrelationId();
        AtencionProcessResponse resp = new AtencionProcessResponse();
        resp.setCorrelationId(correlationId);
        resp.setProceso("ATENCION_MEDICA_ORQUESTRADA");
        resp.setIdCita(req.getIdCita());
        resp.setIdMedico(req.getIdMedico());

        TraceService.logStep(correlationId, "INICIO_ORQUESTACION", "NOVASALUD-ORCHESTRATOR", "IN_PROGRESS", "Iniciando flujo SOA para Cita ID " + req.getIdCita());

        try {
            // PASO 1: Validar Cita en Citas API
            TraceService.logStep(correlationId, "VALIDAR_CITA", "NOVASALUD-CITAS-API", "CALLING", "Consultando cita ID " + req.getIdCita());
            JsonObject citaJson = citasApiClient.obtenerCitaPorId(req.getIdCita(), jwtToken, correlationId);
            
            if (citaJson == null) {
                TraceService.logStep(correlationId, "VALIDAR_CITA", "NOVASALUD-CITAS-API", "FAILED", "La cita ID " + req.getIdCita() + " no fue encontrada.");
                resp.setSuccess(false);
                resp.setEstado("ERROR_CITA_NOT_FOUND");
                resp.setMensaje("La cita médica especificada no existe.");
                return resp;
            }

            int idPaciente = citaJson.has("idPaciente") ? citaJson.get("idPaciente").getAsInt() : 0;
            String estadoCita = citaJson.has("estadoCita") ? citaJson.get("estadoCita").getAsString() : "";
            String estadoPago = citaJson.has("estadoPago") ? citaJson.get("estadoPago").getAsString() : "";

            resp.setIdPaciente(idPaciente);
            TraceService.logStep(correlationId, "VALIDAR_CITA", "NOVASALUD-CITAS-API", "SUCCESS", 
                    String.format("Cita validada. Paciente: %d | Estado Cita: %s | Estado Pago: %s", idPaciente, estadoCita, estadoPago));

            if (!"PAGADO".equalsIgnoreCase(estadoPago)) {
                TraceService.logStep(correlationId, "VALIDAR_CITA", "NOVASALUD-CITAS-API", "FAILED", "La cita no se encuentra pagada.");
                resp.setSuccess(false);
                resp.setEstado("ERROR_CITA_NO_PAGADA");
                resp.setMensaje("No se puede registrar la atención clínica. La cita médica no está registrada como PAGADA.");
                return resp;
            }

            boolean tienePrescripcion = req.getPrescripcion() != null && req.getPrescripcion().getMedicamentos() != null && !req.getPrescripcion().getMedicamentos().isEmpty();
            String estadoInicialFarmacia = tienePrescripcion ? "PENDIENTE_ENVIO_FARMACIA" : "NO_APLICA";

            // PASO 2: Registrar Atención Médica en Atención API
            TraceService.logStep(correlationId, "REGISTRAR_ATENCION", "NOVASALUD-ATENCION-API", "CALLING", "Enviando registro de consulta médica");
            JsonObject atencionJson = atencionApiClient.registrarAtencion(
                    req.getIdCita(),
                    req.getIdMedico(),
                    idPaciente,
                    req.getMotivoConsulta(),
                    req.getSintomas(),
                    req.getDiagnostico(),
                    req.getTratamiento(),
                    req.getRecetaMedica(),
                    req.getIndicaciones(),
                    estadoInicialFarmacia,
                    jwtToken,
                    correlationId
            );

            int idAtencion = atencionJson.has("idAtencion") ? atencionJson.get("idAtencion").getAsInt() : 0;
            resp.setIdAtencion(idAtencion);
            TraceService.logStep(correlationId, "REGISTRAR_ATENCION", "NOVASALUD-ATENCION-API", "SUCCESS", "Atención médica creada con ID " + idAtencion);

            // PASO 3: Transferir Prescripción a Farmacia API
            if (tienePrescripcion) {
                resp.setPrescripcionGenerada(true);
                TraceService.logStep(correlationId, "TRANSFERIR_FARMACIA", "NOVASALUD-FARMACIA-API", "CALLING", "Enviando " + req.getPrescripcion().getMedicamentos().size() + " medicamentos a Farmacia");

                JsonObject farmaciaResp = farmaciaApiClient.enviarPrescripcionAFarmacia(idPaciente, req.getIdMedico(), idAtencion, req.getIndicaciones(), req.getPrescripcion(), jwtToken, correlationId);
                if (farmaciaResp != null) {
                    int ordenId = farmaciaResp.has("idPrescripcion") ? farmaciaResp.get("idPrescripcion").getAsInt() : (farmaciaResp.has("idOrden") ? farmaciaResp.get("idOrden").getAsInt() : 1);
                    resp.setFarmaciaOrdenId(ordenId);
                    resp.setEstadoFarmacia("ENVIADO_A_FARMACIA");
                    atencionApiClient.actualizarEstadoFarmacia(idAtencion, "ENVIADO_A_FARMACIA", jwtToken, correlationId);
                    TraceService.logStep(correlationId, "TRANSFERIR_FARMACIA", "NOVASALUD-FARMACIA-API", "SUCCESS", "Orden de farmacia registrada con ID " + ordenId);
                } else {
                    resp.setEstadoFarmacia("PENDIENTE_ENVIO_FARMACIA");
                    atencionApiClient.actualizarEstadoFarmacia(idAtencion, "PENDIENTE_ENVIO_FARMACIA", jwtToken, correlationId);
                    TraceService.logStep(correlationId, "TRANSFERIR_FARMACIA", "NOVASALUD-FARMACIA-API", "WARNING", "Farmacia API no disponible. Prescripción marcada como PENDIENTE_ENVIO_FARMACIA para reintento diferido.");
                }
            } else {
                resp.setPrescripcionGenerada(false);
                resp.setEstadoFarmacia("NO_APLICA");
            }

            // PASO 4: Consolidar Respuesta Final
            resp.setSuccess(true);
            resp.setEstado("COMPLETADO");
            resp.setMensaje("Proceso de atención médica orquestado exitosamente con ID Trazabilidad: " + correlationId);

            TraceService.logStep(correlationId, "FIN_ORQUESTACION", "NOVASALUD-ORCHESTRATOR", "SUCCESS", "Flujo orquestado finalizado exitosamente");
            return resp;

        } catch (Exception ex) {
            AppLogger.error(AtencionOrchestrationService.class, "Error inesperado durante la orquestación SOA: " + ex.getMessage(), ex);
            TraceService.logStep(correlationId, "ERROR_ORQUESTACION", "NOVASALUD-ORCHESTRATOR", "FAILED", ex.getMessage());

            resp.setSuccess(false);
            resp.setEstado("ERROR_INTERNO");
            resp.setMensaje("Error en el orquestador: " + ex.getMessage());
            return resp;
        }
    }
}
