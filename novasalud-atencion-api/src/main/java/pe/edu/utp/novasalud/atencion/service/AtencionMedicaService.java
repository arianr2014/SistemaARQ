package pe.edu.utp.novasalud.atencion.service;

import pe.edu.utp.novasalud.atencion.dao.AtencionMedicaDAO;
import pe.edu.utp.novasalud.atencion.dao.HistoriaClinicaDAO;
import pe.edu.utp.novasalud.atencion.model.AtencionMedica;
import pe.edu.utp.novasalud.atencion.util.ConexionDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class AtencionMedicaService {

    private final AtencionMedicaDAO atencionDAO = new AtencionMedicaDAO();
    private final HistoriaClinicaDAO historiaDAO = new HistoriaClinicaDAO();

    public AtencionMedica obtenerPorId(int idAtencion) throws SQLException {
        if (idAtencion <= 0) return null;
        return atencionDAO.obtenerPorId(idAtencion);
    }

    public AtencionMedica obtenerPorCita(int idCita) throws SQLException {
        if (idCita <= 0) return null;
        return atencionDAO.obtenerPorCita(idCita);
    }

    public List<AtencionMedica> listarPorPaciente(int idPaciente) throws SQLException {
        if (idPaciente <= 0) return List.of();
        return atencionDAO.listarPorPaciente(idPaciente);
    }

    public List<AtencionMedica> listarPendientesEnvioFarmacia() throws SQLException {
        return atencionDAO.listarPendientesEnvioFarmacia();
    }

    public boolean actualizarEstadoEnvioFarmacia(int idAtencion, String estado) throws SQLException {
        if (idAtencion <= 0 || estado == null || estado.isBlank()) {
            throw new IllegalArgumentException("Parámetros inválidos para actualizar estado de farmacia.");
        }
        return atencionDAO.actualizarEstadoEnvioFarmacia(idAtencion, estado.trim().toUpperCase());
    }

    public void guardar(AtencionMedica atencion, int idMedicoSesion) throws SQLException {
        validar(atencion);
        try (Connection cn = ConexionDB.getConnection()) {
            historiaDAO.asegurarExisteHistoriaClinica(cn, atencion.getIdPaciente(), atencion.getCodigoHistoria(), String.valueOf(idMedicoSesion));
            atencionDAO.guardarOActualizarConConexion(cn, atencion);
        }
    }

    public void guardarYMarcarAtendida(AtencionMedica atencion, int idMedicoSesion) throws SQLException {
        validar(atencion);
        try (Connection cn = ConexionDB.getConnection()) {
            cn.setAutoCommit(false);
            try {
                historiaDAO.asegurarExisteHistoriaClinica(cn, atencion.getIdPaciente(), atencion.getCodigoHistoria(), String.valueOf(idMedicoSesion));
                atencionDAO.guardarOActualizarConConexion(cn, atencion);
                
                // Actualizar estado de la cita a ATENDIDA
                marcarCitaComoAtendida(cn, atencion.getIdCita());
                
                cn.commit();
            } catch (SQLException ex) {
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        }
    }

    private void marcarCitaComoAtendida(Connection cn, int idCita) throws SQLException {
        String sql = "UPDATE cita SET estado_cita='ATENDIDA', usuario_actualizacion='atencion-api', fecha_actualizacion=NOW() WHERE id_cita=?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idCita);
            ps.executeUpdate();
        }
    }

    private void validar(AtencionMedica atencion) {
        if (atencion == null) throw new IllegalArgumentException("No se recibió información de la atención médica.");
        if (atencion.getIdCita() <= 0) throw new IllegalArgumentException("La cita es obligatoria.");
        if (isBlank(atencion.getMotivoConsulta()) || isBlank(atencion.getSintomas()) || isBlank(atencion.getDiagnostico()) || isBlank(atencion.getTratamiento())) {
            throw new IllegalArgumentException("Motivo, síntomas, diagnóstico y tratamiento son obligatorios.");
        }
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
