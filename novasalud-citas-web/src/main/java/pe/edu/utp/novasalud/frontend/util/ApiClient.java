package pe.edu.utp.novasalud.frontend.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Cliente HTTP REST para comunicación desde los Servlets Controllers (Frontend Web)
 * hacia la arquitectura SOA de Microservicios Backend en Java con soporte de enrutamiento dinámico.
 */
public final class ApiClient {

    private static final String DEFAULT_CITAS_URL = "http://localhost:8080/novasalud-citas-api/api";
    private static final String DEFAULT_ATENCION_URL = "http://localhost:8082/novasalud-atencion-api/api";
    private static final String DEFAULT_ORCHESTRATOR_URL = "http://localhost:8083/novasalud-orchestrator/api";
    private static final String DEFAULT_FARMACIA_URL = "http://localhost:8085/novasalud-farmacia-api/api";

    private static final String CONFIG_FILE = "api.properties";
    
    private static String citasApiUrl = DEFAULT_CITAS_URL;
    private static String atencionApiUrl = DEFAULT_ATENCION_URL;
    private static String orchestratorApiUrl = DEFAULT_ORCHESTRATOR_URL;
    private static String farmaciaApiUrl = DEFAULT_FARMACIA_URL;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream is = ApiClient.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                
                String citas = props.getProperty("citas.api.url");
                if (citas != null && !citas.isBlank()) citasApiUrl = citas.trim().replaceAll("/+$", "");

                String atencion = props.getProperty("atencion.api.url");
                if (atencion != null && !atencion.isBlank()) atencionApiUrl = atencion.trim().replaceAll("/+$", "");

                String orchestrator = props.getProperty("orchestrator.api.url");
                if (orchestrator != null && !orchestrator.isBlank()) orchestratorApiUrl = orchestrator.trim().replaceAll("/+$", "");

                String farmacia = props.getProperty("farmacia.api.url");
                if (farmacia != null && !farmacia.isBlank()) farmaciaApiUrl = farmacia.trim().replaceAll("/+$", "");
            }
        } catch (Exception ignored) {
        }
    }

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();

    private ApiClient() {
    }

    public static String getBaseUrlForEndpoint(String endpointPath) {
        String path = endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath;
        if (path.startsWith("/atencion-medica") || path.startsWith("/historia-clinica")) {
            return atencionApiUrl;
        }
        if (path.startsWith("/orquestacion")) {
            return orchestratorApiUrl;
        }
        if (path.startsWith("/medicamentos") || path.startsWith("/farmacia")) {
            return farmaciaApiUrl;
        }
        return citasApiUrl;
    }

    private static List<String> buildUrlCandidates(String endpointPath) {
        String path = endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath;
        String primaryBase = getBaseUrlForEndpoint(path);
        String primaryUrl = primaryBase + path;
        
        List<String> list = new ArrayList<>();
        list.add(primaryUrl);

        // Fallbacks si novasalud-citas-api monolítico aún responde en puerto 8080
        if (!primaryBase.equals(DEFAULT_CITAS_URL)) {
            list.add(DEFAULT_CITAS_URL + path);
        }
        return list;
    }

    private static final ThreadLocal<String> CORRELATION_HOLDER = new ThreadLocal<>();

    public static void setCorrelationId(String correlationId) {
        CORRELATION_HOLDER.set(correlationId);
    }

    public static String getCorrelationId() {
        String id = CORRELATION_HOLDER.get();
        if (id == null || id.isBlank()) {
            return "WEB-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        return id;
    }

    public static void clearCorrelationId() {
        CORRELATION_HOLDER.remove();
    }

    private static void applyHeaders(HttpRequest.Builder builder, String token) {
        String activeToken = resolveToken(token);
        if (activeToken != null && !activeToken.isBlank()) {
            builder.header("Authorization", "Bearer " + activeToken);
        }
        builder.header("X-Correlation-ID", getCorrelationId());
    }

    private static String resolveToken(String explicitToken) {
        if (explicitToken != null && !explicitToken.isBlank()) {
            return explicitToken.trim();
        }
        return JwtTokenContext.getToken();
    }

    public static <T> T get(String endpointPath, Class<T> responseType) throws Exception {
        return get(endpointPath, null, responseType);
    }

    public static <T> T get(String endpointPath, Type typeOfT) throws Exception {
        return get(endpointPath, null, typeOfT);
    }

    public static <T> T get(String endpointPath, String token, Type typeOfT) throws Exception {
        List<String> candidates = buildUrlCandidates(endpointPath);
        Exception lastException = null;

        for (String fullUrl : candidates) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(fullUrl))
                        .header("Accept", "application/json")
                        .GET();

                applyHeaders(builder, token);

                HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return gson.fromJson(response.body(), typeOfT);
                }
                lastException = new RuntimeException("Error en petición REST GET [" + response.statusCode() + "]: " + response.body());
            } catch (Exception ex) {
                lastException = ex;
            }
        }
        throw lastException != null ? lastException : new RuntimeException("No se pudo conectar a la API Backend.");
    }

    public static <T> T get(String endpointPath, String token, Class<T> responseType) throws Exception {
        List<String> candidates = buildUrlCandidates(endpointPath);
        Exception lastException = null;

        for (String fullUrl : candidates) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(fullUrl))
                        .header("Accept", "application/json")
                        .GET();

                applyHeaders(builder, token);

                HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    if (responseType == String.class) {
                        return responseType.cast(response.body());
                    }
                    return gson.fromJson(response.body(), responseType);
                }
                lastException = new RuntimeException("Error en petición REST GET [" + response.statusCode() + "]: " + response.body());
            } catch (Exception ex) {
                lastException = ex;
            }
        }
        throw lastException != null ? lastException : new RuntimeException("No se pudo conectar a la API Backend.");
    }

    public static <T> List<T> getList(String endpointPath, Type listType) throws Exception {
        return getList(endpointPath, null, listType);
    }

    public static <T> List<T> getList(String endpointPath, String token, Type listType) throws Exception {
        List<String> candidates = buildUrlCandidates(endpointPath);
        Exception lastException = null;

        for (String fullUrl : candidates) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(fullUrl))
                        .header("Accept", "application/json")
                        .GET();

                applyHeaders(builder, token);

                HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return gson.fromJson(response.body(), listType);
                }
                lastException = new RuntimeException("Error en petición REST GET List [" + response.statusCode() + "]: " + response.body());
            } catch (Exception ex) {
                lastException = ex;
            }
        }
        throw lastException != null ? lastException : new RuntimeException("No se pudo conectar a la API Backend.");
    }

    public static <T> T post(String endpointPath, Object bodyObject, Class<T> responseType) throws Exception {
        return post(endpointPath, bodyObject, null, responseType);
    }

    public static <T> T post(String endpointPath, Object bodyObject, String token, Class<T> responseType) throws Exception {
        List<String> candidates = buildUrlCandidates(endpointPath);
        Exception lastException = null;
        String jsonBody = bodyObject != null ? gson.toJson(bodyObject) : "";

        for (String fullUrl : candidates) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(fullUrl))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

                applyHeaders(builder, token);

                HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    if (responseType == Void.class || responseType == void.class) {
                        return null;
                    }
                    if (responseType == String.class) {
                        return responseType.cast(response.body());
                    }
                    return gson.fromJson(response.body(), responseType);
                }
                lastException = new RuntimeException("Error en petición REST POST [" + response.statusCode() + "]: " + response.body());
            } catch (Exception ex) {
                lastException = ex;
            }
        }
        throw lastException != null ? lastException : new RuntimeException("No se pudo conectar a la API Backend.");
    }

    public static <T> T put(String endpointPath, Object bodyObject, Class<T> responseType) throws Exception {
        return put(endpointPath, bodyObject, null, responseType);
    }

    public static <T> T put(String endpointPath, Object bodyObject, String token, Class<T> responseType) throws Exception {
        List<String> candidates = buildUrlCandidates(endpointPath);
        Exception lastException = null;
        String jsonBody = bodyObject != null ? gson.toJson(bodyObject) : "";

        for (String fullUrl : candidates) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(fullUrl))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody));

                applyHeaders(builder, token);

                HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    if (responseType == Void.class || responseType == void.class) {
                        return null;
                    }
                    if (responseType == String.class) {
                        return responseType.cast(response.body());
                    }
                    return gson.fromJson(response.body(), responseType);
                }
                lastException = new RuntimeException("Error en petición REST PUT [" + response.statusCode() + "]: " + response.body());
            } catch (Exception ex) {
                lastException = ex;
            }
        }
        throw lastException != null ? lastException : new RuntimeException("No se pudo conectar a la API Backend.");
    }

    public static boolean delete(String endpointPath) throws Exception {
        return delete(endpointPath, null);
    }

    public static boolean delete(String endpointPath, String token) throws Exception {
        List<String> candidates = buildUrlCandidates(endpointPath);
        for (String fullUrl : candidates) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(fullUrl))
                        .DELETE();

                applyHeaders(builder, token);

                HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
