package pe.edu.utp.novasalud.esb.processor;

import org.apache.camel.Exchange;

/** Construye las respuestas de error propias del ESB en formato JSON. */
public final class EsbResponses {

    private EsbResponses() {
    }

    public static void error(Exchange exchange, int status, String codigo, String mensaje) {
        String correlationId = exchange.getProperty(CorrelationIdProcessor.HEADER, String.class);
        String json = String.format(
                "{\"error\":\"%s\",\"mensaje\":\"%s\",\"origen\":\"NOVASALUD-ESB\",\"correlationId\":\"%s\"}",
                escape(codigo), escape(mensaje), escape(correlationId));
        exchange.getMessage().setBody(json);
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, status);
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json; charset=UTF-8");
        if (correlationId != null) {
            exchange.getMessage().setHeader(CorrelationIdProcessor.HEADER, correlationId);
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
