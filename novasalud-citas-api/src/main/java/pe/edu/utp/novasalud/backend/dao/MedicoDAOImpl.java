package pe.edu.utp.novasalud.backend.dao;

import pe.edu.utp.novasalud.backend.model.Medico;
import pe.edu.utp.novasalud.backend.util.ConexionDB;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MedicoDAOImpl implements MedicoDAO {

    @Override
    public List<Medico> listar(String filtro) throws SQLException {
        List<Medico> medicos = new ArrayList<>();
        String texto = filtro == null ? "" : filtro.trim();

        StringBuilder sql = new StringBuilder(
        "SELECT m.id_medico, m.nombres, m.apellidos, m.id_especialidad, e.nombre AS especialidad, m.dni, m.telefono, m.correo, m.cmp "
        + "FROM medico m "
        + "INNER JOIN especialidad e ON m.id_especialidad = e.id_especialidad "
        + "WHERE m.estado_registro = 'ACTIVO' "
        + "AND e.estado_registro = 'ACTIVO' "
        );

        if (!texto.isEmpty()) {
            sql.append("AND (m.nombres LIKE ? OR m.apellidos LIKE ? OR e.nombre LIKE ?) ");
        }

        sql.append("ORDER BY m.id_medico DESC");

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            if (!texto.isEmpty()) {
                String like = "%" + texto + "%";
                statement.setString(1, like);
                statement.setString(2, like);
                statement.setString(3, like);
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Medico medico = new Medico();
                    medico.setIdMedico(rs.getInt("id_medico"));
                    medico.setNombres(rs.getString("nombres"));
                    medico.setApellidos(rs.getString("apellidos"));
                    medico.setIdEspecialidad(rs.getInt("id_especialidad"));
                    medico.setNombreEspecialidad(rs.getString("especialidad"));
                    medico.setDni(rs.getString("dni"));
                    medico.setTelefono(rs.getString("telefono"));
                    medico.setCorreo(rs.getString("correo"));
                    medico.setCmp(rs.getString("cmp"));
                    medicos.add(medico);
                }
            }
        }

        return medicos;
    }

    @Override
    public Medico obtenerPorId(int idMedico) throws SQLException {
        String sql = "SELECT m.id_medico, m.nombres, m.apellidos, m.id_especialidad, "
        + "e.nombre AS especialidad, m.dni, m.telefono, m.correo, m.cmp, "
        + "m.duracion_cita_minutos, m.tolerancia_minutos, m.max_citas_dia, m.consultorio "
        + "FROM medico m "
        + "LEFT JOIN especialidad e ON m.id_especialidad = e.id_especialidad "
        + "WHERE m.id_medico = ? AND m.estado_registro = 'ACTIVO'";

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idMedico);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Medico medico = new Medico();
                    medico.setIdMedico(rs.getInt("id_medico"));
                    medico.setNombres(rs.getString("nombres"));
                    medico.setApellidos(rs.getString("apellidos"));
                    medico.setIdEspecialidad(rs.getInt("id_especialidad"));
                    medico.setNombreEspecialidad(rs.getString("especialidad"));
                    medico.setDni(rs.getString("dni"));
                    medico.setTelefono(rs.getString("telefono"));
                    medico.setCorreo(rs.getString("correo"));
                    medico.setCmp(rs.getString("cmp"));
                    medico.setDuracionCitaMinutos(rs.getInt("duracion_cita_minutos"));
                    medico.setToleranciaMinutos(rs.getInt("tolerancia_minutos"));
                    medico.setMaxCitasDia(rs.getInt("max_citas_dia"));
                    medico.setConsultorio(rs.getString("consultorio"));
                    return medico;
                }
            }
        }

        return null;
    }

    @Override
    public List<Medico> listarPorEspecialidad(int idEspecialidad) throws SQLException {
        List<Medico> medicos = new ArrayList<>();
        String sql = "SELECT id_medico, nombres, apellidos, id_especialidad, telefono "
        + "FROM medico "
        + "WHERE id_especialidad = ? AND estado_registro = 'ACTIVO' "
        + "ORDER BY nombres, apellidos";

        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idEspecialidad);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Medico medico = new Medico();
                    medico.setIdMedico(rs.getInt("id_medico"));
                    medico.setNombres(rs.getString("nombres"));
                    medico.setApellidos(rs.getString("apellidos"));
                    medico.setIdEspecialidad(rs.getInt("id_especialidad"));
                    medico.setTelefono(rs.getString("telefono"));
                    medicos.add(medico);
                }
            }
        }

        return medicos;
    }

    @Override
    public void guardar(Medico medico, String usuarioRegistro) throws SQLException {
        if (medico.getIdMedico() > 0) {
            actualizar(medico);
        } else {
            registrar(medico, usuarioRegistro);
        }
    }

    private void registrar(Medico medico, String usuarioRegistro) throws SQLException {
        String sp = "{CALL sp_registrar_medico(?, ?, ?, ?, ?)}";

        try (Connection connection = ConexionDB.getConnection();
             CallableStatement statement = connection.prepareCall(sp)) {
            statement.setString(1, medico.getNombres());
            statement.setString(2, medico.getApellidos());
            statement.setInt(3, medico.getIdEspecialidad());
            statement.setString(4, medico.getTelefono());
            statement.setString(5, usuarioRegistro);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    medico.setIdMedico(rs.getInt(1));
                }
            }
        }
    }

    private void actualizar(Medico medico) throws SQLException {
        String sp = "{CALL sp_actualizar_medico(?, ?, ?, ?, ?)}";

        try (Connection connection = ConexionDB.getConnection();
             CallableStatement statement = connection.prepareCall(sp)) {
            statement.setInt(1, medico.getIdMedico());
            statement.setString(2, medico.getNombres());
            statement.setString(3, medico.getApellidos());
            statement.setInt(4, medico.getIdEspecialidad());
            statement.setString(5, medico.getTelefono());
            statement.executeUpdate();
        }
    }

    @Override
    public void eliminar(int idMedico) throws SQLException {
        String sp = "{CALL sp_eliminar_medico(?)}";

        try (Connection connection = ConexionDB.getConnection();
             CallableStatement statement = connection.prepareCall(sp)) {
            statement.setInt(1, idMedico);
            statement.executeUpdate();
        }
    }

    @Override
    public int contarCitasActivasDia(int idMedico, String fecha) throws SQLException {
        String sql = "SELECT COUNT(*) FROM cita WHERE id_medico=? AND fecha=? AND estado_registro='ACTIVO' AND UPPER(estado) NOT IN ('CANCELADA','ANULADA','NO_ASISTIO')";
        try (Connection connection = ConexionDB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idMedico);
            statement.setString(2, fecha);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
