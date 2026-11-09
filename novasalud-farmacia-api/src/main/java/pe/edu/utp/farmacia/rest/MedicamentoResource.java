package pe.edu.utp.farmacia.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.edu.utp.farmacia.model.Medicamento;
import pe.edu.utp.farmacia.service.MedicamentoService;
import java.util.List;
import java.util.Map;

@Path("/medicamentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MedicamentoResource {

    private final MedicamentoService service = new MedicamentoService();

    @GET
    public Response listar(@QueryParam("q") String query, @QueryParam("filtro") String filtro) {
        try {
            String criterio = (query != null && !query.isBlank()) ? query : filtro;
            List<Medicamento> lista;
            if (criterio != null && !criterio.isBlank()) {
                lista = service.buscar(criterio);
            } else {
                lista = service.listar();
            }
            return Response.ok(lista).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response obtenerPorId(@PathParam("id") int id) {
        try {
            Medicamento m = service.obtenerPorId(id);
            if (m == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Medicamento no encontrado"))
                        .build();
            }
            return Response.ok(m).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    public Response registrar(Medicamento m, @QueryParam("usuario") @DefaultValue("sistema") String usuario) {
        try {
            int id = service.registrar(m, usuario);
            m.setIdMedicamento(id);
            return Response.status(Response.Status.CREATED).entity(m).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response actualizar(@PathParam("id") int id, Medicamento m, @QueryParam("usuario") @DefaultValue("sistema") String usuario) {
        try {
            m.setIdMedicamento(id);
            service.actualizar(m, usuario);
            return Response.ok(Map.of("mensaje", "Medicamento actualizado exitosamente")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response eliminar(@PathParam("id") int id, @QueryParam("usuario") @DefaultValue("sistema") String usuario) {
        try {
            service.eliminar(id, usuario);
            return Response.ok(Map.of("mensaje", "Medicamento eliminado exitosamente")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }
}
