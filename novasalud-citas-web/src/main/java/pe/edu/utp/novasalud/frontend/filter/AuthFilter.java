package pe.edu.utp.novasalud.frontend.filter;

import pe.edu.utp.novasalud.frontend.util.ApiClient;
import pe.edu.utp.novasalud.frontend.util.CsrfUtil;
import pe.edu.utp.novasalud.frontend.util.JwtTokenContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Filtro de autenticación y autorización para Frontend Web.
 * Valida sesión HTTP, propaga el JWT en JwtTokenContext para ApiClient
 * e invoca la validación de versión de sesión contra la API REST Backend.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override public void init(FilterConfig filterConfig) throws ServletException { }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String contextPath = httpRequest.getContextPath();
        String requestPath = obtenerRutaSolicitada(httpRequest, contextPath);

        if (esRutaPublica(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        boolean autenticado = session != null && session.getAttribute("usuarioLogeado") != null;
        if (!autenticado) {
            httpResponse.sendRedirect(contextPath + "/login");
            return;
        }

        // Registrar token JWT de la sesión en ThreadLocal para ApiClient
        String jwtToken = (String) session.getAttribute("jwtToken");
        if (jwtToken != null && !jwtToken.isBlank()) {
            JwtTokenContext.setToken(jwtToken);
        }

        try {
            if (!sesionSigueVigente(session)) {
                session.invalidate();
                httpResponse.sendRedirect(contextPath + "/login?error=sesion_invalida");
                return;
            }

            CsrfUtil.obtenerToken(session);

            String rol = String.valueOf(session.getAttribute("rolUsuario"));
            if (!esRutaPermitidaPorRol(requestPath, rol)) {
                String rutaCodificada = URLEncoder.encode(requestPath, StandardCharsets.UTF_8);
                httpResponse.sendRedirect(contextPath + "/acceso-denegado?from=" + rutaCodificada);
                return;
            }

            chain.doFilter(request, response);
        } finally {
            // Limpiar ThreadLocal para evitar fuga de memoria entre peticiones HTTP
            JwtTokenContext.clear();
        }
    }

    @Override public void destroy() { }

    private boolean sesionSigueVigente(HttpSession session) throws ServletException {
        try {
            int idUsuario = Integer.parseInt(String.valueOf(session.getAttribute("idUsuario")));
            int sessionVersion = Integer.parseInt(String.valueOf(session.getAttribute("sessionVersion")));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = ApiClient.get("/auth/validar-sesion?idUsuario=" + idUsuario + "&sessionVersion=" + sessionVersion, Map.class);
            if (result != null && result.containsKey("vigente")) {
                return Boolean.parseBoolean(String.valueOf(result.get("vigente")));
            }
            return true;
        } catch (Exception ex) {
            // En caso de fallo de red temporal se mantiene la sesión cliente activa
            return true;
        }
    }

    private String obtenerRutaSolicitada(HttpServletRequest request, String contextPath) {
        String uri = request.getRequestURI();
        return uri.substring(contextPath.length());
    }

    private boolean esRutaPublica(String requestPath) {
        return requestPath.equals("/")
                || requestPath.startsWith("/login")
                || requestPath.startsWith("/logout")
                || requestPath.startsWith("/css/")
                || requestPath.startsWith("/js/")
                || requestPath.startsWith("/resources/")
                || requestPath.endsWith(".css") || requestPath.endsWith(".js")
                || requestPath.endsWith(".png") || requestPath.endsWith(".jpg")
                || requestPath.endsWith(".jpeg") || requestPath.endsWith(".gif")
                || requestPath.endsWith(".svg") || requestPath.endsWith(".ico");
    }

    private boolean esRutaPermitidaPorRol(String requestPath, String rol) {
        String r = normalizarRol(rol);
        if (requestPath.startsWith("/acceso-denegado")) return true;
        if ("ADMIN".equals(r)) return true;

        if ("RECEPCIONISTA".equals(r)) {
            return requestPath.startsWith("/pacientes")
                    || requestPath.startsWith("/citas")
                    || requestPath.startsWith("/agenda-medico");
        }

        if ("CAJERO".equals(r)) {
            return requestPath.startsWith("/pagos")
                    || requestPath.startsWith("/reportes-financieros");
        }

        if ("DOCTOR".equals(r)) {
            return requestPath.startsWith("/agenda-medico")
                    || requestPath.startsWith("/atencion-medica")
                    || requestPath.startsWith("/catalogo-medicamentos");
        }

        if ("DIRECCION".equals(r)) {
            return requestPath.startsWith("/direccion")
                    || requestPath.startsWith("/inicio")
                    || requestPath.startsWith("/reportes-financieros")
                    || requestPath.startsWith("/estadisticas");
        }
        return false;
    }

    private String normalizarRol(String rol) { return rol == null ? "" : rol.trim().toUpperCase(); }
}
