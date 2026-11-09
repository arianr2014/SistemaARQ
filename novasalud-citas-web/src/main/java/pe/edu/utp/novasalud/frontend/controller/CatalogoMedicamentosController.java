package pe.edu.utp.novasalud.frontend.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.novasalud.frontend.util.ApiClient;
import pe.edu.utp.novasalud.frontend.util.AppLogger;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controlador proxy para búsqueda en el catálogo de medicamentos de Farmacia API.
 * Invocado vía AJAX desde el formulario de atención médica para seleccionar fármacos reales.
 */
@WebServlet("/catalogo-medicamentos")
public class CatalogoMedicamentosController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("rolUsuario") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Sesión expirada o no autorizada\"}");
            return;
        }

        String q = request.getParameter("q");
        try {
            String endpoint = "/medicamentos";
            if (q != null && !q.isBlank()) {
                endpoint += "?q=" + URLEncoder.encode(q.trim(), StandardCharsets.UTF_8);
            }
            String jsonResp = ApiClient.get(endpoint, String.class);
            response.getWriter().write(jsonResp != null ? jsonResp : "[]");
        } catch (Exception ex) {
            AppLogger.error(getClass(), "Error al consultar catálogo de medicamentos en Farmacia API", ex);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write("{\"error\":\"No se pudo conectar con el catálogo de Farmacia API\"}");
        }
    }
}