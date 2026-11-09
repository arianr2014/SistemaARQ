<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<c:set var="pageTitle" value="Agenda Médica - NovaSalud V3.2.1" scope="request" />
<c:set var="moduloActivo" value="agenda" scope="request" />
<!DOCTYPE html>
<html lang="es">
<%@ include file="/WEB-INF/views/layout/head.jspf" %>
<body class="bg-light">
<nav class="navbar navbar-expand-lg navbar-dark bg-success shadow-sm">
    <div class="container-fluid px-4">
        <button class="btn btn-outline-light d-lg-none me-2 mobile-menu-toggle" type="button" data-bs-toggle="offcanvas" data-bs-target="#sidebarMenu" aria-controls="sidebarMenu" aria-label="Abrir menú"><i class="bi bi-list fs-4"></i></button>
        <span class="navbar-brand fw-semibold"><i class="bi bi-hospital me-2"></i>NovaSalud V3.2.1</span>
        <div class="d-flex flex-column flex-md-row align-items-md-center gap-2 ms-auto"><span class="text-white small">Módulo: Agenda Médica</span><%@ include file="/WEB-INF/views/layout/usuario-sesion.jspf" %></div>
    </div>
</nav>
<%@ include file="/WEB-INF/views/layout/menu-mobile.jspf" %>
<main class="container-fluid py-4 px-4">
  <div class="row g-4">
    <div class="col-12 col-lg-3 order-1 order-lg-1"><%@ include file="/WEB-INF/views/layout/menu-right.jspf" %></div>
    <div class="col-12 col-lg-9 order-2 order-lg-2">

<div class="card mb-4 border-0 shadow-sm" style="border-radius: 12px; background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%); color: #ffffff;">
    <div class="card-body p-4 d-flex justify-content-between align-items-center flex-wrap gap-3">
        <div>
            <h3 class="fw-bold mb-1"><i class="bi bi-journal-medical me-2"></i> Agenda Médica</h3>
            <p class="mb-0 text-white-50">Consulta la programación diaria de consultas, citas citadas y atención de pacientes.</p>
        </div>
        <div>
            <span class="badge bg-white text-primary fs-6 px-3 py-2 rounded-pill shadow-sm">
                <i class="bi bi-calendar-event me-1"></i> Programación Diaria
            </span>
        </div>
    </div>
</div>

<c:choose>
    <c:when test="${param.msg == 'attended'}">
        <c:choose>
            <c:when test="${param.estadoFarmacia == 'ENVIADO_A_FARMACIA'}">
                <div class="alert alert-success shadow-sm d-flex align-items-center">
                    <i class="bi bi-check-circle-fill me-2 fs-4 text-success"></i>
                    <div>
                        <strong>Atención clínica registrada correctamente.</strong>
                        <div><small>La prescripción médica fue transferida a Farmacia exitosamente (Estado: <code>ENVIADO_A_FARMACIA</code>).</small></div>
                    </div>
                </div>
            </c:when>
            <c:when test="${param.estadoFarmacia == 'PENDIENTE_ENVIO_FARMACIA'}">
                <div class="alert alert-warning shadow-sm d-flex align-items-center">
                    <i class="bi bi-exclamation-triangle-fill me-2 fs-4 text-warning"></i>
                    <div>
                        <strong>Atención clínica registrada con éxito.</strong>
                        <div><small>Aviso de Resiliencia SOA: Farmacia no se encuentra disponible temporalmente. La prescripción médica quedó en estado <code>PENDIENTE_ENVIO_FARMACIA</code> y será reintentada automáticamente por el orquestador.</small></div>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="alert alert-success"><i class="bi bi-check-circle"></i> <strong>Atención registrada correctamente.</strong></div>
            </c:otherwise>
        </c:choose>
    </c:when>
    <c:when test="${param.msg == 'pago_pendiente'}"><div class="alert alert-warning"><i class="bi bi-cash-coin"></i> <strong>Pago pendiente.</strong> La cita debe estar pagada para poder atenderla.</div></c:when>
    <c:when test="${param.msg == 'invalid'}"><div class="alert alert-danger"><i class="bi bi-x-circle"></i> No se pudo procesar la acción solicitada.</div></c:when>
</c:choose>

<div class="card mb-3 border-0 shadow-sm">
  <div class="card-body">
    <c:choose>
      <c:when test="${modoDoctor}">
        <div class="alert alert-success border-0 mb-3">
          <div class="fw-bold"><i class="bi bi-person-badge"></i> Agenda personal del médico</div>
          <div><strong>Médico:</strong> <c:out value="${sessionScope.nombreMedico}" /> <span class="mx-2">|</span> <strong>Especialidad:</strong> <c:out value="${sessionScope.nombreEspecialidad}" /></div>
          <small class="text-muted">Por seguridad, el sistema ignora cualquier médico enviado por URL y usa solo el médico vinculado al usuario autenticado.</small>
        </div>
        <form method="get" action="${pageContext.request.contextPath}/agenda-medico" class="row g-2 align-items-end">
          <input type="hidden" name="buscar" value="1">
          <div class="col-12 col-md-8"><label class="form-label mb-1">Fecha</label><input type="date" class="form-control" name="fecha" required value="${fn:escapeXml(fecha)}"></div>
          <div class="col-12 col-md-4 d-grid"><button type="submit" class="btn btn-success"><i class="bi bi-search"></i> Ver mi agenda</button></div>
        </form>
      </c:when>
      <c:otherwise>
        <form method="get" action="${pageContext.request.contextPath}/agenda-medico" class="row g-2 align-items-end">
          <input type="hidden" name="buscar" value="1">
          <div class="col-12 col-md-3"><label class="form-label mb-1">Fecha</label><input type="date" class="form-control" name="fecha" required value="${fn:escapeXml(fecha)}"></div>
          <div class="col-12 col-md-3"><label class="form-label mb-1">Especialidad</label><select name="idEspecialidad" class="form-select"><option value="0">Todas</option><c:forEach var="e" items="${especialidades}"><option value="${e.idEspecialidad}" ${idEspecialidad == e.idEspecialidad ? 'selected' : ''}><c:out value="${e.nombre}" /></option></c:forEach></select></div>
          <div class="col-12 col-md-4"><label class="form-label mb-1">Médico</label><select name="idMedico" class="form-select"><option value="0">Todos los médicos</option><c:forEach var="m" items="${medicos}"><option value="${m.idMedico}" ${idMedico == m.idMedico ? 'selected' : ''}><c:out value="${m.nombres}" /> <c:out value="${m.apellidos}" /></option></c:forEach></select></div>
          <div class="col-12 col-md-2 d-grid"><button type="submit" class="btn btn-success"><i class="bi bi-search"></i> Ver</button></div>
        </form>
        <div class="form-text mt-2">Puede buscar solo por fecha para ver todos los médicos que atienden ese día.</div>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<c:if test="${modoDoctor and not empty diasMesAgenda}">
  <div class="card premium-card border-0 mb-3">
    <div class="card-body">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <h6 class="fw-bold text-success mb-0"><i class="bi bi-calendar3"></i> Vista mensual <c:out value="${mesAgenda}" /></h6>
        <small class="text-muted">Cada día muestra la cantidad de citas asignadas al médico.</small>
      </div>
      <div style="display: grid; grid-template-columns: repeat(7, 1fr); gap: 0.5rem;" class="mb-2 text-center text-secondary fw-semibold small">
        <div>Lun</div><div>Mar</div><div>Mié</div><div>Jue</div><div>Vie</div><div>Sáb</div><div>Dom</div>
      </div>
      <div style="display: grid; grid-template-columns: repeat(7, 1fr); gap: 0.5rem;">
        <c:forEach var="dia" items="${diasMesAgenda}">
          <c:set var="totalDia" value="${resumenMensual[dia]}" />
          <div>
            <a class="text-decoration-none" href="${pageContext.request.contextPath}/agenda-medico?buscar=1&fecha=${dia}">
              <div class="border rounded-3 p-2 text-center h-100 ${dia == fecha ? 'bg-success text-white shadow-sm' : 'bg-white text-dark'}">
                <div class="small fw-bold">${fn:substring(dia,8,10)}</div>
                <div class="small ${dia == fecha ? 'text-white' : 'text-muted'}" style="font-size: 0.75rem;"><c:out value="${empty totalDia ? 0 : totalDia}" /> cita(s)</div>
              </div>
            </a>
          </div>
        </c:forEach>
      </div>
    </div>
  </div>
</c:if>

<c:if test="${not empty error}"><div class="alert alert-danger"><c:out value="${error}" /></div></c:if>

<div class="card border-0 shadow-sm">
  <div class="card-body table-responsive">
    <div class="d-flex justify-content-between align-items-center mb-2">
      <h6 class="fw-bold mb-0"><i class="bi bi-list-check"></i> Detalle diario</h6>
      <small class="text-muted">Duración y cupos se controlan desde la configuración del médico y sus horarios.</small>
    </div>
    <table class="table table-hover align-middle table-green">
      <thead><tr><th>Hora</th><th>DNI</th><th>Paciente</th><th>Médico</th><th>Especialidad</th><th>Pago</th><th>Observaciones</th><th>Estado</th><th></th></tr></thead>
      <tbody>
        <c:choose>
          <c:when test="${buscar != true}"><tr><td colspan="9" class="text-center text-muted py-4">Seleccione fecha y filtros para consultar la agenda.</td></tr></c:when>
          <c:when test="${empty citas}"><tr><td colspan="9" class="text-center text-muted py-4">No hay citas para los filtros seleccionados.</td></tr></c:when>
          <c:otherwise>
            <c:forEach var="cita" items="${citas}">
              <c:set var="estadoAgenda" value="${fn:toUpperCase(cita.estado)}" />
              <tr>
                <td><c:out value="${cita.hora}" /><br><small class="text-muted"><c:out value="${cita.horaFin}" /></small></td>
                <td><c:out value="${cita.pacienteDni}" /></td>
                <td><c:out value="${cita.pacienteNombreCompleto}" /></td>
                <td><c:out value="${cita.medicoNombreCompleto}" /></td>
                <td><c:out value="${cita.especialidad}" /></td>
                <td><span class="badge ${cita.estadoPago == 'PAGADO' ? 'badge-soft-success' : (cita.estadoPago == 'PARCIAL' ? 'badge-soft-info' : 'badge-soft-warning')}"><c:out value="${cita.estadoPago}" /></span></td>
                <td><c:out value="${cita.observaciones}" /></td>
                <td><span class="badge ${estadoAgenda == 'ATENDIDA' ? 'bg-primary' : (estadoAgenda == 'NO_ASISTIO' ? 'bg-warning text-dark' : 'bg-success')}"><c:out value="${cita.estado}" /></span></td>
                <td class="text-end">
                  <c:if test="${modoDoctor}">
                    <c:choose>
                      <c:when test="${cita.estadoPago == 'PAGADO'}">
                        <a class="btn btn-sm btn-success" href="${pageContext.request.contextPath}/atencion-medica?idCita=${cita.idCita}" title="Registrar síntomas, diagnóstico, tratamiento y receta">
                          <i class="bi bi-clipboard2-pulse"></i>
                          <c:out value="${estadoAgenda == 'ATENDIDA' ? 'Editar atención' : 'Atender'}" />
                        </a>
                      </c:when>
                      <c:otherwise>
                        <span class="badge badge-soft-warning"><i class="bi bi-lock"></i> Pago pendiente</span>
                      </c:otherwise>
                    </c:choose>
                  </c:if>
                </td>
              </tr>
            </c:forEach>
          </c:otherwise>
        </c:choose>
      </tbody>
    </table>
  </div>
</div>

    </div>
  </div>
</main>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
