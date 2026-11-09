package pe.edu.utp.farmacia.dao;

import pe.edu.utp.farmacia.model.Medicamento;
import java.sql.SQLException;
import java.util.List;

public interface MedicamentoDAO {
    List<Medicamento> listar() throws SQLException;
    List<Medicamento> buscar(String criterio) throws SQLException;
    Medicamento obtenerPorId(int id) throws SQLException;
    int registrar(Medicamento m, String usuarioRegistro) throws SQLException;
    void actualizar(Medicamento m, String usuarioActualizacion) throws SQLException;
    void eliminar(int id, String usuarioActualizacion) throws SQLException;
}
