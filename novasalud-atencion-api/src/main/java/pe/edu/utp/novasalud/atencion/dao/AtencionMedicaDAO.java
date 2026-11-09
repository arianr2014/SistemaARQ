package pe.edu.utp.novasalud.atencion.dao;

import pe.edu.utp.novasalud.atencion.model.AtencionMedica;
import pe.edu.utp.novasalud.atencion.util.ConexionDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AtencionMedicaDAO {

    public AtencionMedica obtenerPorId(int idAtencion) throws SQLException {
        String sql = "SELECT a.id_atencion, a.id_cita, a.id_medico, a.id_paciente, a.codigo_historia, a.motivo_consulta, a.sintomas, a.diagnostico, a.tratamiento, a.receta_medica, a.indicaciones, COALESCE(a.estado_envio_farmacia, 'NO_APLICA') AS estado_envio_farmacia, a.fecha_atencion, "
                + "CONCAT(COALESCE(m.nombres, ''), ' ', COALESCE(m.apellidos, '')) AS nombre_medico, "
                + "COALESCE(e.nombre, 'General') AS especialidad_nombre, "
                + "CONCAT(COALESCE(p.nombres, ''), ' ', COALESCE(p.apellidos, '')) AS nombre_paciente "
                + "FROM atencion_medica a "
                + "LEFT JOIN medico m ON a.id_medico = m.id_medico "
                + "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad "
                + "LEFT JOIN paciente p ON a.id_paciente = p.id_paciente "
                + "WHERE a.id_atencion=? AND a.estado_registro='ACTIVO'";
        try (Connection cn = ConexionDB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idAtencion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapperConRelaciones(rs);
            }
        }
        return null;
    }

    public AtencionMedica obtenerPorCita(int idCita) throws SQLException {
        try (Connection cn = ConexionDB.getConnection()) {
            return obtenerPorCitaConConexion(cn, idCita);
        }
    }

    public AtencionMedica obtenerPorCitaConConexion(Connection cn, int idCita) throws SQLException {
        String sql = "SELECT a.id_atencion, a.id_cita, a.id_medico, a.id_paciente, a.codigo_historia, a.motivo_consulta, a.sintomas, a.diagnostico, a.tratamiento, a.receta_medica, a.indicaciones, COALESCE(a.estado_envio_farmacia, 'NO_APLICA') AS estado_envio_farmacia, a.fecha_atencion, "
                + "CONCAT(COALESCE(m.nombres, ''), ' ', COALESCE(m.apellidos, '')) AS nombre_medico, "
                + "COALESCE(e.nombre, 'General') AS especialidad_nombre, "
                + "CONCAT(COALESCE(p.nombres, ''), ' ', COALESCE(p.apellidos, '')) AS nombre_paciente "
                + "FROM atencion_medica a "
                + "LEFT JOIN medico m ON a.id_medico = m.id_medico "
                + "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad "
                + "LEFT JOIN paciente p ON a.id_paciente = p.id_paciente "
                + "WHERE a.id_cita=? AND a.estado_registro='ACTIVO'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idCita);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapperConRelaciones(rs);
            }
        }
        return null;
    }

    public List<AtencionMedica> listarPorPaciente(int idPaciente) throws SQLException {
        List<AtencionMedica> lista = new ArrayList<>();
        String sql = "SELECT a.id_atencion, a.id_cita, a.id_medico, a.id_paciente, a.codigo_historia, a.motivo_consulta, a.sintomas, a.diagnostico, a.tratamiento, a.receta_medica, a.indicaciones, COALESCE(a.estado_envio_farmacia, 'NO_APLICA') AS estado_envio_farmacia, a.fecha_atencion, "
                + "CONCAT(COALESCE(m.nombres, ''), ' ', COALESCE(m.apellidos, '')) AS nombre_medico, "
                + "COALESCE(e.nombre, 'Medicina General') AS especialidad_nombre, "
                + "CONCAT(COALESCE(p.nombres, ''), ' ', COALESCE(p.apellidos, '')) AS nombre_paciente "
                + "FROM atencion_medica a "
                + "LEFT JOIN medico m ON a.id_medico = m.id_medico "
                + "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad "
                + "LEFT JOIN paciente p ON a.id_paciente = p.id_paciente "
                + "WHERE a.id_paciente=? AND a.estado_registro='ACTIVO' "
                + "ORDER BY a.fecha_atencion DESC, a.id_atencion DESC";
        try (Connection cn = ConexionDB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapperConRelaciones(rs));
                }
            }
        }
        return lista;
    }

    public List<AtencionMedica> listarPorCodigoHistoria(String codigoHistoria) throws SQLException {
        List<AtencionMedica> lista = new ArrayList<>();
        String sql = "SELECT a.id_atencion, a.id_cita, a.id_medico, a.id_paciente, a.codigo_historia, a.motivo_consulta, a.sintomas, a.diagnostico, a.tratamiento, a.receta_medica, a.indicaciones, COALESCE(a.estado_envio_farmacia, 'NO_APLICA') AS estado_envio_farmacia, a.fecha_atencion, "
                + "CONCAT(COALESCE(m.nombres, ''), ' ', COALESCE(m.apellidos, '')) AS nombre_medico, "
                + "COALESCE(e.nombre, 'Medicina General') AS especialidad_nombre, "
                + "CONCAT(COALESCE(p.nombres, ''), ' ', COALESCE(p.apellidos, '')) AS nombre_paciente "
                + "FROM atencion_medica a "
                + "LEFT JOIN medico m ON a.id_medico = m.id_medico "
                + "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad "
                + "LEFT JOIN paciente p ON a.id_paciente = p.id_paciente "
                + "WHERE a.codigo_historia=? AND a.estado_registro='ACTIVO' "
                + "ORDER BY a.fecha_atencion DESC, a.id_atencion DESC";
        try (Connection cn = ConexionDB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codigoHistoria);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapperConRelaciones(rs));
                }
            }
        }
        return lista;
    }

    public void guardarOActualizar(AtencionMedica atencion) throws SQLException {
        try (Connection cn = ConexionDB.getConnection()) {
            guardarOActualizarConConexion(cn, atencion);
        }
    }

    public void guardarOActualizarConConexion(Connection cn, AtencionMedica atencion) throws SQLException {
        AtencionMedica existente = obtenerPorCitaConConexion(cn, atencion.getIdCita());
        if (existente == null) {
            insertarConConexion(cn, atencion);
        } else {
            atencion.setIdAtencion(existente.getIdAtencion());
            actualizarConConexion(cn, atencion);
        }
    }

    private void insertarConConexion(Connection cn, AtencionMedica a) throws SQLException {
        String sql = "INSERT INTO atencion_medica(id_cita,id_medico,id_paciente,codigo_historia,motivo_consulta,sintomas,diagnostico,tratamiento,receta_medica,indicaciones,estado_envio_farmacia,fecha_atencion,estado_registro) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,NOW(),'ACTIVO')";
        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setValues(ps, a);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) a.setIdAtencion(rs.getInt(1)); }
        }
    }

    private void actualizarConConexion(Connection cn, AtencionMedica a) throws SQLException {
        String sql = "UPDATE atencion_medica SET motivo_consulta=?, sintomas=?, diagnostico=?, tratamiento=?, receta_medica=?, indicaciones=?, estado_envio_farmacia=COALESCE(?, estado_envio_farmacia), fecha_atencion=NOW() "
                + "WHERE id_atencion=? AND id_cita=? AND id_medico=? AND estado_registro='ACTIVO'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, a.getMotivoConsulta());
            ps.setString(2, a.getSintomas());
            ps.setString(3, a.getDiagnostico());
            ps.setString(4, a.getTratamiento());
            ps.setString(5, a.getRecetaMedica());
            ps.setString(6, a.getIndicaciones());
            ps.setString(7, a.getEstadoEnvioFarmacia());
            ps.setInt(8, a.getIdAtencion());
            ps.setInt(9, a.getIdCita());
            ps.setInt(10, a.getIdMedico());
            ps.executeUpdate();
        }
    }

    private void setValues(PreparedStatement ps, AtencionMedica a) throws SQLException {
        ps.setInt(1, a.getIdCita());
        ps.setInt(2, a.getIdMedico());
        ps.setInt(3, a.getIdPaciente());
        ps.setString(4, a.getCodigoHistoria());
        ps.setString(5, a.getMotivoConsulta());
        ps.setString(6, a.getSintomas());
        ps.setString(7, a.getDiagnostico());
        ps.setString(8, a.getTratamiento());
        ps.setString(9, a.getRecetaMedica());
        ps.setString(10, a.getIndicaciones());
        ps.setString(11, a.getEstadoEnvioFarmacia() != null && !a.getEstadoEnvioFarmacia().isBlank() ? a.getEstadoEnvioFarmacia() : "NO_APLICA");
    }

    private AtencionMedica mapperConRelaciones(ResultSet rs) throws SQLException {
        AtencionMedica a = new AtencionMedica();
        a.setIdAtencion(rs.getInt("id_atencion"));
        a.setIdCita(rs.getInt("id_cita"));
        a.setIdMedico(rs.getInt("id_medico"));
        a.setIdPaciente(rs.getInt("id_paciente"));
        a.setCodigoHistoria(rs.getString("codigo_historia"));
        a.setMotivoConsulta(rs.getString("motivo_consulta"));
        a.setSintomas(rs.getString("sintomas"));
        a.setDiagnostico(rs.getString("diagnostico"));
        a.setTratamiento(rs.getString("tratamiento"));
        a.setRecetaMedica(rs.getString("receta_medica"));
        a.setIndicaciones(rs.getString("indicaciones"));
        try { a.setEstadoEnvioFarmacia(rs.getString("estado_envio_farmacia")); } catch (Exception ignored) {}
        a.setFechaAtencion(rs.getString("fecha_atencion"));
        try { a.setNombreMedico(rs.getString("nombre_medico")); } catch (Exception ignored) {}
        try { a.setEspecialidadNombre(rs.getString("especialidad_nombre")); } catch (Exception ignored) {}
        try { a.setNombrePaciente(rs.getString("nombre_paciente")); } catch (Exception ignored) {}
        return a;
    }

    public boolean actualizarEstadoEnvioFarmacia(int idAtencion, String estado) throws SQLException {
        String sql = "UPDATE atencion_medica SET estado_envio_farmacia=? WHERE id_atencion=? AND estado_registro='ACTIVO'";
        try (Connection cn = ConexionDB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, idAtencion);
            return ps.executeUpdate() > 0;
        }
    }

    public List<AtencionMedica> listarPendientesEnvioFarmacia() throws SQLException {
        List<AtencionMedica> lista = new ArrayList<>();
        String sql = "SELECT a.id_atencion, a.id_cita, a.id_medico, a.id_paciente, a.codigo_historia, a.motivo_consulta, a.sintomas, a.diagnostico, a.tratamiento, a.receta_medica, a.indicaciones, COALESCE(a.estado_envio_farmacia, 'NO_APLICA') AS estado_envio_farmacia, a.fecha_atencion, "
                + "CONCAT(COALESCE(m.nombres, ''), ' ', COALESCE(m.apellidos, '')) AS nombre_medico, "
                + "COALESCE(e.nombre, 'General') AS especialidad_nombre, "
                + "CONCAT(COALESCE(p.nombres, ''), ' ', COALESCE(p.apellidos, '')) AS nombre_paciente "
                + "FROM atencion_medica a "
                + "LEFT JOIN medico m ON a.id_medico = m.id_medico "
                + "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad "
                + "LEFT JOIN paciente p ON a.id_paciente = p.id_paciente "
                + "WHERE a.estado_envio_farmacia='PENDIENTE_ENVIO_FARMACIA' AND a.estado_registro='ACTIVO' "
                + "ORDER BY a.fecha_atencion ASC";
        try (Connection cn = ConexionDB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapperConRelaciones(rs));
            }
        }
        return lista;
    }
}
