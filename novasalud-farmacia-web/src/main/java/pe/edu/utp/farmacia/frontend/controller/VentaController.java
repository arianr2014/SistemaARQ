package pe.edu.utp.farmacia.frontend.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.farmacia.frontend.model.LoteMedicamento;
import pe.edu.utp.farmacia.frontend.model.Medicamento;
import pe.edu.utp.farmacia.frontend.model.Prescripcion;
import pe.edu.utp.farmacia.frontend.model.UsuarioFarmacia;
import pe.edu.utp.farmacia.frontend.model.Venta;
import pe.edu.utp.farmacia.frontend.util.ApiClient;
import pe.edu.utp.farmacia.frontend.util.AppLogger;
import pe.edu.utp.farmacia.frontend.util.JwtTokenContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;

@WebServlet("/ventas")
public class VentaController extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        String token = (session != null) ? (String) session.getAttribute("jwtToken") : null;
        JwtTokenContext.setToken(token);

        String mode = request.getParameter("mode");
        String action = request.getParameter("action");
        String idParam = request.getParameter("id");

        if ("boleta".equals(action) || "detalle".equals(action)) {
            if (idParam != null && !idParam.isBlank()) {
                try {
                    int idVenta = Integer.parseInt(idParam);
                    ApiClient.ApiResponse apiResp = ApiClient.get("/ventas/" + idVenta, token);
                    if (apiResp.getStatusCode() == 200) {
                        Venta venta = gson.fromJson(apiResp.getBody(), Venta.class);
                        request.setAttribute("venta", venta);
                    }
                } catch (Exception e) {
                    AppLogger.error("Error cargando boleta de venta id=" + idParam + ": " + e.getMessage(), e);
                    request.setAttribute("error", "No se pudo obtener el detalle de la venta.");
                }
            }
            request.getRequestDispatcher("/WEB-INF/views/venta/boleta.jsp").forward(request, response);
            return;
        }

        if ("pos".equals(mode)) {
            // Check if loading from a prescription
            String fromPrescripcion = request.getParameter("fromPrescripcion");
            if (fromPrescripcion != null && !fromPrescripcion.isBlank()) {
                try {
                    int idPrescripcion = Integer.parseInt(fromPrescripcion);
                    ApiClient.ApiResponse presResp = ApiClient.get("/prescripciones/" + idPrescripcion, token);
                    if (presResp.getStatusCode() == 200) {
                        Prescripcion prescripcion = gson.fromJson(presResp.getBody(), Prescripcion.class);
                        request.setAttribute("prescripcionPreload", prescripcion);
                        request.setAttribute("prescripcionPreloadJson", presResp.getBody());
                    }
                } catch (Exception e) {
                    AppLogger.error("Error precargando prescripción id=" + fromPrescripcion + ": " + e.getMessage(), e);
                }
            }

            // Load available lotes and medicamentos for POS billing
            try {
                ApiClient.ApiResponse medResp = ApiClient.get("/medicamentos", token);
                if (medResp.getStatusCode() == 200) {
                    Type listType = new TypeToken<List<Medicamento>>() {}.getType();
                    List<Medicamento> medicamentos = gson.fromJson(medResp.getBody(), listType);
                    request.setAttribute("medicamentos", medicamentos);
                }

                ApiClient.ApiResponse loteResp = ApiClient.get("/lotes", token);
                if (loteResp.getStatusCode() == 200) {
                    Type listType = new TypeToken<List<LoteMedicamento>>() {}.getType();
                    List<LoteMedicamento> lotes = gson.fromJson(loteResp.getBody(), listType);
                    request.setAttribute("lotes", lotes);
                }
            } catch (Exception e) {
                AppLogger.error("Error cargando datos para Punto de Venta: " + e.getMessage(), e);
            }

            request.getRequestDispatcher("/WEB-INF/views/venta/pos.jsp").forward(request, response);
            return;
        }

        // Default: sales history list
        try {
            ApiClient.ApiResponse apiResp = ApiClient.get("/ventas", token);
            if (apiResp.getStatusCode() == 200) {
                Type listType = new TypeToken<List<Venta>>() {}.getType();
                List<Venta> ventas = gson.fromJson(apiResp.getBody(), listType);
                request.setAttribute("ventas", ventas);
            }
        } catch (Exception e) {
            AppLogger.error("Error obteniendo lista de ventas: " + e.getMessage(), e);
            request.setAttribute("error", "Error de conexión con la API.");
        }

        request.getRequestDispatcher("/WEB-INF/views/venta/list.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        String token = (session != null) ? (String) session.getAttribute("jwtToken") : null;
        UsuarioFarmacia usuarioLogueado = (session != null) ? (UsuarioFarmacia) session.getAttribute("usuarioLogueado") : null;
        JwtTokenContext.setToken(token);

        String contentType = request.getContentType();

        try {
            if (contentType != null && contentType.contains("application/json")) {
                // Read JSON body for POS sales submission
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = request.getReader();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                Venta ventaData = gson.fromJson(sb.toString(), Venta.class);
                if (usuarioLogueado != null && (ventaData.getIdUsuario() == null || ventaData.getIdUsuario() == 0)) {
                    ventaData.setIdUsuario(usuarioLogueado.getIdUsuario());
                }

                ApiClient.ApiResponse apiResp = ApiClient.post("/ventas", gson.toJson(ventaData), token);
                
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setStatus(apiResp.getStatusCode());
                response.getWriter().write(apiResp.getBody());
                return;
            }
        } catch (Exception e) {
            AppLogger.error("Error procesando registro de venta: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/ventas");
    }
}
