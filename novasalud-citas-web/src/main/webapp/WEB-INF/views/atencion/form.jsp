<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<c:set var="pageTitle" value="Atención médica - NovaSalud V3.2.1" scope="request" />
<c:set var="moduloActivo" value="agenda" scope="request" />
<c:set var="estadoAgenda" value="${fn:toUpperCase(cita.estado)}" />
<!DOCTYPE html>
<html lang="es">
<%@ include file="/WEB-INF/views/layout/head.jspf" %>
<body class="bg-light">
<nav class="navbar navbar-expand-lg navbar-dark bg-success shadow-sm">
    <div class="container-fluid px-4">
        <button class="btn btn-outline-light d-lg-none me-2 mobile-menu-toggle" type="button" data-bs-toggle="offcanvas" data-bs-target="#sidebarMenu" aria-label="Abrir menú"><i class="bi bi-list fs-4"></i></button>
        <span class="navbar-brand fw-semibold"><i class="bi bi-hospital me-2"></i>NovaSalud V3.2.1</span>
        <div class="d-flex flex-column flex-md-row align-items-md-center gap-2 ms-auto"><span class="text-white small">Módulo: Atención médica</span><%@ include file="/WEB-INF/views/layout/usuario-sesion.jspf" %></div>
    </div>
</nav>
<%@ include file="/WEB-INF/views/layout/menu-mobile.jspf" %>
<main class="container-fluid py-4 px-4">
  <div class="row g-4">
    <div class="col-12 col-lg-3"><%@ include file="/WEB-INF/views/layout/menu-right.jspf" %></div>
    <div class="col-12 col-lg-9">

      <div class="d-flex justify-content-between align-items-center mb-3">
        <h4 class="fw-bold text-success mb-0"><span class="page-title-icon"><i class="bi bi-clipboard2-pulse"></i></span> Atención médica del paciente</h4>
        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/agenda-medico?buscar=1&fecha=${fn:escapeXml(cita.fecha)}"><i class="bi bi-arrow-left"></i> Volver a mi agenda</a>
      </div>

      <c:if test="${param.msg == 'saved'}"><div class="alert alert-success"><i class="bi bi-check-circle"></i> Registro clínico guardado correctamente. Puede editarlo nuevamente o marcar la cita como atendida.</div></c:if>
      <c:if test="${not empty error}"><div class="alert alert-danger"><i class="bi bi-exclamation-triangle"></i> <c:out value="${error}" /></div></c:if>

      <div class="card premium-card border-0 mb-3">
        <div class="card-body">
          <div class="row g-3">
            <div class="col-md-3"><strong>Historia clínica</strong><div class="badge badge-soft-info fs-6"><c:out value="${cita.historiaClinicaCodigo}" /></div></div>
            <div class="col-md-3"><strong>DNI</strong><div><c:out value="${cita.pacienteDni}" /></div></div>
            <div class="col-md-6"><strong>Paciente</strong><div><c:out value="${cita.pacienteNombreCompleto}" /></div></div>
            <div class="col-md-3"><strong>Fecha</strong><div><c:out value="${cita.fecha}" /> <c:out value="${cita.hora}" /></div></div>
            <div class="col-md-3"><strong>Especialidad</strong><div><c:out value="${cita.especialidad}" /></div></div>
            <div class="col-md-3"><strong>Pago</strong><div><span class="badge ${cita.estadoPago == 'PAGADO' ? 'badge-soft-success' : 'badge-soft-warning'}"><c:out value="${cita.estadoPago}" /></span></div></div>
            <div class="col-md-3"><strong>Estado cita</strong><div><span class="badge ${estadoAgenda == 'ATENDIDA' ? 'bg-primary' : 'bg-success'}"><c:out value="${cita.estado}" /></span></div></div>
          </div>
        </div>
      </div>

      <c:choose>
        <c:when test="${cita.estadoPago != 'PAGADO'}">
          <div class="alert alert-warning"><i class="bi bi-cash-coin"></i> La cita aún no está pagada. Caja debe completar el pago antes de registrar atención médica.</div>
        </c:when>
        <c:otherwise>
          <form method="post" action="${pageContext.request.contextPath}/atencion-medica" class="card premium-card border-0">
            <%@ include file="/WEB-INF/views/layout/csrf-token.jspf" %>
            <input type="hidden" name="idCita" value="${cita.idCita}">
            <input type="hidden" name="idPaciente" value="${cita.idPaciente}">
            <input type="hidden" name="idMedico" value="${cita.idMedico}">
            <input type="hidden" name="fecha" value="${fn:escapeXml(cita.fecha)}">
            <input type="hidden" name="codigoHistoria" value="${fn:escapeXml(cita.historiaClinicaCodigo)}">
            <div class="card-body">
              <div class="alert alert-light border">
                <i class="bi bi-info-circle"></i> Registre primero los síntomas, diagnóstico, tratamiento y receta. Después de guardar puede cerrar la atención con <strong>Guardar y marcar atendida</strong>.
              </div>
              <div class="row g-3">
                <div class="col-md-6">
                  <label class="form-label">Motivo de consulta *</label>
                  <textarea name="motivoConsulta" class="form-control" rows="3" maxlength="255" required>${fn:escapeXml(atencion.motivoConsulta)}</textarea>
                </div>
                <div class="col-md-6">
                  <label class="form-label">Síntomas del paciente *</label>
                  <textarea name="sintomas" class="form-control" rows="3" maxlength="700" required>${fn:escapeXml(atencion.sintomas)}</textarea>
                </div>
                <div class="col-md-6">
                  <label class="form-label">Diagnóstico *</label>
                  <textarea name="diagnostico" class="form-control" rows="4" maxlength="700" required>${fn:escapeXml(atencion.diagnostico)}</textarea>
                </div>
                <div class="col-md-6">
                  <label class="form-label">Tratamiento indicado *</label>
                  <textarea name="tratamiento" class="form-control" rows="4" maxlength="700" required>${fn:escapeXml(atencion.tratamiento)}</textarea>
                </div>
                <div class="col-md-6">
                  <label class="form-label">Receta médica (Resumen / Indicaciones generales)</label>
                  <textarea name="recetaMedica" class="form-control" rows="3" maxlength="700" placeholder="Indicaciones generales de la receta médica.">${fn:escapeXml(atencion.recetaMedica)}</textarea>
                </div>
                <div class="col-md-6">
                  <label class="form-label">Indicaciones y recomendaciones</label>
                  <textarea name="indicaciones" class="form-control" rows="3" maxlength="700" placeholder="Reposo, exámenes, controles, señales de alarma.">${fn:escapeXml(atencion.indicaciones)}</textarea>
                </div>

                <div class="col-12 mt-3">
                  <div class="card border shadow-sm">
                    <div class="card-header bg-light d-flex justify-content-between align-items-center py-2">
                      <span class="fw-semibold text-success"><i class="bi bi-capsule me-1"></i> Medicamentos estructurados para Farmacia</span>
                      <div>
                        <button type="button" class="btn btn-sm btn-success" onclick="abrirModalBusquedaMedicamentos()"><i class="bi bi-search me-1"></i> Buscar en Farmacia</button>
                        <button type="button" class="btn btn-sm btn-outline-secondary ms-1" onclick="agregarMedicamento()" title="Agregar entrada libre manual"><i class="bi bi-plus-circle me-1"></i> Manual</button>
                      </div>
                    </div>
                    <div class="card-body p-0">
                      <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0" id="tablaMedicamentos">
                          <thead class="table-light">
                            <tr>
                              <th style="width: 32%;">Medicamento *</th>
                              <th style="width: 18%;">Dosis</th>
                              <th style="width: 18%;">Frecuencia</th>
                              <th style="width: 16%;">Duración</th>
                              <th style="width: 10%;">Cant.</th>
                              <th style="width: 6%; text-align: center;">Quitar</th>
                            </tr>
                          </thead>
                          <tbody id="cuerpoMedicamentos">
                          </tbody>
                        </table>
                      </div>
                      <div id="sinMedicamentos" class="text-muted text-center py-3 small">
                        <i class="bi bi-info-circle me-1"></i> No hay medicamentos agregados. Haga clic en <strong>"Buscar en Farmacia"</strong> para seleccionar del catálogo oficial.
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="d-flex flex-column flex-md-row justify-content-end gap-2 mt-4">
                <button type="submit" name="accion" value="guardar" class="btn btn-outline-success"><i class="bi bi-save"></i> Guardar atención</button>
                <button type="submit" name="accion" value="guardar_atender" class="btn btn-success" ${estadoAgenda == 'ATENDIDA' ? 'disabled' : ''}><i class="bi bi-check2-square"></i> Guardar y marcar atendida</button>
              </div>
            </div>
          </form>
              
              
        </c:otherwise>
      </c:choose>

    </div>
  </div>
</main>

<!-- Modal Buscar Medicamento en Catálogo de Farmacia -->
<div class="modal fade" id="modalBuscarMedicamento" tabindex="-1" aria-labelledby="modalBuscarMedicamentoLabel" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-dialog-scrollable">
    <div class="modal-content">
      <div class="modal-header bg-success text-white">
        <h5 class="modal-title" id="modalBuscarMedicamentoLabel"><i class="bi bi-capsule me-2"></i> Catálogo de Medicamentos — Farmacia</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Cerrar"></button>
      </div>
      <div class="modal-body p-3">
        <div class="input-group mb-3">
          <span class="input-group-text bg-white"><i class="bi bi-search text-success"></i></span>
          <input type="text" id="filtroMedicamento" class="form-control" placeholder="Escriba nombre o principio activo (ej. Paracetamol, Amoxicilina, Ibuprofeno)..." onkeyup="manejarTeclaBusqueda(event)">
          <button class="btn btn-success" type="button" onclick="ejecutarBusquedaMedicamentos()"><i class="bi bi-search me-1"></i> Buscar</button>
        </div>

        <div id="loadingMedicamentos" class="text-center py-4 text-muted" style="display: none;">
          <div class="spinner-border spinner-border-sm text-success me-2" role="status"></div>
          Consultando catálogo en tiempo real desde Farmacia API...
        </div>

        <div class="table-responsive" style="max-height: 380px;">
          <table class="table table-hover align-middle mb-0" id="tablaResultadosMedicamentos">
            <thead class="table-light sticky-top">
              <tr>
                <th style="width: 15%;">Código</th>
                <th style="width: 32%;">Medicamento</th>
                <th style="width: 25%;">Principio Activo</th>
                <th style="width: 16%;">Presentación</th>
                <th style="width: 12%; text-align: center;">Acción</th>
              </tr>
            </thead>
            <tbody id="cuerpoResultadosMedicamentos">
              <tr>
                <td colspan="5" class="text-center text-muted py-4">
                  <i class="bi bi-info-circle me-1"></i> Ingrese un término para buscar o presione <strong>Buscar</strong> para listar el catálogo.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="modal-footer bg-light py-2">
        <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cerrar</button>
      </div>
    </div>
  </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
var modalMedicamentosInstancia = null;

function abrirModalBusquedaMedicamentos() {
    var modalEl = document.getElementById('modalBuscarMedicamento');
    if (!modalMedicamentosInstancia) {
        modalMedicamentosInstancia = new bootstrap.Modal(modalEl);
    }
    modalMedicamentosInstancia.show();
    setTimeout(function() {
        var input = document.getElementById('filtroMedicamento');
        if (input) {
            input.focus();
            if (!input.value) ejecutarBusquedaMedicamentos();
        }
    }, 300);
}

function manejarTeclaBusqueda(event) {
    if (event.key === 'Enter') {
        event.preventDefault();
        ejecutarBusquedaMedicamentos();
    }
}

function ejecutarBusquedaMedicamentos() {
    var query = document.getElementById('filtroMedicamento').value || '';
    var loading = document.getElementById('loadingMedicamentos');
    var tbody = document.getElementById('cuerpoResultadosMedicamentos');

    if (loading) loading.style.display = 'block';
    if (tbody) tbody.style.display = 'none';

    var url = '${pageContext.request.contextPath}/catalogo-medicamentos' + (query.trim() ? '?q=' + encodeURIComponent(query.trim()) : '');

    fetch(url)
        .then(function(resp) { return resp.json(); })
        .then(function(data) {
            if (loading) loading.style.display = 'none';
            if (tbody) tbody.style.display = '';
            tbody.innerHTML = '';

            if (!data || data.length === 0 || data.error) {
                var errTxt = (data && data.error) ? data.error : 'No se encontraron medicamentos coincidentes en Farmacia.';
                tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4"><i class="bi bi-exclamation-circle me-1"></i> ' + escapeHtml(errTxt) + '</td></tr>';
                return;
            }

            data.forEach(function(m) {
                var tr = document.createElement('tr');
                var codigo = m.codigo || ('MED-' + m.idMedicamento);
                var nombre = m.nombre || '';
                var pa = m.principioActivo ? (m.principioActivo + (m.concentracion ? ' ' + m.concentracion : '')) : (m.concentracion || '-');
                var pres = m.presentacion ? (m.presentacion + (m.formaFarmaceutica ? ' (' + m.formaFarmaceutica + ')' : '')) : (m.formaFarmaceutica || 'Tableta');

                tr.innerHTML = '<td><span class="badge bg-light text-dark border">' + escapeHtml(codigo) + '</span></td>' +
                               '<td class="fw-semibold text-success">' + escapeHtml(nombre) + '</td>' +
                               '<td><small>' + escapeHtml(pa) + '</small></td>' +
                               '<td><small class="text-muted">' + escapeHtml(pres) + '</small></td>' +
                               '<td class="text-center">' +
                                 '<button type="button" class="btn btn-success btn-sm py-1 px-2" onclick="seleccionarMedicamentoCatalogo(\'' + escapeJs(nombre) + '\', \'' + escapeJs(m.concentracion || '') + '\', \'' + escapeJs(m.formaFarmaceutica || '') + '\')">' +
                                   '<i class="bi bi-plus-lg me-1"></i> Seleccionar' +
                                 '</button>' +
                               '</td>';
                tbody.appendChild(tr);
            });
        })
        .catch(function(err) {
            if (loading) loading.style.display = 'none';
            if (tbody) tbody.style.display = '';
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger py-4"><i class="bi bi-wifi-off me-1"></i> Error al conectar con el servicio de Farmacia API.</td></tr>';
        });
}

function seleccionarMedicamentoCatalogo(nombre, concentracion, forma) {
    var nombreCompleto = nombre;
    if (concentracion && !nombreCompleto.toLowerCase().includes(concentracion.toLowerCase())) {
        nombreCompleto += ' ' + concentracion;
    }
    if (forma && !nombreCompleto.toLowerCase().includes(forma.toLowerCase())) {
        nombreCompleto += ' (' + forma + ')';
    }

    var dosisSugerida = forma ? ('1 ' + forma.toLowerCase()) : '1 tableta';
    agregarMedicamento(nombreCompleto, dosisSugerida, 'Cada 8 horas', '3 días', 1);

    if (modalMedicamentosInstancia) {
        modalMedicamentosInstancia.hide();
    }
}

function agregarMedicamento(nombre, dosis, frec, dur, cant) {
    var tbody = document.getElementById('cuerpoMedicamentos');
    var emptyMsg = document.getElementById('sinMedicamentos');
    if (emptyMsg) emptyMsg.style.display = 'none';

    var tr = document.createElement('tr');
    tr.innerHTML = '<td><input type="text" name="medicamentoNombre" class="form-control form-control-sm" placeholder="Ej. Paracetamol 500 mg" required value="' + (nombre ? escapeHtml(nombre) : '') + '"></td>' +
                   '<td><input type="text" name="medicamentoDosis" class="form-control form-control-sm" placeholder="Ej. 1 tableta" value="' + (dosis ? escapeHtml(dosis) : '1 tableta') + '"></td>' +
                   '<td><input type="text" name="medicamentoFrecuencia" class="form-control form-control-sm" placeholder="Ej. Cada 8 horas" value="' + (frec ? escapeHtml(frec) : 'Cada 8 horas') + '"></td>' +
                   '<td><input type="text" name="medicamentoDuracion" class="form-control form-control-sm" placeholder="Ej. 3 días" value="' + (dur ? escapeHtml(dur) : '3 días') + '"></td>' +
                   '<td><input type="number" name="medicamentoCantidad" class="form-control form-control-sm" min="1" max="100" value="' + (cant ? cant : '1') + '"></td>' +
                   '<td class="text-center"><button type="button" class="btn btn-outline-danger btn-sm" onclick="eliminarFilaMedicamento(this)" title="Quitar fila"><i class="bi bi-trash"></i></button></td>';
    tbody.appendChild(tr);
}

function eliminarFilaMedicamento(btn) {
    var tr = btn.closest('tr');
    if (tr) tr.remove();
    var tbody = document.getElementById('cuerpoMedicamentos');
    var emptyMsg = document.getElementById('sinMedicamentos');
    if (tbody && tbody.children.length === 0 && emptyMsg) {
        emptyMsg.style.display = 'block';
    }
}

function escapeHtml(text) {
    if (!text) return '';
    return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function escapeJs(text) {
    if (!text) return '';
    return String(text).replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}
</script>
</body>
</html>
