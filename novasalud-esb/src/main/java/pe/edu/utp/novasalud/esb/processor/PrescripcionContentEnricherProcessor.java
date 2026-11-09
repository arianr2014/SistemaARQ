package pe.edu.utp.novasalud.esb.processor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.utp.novasalud.esb.config.EsbConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Patrón Content Enricher (SOA EIP) para Prescripciones en el ESB.
 *
 * Intercepta las prescripciones entregadas por novasalud-farmacia-api y las enriquece
 * con información clínica y demográfica de fuentes externas:
 *   - Paciente (Nombre y DNI) desde novasalud-citas-api (/pacientes/{id})
 *   - Médico (Nombre y Colegiatura CMP) desde novasalud-citas-api (/medicos/{id})
 *   - Diagnóstico desde novasalud-atencion-api (/atencion-medica/{id})
 *   - Cálculo estandarizado de fecha de emisión y vencimiento (7 días).
 *
 * Incluye caché en memoria por petición (para evitar llamadas N+1 a un mismo paciente/médico)
 * y tolerancia total a fallos (si un servicio no responde, la prescripción se entrega igual).
 */
public class PrescripcionContentEnricherProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(PrescripcionContentEnricherProcessor.class);

    private final HttpClient httpClient;

    public PrescripcionContentEnricherProcessor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(EsbConfig.connectTimeoutSeconds()))
                .build();
    }

    @Override
    public void process(Exchange exchange) {
        Integer statusCode = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        if (statusCode != null && (statusCode < 200 || statusCode >= 300)) {
            return; // No enriquecer errores de Farmacia API
        }

        String body = exchange.getMessage().getBody(String.class);
        if (body == null || body.trim().isEmpty()) {
            return;
        }

        JsonElement rootElement;
        try {
            rootElement = JsonParser.parseString(body);
        } catch (Exception e) {
            log.warn("No se pudo parsear el JSON de prescripción para enriquecimiento: {}", e.getMessage());
            return;
        }

        String token = exchange.getMessage().getHeader("Authorization", String.class);
        String correlationId = exchange.getProperty(CorrelationIdProcessor.HEADER, String.class);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = exchange.getMessage().getHeader(CorrelationIdProcessor.HEADER, String.class);
        }

        // Caché local por ciclo de petición HTTP (evita N+1 en listados)
        Map<Integer, PacienteInfo> pacienteCache = new HashMap<>();
        Map<Integer, MedicoInfo> medicoCache = new HashMap<>();
        Map<Integer, String> diagnosticoCache = new HashMap<>();

        if (rootElement.isJsonArray()) {
            JsonArray array = rootElement.getAsJsonArray();
            for (JsonElement item : array) {
                if (item.isJsonObject()) {
                    enriquecerPrescripcion(item.getAsJsonObject(), token, correlationId, pacienteCache, medicoCache, diagnosticoCache);
                }
            }
        } else if (rootElement.isJsonObject()) {
            enriquecerPrescripcion(rootElement.getAsJsonObject(), token, correlationId, pacienteCache, medicoCache, diagnosticoCache);
        }

        exchange.getMessage().setBody(rootElement.toString());
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json; charset=UTF-8");
    }

    private void enriquecerPrescripcion(JsonObject p, String token, String correlationId,
                                        Map<Integer, PacienteInfo> pacienteCache,
                                        Map<Integer, MedicoInfo> medicoCache,
                                        Map<Integer, String> diagnosticoCache) {

        // 1. Enriquecer Paciente
        if (p.has("idPacienteExterno") && !p.get("idPacienteExterno").isJsonNull()) {
            int idPaciente = p.get("idPacienteExterno").getAsInt();
            if (idPaciente > 0) {
                PacienteInfo info = pacienteCache.computeIfAbsent(idPaciente, id -> consultarPaciente(id, token, correlationId));
                if (info != null) {
                    if (info.nombreCompleto != null) p.addProperty("nombrePaciente", info.nombreCompleto);
                    if (info.dni != null) p.addProperty("dniPaciente", info.dni);
                }
            }
        }

        // 2. Enriquecer Médico
        if (p.has("idMedicoExterno") && !p.get("idMedicoExterno").isJsonNull()) {
            int idMedico = p.get("idMedicoExterno").getAsInt();
            if (idMedico > 0) {
                MedicoInfo info = medicoCache.computeIfAbsent(idMedico, id -> consultarMedico(id, token, correlationId));
                if (info != null) {
                    if (info.nombreCompleto != null) p.addProperty("nombreMedico", info.nombreCompleto);
                    if (info.cmp != null) p.addProperty("numeroColegiaturaMedico", info.cmp);
                }
            }
        }

        // 3. Enriquecer Diagnóstico
        if (p.has("idAtencionExterna") && !p.get("idAtencionExterna").isJsonNull()) {
            int idAtencion = p.get("idAtencionExterna").getAsInt();
            if (idAtencion > 0) {
                String diagnostico = diagnosticoCache.computeIfAbsent(idAtencion, id -> consultarDiagnostico(id, token, correlationId));
                if (diagnostico != null) {
                    p.addProperty("diagnostico", diagnostico);
                }
            }
        }

        // 4. Fechas de emisión y vencimiento coherentes
        String fechaPrescripcion = (p.has("fechaPrescripcion") && !p.get("fechaPrescripcion").isJsonNull())
                ? p.get("fechaPrescripcion").getAsString() : null;

        if (fechaPrescripcion != null && !fechaPrescripcion.isBlank()) {
            String fechaEmisionStr = fechaPrescripcion.length() >= 10 ? fechaPrescripcion.substring(0, 10) : fechaPrescripcion;
            p.addProperty("fechaEmision", fechaEmisionStr);
            try {
                LocalDate fechaEmision = LocalDate.parse(fechaEmisionStr);
                LocalDate fechaVencimiento = fechaEmision.plusDays(7);
                p.addProperty("fechaVencimiento", fechaVencimiento.format(DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (Exception ignored) {
            }
        }
    }

    private PacienteInfo consultarPaciente(int idPaciente, String token, String correlationId) {
        String url = EsbConfig.citasApiUrl() + "/pacientes/" + idPaciente;
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "application/json");
            if (token != null && !token.isBlank()) reqBuilder.header("Authorization", token);
            if (correlationId != null && !correlationId.isBlank()) reqBuilder.header("X-Correlation-ID", correlationId);

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
                String nombres = json.has("nombres") && !json.get("nombres").isJsonNull() ? json.get("nombres").getAsString() : "";
                String apellidos = json.has("apellidos") && !json.get("apellidos").isJsonNull() ? json.get("apellidos").getAsString() : "";
                String dni = json.has("dni") && !json.get("dni").isJsonNull() ? json.get("dni").getAsString() : "";
                String nombreCompleto = (nombres + " " + apellidos).trim();
                return new PacienteInfo(nombreCompleto.isEmpty() ? null : nombreCompleto, dni.isEmpty() ? null : dni);
            }
        } catch (Exception e) {
            log.debug("Content Enricher: no se pudo consultar paciente ID {}: {}", idPaciente, e.getMessage());
        }
        return null;
    }

    private MedicoInfo consultarMedico(int idMedico, String token, String correlationId) {
        String url = EsbConfig.citasApiUrl() + "/medicos/" + idMedico;
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "application/json");
            if (token != null && !token.isBlank()) reqBuilder.header("Authorization", token);
            if (correlationId != null && !correlationId.isBlank()) reqBuilder.header("X-Correlation-ID", correlationId);

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
                String nombres = json.has("nombres") && !json.get("nombres").isJsonNull() ? json.get("nombres").getAsString() : "";
                String apellidos = json.has("apellidos") && !json.get("apellidos").isJsonNull() ? json.get("apellidos").getAsString() : "";
                String cmp = json.has("cmp") && !json.get("cmp").isJsonNull() ? json.get("cmp").getAsString() : null;
                String nombreCompleto = (nombres + " " + apellidos).trim();
                return new MedicoInfo(nombreCompleto.isEmpty() ? null : "Dr. " + nombreCompleto, cmp);
            }
        } catch (Exception e) {
            log.debug("Content Enricher: no se pudo consultar médico ID {}: {}", idMedico, e.getMessage());
        }
        return null;
    }

    private String consultarDiagnostico(int idAtencion, String token, String correlationId) {
        String url = EsbConfig.atencionApiUrl() + "/atencion-medica/" + idAtencion;
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "application/json");
            if (token != null && !token.isBlank()) reqBuilder.header("Authorization", token);
            if (correlationId != null && !correlationId.isBlank()) reqBuilder.header("X-Correlation-ID", correlationId);

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
                if (json.has("diagnostico") && !json.get("diagnostico").isJsonNull()) {
                    return json.get("diagnostico").getAsString();
                }
            }
        } catch (Exception e) {
            log.debug("Content Enricher: no se pudo consultar atención médica ID {}: {}", idAtencion, e.getMessage());
        }
        return null;
    }

    private static class PacienteInfo {
        final String nombreCompleto;
        final String dni;
        PacienteInfo(String nombreCompleto, String dni) {
            this.nombreCompleto = nombreCompleto;
            this.dni = dni;
        }
    }

    private static class MedicoInfo {
        final String nombreCompleto;
        final String cmp;
        MedicoInfo(String nombreCompleto, String cmp) {
            this.nombreCompleto = nombreCompleto;
            this.cmp = cmp;
        }
    }
}
