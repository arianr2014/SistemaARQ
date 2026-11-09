package pe.edu.utp.novasalud.esb.config;

/**
 * Configuración del ESB: ubicación de los servicios que integra.
 *
 * Cada URL se lee de una variable de entorno (Docker) y, si no existe, se usa
 * el valor por defecto para desarrollo local. Los consumidores solo conocen la
 * dirección del ESB; si un servicio cambia de ubicación, se actualiza aquí.
 */
public final class EsbConfig {

    private EsbConfig() {
    }

    public static String citasApiUrl() {
        return env("CITAS_API_URL", "http://localhost:8080/novasalud-citas-api/api");
    }

    public static String atencionApiUrl() {
        return env("ATENCION_API_URL", "http://localhost:8082/novasalud-atencion-api/api");
    }

    public static String farmaciaApiUrl() {
        return env("FARMACIA_API_URL", "http://localhost:8081/novasalud-farmacia-api/api");
    }

    public static String orchestratorApiUrl() {
        return env("ORCHESTRATOR_API_URL", "http://localhost:8083/novasalud-orchestrator/api");
    }

    /** Tiempo máximo de conexión a un servicio (segundos). */
    public static int connectTimeoutSeconds() {
        return Integer.parseInt(env("ESB_CONNECT_TIMEOUT_SECONDS", "3"));
    }

    /** Tiempo máximo de espera de la respuesta de un servicio (segundos). */
    public static int responseTimeoutSeconds() {
        return Integer.parseInt(env("ESB_RESPONSE_TIMEOUT_SECONDS", "15"));
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value.trim().replaceAll("/+$", "") : defaultValue;
    }
}
