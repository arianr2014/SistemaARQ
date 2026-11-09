package pe.edu.utp.farmacia.frontend.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

public final class ApiClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080/novasalud-farmacia-api/api";
    private static final String CONFIG_FILE = "api.properties";
    private static String fileConfiguredUrl = null;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream is = ApiClient.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String url = props.getProperty("api.base.url");
                if (url != null && !url.isBlank()) {
                    fileConfiguredUrl = url.trim().replaceAll("/+$", "");
                }
            }
        } catch (Exception e) {
            AppLogger.error("No se pudo cargar la configuración de " + CONFIG_FILE + ": " + e.getMessage(), e);
        }
    }

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();

    private ApiClient() {
    }

    public static class ApiResponse {
        private final int statusCode;
        private final String body;

        public ApiResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    public static String getBaseUrl() {
        // 1. Prioridad #1: Configuración en el archivo api.properties (src/main/resources/api.properties)
        if (fileConfiguredUrl != null && !fileConfiguredUrl.isBlank()) {
            return fileConfiguredUrl;
        }
        // 2. Variable de Entorno del Sistema Operativo / Contenedor
        String envUrl = System.getenv("NOVASALUD_FARMACIA_BACKEND_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            return envUrl.trim().replaceAll("/+$", "");
        }
        // 3. Propiedad -D de la máquina virtual Java (JVM)
        String propUrl = System.getProperty("novasalud.farmacia.backend.url");
        if (propUrl != null && !propUrl.isBlank()) {
            return propUrl.trim().replaceAll("/+$", "");
        }
        // 4. Valor por defecto de contingencia (fallback)
        return DEFAULT_BASE_URL;
    }

    private static String resolveToken(String explicitToken) {
        if (explicitToken != null && !explicitToken.isBlank()) {
            return explicitToken.trim();
        }
        return JwtTokenContext.getToken();
    }

    private static void applyHeaders(HttpRequest.Builder builder, String token) {
        String jwt = resolveToken(token);
        if (jwt != null && !jwt.isBlank()) {
            builder.header("Authorization", "Bearer " + jwt);
        }
        builder.header("X-Correlation-ID", "FARMACIAWEB-" + java.util.UUID.randomUUID().toString().substring(0, 8));
    }

    public static ApiResponse get(String endpointPath, String token) throws Exception {
        String fullUrl = getBaseUrl() + (endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Accept", "application/json")
                .GET();

        applyHeaders(builder, token);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }

    public static ApiResponse post(String endpointPath, String jsonPayload, String token) throws Exception {
        String fullUrl = getBaseUrl() + (endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload != null ? jsonPayload : ""));

        applyHeaders(builder, token);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }

    public static ApiResponse put(String endpointPath, String jsonPayload, String token) throws Exception {
        String fullUrl = getBaseUrl() + (endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload != null ? jsonPayload : ""));

        applyHeaders(builder, token);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }

    public static ApiResponse delete(String endpointPath, String token) throws Exception {
        String fullUrl = getBaseUrl() + (endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Accept", "application/json")
                .DELETE();

        applyHeaders(builder, token);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }
}
