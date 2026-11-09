package pe.edu.utp.farmacia.service;

import pe.edu.utp.farmacia.dao.MedicamentoDAO;
import pe.edu.utp.farmacia.dao.MedicamentoDAOImpl;
import pe.edu.utp.farmacia.model.Medicamento;
import java.sql.SQLException;
import java.util.List;

public class MedicamentoService {

    private final MedicamentoDAO dao;

    public MedicamentoService() {
        this.dao = new MedicamentoDAOImpl();
    }

    public List<Medicamento> listar() throws SQLException {
        return dao.listar();
    }

    public List<Medicamento> buscar(String criterio) throws SQLException {
        return dao.buscar(criterio);
    }

    public Medicamento obtenerPorId(int id) throws SQLException {
        return dao.obtenerPorId(id);
    }

    public int registrar(Medicamento m, String usuario) throws SQLException {
        if (m.getCodigo() == null || m.getCodigo().isBlank()) {
            throw new IllegalArgumentException("El código del medicamento es obligatorio.");
        }
        if (m.getNombre() == null || m.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del medicamento es obligatorio.");
        }
        return dao.registrar(m, usuario);
    }

    public void actualizar(Medicamento m, String usuario) throws SQLException {
        if (m.getIdMedicamento() <= 0) {
            throw new IllegalArgumentException("ID de medicamento inválido.");
        }
        dao.actualizar(m, usuario);
    }

    public void eliminar(int id, String usuario) throws SQLException {
        dao.eliminar(id, usuario);
    }
}
