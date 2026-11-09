package pe.edu.utp.novasalud.atencion.rest;

import pe.edu.utp.novasalud.atencion.model.AtencionMedica;
import pe.edu.utp.novasalud.atencion.service.AtencionMedicaService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/atencion-medica")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AtencionMedicaResource {

    private final AtencionMedicaService atencionService = new AtencionMedicaService();

    @GET
    @Path("/{idAtencion}")
    public Response obtenerPorId(@PathParam("idAtencion") int idAtencion) {
        try {
            AtencionMedica atencion = atencionService.obtenerPorId(idAtencion);
            if (atencion == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(atencion).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/cita/{idCita}")
    public Response obtenerPorCita(@PathParam("idCita") int idCita) {
        try {
            AtencionMedica atencion = atencionService.obtenerPorCita(idCita);
            if (atencion == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(atencion).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/paciente/{idPaciente}")
    public Response listarPorPaciente(@PathParam("idPaciente") int idPaciente) {
        try {
            List<AtencionMedica> lista = atencionService.listarPorPaciente(idPaciente);
            return Response.ok(lista).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/pendientes-farmacia")
    public Response listarPendientesFarmacia() {
        try {
            List<AtencionMedica> lista = atencionService.listarPendientesEnvioFarmacia();
            return Response.ok(lista).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @PUT
    @Path("/{idAtencion}/estado-farmacia")
    public Response actualizarEstadoFarmacia(@PathParam("idAtencion") int idAtencion,
                                            @QueryParam("estado") String estado) {
        try {
            boolean actualizado = atencionService.actualizarEstadoEnvioFarmacia(idAtencion, estado);
            if (!actualizado) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"Atención no encontrada\"}").build();
            }
            return Response.ok("{\"mensaje\":\"Estado de farmacia actualizado exitosamente\"}").build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @POST
    public Response guardar(@QueryParam("idMedicoSesion") @DefaultValue("0") int idMedicoSesion,
                            @QueryParam("marcarAtendida") @DefaultValue("true") boolean marcarAtendida,
                            AtencionMedica atencion) {
        try {
            if (marcarAtendida) {
                atencionService.guardarYMarcarAtendida(atencion, idMedicoSesion);
            } else {
                atencionService.guardar(atencion, idMedicoSesion);
            }
            return Response.status(Response.Status.CREATED).entity(atencion).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        }
    }
}
