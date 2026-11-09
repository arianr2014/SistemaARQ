package pe.edu.utp.novasalud.orchestrator.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import pe.edu.utp.novasalud.orchestrator.config.ServiceUrlsConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class AtencionApiClient {

    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public AtencionApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public JsonObject registrarAtencion(int idCita, int idMedico, int idPaciente, String motivo, String sintomas, String diagnostico, String tratamiento, String receta, String indicaciones, String jwtToken, String correlationId) throws Exception {
        return registrarAtencion(idCita, idMedico, idPaciente, null, motivo, sintomas, diagnostico, tratamiento, receta, indicaciones, "NO_APLICA", jwtToken, correlationId);
    }

    public JsonObject registrarAtencion(int idCita, int idMedico, int idPaciente, String codigoHistoria, String motivo, String sintomas, String diagnostico, String tratamiento, String receta, String indicaciones, String estadoEnvioFarmacia, String jwtToken, String correlationId) throws Exception {
        String url = ServiceUrlsConfig.getAtencionApiUrl() + "/atencion-medica?idMedicoSesion=" + idMedico + "&marcarAtendida=true";
        
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("idCita", idCita);
        bodyMap.put("idMedico", idMedico);
        bodyMap.put("idPaciente", idPaciente);
        if (codigoHistoria != null && !codigoHistoria.isBlank()) {
            bodyMap.put("codigoHistoria", codigoHistoria.trim());
        }
        bodyMap.put("motivoConsulta", motivo);
        bodyMap.put("sintomas", sintomas);
        bodyMap.put("diagnostico", diagnostico);
        bodyMap.put("tratamiento", tratamiento);
        bodyMap.put("recetaMedica", receta);
        bodyMap.put("indicaciones", indicaciones);
        bodyMap.put("estadoEnvioFarmacia", estadoEnvioFarmacia != null ? estadoEnvioFarmacia : "NO_APLICA");

        String jsonBody = gson.toJson(bodyMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", jwtToken.startsWith("Bearer ") ? jwtToken : "Bearer " + jwtToken)
                .header("X-Correlation-ID", correlationId)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("Error al registrar en Atención API. HTTP Status: " + response.statusCode() + " - Body: " + response.body());
        }
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public boolean actualizarEstadoFarmacia(int idAtencion, String nuevoEstado, String jwtToken, String correlationId) {
        try {
            String url = ServiceUrlsConfig.getAtencionApiUrl() + "/atencion-medica/" + idAtencion + "/estado-farmacia?estado=" + nuevoEstado;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .header("Authorization", jwtToken.startsWith("Bearer ") ? jwtToken : "Bearer " + jwtToken)
                    .header("X-Correlation-ID", correlationId)
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            return false;
        }
    }

    public JsonArray obtenerPendientesEnvioFarmacia(String jwtToken, String correlationId) {
        try {
            String url = ServiceUrlsConfig.getAtencionApiUrl() + "/atencion-medica/pendientes-farmacia";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .header("Authorization", jwtToken.startsWith("Bearer ") ? jwtToken : "Bearer " + jwtToken)
                    .header("X-Correlation-ID", correlationId)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), JsonArray.class);
            }
        } catch (Exception ignored) {
        }
        return new JsonArray();
    }
}
