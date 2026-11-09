package pe.edu.utp.novasalud.esb.processor;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registro de mensajes: deja constancia de cada solicitud que atraviesa el ESB
 * (correlation ID, servicio destino, método, ruta, código de respuesta y tiempo).
 */
public final class AuditLogProcessor {

    private static final Logger LOG = LoggerFactory.getLogger("NOVASALUD-ESB");
    private static final String INICIO = "esbInicio";

    private AuditLogProcessor() {
    }

    public static void inicio(Exchange exchange) {
        exchange.setProperty(INICIO, System.currentTimeMillis());
    }

    public static void fin(Exchange exchange, String servicio) {
        Long inicio = exchange.getProperty(INICIO, Long.class);
        long ms = (inicio != null) ? System.currentTimeMillis() - inicio : -1;
        Integer status = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String query = exchange.getIn().getHeader(Exchange.HTTP_QUERY, String.class);
        LOG.info("[{}] {} -> {} {}{} | HTTP {} | {} ms",
                exchange.getProperty(CorrelationIdProcessor.HEADER),
                servicio,
                exchange.getIn().getHeader(Exchange.HTTP_METHOD),
                exchange.getProperty("esbRuta"),
                (query != null && !query.isBlank()) ? "?" + query : "",
                status, ms);
    }
}
