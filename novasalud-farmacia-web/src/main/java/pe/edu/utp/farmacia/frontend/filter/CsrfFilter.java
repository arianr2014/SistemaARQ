package pe.edu.utp.farmacia.frontend.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.farmacia.frontend.util.CsrfUtil;

import java.io.IOException;

@WebFilter("/*")
public class CsrfFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

        // Excluir /login y recursos estáticos para permitir autenticación sin fricción
        if ("/login".equals(path) || path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/") || path.startsWith("/error/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(true);
        CsrfUtil.getToken(session); // Ensure session has CSRF token

        String method = httpRequest.getMethod();

        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            String requestToken = httpRequest.getParameter("csrfToken");
            if (requestToken == null) {
                requestToken = httpRequest.getHeader("X-CSRF-TOKEN");
            }

            if (!CsrfUtil.validateToken(session, requestToken)) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Solicitud no autorizada: Token CSRF inválido o ausente.");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
