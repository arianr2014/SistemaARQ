package pe.edu.utp.novasalud.orchestrator.rest;

import pe.edu.utp.novasalud.orchestrator.dto.AtencionProcessRequest;
import pe.edu.utp.novasalud.orchestrator.dto.AtencionProcessResponse;
import pe.edu.utp.novasalud.orchestrator.service.AtencionOrchestrationService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/orquestacion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AtencionOrchestrationResource {

    private final AtencionOrchestrationService orchestrationService = new AtencionOrchestrationService();

    @POST
    @Path("/atencion")
    public Response orquestarAtencionMedica(@Context HttpHeaders headers, AtencionProcessRequest req) {
        String authHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Se requiere token Bearer JWT en el encabezado Authorization\"}")
                    .build();
        }

        String incomingCorrelationId = headers.getHeaderString("X-Correlation-ID");
        AtencionProcessResponse result = orchestrationService.ejecutarProcesoAtencion(req, authHeader, incomingCorrelationId);
        if (!result.isSuccess()) {
            if ("ERROR_CITA_NOT_FOUND".equalsIgnoreCase(result.getEstado())) {
                return Response.status(Response.Status.NOT_FOUND)
                        .header("X-Correlation-ID", result.getCorrelationId())
                        .entity(result).build();
            }
            return Response.status(Response.Status.BAD_REQUEST)
                    .header("X-Correlation-ID", result.getCorrelationId())
                    .entity(result).build();
        }
        return Response.ok(result)
                .header("X-Correlation-ID", result.getCorrelationId())
                .build();
    }
}
