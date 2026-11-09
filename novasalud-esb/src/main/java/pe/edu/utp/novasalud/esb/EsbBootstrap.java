package pe.edu.utp.novasalud.esb;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.utp.novasalud.esb.config.EsbConfig;
import pe.edu.utp.novasalud.esb.routes.EsbRoutes;

/**
 * Inicia y detiene el motor de integración (Apache Camel) junto con la
 * aplicación web desplegada en Tomcat.
 */
@WebListener
public class EsbBootstrap implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger("NOVASALUD-ESB");
    private CamelContext camel;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            camel = new DefaultCamelContext();
            camel.getCamelContextExtension().setName("novasalud-esb");

            HttpComponent http = camel.getComponent("http", HttpComponent.class);
            http.setConnectTimeout(Timeout.ofSeconds(EsbConfig.connectTimeoutSeconds()));
            http.setConnectionRequestTimeout(Timeout.ofSeconds(EsbConfig.connectTimeoutSeconds()));
            http.setResponseTimeout(Timeout.ofSeconds(EsbConfig.responseTimeoutSeconds()));
            http.setSoTimeout(Timeout.ofSeconds(EsbConfig.responseTimeoutSeconds()));

            camel.addRoutes(new EsbRoutes());
            camel.start();

            LOG.info("ESB iniciado. Destinos: citas={} | atencion={} | farmacia={} | orquestacion={}",
                    EsbConfig.citasApiUrl(), EsbConfig.atencionApiUrl(),
                    EsbConfig.farmaciaApiUrl(), EsbConfig.orchestratorApiUrl());
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo iniciar el ESB", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (camel != null) {
            camel.stop();
        }
    }
}
