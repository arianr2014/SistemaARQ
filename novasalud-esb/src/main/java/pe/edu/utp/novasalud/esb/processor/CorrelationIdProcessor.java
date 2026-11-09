package pe.edu.utp.novasalud.esb.processor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Política de trazabilidad: toda solicitud que atraviesa el ESB lleva un
 * X-Correlation-ID. Si el consumidor no lo envía, el ESB lo genera, y se
 * propaga al servicio destino y en la respuesta.
 */
public class CorrelationIdProcessor implements Processor {

    public static final String HEADER = "X-Correlation-ID";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public void process(Exchange exchange) {
        String id = exchange.getIn().getHeader(HEADER, String.class);
        if (id == null || id.isBlank()) {
            id = "ESB-" + LocalDateTime.now().format(FORMATO) + "-"
                    + UUID.randomUUID().toString().substring(0, 8);
        }
        exchange.getIn().setHeader(HEADER, id);
        exchange.setProperty(HEADER, id);
    }
}
