package pe.edu.utp.novasalud.frontend.controller;

import pe.edu.utp.novasalud.frontend.model.AtencionMedica;
import pe.edu.utp.novasalud.frontend.model.Cita;
import pe.edu.utp.novasalud.frontend.util.ApiClient;
import pe.edu.utp.novasalud.frontend.util.AppLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador de Atención Médica para Frontend Web Java.
 * Invoca el Orquestador REST SOA (`novasalud-orchestrator`) o la API de Atención (`novasalud-atencion-api`).
 */
@WebServlet("/atencion-medica")
public class AtencionMedicaController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        cargarFormulario(request, response, null, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        int idMedicoSesion = obtenerIdMedicoSesion(session);
        int idCita = parseEntero(request.getParameter("idCita"));
        String accion = texto(request.getParameter("accion"));

        try {
            validarDoctor(session);
            AtencionMedica atencion = leerAtencion(request);
            atencion.setIdMedico(idMedicoSesion);

            if (atencion.getIdPaciente() <= 0 || atencion.getCodigoHistoria().isBlank()) {
                try {
                    Cita cita = ApiClient.get("/citas/" + idCita, Cita.class);
                    if (cita != null) {
                        if (atencion.getIdPaciente() <= 0) {
                            atencion.setIdPaciente(cita.getIdPaciente());
                        }
                        if (atencion.getCodigoHistoria().isBlank()) {
                            atencion.setCodigoHistoria(cita.getHistoriaClinicaCodigo());
                        }
                    }
                } catch (Exception ex) {
                    AppLogger.warning(getClass(), "No se pudo autocompletar paciente desde cita: " + ex.getMessage(), ex);
                }
            }

            boolean marcarAtendida = "guardar_atender".equals(accion);

            if (marcarAtendida) {
                // Invocar al Orquestador REST SOA para ejecutar el flujo end-to-end (Cita -> Atención -> Farmacia)
                // Se elimina el fallback directo a Atención API para respetar estrictamente la arquitectura SOA
                List<Map<String, Object>> medList = extraerMedicamentosEstructurados(request);
                if (!medList.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (Map<String, Object> m : medList) {
                        if (sb.length() > 0) sb.append("; ");
                        sb.append(m.get("nombre")).append(" (").append(m.get("dosis")).append(", ")
                          .append(m.get("frecuencia")).append(", ").append(m.get("duracion"))
                          .append(", Cant: ").append(m.get("cantidad")).append(")");
                    }
                    if (atencion.getRecetaMedica() != null && !atencion.getRecetaMedica().isBlank()) {
                        sb.append(" | ").append(atencion.getRecetaMedica());
                    }
                    atencion.setRecetaMedica(sb.toString());
                } else if (atencion.getRecetaMedica() != null && !atencion.getRecetaMedica().isBlank()) {
                    Map<String, Object> med = new HashMap<>();
                    med.put("nombre", atencion.getRecetaMedica());
                    med.put("dosis", "Según indicación médica");
                    med.put("frecuencia", "Cada 8 horas");
                    med.put("duracion", "3 días");
                    med.put("cantidad", 1);
                    medList.add(med);
                }

                Map<String, Object> reqOrquestador = new HashMap<>();
                reqOrquestador.put("idCita", atencion.getIdCita());
                reqOrquestador.put("idMedico", idMedicoSesion);
                reqOrquestador.put("motivoConsulta", atencion.getMotivoConsulta());
                reqOrquestador.put("sintomas", atencion.getSintomas());
                reqOrquestador.put("diagnostico", atencion.getDiagnostico());
                reqOrquestador.put("tratamiento", atencion.getTratamiento());
                reqOrquestador.put("indicaciones", atencion.getIndicaciones());
                reqOrquestador.put("recetaMedica", atencion.getRecetaMedica());

                if (!medList.isEmpty()) {
                    Map<String, Object> presc = new HashMap<>();
                    presc.put("medicamentos", medList);
                    reqOrquestador.put("prescripcion", presc);
                }

                Map<?, ?> respOrq = ApiClient.post("/orquestacion/atencion", reqOrquestador, Map.class);
                String estadoFarmacia = (respOrq != null && respOrq.get("estadoFarmacia") != null) 
                        ? String.valueOf(respOrq.get("estadoFarmacia")) : "NO_APLICA";

                response.sendRedirect(request.getContextPath() + "/agenda-medico?buscar=1&fecha=" 
                        + texto(request.getParameter("fecha")) + "&msg=attended&estadoFarmacia=" + estadoFarmacia);
            } else {
                String url = "/atencion-medica?idMedicoSesion=" + idMedicoSesion + "&marcarAtendida=false";
                ApiClient.post(url, atencion, AtencionMedica.class);
                response.sendRedirect(request.getContextPath() + "/atencion-medica?idCita=" + idCita + "&msg=saved");
            }
        } catch (Exception ex) {
            AppLogger.error(getClass(), "Error al guardar atención médica vía REST API", ex);
            String errorMsg = ex.getMessage();
            if (errorMsg != null && (errorMsg.contains("ERROR_CITA_NO_PAGADA") || errorMsg.contains("no está registrada como PAGADA"))) {
                errorMsg = "Cita no pagada en caja: Debe registrarse el pago de la cita antes de poder finalizar la atención médica.";
            } else if (errorMsg != null && errorMsg.contains("ERROR_CITA_NOT_FOUND")) {
                errorMsg = "La cita médica indicada no existe en el sistema.";
            }
            cargarFormulario(request, response, errorMsg, leerAtencion(request));
        }
    }

    private void cargarFormulario(HttpServletRequest request, HttpServletResponse response, String error, AtencionMedica atencionTemporal)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        int idCita = parseEntero(request.getParameter("idCita"));
        int idMedicoSesion = obtenerIdMedicoSesion(session);
        try {
            validarDoctor(session);
            Cita cita = ApiClient.get("/citas/" + idCita, Cita.class);
            if (cita == null || cita.getIdMedico() != idMedicoSesion) {
                response.sendRedirect(request.getContextPath() + "/acceso-denegado?from=atencion-medica");
                return;
            }

            AtencionMedica atencion = atencionTemporal;
            if (atencion == null) {
                try {
                    atencion = ApiClient.get("/atencion-medica/cita/" + idCita, AtencionMedica.class);
                } catch (Exception ignored) {}
            }

            if (atencion == null) {
                atencion = new AtencionMedica();
                atencion.setIdCita(cita.getIdCita());
                atencion.setIdMedico(cita.getIdMedico());
                atencion.setIdPaciente(cita.getIdPaciente());
                atencion.setCodigoHistoria(cita.getHistoriaClinicaCodigo());
            } else {
                if (atencion.getIdPaciente() <= 0) {
                    atencion.setIdPaciente(cita.getIdPaciente());
                }
                if (atencion.getIdMedico() <= 0) {
                    atencion.setIdMedico(cita.getIdMedico());
                }
                if (atencion.getCodigoHistoria() == null || atencion.getCodigoHistoria().isBlank()) {
                    atencion.setCodigoHistoria(cita.getHistoriaClinicaCodigo());
                }
            }

            request.setAttribute("cita", cita);
            request.setAttribute("atencion", atencion);
            if (error != null) request.setAttribute("error", error);
            request.getRequestDispatcher("/WEB-INF/views/atencion/form.jsp").forward(request, response);
        } catch (Exception ex) {
            AppLogger.error(getClass(), "Error al cargar atención médica vía REST API", ex);
            response.sendRedirect(request.getContextPath() + "/acceso-denegado?from=atencion-medica");
        }
    }

    private AtencionMedica leerAtencion(HttpServletRequest request) {
        AtencionMedica atencion = new AtencionMedica();
        atencion.setIdCita(parseEntero(request.getParameter("idCita")));
        atencion.setIdPaciente(parseEntero(request.getParameter("idPaciente")));
        atencion.setIdMedico(parseEntero(request.getParameter("idMedico")));
        atencion.setMotivoConsulta(texto(request.getParameter("motivoConsulta")));
        atencion.setSintomas(texto(request.getParameter("sintomas")));
        atencion.setDiagnostico(texto(request.getParameter("diagnostico")));
        atencion.setTratamiento(texto(request.getParameter("tratamiento")));
        atencion.setRecetaMedica(texto(request.getParameter("recetaMedica")));
        atencion.setIndicaciones(texto(request.getParameter("indicaciones")));
        atencion.setCodigoHistoria(texto(request.getParameter("codigoHistoria")));
        return atencion;
    }

    private void validarDoctor(HttpSession session) {
        if (session == null || !"DOCTOR".equalsIgnoreCase(String.valueOf(session.getAttribute("rolUsuario")))) {
            throw new IllegalArgumentException("Solo el médico responsable puede registrar la atención clínica.");
        }
        if (obtenerIdMedicoSesion(session) <= 0) {
            throw new IllegalArgumentException("Su usuario no está vinculado a un médico real.");
        }
    }

    private List<Map<String, Object>> extraerMedicamentosEstructurados(HttpServletRequest request) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String[] nombres = request.getParameterValues("medicamentoNombre");
        if (nombres == null) nombres = request.getParameterValues("medicamentoNombre[]");
        String[] dosis = request.getParameterValues("medicamentoDosis");
        if (dosis == null) dosis = request.getParameterValues("medicamentoDosis[]");
        String[] frecuencias = request.getParameterValues("medicamentoFrecuencia");
        if (frecuencias == null) frecuencias = request.getParameterValues("medicamentoFrecuencia[]");
        String[] duraciones = request.getParameterValues("medicamentoDuracion");
        if (duraciones == null) duraciones = request.getParameterValues("medicamentoDuracion[]");
        String[] cantidades = request.getParameterValues("medicamentoCantidad");
        if (cantidades == null) cantidades = request.getParameterValues("medicamentoCantidad[]");

        if (nombres != null) {
            for (int i = 0; i < nombres.length; i++) {
                String nombre = nombres[i] != null ? nombres[i].trim() : "";
                if (nombre.isBlank()) continue;

                Map<String, Object> med = new HashMap<>();
                med.put("nombre", nombre);
                med.put("dosis", (dosis != null && i < dosis.length && !dosis[i].isBlank()) ? dosis[i].trim() : "1 tableta");
                med.put("frecuencia", (frecuencias != null && i < frecuencias.length && !frecuencias[i].isBlank()) ? frecuencias[i].trim() : "Cada 8 horas");
                med.put("duracion", (duraciones != null && i < duraciones.length && !duraciones[i].isBlank()) ? duraciones[i].trim() : "3 días");
                int cant = 1;
                if (cantidades != null && i < cantidades.length) {
                    try { cant = Integer.parseInt(cantidades[i].trim()); } catch (Exception ignored) {}
                }
                med.put("cantidad", cant > 0 ? cant : 1);
                lista.add(med);
            }
        }
        return lista;
    }

    private int obtenerIdMedicoSesion(HttpSession session) {
        if (session == null || session.getAttribute("idMedico") == null) return 0;
        try { return Integer.parseInt(String.valueOf(session.getAttribute("idMedico"))); } catch (NumberFormatException ex) { return 0; }
    }

    private int parseEntero(String value) { try { return Integer.parseInt(value); } catch (Exception ex) { return 0; } }
    private String texto(String value) { return value == null ? "" : value.trim(); }
}
