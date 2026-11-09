package pe.edu.utp.novasalud.orchestrator.scheduler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import pe.edu.utp.novasalud.orchestrator.audit.TraceService;
import pe.edu.utp.novasalud.orchestrator.client.AtencionApiClient;
import pe.edu.utp.novasalud.orchestrator.client.FarmaciaApiClient;
import pe.edu.utp.novasalud.orchestrator.dto.MedicamentoItemDto;
import pe.edu.utp.novasalud.orchestrator.dto.PrescripcionDto;
import pe.edu.utp.novasalud.orchestrator.util.AppLogger;
import pe.edu.utp.novasalud.orchestrator.util.JwtUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tarea programada en el Orquestador para reintentar prescripciones con estado
 * PENDIENTE_ENVIO_FARMACIA ante caídas temporales de la API de Farmacia (Resiliencia SOA Sección 12.4).
 */
@WebListener
public class FarmaciaRetryListener implements ServletContextListener {

    private ScheduledExecutorService scheduler;
    private final AtencionApiClient atencionApiClient = new AtencionApiClient();
    private final FarmaciaApiClient farmaciaApiClient = new FarmaciaApiClient();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "novasalud-farmacia-retry-worker");
            t.setDaemon(true);
            return t;
        });

        // Ejecutar cada 2 minutos con delay inicial de 30 segundos
        scheduler.scheduleWithFixedDelay(this::reintentarPrescripcionesPendientes, 30, 120, TimeUnit.SECONDS);
        AppLogger.info(FarmaciaRetryListener.class, "Planificador de Resiliencia SOA inicializado: reintento de prescripciones cada 2 minutos.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            AppLogger.info(FarmaciaRetryListener.class, "Planificador de Resiliencia SOA detenido correctamente.");
        }
    }

    private void reintentarPrescripcionesPendientes() {
        try {
            String token = JwtUtil.generarTokenSistema();
            String correlationId = TraceService.generateCorrelationId();

            JsonArray pendientes = atencionApiClient.obtenerPendientesEnvioFarmacia(token, correlationId);
            if (pendientes == null || pendientes.isEmpty()) {
                return;
            }

            AppLogger.info(FarmaciaRetryListener.class, "Iniciando reintento de resiliencia para " + pendientes.size() + " atenciones pendientes.");

            for (JsonElement elem : pendientes) {
                if (!elem.isJsonObject()) continue;
                JsonObject atencion = elem.getAsJsonObject();

                int idAtencion = atencion.has("idAtencion") ? atencion.get("idAtencion").getAsInt() : 0;
                int idPaciente = atencion.has("idPaciente") ? atencion.get("idPaciente").getAsInt() : 0;
                int idMedico = atencion.has("idMedico") ? atencion.get("idMedico").getAsInt() : 0;
                String recetaMedica = atencion.has("recetaMedica") && !atencion.get("recetaMedica").isJsonNull() ? atencion.get("recetaMedica").getAsString() : "";
                String indicaciones = atencion.has("indicaciones") && !atencion.get("indicaciones").isJsonNull() ? atencion.get("indicaciones").getAsString() : "";

                if (idAtencion <= 0 || recetaMedica.isBlank()) {
                    continue;
                }

                PrescripcionDto prescripcion = new PrescripcionDto();
                List<MedicamentoItemDto> items = new ArrayList<>();
                MedicamentoItemDto med = new MedicamentoItemDto();
                med.setNombre(recetaMedica);
                med.setCantidad(1);
                med.setDosis("Según indicación médica");
                med.setFrecuencia("Según tratamiento");
                med.setDuracion("Según evolución");
                med.setIndicaciones(indicaciones);
                items.add(med);
                prescripcion.setMedicamentos(items);

                JsonObject farmaciaResp = farmaciaApiClient.enviarPrescripcionAFarmacia(idPaciente, idMedico, idAtencion, indicaciones, prescripcion, token, correlationId);
                if (farmaciaResp != null) {
                    atencionApiClient.actualizarEstadoFarmacia(idAtencion, "ENVIADO_A_FARMACIA", token, correlationId);
                    TraceService.logStep(correlationId, "REINTENTO_FARMACIA_EXITOSO", "NOVASALUD-ORCHESTRATOR", "SUCCESS",
                            "Prescripción pendiente para atención ID " + idAtencion + " transferida a Farmacia con éxito.");
                    AppLogger.info(FarmaciaRetryListener.class, "Atención ID " + idAtencion + " reintentada y actualizada a ENVIADO_A_FARMACIA.");
                }
            }
        } catch (Exception ex) {
            AppLogger.error(FarmaciaRetryListener.class, "Error en ciclo de reintento de resiliencia SOA: " + ex.getMessage(), ex);
        }
    }
}