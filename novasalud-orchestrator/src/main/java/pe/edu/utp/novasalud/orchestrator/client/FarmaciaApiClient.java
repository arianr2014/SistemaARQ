package pe.edu.utp.novasalud.orchestrator.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import pe.edu.utp.novasalud.orchestrator.config.ServiceUrlsConfig;
import pe.edu.utp.novasalud.orchestrator.dto.MedicamentoItemDto;
import pe.edu.utp.novasalud.orchestrator.dto.PrescripcionDto;
import pe.edu.utp.novasalud.orchestrator.util.AppLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FarmaciaApiClient {

    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public FarmaciaApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public JsonObject enviarPrescripcionAFarmacia(int idPaciente, int idAtencion, PrescripcionDto prescripcion, String jwtToken, String correlationId) {
        return enviarPrescripcionAFarmacia(idPaciente, 0, idAtencion, null, prescripcion, jwtToken, correlationId);
    }

    public JsonObject enviarPrescripcionAFarmacia(int idPaciente, int idMedico, int idAtencion, String indicacionesGenerales, PrescripcionDto prescripcion, String jwtToken, String correlationId) {
        try {
            String url = ServiceUrlsConfig.getFarmaciaApiUrl() + "/prescripciones";

            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("codigoPrescripcion", "RX-" + idAtencion);
            if (idPaciente > 0) bodyMap.put("idPacienteExterno", idPaciente);
            if (idMedico > 0) bodyMap.put("idMedicoExterno", idMedico);
            if (idAtencion > 0) bodyMap.put("idAtencionExterna", idAtencion);
            bodyMap.put("instruccionesGenerales", indicacionesGenerales != null && !indicacionesGenerales.isBlank() ? indicacionesGenerales : "Prescripción médica emitida");
            bodyMap.put("estado", "PENDIENTE");

            List<Map<String, Object>> detalles = new ArrayList<>();
            if (prescripcion != null && prescripcion.getMedicamentos() != null) {
                for (MedicamentoItemDto item : prescripcion.getMedicamentos()) {
                    Map<String, Object> det = new HashMap<>();
                    det.put("idMedicamento", null);
                    det.put("nombreMedicamento", item.getNombre());
                    det.put("cantidad", item.getCantidad() > 0 ? item.getCantidad() : 1);
                    det.put("dosis", item.getDosis() != null ? item.getDosis() : "Según indicación médica");
                    det.put("frecuencia", item.getFrecuencia() != null ? item.getFrecuencia() : "Según indicación");
                    det.put("duracion", item.getDuracion() != null ? item.getDuracion() : "Según tratamiento");
                    det.put("indicaciones", item.getIndicaciones());
                    detalles.add(det);
                }
            }
            bodyMap.put("detalles", detalles);

            String jsonBody = gson.toJson(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", jwtToken.startsWith("Bearer ") ? jwtToken : "Bearer " + jwtToken)
                    .header("X-Correlation-ID", correlationId)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return gson.fromJson(response.body(), JsonObject.class);
            } else if (response.statusCode() == 409) {
                // Idempotencia: Receta ya registrada con anterioridad (mismo RX-{idAtencion})
                JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                if (json == null) json = new JsonObject();
                json.addProperty("duplicado", true);
                return json;
            } else {
                AppLogger.error(FarmaciaApiClient.class, "Farmacia API retornó HTTP Status: " + response.statusCode(), null);
                return null;
            }
        } catch (Exception ex) {
            AppLogger.error(FarmaciaApiClient.class, "Error conectando con Farmacia API (Servicio posiblemente fuera de línea). Aplicando resiliencia.", ex);
            return null;
        }
    }
}
