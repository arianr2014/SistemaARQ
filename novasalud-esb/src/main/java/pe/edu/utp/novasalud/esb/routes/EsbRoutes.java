package pe.edu.utp.novasalud.esb.routes;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.HttpHostConnectException;
import pe.edu.utp.novasalud.esb.config.EsbConfig;
import pe.edu.utp.novasalud.esb.processor.AuditLogProcessor;
import pe.edu.utp.novasalud.esb.processor.CorrelationIdProcessor;
import pe.edu.utp.novasalud.esb.processor.EsbResponses;
import pe.edu.utp.novasalud.esb.processor.SecurityPolicyProcessor;

/**
 * Rutas del Enterprise Service Bus de NovaSalud.
 *
 * Enrutamiento por prefijo de ruta:
 *   /esb/citas/**        → Citas API
 *   /esb/atencion/**     → Atención API
 *   /esb/farmacia/**     → Farmacia API
 *   /esb/orquestacion/** → Orquestador
 *
 * Cada ruta aplica las políticas comunes de integración: trazabilidad
 * (X-Correlation-ID), seguridad (JWT) y registro de mensajes.
 * La transformación y el enriquecimiento de la prescripción se agregan
 * en la Fase 4.
 */
public class EsbRoutes extends RouteBuilder {

    @Override
    public void configure() {

        // Servicio destino caído o sin respuesta: 503 con un mensaje claro.
        onException(ConnectException.class, HttpHostConnectException.class, ConnectTimeoutException.class,
                UnknownHostException.class, SocketTimeoutException.class)
                .handled(true)
                .process(e -> EsbResponses.error(e, 503, "SERVICIO_NO_DISPONIBLE",
                        "El servicio " + e.getProperty("esbServicio") + " no está disponible"))
                .process(e -> AuditLogProcessor.fin(e, e.getProperty("esbServicio", String.class)));

        // Cualquier otro error inesperado durante la integración: 502.
        onException(Exception.class)
                .handled(true)
                .process(e -> EsbResponses.error(e, 502, "ERROR_INTEGRACION",
                        e.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class).getMessage()))
                .process(e -> AuditLogProcessor.fin(e, e.getProperty("esbServicio", String.class)));

        // Estado del ESB (público, sin token).
        from("servlet:/health")
                .routeId("esb-health")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json; charset=UTF-8"))
                .setBody(constant("{\"estado\":\"UP\",\"servicio\":\"NOVASALUD-ESB\","
                        + "\"rutas\":[\"/esb/citas\",\"/esb/atencion\",\"/esb/farmacia\",\"/esb/orquestacion\"]}"));

        ruta("citas", "CITAS-API", EsbConfig.citasApiUrl());
        ruta("atencion", "ATENCION-API", EsbConfig.atencionApiUrl());
        ruta("farmacia", "FARMACIA-API", EsbConfig.farmaciaApiUrl());
        ruta("orquestacion", "ORCHESTRATOR", EsbConfig.orchestratorApiUrl());
    }

    /**
     * Crea una ruta que recibe /esb/{prefijo}/** y la reenvía a {destino}/**,
     * conservando método, cuerpo, query y cabeceras (incluido el token).
     */
    private void ruta(String prefijo, String servicio, String destino) {
        String endpointDestino = destino
                + "?bridgeEndpoint=true"            // usa la ruta recibida en lugar de la del endpoint
                + "&throwExceptionOnFailure=false"; // los 4xx/5xx del servicio se devuelven tal cual

        from("servlet:/" + prefijo + "?matchOnUriPrefix=true")
                .routeId("ruta-" + prefijo)
                .setProperty("esbServicio", constant(servicio))
                .setProperty("esbRuta", simple("/" + prefijo + "${header.CamelHttpPath}"))
                .process(AuditLogProcessor::inicio)
                .process(new CorrelationIdProcessor())
                .process(new SecurityPolicyProcessor())
                .to(endpointDestino)
                .setHeader(CorrelationIdProcessor.HEADER, exchangeProperty(CorrelationIdProcessor.HEADER))
                .process(e -> AuditLogProcessor.fin(e, servicio));
    }
}
