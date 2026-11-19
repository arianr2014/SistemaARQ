<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<jsp:include page="/WEB-INF/views/layout/header.jsp">
    <jsp:param name="title" value="Punto de Venta POS - NovaSalud Farmacia" />
    <jsp:param name="pageTitle" value="Punto de Venta (POS) - Venta Directa" />
    <jsp:param name="active" value="pos" />
</jsp:include>

<c:if test="${not empty prescripcionPreload}">
    <div style="background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%); border: 1px solid #10b981; border-radius: 10px; padding: 14px 18px; margin-bottom: 16px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
        <div style="display: flex; align-items: center; gap: 14px;">
            <div style="background: #10b981; color: white; width: 38px; height: 38px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 18px;">
                <i class="fa-solid fa-file-prescription"></i>
            </div>
            <div>
                <div style="font-weight: 700; color: #065f46; font-size: 15px;">
                    Despacho de Receta Médica: 
                    <span style="background: #047857; color: white; padding: 2px 8px; border-radius: 4px; font-size: 13px;">
                        ${empty prescripcionPreload.codigoPrescripcion ? ('PRES-00' += prescripcionPreload.idPrescripcion) : prescripcionPreload.codigoPrescripcion}
                    </span>
                </div>
                <div style="font-size: 13px; color: #047857; margin-top: 2px;">
                    <strong>Paciente:</strong> ${prescripcionPreload.nombrePaciente} (DNI: ${prescripcionPreload.dniPaciente}) &nbsp;|&nbsp; 
                    <strong>Médico:</strong> ${prescripcionPreload.nombreMedico} (${prescripcionPreload.numeroColegiaturaMedico})
                </div>
            </div>
        </div>
        <div>
            <span class="badge badge-success" style="font-size: 12px; padding: 6px 12px; display: inline-flex; align-items: center; gap: 6px;">
                <i class="fa-solid fa-check"></i> Medicamentos Precargados en Carrito
            </span>
        </div>
    </div>
</c:if>

<div class="pos-container">
    <!-- Catalog Section -->
    <div class="pos-catalog">
        <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-shrink: 0;">
            <input type="text" id="posSearch" onkeyup="filterProducts()" class="form-control" placeholder="🔍 Buscar por código de barras, nombre o síntoma..." autofocus>
        </div>

        <div style="overflow-y: auto; flex-grow: 1; padding-right: 6px;">
            <div id="productGrid" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px;">
                <c:forEach items="${lotes}" var="l">
                    <c:if test="${l.cantidadActual > 0}">
                        <div class="product-card glass-panel" style="padding: 16px; cursor: pointer; border: 1px solid #e2e8f0; transition: var(--transition);"
                             onclick="addToCart('${l.idLote}', '${l.idMedicamento}', '${l.nombreMedicamento}', '${l.numeroLote}', ${l.precioCompra * 1.3}, ${l.cantidadActual})">
                            <div style="font-size: 11px; text-transform: uppercase; font-weight: 700; color: #10b981;">Lote: ${l.numeroLote}</div>
                            <h4 style="font-size: 14px; font-weight: 700; color: #0f172a; margin: 4px 0;">${l.nombreMedicamento}</h4>
                            <div style="font-size: 12px; color: #64748b; margin-bottom: 8px;">Stock: <strong>${l.cantidadActual} u.</strong> | Venc: ${l.fechaVencimiento}</div>
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <span style="font-size: 16px; font-weight: 800; color: #059669;">S/ <fmt:formatNumber value="${l.precioCompra * 1.3}" pattern="0.00"/></span>
                                <button class="btn btn-primary btn-sm"><i class="fa-solid fa-plus"></i></button>
                            </div>
                        </div>
                    </c:if>
                </c:forEach>
            </div>
        </div>
    </div>

    <!-- Cart Section -->
    <div class="pos-cart">
        <h3 style="font-size: 16px; font-weight: 700; margin-bottom: 12px; flex-shrink: 0; display: flex; align-items: center; gap: 8px;">
            <i class="fa-solid fa-cart-shopping" style="color: #10b981;"></i> Carrito de Compra
        </h3>

        <div style="flex-shrink: 0;">
            <div class="form-group" style="margin-bottom: 10px;">
                <label class="form-label" style="margin-bottom: 4px; font-weight: 700;">Tipo de Comprobante</label>
                <select id="tipoComprobante" onchange="onTipoComprobanteChange()" class="form-control" style="border: 2px solid #0284c7; font-weight: 700; color: #0284c7;">
                    <option value="BOLETA">BOLETA ELECTRÓNICA</option>
                    <option value="FACTURA">FACTURA ELECTRÓNICA</option>
                </select>
            </div>
            <div class="form-group" style="margin-bottom: 10px;">
                <label class="form-label" id="lblDni" style="margin-bottom: 4px;">DNI Cliente</label>
                <input type="text" id="dniCliente" class="form-control" placeholder="DNI Cliente (8 dígitos)" maxlength="8">
            </div>
            <div class="form-group" style="margin-bottom: 12px;">
                <label class="form-label" id="lblNombre" style="margin-bottom: 4px;">Nombre Cliente / Razón Social</label>
                <input type="text" id="nombreCliente" class="form-control" placeholder="Nombre Cliente (Público General)">
            </div>
        </div>

        <div class="cart-items" id="cartItems">
            <p id="emptyCartMsg" style="text-align: center; color: #94a3b8; margin-top: 40px;">
                <i class="fa-solid fa-basket-shopping" style="font-size: 32px; display: block; margin-bottom: 8px;"></i>
                El carrito está vacío
            </p>
        </div>

        <div class="cart-summary">
            <div class="summary-row">
                <span>Subtotal:</span>
                <strong id="subtotalVal">S/ 0.00</strong>
            </div>
            <div class="summary-row">
                <span>IGV (18%):</span>
                <strong id="igvVal">S/ 0.00</strong>
            </div>
            <div class="summary-row summary-total">
                <span>Total a Pagar:</span>
                <strong id="totalVal">S/ 0.00</strong>
            </div>

            <div class="form-group" style="margin-top: 10px; margin-bottom: 14px;">
                <label class="form-label" style="margin-bottom: 4px;">Método de Pago</label>
                <select id="tipoPago" class="form-control">
                    <option value="EFECTIVO">Efectivo</option>
                    <option value="TARJETA">Tarjeta de Débito/Crédito</option>
                    <option value="YAPE">Yape / Plin</option>
                </select>
            </div>

            <button onclick="processSale()" id="btnProcessSale" class="btn btn-primary" style="width: 100%; justify-content: center; padding: 12px; font-size: 15px;" disabled>
                <i class="fa-solid fa-receipt"></i> Emitir Boleta de Venta
            </button>
        </div>
    </div>
</div>

<script>
    let cart = [];
    const csrfToken = "${sessionScope.csrfToken}";

    const preloadPrescripcionId = ${not empty prescripcionPreload ? prescripcionPreload.idPrescripcion : 'null'};
    const preloadDni = "${not empty prescripcionPreload ? prescripcionPreload.dniPaciente : ''}";
    const preloadNombre = "${not empty prescripcionPreload ? prescripcionPreload.nombrePaciente : ''}";
    const preloadDetalles = [
        <c:forEach items="${prescripcionPreload.detalles}" var="d" varStatus="loop">
        {
            idMedicamento: "${d.idMedicamento}",
            nombreMedicamento: "${d.nombreMedicamento}",
            cantidad: ${d.cantidad > 0 ? d.cantidad : 1}
        }${!loop.last ? ',' : ''}
        </c:forEach>
    ];

    const availableLotes = [
        <c:forEach items="${lotes}" var="l" varStatus="loop">
            <c:if test="${l.cantidadActual > 0}">
            {
                idLote: "${l.idLote}",
                idMedicamento: "${l.idMedicamento}",
                nombre: "${l.nombreMedicamento}",
                numLote: "${l.numeroLote}",
                precio: ${l.precioCompra * 1.3},
                maxStock: ${l.cantidadActual}
            }${!loop.last ? ',' : ''}
            </c:if>
        </c:forEach>
    ];

    document.addEventListener('DOMContentLoaded', function() {
        if (preloadPrescripcionId) {
            if (preloadDni) {
                const dniInput = document.getElementById('dniCliente');
                if (dniInput) dniInput.value = preloadDni;
            }
            if (preloadNombre) {
                const nomInput = document.getElementById('nombreCliente');
                if (nomInput) nomInput.value = preloadNombre;
            }

            if (preloadDetalles && preloadDetalles.length > 0) {
                preloadDetalles.forEach(function(det) {
                    let detName = (det.nombreMedicamento || '').toLowerCase().trim();
                    let matchedLote = availableLotes.find(function(l) {
                        if (det.idMedicamento && det.idMedicamento !== 'null' && det.idMedicamento !== '' && det.idMedicamento !== '0' && det.idMedicamento === l.idMedicamento) {
                            return true;
                        }
                        let loteName = (l.nombre || '').toLowerCase().trim();
                        if (loteName === detName) return true;
                        let firstWord = detName.split(/\s+/)[0];
                        if (firstWord.length >= 4 && loteName.includes(firstWord)) return true;
                        return false;
                    });

                    if (matchedLote) {
                        let desiredQty = Math.min(det.cantidad, matchedLote.maxStock);
                        if (desiredQty > 0) {
                            cart.push({
                                idLote: matchedLote.idLote,
                                idMedicamento: matchedLote.idMedicamento,
                                nombre: matchedLote.nombre,
                                numLote: matchedLote.numLote,
                                precioUnitario: parseFloat(matchedLote.precio),
                                cantidad: desiredQty,
                                maxStock: matchedLote.maxStock
                            });
                        }
                    }
                });
                renderCart();
            }
        }
    });

    function addToCart(idLote, idMedicamento, nombre, numLote, precio, maxStock) {
        let item = cart.find(i => i.idLote === idLote);
        if (item) {
            if (item.cantidad < maxStock) {
                item.cantidad++;
            } else {
                alert('Stock máximo alcanzado para este lote.');
            }
        } else {
            cart.push({
                idLote: idLote,
                idMedicamento: idMedicamento,
                nombre: nombre,
                numLote: numLote,
                precioUnitario: parseFloat(precio),
                cantidad: 1,
                maxStock: maxStock
            });
        }
        renderCart();
    }

    function updateQty(idLote, change) {
        let item = cart.find(i => i.idLote === idLote);
        if (item) {
            item.cantidad += change;
            if (item.cantidad <= 0) {
                cart = cart.filter(i => i.idLote !== idLote);
            } else if (item.cantidad > item.maxStock) {
                item.cantidad = item.maxStock;
                alert('Excede el stock disponible.');
            }
        }
        renderCart();
    }

    function renderCart() {
        const cartContainer = document.getElementById('cartItems');
        if (cart.length === 0) {
            cartContainer.innerHTML = `<p id="emptyCartMsg" style="text-align: center; color: #94a3b8; margin-top: 40px;"><i class="fa-solid fa-basket-shopping" style="font-size: 32px; display: block; margin-bottom: 8px;"></i>El carrito está vacío</p>`;
            document.getElementById('subtotalVal').innerText = 'S/ 0.00';
            document.getElementById('igvVal').innerText = 'S/ 0.00';
            document.getElementById('totalVal').innerText = 'S/ 0.00';
            document.getElementById('btnProcessSale').disabled = true;
            return;
        }

        let html = '';
        let total = 0;
        cart.forEach(item => {
            let itemTotal = item.precioUnitario * item.cantidad;
            total += itemTotal;
            html += `
                <div class="cart-item">
                    <div>
                        <div style="font-weight: 700; font-size: 13px;">\${item.nombre}</div>
                        <div style="font-size: 11px; color: #64748b;">Lote: \${item.numLote} | S/ \${item.precioUnitario.toFixed(2)}</div>
                    </div>
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <button onclick="updateQty('\${item.idLote}', -1)" class="btn btn-secondary btn-sm" style="padding: 2px 8px;">-</button>
                        <span style="font-weight: 700;">\${item.cantidad}</span>
                        <button onclick="updateQty('\${item.idLote}', 1)" class="btn btn-secondary btn-sm" style="padding: 2px 8px;">+</button>
                        <span style="font-weight: 700; color: #059669; margin-left: 8px;">S/ \${itemTotal.toFixed(2)}</span>
                    </div>
                </div>
            `;
        });

        let subtotal = total / 1.18;
        let igv = total - subtotal;

        cartContainer.innerHTML = html;
        document.getElementById('subtotalVal').innerText = 'S/ ' + subtotal.toFixed(2);
        document.getElementById('igvVal').innerText = 'S/ ' + igv.toFixed(2);
        document.getElementById('totalVal').innerText = 'S/ ' + total.toFixed(2);
        document.getElementById('btnProcessSale').disabled = false;
    }

    function onTipoComprobanteChange() {
        const tipo = document.getElementById('tipoComprobante').value;
        const inputDni = document.getElementById('dniCliente');
        const inputNombre = document.getElementById('nombreCliente');
        const btn = document.getElementById('btnProcessSale');
        const lblDni = document.getElementById('lblDni');
        const lblNombre = document.getElementById('lblNombre');

        if (tipo === 'FACTURA') {
            lblDni.innerText = 'RUC Cliente (11 dígitos)';
            inputDni.placeholder = 'Ej. 20601234567';
            inputDni.maxLength = 11;
            lblNombre.innerText = 'Razón Social';
            inputNombre.placeholder = 'Ej. SOMA LIMA S.A.C.';
            btn.innerHTML = '<i class="fa-solid fa-file-invoice"></i> Emitir Factura Electrónica';
        } else {
            lblDni.innerText = 'DNI Cliente (8 dígitos)';
            inputDni.placeholder = 'DNI Cliente (8 dígitos)';
            inputDni.maxLength = 8;
            lblNombre.innerText = 'Nombre Cliente / Razón Social';
            inputNombre.placeholder = 'Nombre Cliente (Público General)';
            btn.innerHTML = '<i class="fa-solid fa-receipt"></i> Emitir Boleta Electrónica';
        }
    }

    function processSale() {
        if (cart.length === 0) return;

        const tipoComp = document.getElementById('tipoComprobante').value;
        const isFactura = (tipoComp === 'FACTURA');
        const dniRucVal = document.getElementById('dniCliente').value.trim();
        const nombreVal = document.getElementById('nombreCliente').value.trim();

        if (isFactura && dniRucVal.length !== 11) {
            if (!confirm('Ha seleccionado FACTURA pero el RUC ingresado no tiene 11 dígitos. ¿Desea continuar con el RUC por defecto (20601234567)?')) {
                return;
            }
        }

        const defaultNum = isFactura ? '20601234567' : '99999999';
        const defaultNom = isFactura ? 'SOMA LIMA S.A.C.' : 'PUBLICO GENERAL';
        const finalDni = dniRucVal || defaultNum;
        const finalNombre = nombreVal || defaultNom;

        const seq = String(Math.floor(Date.now() % 100000000)).padStart(8, '0');
        const seriesPrefix = isFactura ? 'F001-' : 'B001-';
        const generatedCode = seriesPrefix + seq;

        const total = cart.reduce((acc, item) => acc + (item.precioUnitario * item.cantidad), 0);
        const subtotal = total / 1.18;
        const igv = total - subtotal;

        const payload = {
            idPrescripcion: preloadPrescripcionId || null,
            codigoVenta: generatedCode,
            numeroBoleta: generatedCode,
            tipoComprobante: isFactura ? 'FACTURA ELECTRÓNICA' : 'BOLETA ELECTRÓNICA',
            dniCliente: finalDni,
            dni: finalDni,
            nombreCliente: finalNombre,
            nombres: finalNombre,
            apellidos: isFactura ? '' : 'CLIENTE',
            subtotal: parseFloat(subtotal.toFixed(2)),
            igv: parseFloat(igv.toFixed(2)),
            montoTotal: parseFloat(total.toFixed(2)),
            total: parseFloat(total.toFixed(2)),
            tipoPago: document.getElementById('tipoPago').value,
            metodoPago: document.getElementById('tipoPago').value,
            detalles: cart.map(i => ({
                idMedicamento: parseInt(i.idMedicamento) || parseInt(i.idLote),
                idLote: parseInt(i.idLote),
                cantidad: i.cantidad,
                precioUnitario: i.precioUnitario,
                subtotal: parseFloat((i.precioUnitario * i.cantidad).toFixed(2))
            }))
        };

        fetch('${pageContext.request.contextPath}/ventas', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': csrfToken
            },
            body: JSON.stringify(payload)
        })
        .then(async res => {
            const text = await res.text();
            let json;
            try { json = JSON.parse(text); } catch (e) {}
            if (!res.ok) {
                const errorMsg = (json && (json.error || json.mensaje)) ? (json.error || json.mensaje) : ('Error HTTP ' + res.status + ': ' + text);
                throw new Error(errorMsg);
            }
            return json;
        })
        .then(data => {
            cart = [];
            renderCart();
            let targetUrl = '${pageContext.request.contextPath}/ventas?action=boleta';
            if (data && data.idVenta) {
                targetUrl += '&id=' + data.idVenta;
            }
            window.location.href = targetUrl;
        })
        .catch(err => {
            alert('Error al registrar venta: ' + err.message);
        });
    }

    function filterProducts() {
        let input = document.getElementById('posSearch').value.toLowerCase();
        let cards = document.querySelectorAll('#productGrid .product-card');
        cards.forEach(card => {
            let text = card.innerText.toLowerCase();
            card.style.display = text.includes(input) ? 'block' : 'none';
        });
    }
</script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
