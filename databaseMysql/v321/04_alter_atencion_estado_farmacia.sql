-- =============================================================================
-- Migración: Incorporar estado_envio_farmacia a atencion_medica
-- Propósito: Soportar desacoplamiento temporal y resiliencia SOA hacia Farmacia
-- Estados: 'NO_APLICA', 'ENVIADO_A_FARMACIA', 'PENDIENTE_ENVIO_FARMACIA'
-- =============================================================================

USE bd_citasmedicas_v321;

SET @existe_columna = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'bd_citasmedicas_v321' 
      AND TABLE_NAME = 'atencion_medica' 
      AND COLUMN_NAME = 'estado_envio_farmacia'
);

SET @sql = IF(@existe_columna = 0, 
    'ALTER TABLE atencion_medica ADD COLUMN estado_envio_farmacia VARCHAR(30) NOT NULL DEFAULT ''NO_APLICA'' AFTER indicaciones;', 
    'SELECT ''Columna estado_envio_farmacia ya existe en atencion_medica'' AS mensaje;'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Actualizar registros existentes con receta médica
UPDATE atencion_medica 
SET estado_envio_farmacia = 'ENVIADO_A_FARMACIA' 
WHERE receta_medica IS NOT NULL 
  AND TRIM(receta_medica) <> '' 
  AND (estado_envio_farmacia IS NULL OR estado_envio_farmacia = 'NO_APLICA');