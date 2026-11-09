package pe.edu.utp.farmacia.dao;

import pe.edu.utp.farmacia.model.Prescripcion;
import pe.edu.utp.farmacia.model.PrescripcionDetalle;
import pe.edu.utp.farmacia.util.ConexionDB;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrescripcionDAOImpl implements PrescripcionDAO {

    @Override
    public List<Prescripcion> listar() throws SQLException {
        List<Prescripcion> lista = new ArrayList<>();
        String sp = "{CALL sp_listar_prescripciones()}";

        try (Connection conn = ConexionDB.getConnection();
             CallableStatement cs = conn.prepareCall(sp);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearResultSetPrescripcion(rs));
            }
        }

        for (Prescripcion p : lista) {
            p.setDetalles(listarDetalles(p.getIdPrescripcion()));
        }
        return lista;
    }

    @Override
    public Prescripcion obtenerPorId(int id) throws SQLException {
        String sp = "{CALL sp_obtener_prescripcion_por_id(?)}";
        Prescripcion prescripcion = null;

        try (Connection conn = ConexionDB.getConnection();
             CallableStatement cs = conn.prepareCall(sp)) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    prescripcion = mapearResultSetPrescripcion(rs);
                }
            }
        }

        if (prescripcion != null) {
            prescripcion.setDetalles(listarDetalles(id));
        }

        return prescripcion;
    }

    @Override
    public int registrar(Prescripcion p, String usuarioRegistro) throws SQLException {
        String sp = "{CALL sp_registrar_prescripcion(?, ?, ?, ?, ?, ?, ?, ?)}";
        int idGenerado = 0;

        try (Connection conn = ConexionDB.getConnection();
             CallableStatement cs = conn.prepareCall(sp)) {
            cs.setString(1, p.getCodigoPrescripcion());
            if (p.getIdPacienteExterno() != null) cs.setInt(2, p.getIdPacienteExterno()); else cs.setNull(2, java.sql.Types.INTEGER);
            if (p.getIdMedicoExterno() != null) cs.setInt(3, p.getIdMedicoExterno()); else cs.setNull(3, java.sql.Types.INTEGER);
            if (p.getIdAtencionExterna() != null) cs.setInt(4, p.getIdAtencionExterna()); else cs.setNull(4, java.sql.Types.INTEGER);
            cs.setTimestamp(5, p.getFechaPrescripcion());
            cs.setString(6, p.getInstruccionesGenerales());
            cs.setString(7, p.getEstado());
            cs.setString(8, usuarioRegistro);

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    idGenerado = rs.getInt("id_prescripcion");
                    p.setIdPrescripcion(idGenerado);
                }
            }
        }

        if (idGenerado > 0 && p.getDetalles() != null && !p.getDetalles().isEmpty()) {
            for (PrescripcionDetalle detalle : p.getDetalles()) {
                detalle.setIdPrescripcion(idGenerado);
                registrarDetalle(detalle, usuarioRegistro);
            }
        }

        return idGenerado;
    }

    @Override
    public void actualizar(Prescripcion p, String usuarioActualizacion) throws SQLException {
        String sp = "{CALL sp_actualizar_prescripcion(?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        try (Connection conn = ConexionDB.getConnection();
             CallableStatement cs = conn.prepareCall(sp)) {
            cs.setInt(1, p.getIdPrescripcion());
            cs.setString(2, p.getCodigoPrescripcion());
            if (p.getIdPacienteExterno() != null) cs.setInt(3, p.getIdPacienteExterno()); else cs.setNull(3, java.sql.Types.INTEGER);
            if (p.getIdMedicoExterno() != null) cs.setInt(4, p.getIdMedicoExterno()); else cs.setNull(4, java.sql.Types.INTEGER);
            if (p.getIdAtencionExterna() != null) cs.setInt(5, p.getIdAtencionExterna()); else cs.setNull(5, java.sql.Types.INTEGER);
            cs.setTimestamp(6, p.getFechaPrescripcion());
            cs.setString(7, p.getInstruccionesGenerales());
            cs.setString(8, p.getEstado());
            cs.setString(9, usuarioActualizacion);
            cs.executeUpdate();
        }
    }

    @Override
    public void eliminar(int id, String usuarioActualizacion) throws SQLException {
        String sp = "{CALL sp_eliminar_prescripcion(?, ?)}";
        try (Connection conn = ConexionDB.getConnection();
             CallableStatement cs = conn.prepareCall(sp)) {
            cs.setInt(1, id);
            cs.setString(2, usuarioActualizacion);
            cs.executeUpdate();
        }
    }

    @Override
    public List<PrescripcionDetalle> listarDetalles(int idPrescripcion) throws SQLException {
        List<PrescripcionDetalle> detalles = new ArrayList<>();
        String sp = "{CALL sp_listar_prescripcion_detalles_por_prescripcion(?)}";

        try (Connection conn = ConexionDB.getConnection();
             CallableStatement cs = conn.prepareCall(sp)) {
            cs.setInt(1, idPrescripcion);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    PrescripcionDetalle d = new PrescripcionDetalle();
                    d.setIdDetallePrescripcion(rs.getInt("id_detalle_prescripcion"));
                    d.setIdPrescripcion(rs.getInt("id_prescripcion"));
                    d.setIdMedicamento(rs.getObject("id_medicamento") != null ? rs.getInt("id_medicamento") : null);
                    d.setNombreMedicamento(rs.getString("nombre_medicamento"));
                    d.setCantidad(rs.getInt("cantidad"));
                    d.setDosis(rs.getString("dosis"));
                    d.setFrecuencia(rs.getString("frecuencia"));
                    d.setDuracion(rs.getString("duracion"));
                    d.setIndicaciones(rs.getString("indicaciones"));
                    d.setUsuarioRegistro(rs.getString("usuario_registro"));
                    d.setFechaRegistro(rs.getTimestamp("fecha_registro"));
                    d.setEstadoRegistro(rs.getString("estado_registro"));
                    detalles.add(d);
                }
            }
        }
        return detalles;
    }

    @Override
    public int registrarDetalle(PrescripcionDetalle d, String usuarioRegistro) throws SQLException {
        String sp = "{CALL sp_registrar_prescripcion_detalle(?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        try (Connection conn = ConexionDB.getConnection();
             CallableStatement cs = conn.prepareCall(sp)) {
            cs.setInt(1, d.getIdPrescripcion());
            if (d.getIdMedicamento() != null) cs.setInt(2, d.getIdMedicamento()); else cs.setNull(2, java.sql.Types.INTEGER);
            cs.setString(3, d.getNombreMedicamento());
            cs.setInt(4, d.getCantidad());
            cs.setString(5, d.getDosis());
            cs.setString(6, d.getFrecuencia());
            cs.setString(7, d.getDuracion());
            cs.setString(8, d.getIndicaciones());
            cs.setString(9, usuarioRegistro);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id_detalle_prescripcion");
                    d.setIdDetallePrescripcion(id);
                    return id;
                }
            }
        }
        return 0;
    }

    private Prescripcion mapearResultSetPrescripcion(ResultSet rs) throws SQLException {
        Prescripcion p = new Prescripcion();
        p.setIdPrescripcion(rs.getInt("id_prescripcion"));
        p.setCodigoPrescripcion(rs.getString("codigo_prescripcion"));
        p.setIdPacienteExterno(rs.getObject("id_paciente_externo") != null ? rs.getInt("id_paciente_externo") : null);
        p.setIdMedicoExterno(rs.getObject("id_medico_externo") != null ? rs.getInt("id_medico_externo") : null);
        p.setIdAtencionExterna(rs.getObject("id_atencion_externa") != null ? rs.getInt("id_atencion_externa") : null);
        p.setFechaPrescripcion(rs.getTimestamp("fecha_prescripcion"));
        p.setInstruccionesGenerales(rs.getString("instrucciones_generales"));
        p.setEstado(rs.getString("estado"));
        p.setFechaRecepcion(rs.getTimestamp("fecha_recepcion"));
        p.setUsuarioRegistro(rs.getString("usuario_registro"));
        p.setFechaRegistro(rs.getTimestamp("fecha_registro"));
        p.setEstadoRegistro(rs.getString("estado_registro"));
        return p;
    }
}
