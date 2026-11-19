-- =============================================================================
-- SCRIPT DE PROCEDIMIENTOS ALMACENADOS (CRUD) DE FARMACIA
-- Base de Datos: bd_famacia
-- Motor: MySQL
-- =============================================================================

USE bd_famacia;

DELIMITER $$

-- =============================================================================
-- 1. CATEGORIA DE MEDICAMENTOS
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_registrar_categoria_medicamento$$
CREATE PROCEDURE sp_registrar_categoria_medicamento(
    IN p_nombre VARCHAR(100),
    IN p_descripcion VARCHAR(255),
    IN p_usuario_registro VARCHAR(50)
)
BEGIN
    INSERT INTO categoria_medicamento (
        nombre, descripcion, usuario_registro, fecha_registro, estado_registro
    ) VALUES (
        p_nombre, p_descripcion, COALESCE(p_usuario_registro, 'sistema'), NOW(), 'ACTIVO'
    );

    SELECT LAST_INSERT_ID() AS id_categoria;
END$$

DROP PROCEDURE IF EXISTS sp_actualizar_categoria_medicamento$$
CREATE PROCEDURE sp_actualizar_categoria_medicamento(
    IN p_id_categoria INT,
    IN p_nombre VARCHAR(100),
    IN p_descripcion VARCHAR(255),
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE categoria_medicamento SET
        nombre = p_nombre,
        descripcion = p_descripcion,
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_categoria = p_id_categoria;
END$$

DROP PROCEDURE IF EXISTS sp_eliminar_categoria_medicamento$$
CREATE PROCEDURE sp_eliminar_categoria_medicamento(
    IN p_id_categoria INT,
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE categoria_medicamento SET
        estado_registro = 'INACTIVO',
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_categoria = p_id_categoria;
END$$

DROP PROCEDURE IF EXISTS sp_obtener_categoria_medicamento_por_id$$
CREATE PROCEDURE sp_obtener_categoria_medicamento_por_id(
    IN p_id_categoria INT
)
BEGIN
    SELECT 
        id_categoria, nombre, descripcion,
        usuario_registro, fecha_registro, usuario_actualizacion, fecha_actualizacion, estado_registro
    FROM categoria_medicamento
    WHERE id_categoria = p_id_categoria AND estado_registro = 'ACTIVO';
END$$

DROP PROCEDURE IF EXISTS sp_listar_categoria_medicamentos$$
CREATE PROCEDURE sp_listar_categoria_medicamentos()
BEGIN
    SELECT 
        id_categoria, nombre, descripcion,
        usuario_registro, fecha_registro, usuario_actualizacion, fecha_actualizacion, estado_registro
    FROM categoria_medicamento
    WHERE estado_registro = 'ACTIVO'
    ORDER BY nombre ASC;
END$$


-- =============================================================================
-- 2. MEDICAMENTOS
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_registrar_medicamento$$
CREATE PROCEDURE sp_registrar_medicamento(
    IN p_codigo VARCHAR(30),
    IN p_nombre VARCHAR(150),
    IN p_principio_activo VARCHAR(150),
    IN p_concentracion VARCHAR(100),
    IN p_forma_farmaceutica VARCHAR(100),
    IN p_presentacion VARCHAR(100),
    IN p_id_categoria INT,
    IN p_precio_venta DECIMAL(10,2),
    IN p_requiere_receta BOOLEAN,
    IN p_usuario_registro VARCHAR(50)
)
BEGIN
    INSERT INTO medicamento (
        codigo, nombre, principio_activo, concentracion, forma_farmaceutica,
        presentacion, id_categoria, precio_venta, requiere_receta,
        usuario_registro, fecha_registro, estado_registro
    ) VALUES (
        p_codigo, p_nombre, p_principio_activo, p_concentracion, p_forma_farmaceutica,
        p_presentacion, p_id_categoria, COALESCE(p_precio_venta, 0.00), COALESCE(p_requiere_receta, TRUE),
        COALESCE(p_usuario_registro, 'sistema'), NOW(), 'ACTIVO'
    );

    SELECT LAST_INSERT_ID() AS id_medicamento;
END$$

DROP PROCEDURE IF EXISTS sp_actualizar_medicamento$$
CREATE PROCEDURE sp_actualizar_medicamento(
    IN p_id_medicamento INT,
    IN p_codigo VARCHAR(30),
    IN p_nombre VARCHAR(150),
    IN p_principio_activo VARCHAR(150),
    IN p_concentracion VARCHAR(100),
    IN p_forma_farmaceutica VARCHAR(100),
    IN p_presentacion VARCHAR(100),
    IN p_id_categoria INT,
    IN p_precio_venta DECIMAL(10,2),
    IN p_requiere_receta BOOLEAN,
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE medicamento SET
        codigo = p_codigo,
        nombre = p_nombre,
        principio_activo = p_principio_activo,
        concentracion = p_concentracion,
        forma_farmaceutica = p_forma_farmaceutica,
        presentacion = p_presentacion,
        id_categoria = p_id_categoria,
        precio_venta = p_precio_venta,
        requiere_receta = p_requiere_receta,
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_medicamento = p_id_medicamento;
END$$

DROP PROCEDURE IF EXISTS sp_eliminar_medicamento$$
CREATE PROCEDURE sp_eliminar_medicamento(
    IN p_id_medicamento INT,
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE medicamento SET
        estado_registro = 'INACTIVO',
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_medicamento = p_id_medicamento;
END$$

DROP PROCEDURE IF EXISTS sp_obtener_medicamento_por_id$$
CREATE PROCEDURE sp_obtener_medicamento_por_id(
    IN p_id_medicamento INT
)
BEGIN
    SELECT 
        m.id_medicamento, m.codigo, m.nombre, m.principio_activo, m.concentracion,
        m.forma_farmaceutica, m.presentacion, m.id_categoria, c.nombre AS nombre_categoria,
        m.precio_venta, m.requiere_receta, m.usuario_registro, m.fecha_registro,
        m.usuario_actualizacion, m.fecha_actualizacion, m.estado_registro
    FROM medicamento m
    LEFT JOIN categoria_medicamento c ON m.id_categoria = c.id_categoria
    WHERE m.id_medicamento = p_id_medicamento AND m.estado_registro = 'ACTIVO';
END$$

DROP PROCEDURE IF EXISTS sp_listar_medicamentos$$
CREATE PROCEDURE sp_listar_medicamentos()
BEGIN
    SELECT 
        m.id_medicamento, m.codigo, m.nombre, m.principio_activo, m.concentracion,
        m.forma_farmaceutica, m.presentacion, m.id_categoria, c.nombre AS nombre_categoria,
        m.precio_venta, m.requiere_receta, m.usuario_registro, m.fecha_registro,
        m.usuario_actualizacion, m.fecha_actualizacion, m.estado_registro
    FROM medicamento m
    LEFT JOIN categoria_medicamento c ON m.id_categoria = c.id_categoria
    WHERE m.estado_registro = 'ACTIVO'
    ORDER BY m.nombre ASC;
END$$


-- =============================================================================
-- 3. PROVEEDORES
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_registrar_proveedor$$
CREATE PROCEDURE sp_registrar_proveedor(
    IN p_razon_social VARCHAR(150),
    IN p_ruc VARCHAR(20),
    IN p_telefono VARCHAR(20),
    IN p_correo VARCHAR(100),
    IN p_direccion VARCHAR(255),
    IN p_usuario_registro VARCHAR(50)
)
BEGIN
    INSERT INTO proveedor (
        razon_social, ruc, telefono, correo, direccion,
        usuario_registro, fecha_registro, estado_registro
    ) VALUES (
        p_razon_social, p_ruc, p_telefono, p_correo, p_direccion,
        COALESCE(p_usuario_registro, 'sistema'), NOW(), 'ACTIVO'
    );

    SELECT LAST_INSERT_ID() AS id_proveedor;
END$$

DROP PROCEDURE IF EXISTS sp_actualizar_proveedor$$
CREATE PROCEDURE sp_actualizar_proveedor(
    IN p_id_proveedor INT,
    IN p_razon_social VARCHAR(150),
    IN p_ruc VARCHAR(20),
    IN p_telefono VARCHAR(20),
    IN p_correo VARCHAR(100),
    IN p_direccion VARCHAR(255),
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE proveedor SET
        razon_social = p_razon_social,
        ruc = p_ruc,
        telefono = p_telefono,
        correo = p_correo,
        direccion = p_direccion,
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_proveedor = p_id_proveedor;
END$$

DROP PROCEDURE IF EXISTS sp_eliminar_proveedor$$
CREATE PROCEDURE sp_eliminar_proveedor(
    IN p_id_proveedor INT,
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE proveedor SET
        estado_registro = 'INACTIVO',
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_proveedor = p_id_proveedor;
END$$

DROP PROCEDURE IF EXISTS sp_obtener_proveedor_por_id$$
CREATE PROCEDURE sp_obtener_proveedor_por_id(
    IN p_id_proveedor INT
)
BEGIN
    SELECT 
        id_proveedor, razon_social, ruc, telefono, correo, direccion,
        usuario_registro, fecha_registro, usuario_actualizacion, fecha_actualizacion, estado_registro
    FROM proveedor
    WHERE id_proveedor = p_id_proveedor AND estado_registro = 'ACTIVO';
END$$

DROP PROCEDURE IF EXISTS sp_listar_proveedores$$
CREATE PROCEDURE sp_listar_proveedores()
BEGIN
    SELECT 
        id_proveedor, razon_social, ruc, telefono, correo, direccion,
        usuario_registro, fecha_registro, usuario_actualizacion, fecha_actualizacion, estado_registro
    FROM proveedor
    WHERE estado_registro = 'ACTIVO'
    ORDER BY razon_social ASC;
END$$


-- =============================================================================
-- 4. LOTES DE MEDICAMENTOS
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_registrar_lote_medicamento$$
CREATE PROCEDURE sp_registrar_lote_medicamento(
    IN p_id_medicamento INT,
    IN p_id_proveedor INT,
    IN p_numero_lote VARCHAR(50),
    IN p_fecha_fabricacion DATE,
    IN p_fecha_vencimiento DATE,
    IN p_cantidad_inicial INT,
    IN p_cantidad_disponible INT,
    IN p_usuario_registro VARCHAR(50)
)
BEGIN
    INSERT INTO lote_medicamento (
        id_medicamento, id_proveedor, numero_lote, fecha_fabricacion, fecha_vencimiento,
        cantidad_inicial, cantidad_disponible, usuario_registro, fecha_registro, estado_registro
    ) VALUES (
        p_id_medicamento, p_id_proveedor, p_numero_lote, p_fecha_fabricacion, p_fecha_vencimiento,
        COALESCE(p_cantidad_inicial, 0), COALESCE(p_cantidad_disponible, p_cantidad_inicial, 0),
        COALESCE(p_usuario_registro, 'sistema'), NOW(), 'ACTIVO'
    );

    SELECT LAST_INSERT_ID() AS id_lote;
END$$

DROP PROCEDURE IF EXISTS sp_actualizar_lote_medicamento$$
CREATE PROCEDURE sp_actualizar_lote_medicamento(
    IN p_id_lote INT,
    IN p_id_medicamento INT,
    IN p_id_proveedor INT,
    IN p_numero_lote VARCHAR(50),
    IN p_fecha_fabricacion DATE,
    IN p_fecha_vencimiento DATE,
    IN p_cantidad_inicial INT,
    IN p_cantidad_disponible INT,
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE lote_medicamento SET
        id_medicamento = p_id_medicamento,
        id_proveedor = p_id_proveedor,
        numero_lote = p_numero_lote,
        fecha_fabricacion = p_fecha_fabricacion,
        fecha_vencimiento = p_fecha_vencimiento,
        cantidad_inicial = p_cantidad_inicial,
        cantidad_disponible = p_cantidad_disponible,
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_lote = p_id_lote;
END$$

DROP PROCEDURE IF EXISTS sp_eliminar_lote_medicamento$$
CREATE PROCEDURE sp_eliminar_lote_medicamento(
    IN p_id_lote INT,
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE lote_medicamento SET
        estado_registro = 'INACTIVO',
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_lote = p_id_lote;
END$$

DROP PROCEDURE IF EXISTS sp_obtener_lote_medicamento_por_id$$
CREATE PROCEDURE sp_obtener_lote_medicamento_por_id(
    IN p_id_lote INT
)
BEGIN
    SELECT 
        l.id_lote, l.id_medicamento, m.nombre AS nombre_medicamento,
        l.id_proveedor, p.razon_social AS razon_social_proveedor,
        l.numero_lote, l.fecha_fabricacion, l.fecha_vencimiento,
        l.cantidad_inicial, l.cantidad_disponible,
        l.usuario_registro, l.fecha_registro, l.usuario_actualizacion, l.fecha_actualizacion, l.estado_registro
    FROM lote_medicamento l
    INNER JOIN medicamento m ON l.id_medicamento = m.id_medicamento
    LEFT JOIN proveedor p ON l.id_proveedor = p.id_proveedor
    WHERE l.id_lote = p_id_lote AND l.estado_registro = 'ACTIVO';
END$$

DROP PROCEDURE IF EXISTS sp_listar_lotes_medicamento$$
CREATE PROCEDURE sp_listar_lotes_medicamento()
BEGIN
    SELECT 
        l.id_lote, l.id_medicamento, m.nombre AS nombre_medicamento,
        l.id_proveedor, p.razon_social AS razon_social_proveedor,
        l.numero_lote, l.fecha_fabricacion, l.fecha_vencimiento,
        l.cantidad_inicial, l.cantidad_disponible,
        l.usuario_registro, l.fecha_registro, l.usuario_actualizacion, l.fecha_actualizacion, l.estado_registro
    FROM lote_medicamento l
    INNER JOIN medicamento m ON l.id_medicamento = m.id_medicamento
    LEFT JOIN proveedor p ON l.id_proveedor = p.id_proveedor
    WHERE l.estado_registro = 'ACTIVO'
    ORDER BY l.fecha_vencimiento ASC;
END$$


-- =============================================================================
-- 5. INVENTARIO
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_registrar_inventario$$
CREATE PROCEDURE sp_registrar_inventario(
    IN p_id_medicamento INT,
    IN p_stock_actual INT,
    IN p_stock_minimo INT,
    IN p_ubicacion VARCHAR(100),
    IN p_usuario_registro VARCHAR(50)
)
BEGIN
    INSERT INTO inventario (
        id_medicamento, stock_actual, stock_minimo, ubicacion,
        usuario_registro, fecha_registro, estado_registro
    ) VALUES (
        p_id_medicamento, COALESCE(p_stock_actual, 0), COALESCE(p_stock_minimo, 10), p_ubicacion,
        COALESCE(p_usuario_registro, 'sistema'), NOW(), 'ACTIVO'
    );

    SELECT LAST_INSERT_ID() AS id_inventario;
END$$

DROP PROCEDURE IF EXISTS sp_actualizar_inventario$$
CREATE PROCEDURE sp_actualizar_inventario(
    IN p_id_inventario INT,
    IN p_id_medicamento INT,
    IN p_stock_actual INT,
    IN p_stock_minimo INT,
    IN p_ubicacion VARCHAR(100),
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE inventario SET
        id_medicamento = p_id_medicamento,
        stock_actual = p_stock_actual,
        stock_minimo = p_stock_minimo,
        ubicacion = p_ubicacion,
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_inventario = p_id_inventario;
END$$

DROP PROCEDURE IF EXISTS sp_eliminar_inventario$$
CREATE PROCEDURE sp_eliminar_inventario(
    IN p_id_inventario INT,
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE inventario SET
        estado_registro = 'INACTIVO',
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_inventario = p_id_inventario;
END$$

DROP PROCEDURE IF EXISTS sp_obtener_inventario_por_id$$
CREATE PROCEDURE sp_obtener_inventario_por_id(
    IN p_id_inventario INT
)
BEGIN
    SELECT 
        i.id_inventario, i.id_medicamento, m.codigo AS codigo_medicamento, m.nombre AS nombre_medicamento,
        i.stock_actual, i.stock_minimo, i.ubicacion,
        i.usuario_registro, i.fecha_registro, i.usuario_actualizacion, i.fecha_actualizacion, i.estado_registro
    FROM inventario i
    INNER JOIN medicamento m ON i.id_medicamento = m.id_medicamento
    WHERE i.id_inventario = p_id_inventario AND i.estado_registro = 'ACTIVO';
END$$

DROP PROCEDURE IF EXISTS sp_listar_inventario$$
CREATE PROCEDURE sp_listar_inventario()
BEGIN
    SELECT 
        i.id_inventario, i.id_medicamento, m.codigo AS codigo_medicamento, m.nombre AS nombre_medicamento,
        i.stock_actual, i.stock_minimo, i.ubicacion,
        i.usuario_registro, i.fecha_registro, i.usuario_actualizacion, i.fecha_actualizacion, i.estado_registro
    FROM inventario i
    INNER JOIN medicamento m ON i.id_medicamento = m.id_medicamento
    WHERE i.estado_registro = 'ACTIVO'
    ORDER BY m.nombre ASC;
END$$


-- =============================================================================
-- 6. USUARIOS DE FARMACIA
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_registrar_usuario_farmacia$$
CREATE PROCEDURE sp_registrar_usuario_farmacia(
    IN p_username VARCHAR(50),
    IN p_nombres VARCHAR(100),
    IN p_apellidos VARCHAR(100),
    IN p_rol VARCHAR(50),
    IN p_password_hash VARCHAR(255),
    IN p_usuario_registro VARCHAR(50)
)
BEGIN
    INSERT INTO usuario_farmacia (
        username, nombres, apellidos, rol, password_hash,
        usuario_registro, fecha_registro, estado_registro
    ) VALUES (
        p_username, p_nombres, p_apellidos, p_rol, p_password_hash,
        COALESCE(p_usuario_registro, 'sistema'), NOW(), 'ACTIVO'
    );

    SELECT LAST_INSERT_ID() AS id_usuario;
END$$

DROP PROCEDURE IF EXISTS sp_actualizar_usuario_farmacia$$
CREATE PROCEDURE sp_actualizar_usuario_farmacia(
    IN p_id_usuario INT,
    IN p_username VARCHAR(50),
    IN p_nombres VARCHAR(100),
    IN p_apellidos VARCHAR(100),
    IN p_rol VARCHAR(50),
    IN p_password_hash VARCHAR(255),
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE usuario_farmacia SET
        username = p_username,
        nombres = p_nombres,
        apellidos = p_apellidos,
        rol = p_rol,
        password_hash = COALESCE(p_password_hash, password_hash),
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_usuario = p_id_usuario;
END$$

DROP PROCEDURE IF EXISTS sp_eliminar_usuario_farmacia$$
CREATE PROCEDURE sp_eliminar_usuario_farmacia(
    IN p_id_usuario INT,
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE usuario_farmacia SET
        estado_registro = 'INACTIVO',
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_usuario = p_id_usuario;
END$$

DROP PROCEDURE IF EXISTS sp_obtener_usuario_farmacia_por_id$$
CREATE PROCEDURE sp_obtener_usuario_farmacia_por_id(
    IN p_id_usuario INT
)
BEGIN
    SELECT 
        id_usuario, username, nombres, apellidos, rol,
        usuario_registro, fecha_registro, usuario_actualizacion, fecha_actualizacion, estado_registro
    FROM usuario_farmacia
    WHERE id_usuario = p_id_usuario AND estado_registro = 'ACTIVO';
END$$

DROP PROCEDURE IF EXISTS sp_listar_usuarios_farmacia$$
CREATE PROCEDURE sp_listar_usuarios_farmacia()
BEGIN
    SELECT 
        id_usuario, username, nombres, apellidos, rol,
        usuario_registro, fecha_registro, usuario_actualizacion, fecha_actualizacion, estado_registro
    FROM usuario_farmacia
    WHERE estado_registro = 'ACTIVO'
    ORDER BY apellidos ASC, nombres ASC;
END$$


-- =============================================================================
-- 7. PRESCRIPCIONES
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_registrar_prescripcion$$
CREATE PROCEDURE sp_registrar_prescripcion(
    IN p_codigo_prescripcion VARCHAR(50),
    IN p_id_paciente_externo INT,
    IN p_id_medico_externo INT,
    IN p_id_atencion_externa INT,
    IN p_fecha_prescripcion DATETIME,
    IN p_instrucciones_generales VARCHAR(700),
    IN p_estado VARCHAR(30),
    IN p_usuario_registro VARCHAR(50)
)
BEGIN
    INSERT INTO prescripcion (
        codigo_prescripcion, id_paciente_externo, id_medico_externo, id_atencion_externa,
        fecha_prescripcion, instrucciones_generales, estado, fecha_recepcion,
        usuario_registro, fecha_registro, estado_registro
    ) VALUES (
        p_codigo_prescripcion, p_id_paciente_externo, p_id_medico_externo, p_id_atencion_externa,
        COALESCE(p_fecha_prescripcion, NOW()), p_instrucciones_generales, COALESCE(p_estado, 'PENDIENTE'), NOW(),
        COALESCE(p_usuario_registro, 'sistema'), NOW(), 'ACTIVO'
    );

    SELECT LAST_INSERT_ID() AS id_prescripcion;
END$$

DROP PROCEDURE IF EXISTS sp_actualizar_prescripcion$$
CREATE PROCEDURE sp_actualizar_prescripcion(
    IN p_id_prescripcion INT,
    IN p_codigo_prescripcion VARCHAR(50),
    IN p_id_paciente_externo INT,
    IN p_id_medico_externo INT,
    IN p_id_atencion_externa INT,
    IN p_fecha_prescripcion DATETIME,
    IN p_instrucciones_generales VARCHAR(700),
    IN p_estado VARCHAR(30),
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE prescripcion SET
        codigo_prescripcion = p_codigo_prescripcion,
        id_paciente_externo = p_id_paciente_externo,
        id_medico_externo = p_id_medico_externo,
        id_atencion_externa = p_id_atencion_externa,
        fecha_prescripcion = p_fecha_prescripcion,
        instrucciones_generales = p_instrucciones_generales,
        estado = COALESCE(p_estado, estado),
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_prescripcion = p_id_prescripcion;
END$$

DROP PROCEDURE IF EXISTS sp_eliminar_prescripcion$$
CREATE PROCEDURE sp_eliminar_prescripcion(
    IN p_id_prescripcion INT,
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE prescripcion SET
        estado_registro = 'INACTIVO',
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_prescripcion = p_id_prescripcion;
END$$

DROP PROCEDURE IF EXISTS sp_obtener_prescripcion_por_id$$
CREATE PROCEDURE sp_obtener_prescripcion_por_id(
    IN p_id_prescripcion INT
)
BEGIN
    SELECT 
        id_prescripcion, codigo_prescripcion, id_paciente_externo, id_medico_externo, id_atencion_externa,
        fecha_prescripcion, instrucciones_generales, estado, fecha_recepcion,
        usuario_registro, fecha_registro, usuario_actualizacion, fecha_actualizacion, estado_registro
    FROM prescripcion
    WHERE id_prescripcion = p_id_prescripcion AND estado_registro = 'ACTIVO';
END$$

DROP PROCEDURE IF EXISTS sp_listar_prescripciones$$
CREATE PROCEDURE sp_listar_prescripciones()
BEGIN
    SELECT 
        id_prescripcion, codigo_prescripcion, id_paciente_externo, id_medico_externo, id_atencion_externa,
        fecha_prescripcion, instrucciones_generales, estado, fecha_recepcion,
        usuario_registro, fecha_registro, usuario_actualizacion, fecha_actualizacion, estado_registro
    FROM prescripcion
    WHERE estado_registro = 'ACTIVO'
    ORDER BY fecha_prescripcion DESC;
END$$


-- =============================================================================
-- 8. DETALLE DE PRESCRIPCION
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_registrar_prescripcion_detalle$$
CREATE PROCEDURE sp_registrar_prescripcion_detalle(
    IN p_id_prescripcion INT,
    IN p_id_medicamento INT,
    IN p_nombre_medicamento VARCHAR(150),
    IN p_cantidad INT,
    IN p_dosis VARCHAR(100),
    IN p_frecuencia VARCHAR(100),
    IN p_duracion VARCHAR(100),
    IN p_indicaciones VARCHAR(500),
    IN p_usuario_registro VARCHAR(50)
)
BEGIN
    INSERT INTO prescripcion_detalle (
        id_prescripcion, id_medicamento, nombre_medicamento, cantidad,
        dosis, frecuencia, duracion, indicaciones,
        usuario_registro, fecha_registro, estado_registro
    ) VALUES (
        p_id_prescripcion, p_id_medicamento, p_nombre_medicamento, p_cantidad,
        p_dosis, p_frecuencia, p_duracion, p_indicaciones,
        COALESCE(p_usuario_registro, 'sistema'), NOW(), 'ACTIVO'
    );

    SELECT LAST_INSERT_ID() AS id_detalle_prescripcion;
END$$

DROP PROCEDURE IF EXISTS sp_actualizar_prescripcion_detalle$$
CREATE PROCEDURE sp_actualizar_prescripcion_detalle(
    IN p_id_detalle_prescripcion INT,
    IN p_id_prescripcion INT,
    IN p_id_medicamento INT,
    IN p_nombre_medicamento VARCHAR(150),
    IN p_cantidad INT,
    IN p_dosis VARCHAR(100),
    IN p_frecuencia VARCHAR(100),
    IN p_duracion VARCHAR(100),
    IN p_indicaciones VARCHAR(500),
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE prescripcion_detalle SET
        id_prescripcion = p_id_prescripcion,
        id_medicamento = p_id_medicamento,
        nombre_medicamento = p_nombre_medicamento,
        cantidad = p_cantidad,
        dosis = p_dosis,
        frecuencia = p_frecuencia,
        duracion = p_duracion,
        indicaciones = p_indicaciones,
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_detalle_prescripcion = p_id_detalle_prescripcion;
END$$

DROP PROCEDURE IF EXISTS sp_eliminar_prescripcion_detalle$$
CREATE PROCEDURE sp_eliminar_prescripcion_detalle(
    IN p_id_detalle_prescripcion INT,
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE prescripcion_detalle SET
        estado_registro = 'INACTIVO',
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_detalle_prescripcion = p_id_detalle_prescripcion;
END$$

DROP PROCEDURE IF EXISTS sp_obtener_prescripcion_detalle_por_id$$
CREATE PROCEDURE sp_obtener_prescripcion_detalle_por_id(
    IN p_id_detalle_prescripcion INT
)
BEGIN
    SELECT 
        pd.id_detalle_prescripcion, pd.id_prescripcion, pd.id_medicamento, pd.nombre_medicamento,
        pd.cantidad, pd.dosis, pd.frecuencia, pd.duracion, pd.indicaciones,
        pd.usuario_registro, pd.fecha_registro, pd.usuario_actualizacion, pd.fecha_actualizacion, pd.estado_registro
    FROM prescripcion_detalle pd
    WHERE pd.id_detalle_prescripcion = p_id_detalle_prescripcion AND pd.estado_registro = 'ACTIVO';
END$$

DROP PROCEDURE IF EXISTS sp_listar_prescripcion_detalles_por_prescripcion$$
CREATE PROCEDURE sp_listar_prescripcion_detalles_por_prescripcion(
    IN p_id_prescripcion INT
)
BEGIN
    SELECT 
        pd.id_detalle_prescripcion, pd.id_prescripcion, pd.id_medicamento, pd.nombre_medicamento,
        pd.cantidad, pd.dosis, pd.frecuencia, pd.duracion, pd.indicaciones,
        pd.usuario_registro, pd.fecha_registro, pd.usuario_actualizacion, pd.fecha_actualizacion, pd.estado_registro
    FROM prescripcion_detalle pd
    WHERE pd.id_prescripcion = p_id_prescripcion AND pd.estado_registro = 'ACTIVO'
    ORDER BY pd.id_detalle_prescripcion ASC;
END$$


-- =============================================================================
-- 9. VENTAS
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_registrar_venta$$
CREATE PROCEDURE sp_registrar_venta(
    IN p_codigo_venta VARCHAR(30),
    IN p_dni VARCHAR(15),
    IN p_nombres VARCHAR(50),
    IN p_apellidos VARCHAR(100),
    IN p_id_prescripcion INT,
    IN p_fecha_venta DATETIME,
    IN p_subtotal DECIMAL(10,2),
    IN p_igv DECIMAL(10,2),
    IN p_total DECIMAL(10,2),
    IN p_metodo_pago VARCHAR(30),
    IN p_id_usuario INT,
    IN p_usuario_registro VARCHAR(50)
)
BEGIN
    INSERT INTO venta (
        codigo_venta, dni, nombres, apellidos, id_prescripcion,
        fecha_venta, subtotal, igv, total, metodo_pago, id_usuario,
        usuario_registro, fecha_registro, estado_registro
    ) VALUES (
        p_codigo_venta, p_dni, p_nombres, p_apellidos, p_id_prescripcion,
        COALESCE(p_fecha_venta, NOW()), COALESCE(p_subtotal, 0.00), COALESCE(p_igv, 0.00), COALESCE(p_total, 0.00), p_metodo_pago, p_id_usuario,
        COALESCE(p_usuario_registro, 'sistema'), NOW(), 'ACTIVO'
    );

    IF p_id_prescripcion IS NOT NULL AND p_id_prescripcion > 0 THEN
        UPDATE prescripcion 
        SET estado = 'ATENDIDA',
            usuario_actualizacion = COALESCE(p_usuario_registro, 'sistema'),
            fecha_actualizacion = NOW()
        WHERE id_prescripcion = p_id_prescripcion;
    END IF;

    SELECT LAST_INSERT_ID() AS id_venta;
END$$

DROP PROCEDURE IF EXISTS sp_actualizar_venta$$
CREATE PROCEDURE sp_actualizar_venta(
    IN p_id_venta INT,
    IN p_codigo_venta VARCHAR(30),
    IN p_dni VARCHAR(15),
    IN p_nombres VARCHAR(50),
    IN p_apellidos VARCHAR(100),
    IN p_id_prescripcion INT,
    IN p_fecha_venta DATETIME,
    IN p_subtotal DECIMAL(10,2),
    IN p_igv DECIMAL(10,2),
    IN p_total DECIMAL(10,2),
    IN p_metodo_pago VARCHAR(30),
    IN p_id_usuario INT,
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE venta SET
        codigo_venta = p_codigo_venta,
        dni = p_dni,
        nombres = p_nombres,
        apellidos = p_apellidos,
        id_prescripcion = p_id_prescripcion,
        fecha_venta = p_fecha_venta,
        subtotal = p_subtotal,
        igv = p_igv,
        total = p_total,
        metodo_pago = p_metodo_pago,
        id_usuario = p_id_usuario,
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_venta = p_id_venta;
END$$

DROP PROCEDURE IF EXISTS sp_eliminar_venta$$
CREATE PROCEDURE sp_eliminar_venta(
    IN p_id_venta INT,
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE venta SET
        estado_registro = 'INACTIVO',
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_venta = p_id_venta;
END$$

DROP PROCEDURE IF EXISTS sp_obtener_venta_por_id$$
CREATE PROCEDURE sp_obtener_venta_por_id(
    IN p_id_venta INT
)
BEGIN
    SELECT 
        v.id_venta, v.codigo_venta, v.dni, v.nombres, v.apellidos, v.id_prescripcion,
        v.fecha_venta, v.subtotal, v.igv, v.total, v.metodo_pago, v.id_usuario,
        u.username AS username_usuario,
        v.usuario_registro, v.fecha_registro, v.usuario_actualizacion, v.fecha_actualizacion, v.estado_registro
    FROM venta v
    LEFT JOIN usuario_farmacia u ON v.id_usuario = u.id_usuario
    WHERE v.id_venta = p_id_venta AND v.estado_registro = 'ACTIVO';
END$$

DROP PROCEDURE IF EXISTS sp_listar_ventas$$
CREATE PROCEDURE sp_listar_ventas()
BEGIN
    SELECT 
        v.id_venta, v.codigo_venta, v.dni, v.nombres, v.apellidos, v.id_prescripcion,
        v.fecha_venta, v.subtotal, v.igv, v.total, v.metodo_pago, v.id_usuario,
        u.username AS username_usuario,
        v.usuario_registro, v.fecha_registro, v.usuario_actualizacion, v.fecha_actualizacion, v.estado_registro
    FROM venta v
    LEFT JOIN usuario_farmacia u ON v.id_usuario = u.id_usuario
    WHERE v.estado_registro = 'ACTIVO'
    ORDER BY v.fecha_venta DESC;
END$$


-- =============================================================================
-- 10. DETALLE DE VENTA
-- =============================================================================

DROP PROCEDURE IF EXISTS sp_registrar_venta_detalle$$
CREATE PROCEDURE sp_registrar_venta_detalle(
    IN p_id_venta INT,
    IN p_id_medicamento INT,
    IN p_cantidad INT,
    IN p_precio_unitario DECIMAL(10,2),
    IN p_subtotal DECIMAL(10,2),
    IN p_usuario_registro VARCHAR(50)
)
BEGIN
    INSERT INTO venta_detalle (
        id_venta, id_medicamento, cantidad, precio_unitario, subtotal,
        usuario_registro, fecha_registro, estado_registro
    ) VALUES (
        p_id_venta, p_id_medicamento, p_cantidad, p_precio_unitario, p_subtotal,
        COALESCE(p_usuario_registro, 'sistema'), NOW(), 'ACTIVO'
    );

    SELECT LAST_INSERT_ID() AS id_detalle;
END$$

DROP PROCEDURE IF EXISTS sp_actualizar_venta_detalle$$
CREATE PROCEDURE sp_actualizar_venta_detalle(
    IN p_id_detalle INT,
    IN p_id_venta INT,
    IN p_id_medicamento INT,
    IN p_cantidad INT,
    IN p_precio_unitario DECIMAL(10,2),
    IN p_subtotal DECIMAL(10,2),
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE venta_detalle SET
        id_venta = p_id_venta,
        id_medicamento = p_id_medicamento,
        cantidad = p_cantidad,
        precio_unitario = p_precio_unitario,
        subtotal = p_subtotal,
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_detalle = p_id_detalle;
END$$

DROP PROCEDURE IF EXISTS sp_eliminar_venta_detalle$$
CREATE PROCEDURE sp_eliminar_venta_detalle(
    IN p_id_detalle INT,
    IN p_usuario_actualizacion VARCHAR(50)
)
BEGIN
    UPDATE venta_detalle SET
        estado_registro = 'INACTIVO',
        usuario_actualizacion = COALESCE(p_usuario_actualizacion, 'sistema'),
        fecha_actualizacion = NOW()
    WHERE id_detalle = p_id_detalle;
END$$

DROP PROCEDURE IF EXISTS sp_obtener_venta_detalle_por_id$$
CREATE PROCEDURE sp_obtener_venta_detalle_por_id(
    IN p_id_detalle INT
)
BEGIN
    SELECT 
        vd.id_detalle, vd.id_venta, vd.id_medicamento, m.nombre AS nombre_medicamento,
        vd.cantidad, vd.precio_unitario, vd.subtotal,
        vd.usuario_registro, vd.fecha_registro, vd.usuario_actualizacion, vd.fecha_actualizacion, vd.estado_registro
    FROM venta_detalle vd
    INNER JOIN medicamento m ON vd.id_medicamento = m.id_medicamento
    WHERE vd.id_detalle = p_id_detalle AND vd.estado_registro = 'ACTIVO';
END$$

DROP PROCEDURE IF EXISTS sp_listar_venta_detalles_por_venta$$
CREATE PROCEDURE sp_listar_venta_detalles_por_venta(
    IN p_id_venta INT
)
BEGIN
    SELECT 
        vd.id_detalle, vd.id_venta, vd.id_medicamento, m.nombre AS nombre_medicamento,
        vd.cantidad, vd.precio_unitario, vd.subtotal,
        vd.usuario_registro, vd.fecha_registro, vd.usuario_actualizacion, vd.fecha_actualizacion, vd.estado_registro
    FROM venta_detalle vd
    INNER JOIN medicamento m ON vd.id_medicamento = m.id_medicamento
    WHERE vd.id_venta = p_id_venta AND vd.estado_registro = 'ACTIVO'
    ORDER BY vd.id_detalle ASC;
END$$

DELIMITER ;
