package pe.edu.utp.novasalud.esb.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import pe.edu.utp.novasalud.esb.util.JwtValidator;

/**
 * Política de seguridad del ESB: rechaza las solicitudes sin un token JWT
 * válido antes de que lleguen a los servicios.
 *
 * Quedan exentas las operaciones públicas: el login, la documentación OpenAPI
 * y las solicitudes OPTIONS (preflight CORS).
 */
public class SecurityPolicyProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        String metodo = exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class);
        String ruta = exchange.getIn().getHeader(Exchange.HTTP_PATH, "", String.class);

        if ("OPTIONS".equalsIgnoreCase(metodo) || ruta.endsWith("auth/login")
                || ruta.contains("openapi") || ruta.contains("swagger")) {
            return;
        }

        String authorization = exchange.getIn().getHeader("Authorization", String.class);
        String token = (authorization != null && authorization.startsWith("Bearer "))
                ? authorization.substring("Bearer ".length()) : null;

        if (JwtValidator.validar(token) == null) {
            String motivo = (token == null)
                    ? "Falta la cabecera Authorization: Bearer <token>"
                    : "Token JWT inválido o expirado";
            EsbResponses.error(exchange, 401, "NO_AUTORIZADO", motivo);
            // Las solicitudes rechazadas también quedan en el registro del ESB.
            AuditLogProcessor.fin(exchange, exchange.getProperty("esbServicio", String.class));
            exchange.setRouteStop(true);
        }
    }
}
