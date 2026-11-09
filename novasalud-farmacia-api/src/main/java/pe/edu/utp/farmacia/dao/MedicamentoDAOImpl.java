package pe.edu.utp.farmacia.dao;

import pe.edu.utp.farmacia.model.Medicamento;
import pe.edu.utp.farmacia.util.ConexionDB;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MedicamentoDAOImpl implements MedicamentoDAO {

    @Override
    public List<Medicamento> listar() throws SQLException {
        List<Medicamento> lista = new ArrayList<>();
        String sp = "{CALL sp_listar_medicamentos()}";

        try (Connection conn = ConexionDB.getConnection();
             CallableStatement cs = conn.prepareCall(sp);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearResultSet(rs));
            }
        }
        return lista;
    }

    @Override
    public List<Medicamento> buscar(String criterio) throws SQLException {
        if (criterio == null || criterio.isBlank()) {
            return listar();
        }
        List<Medicamento> lista = new ArrayList<>();
        String sql = "SELECT m.id_medicamento, m.codigo, m.nombre, m.principio_activo, m.concentracion, "
                   + "m.forma_farmaceutica, m.presentacion, m.id_categoria, c.nombre AS nombre_categoria, "
                   + "m.precio_venta, m.requiere_receta, m.usuario_registro, m.fecha_registro, "
                   + "m.usuario_actualizacion, m.fecha_actualizacion, m.estado_registro "
                   + "FROM medicamento m "
                   + "LEFT JOIN categoria_medicamento c ON m.id_categoria = c.id_categoria "
                   + "WHERE m.estado_registro = 'ACTIVO' AND ("
                   + "  LOWER(m.nombre) LIKE LOWER(?) OR "
                   + "  LOWER(m.principio_activo) LIKE LOWER(?) OR "
                   + "  LOWER(m.codigo) LIKE LOWER(?)"
                   + ") ORDER BY m.nombre ASC LIMIT 50";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String patron = "%" + criterio.trim() + "%";
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearResultSet(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public Medicamento obtenerPorId(int id) throws SQLException {
        String sp = "{CALL sp_obtener_medicamento_por_id(?)}";
        try (Connection conn = ConexionDB.getConnection();
             CallableStatement cs = conn.prepareCall(sp)) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return mapearResultSet(rs);
                }
            }
        }
        return null;
    }

    @Override
    public int registrar(Medicamento m, String usuarioRegistro) throws SQLException {
        String sp = "{CALL sp_registrar_medicamento(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        try (Connection conn = ConexionDB.getConnection();
             CallableStatement cs = conn.prepareCall(sp)) {
            cs.setString(1, m.getCodigo());
            cs.setString(2, m.getNombre());
            cs.setString(3, m.getPrincipioActivo());
            cs.setString(4, m.getConcentracion());
            cs.setString(5, m.getFormaFarmaceutica());
            cs.setString(6, m.getPresentacion());
            cs.setInt(7, m.getIdCategoria());
            cs.setBigDecimal(8, m.getPrecioVenta());
            cs.setBoolean(9, m.isRequiereReceta());
            cs.setString(10, usuarioRegistro);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id_medicamento");
                    m.setIdMedicamento(id);
                    return id;
                }
            }
        }
        return 0;
    }

    @Override
    public void actualizar(Medicamento m, String usuarioActualizacion) throws SQLException {
        String sp = "{CALL sp_actualizar_medicamento(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        try (Connection conn = ConexionDB.getConnection();
             CallableStatement cs = conn.prepareCall(sp)) {
            cs.setInt(1, m.getIdMedicamento());
            cs.setString(2, m.getCodigo());
            cs.setString(3, m.getNombre());
            cs.setString(4, m.getPrincipioActivo());
            cs.setString(5, m.getConcentracion());
            cs.setString(6, m.getFormaFarmaceutica());
            cs.setString(7, m.getPresentacion());
            cs.setInt(8, m.getIdCategoria());
            cs.setBigDecimal(9, m.getPrecioVenta());
            cs.setBoolean(10, m.isRequiereReceta());
            cs.setString(11, usuarioActualizacion);
            cs.executeUpdate();
        }
    }

    @Override
    public void eliminar(int id, String usuarioActualizacion) throws SQLException {
        String sp = "{CALL sp_eliminar_medicamento(?, ?)}";
        try (Connection conn = ConexionDB.getConnection();
             CallableStatement cs = conn.prepareCall(sp)) {
            cs.setInt(1, id);
            cs.setString(2, usuarioActualizacion);
            cs.executeUpdate();
        }
    }

    private Medicamento mapearResultSet(ResultSet rs) throws SQLException {
        Medicamento m = new Medicamento();
        m.setIdMedicamento(rs.getInt("id_medicamento"));
        m.setCodigo(rs.getString("codigo"));
        m.setNombre(rs.getString("nombre"));
        m.setPrincipioActivo(rs.getString("principio_activo"));
        m.setConcentracion(rs.getString("concentracion"));
        m.setFormaFarmaceutica(rs.getString("forma_farmaceutica"));
        m.setPresentacion(rs.getString("presentacion"));
        m.setIdCategoria(rs.getInt("id_categoria"));
        m.setNombreCategoria(rs.getString("nombre_categoria"));
        m.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        m.setRequiereReceta(rs.getBoolean("requiere_receta"));
        m.setUsuarioRegistro(rs.getString("usuario_registro"));
        m.setFechaRegistro(rs.getTimestamp("fecha_registro"));
        m.setEstadoRegistro(rs.getString("estado_registro"));
        return m;
    }
}
