# NovaSalud SOA – Guía de continuidad del desarrollo

**Repositorio:** https://github.com/arianr2014/SistemaARQ
**Curso:** Arquitectura Orientada a Servicios (SOA) – Trabajo final 2026
**Última actualización:** 8 de noviembre de 2026
**Estado:** Fases 1, 2, 3, 4 y 5 terminadas y fusionadas en main.

Este documento resume todo lo desarrollado y probado hasta ahora, explica cómo
levantar el entorno, cómo trabajar con Git y qué falta hacer, para que cualquier
integrante pueda continuar el desarrollo sin repetir el análisis.

---

## Índice

1. [Contexto del proyecto](#1-contexto-del-proyecto)
2. [Arquitectura actual](#2-arquitectura-actual)
3. [Cómo levantar el entorno](#3-cómo-levantar-el-entorno)
4. [Flujo de trabajo con Git](#4-flujo-de-trabajo-con-git)
5. [Historial de cambios (Pull Requests)](#5-historial-de-cambios-pull-requests)
6. [Pruebas realizadas y cómo repetirlas](#6-pruebas-realizadas-y-cómo-repetirlas)
7. [Contrato de la prescripción (resumen)](#7-contrato-de-la-prescripción-resumen)
8. [El ESB (novasalud-esb)](#8-el-esb-novasalud-esb)
9. [Hallazgos y problemas conocidos](#9-hallazgos-y-problemas-conocidos)
10. [Cómo continuar: plan de trabajo](#10-cómo-continuar-plan-de-trabajo)
11. [Puntos clave y errores frecuentes](#11-puntos-clave-y-errores-frecuentes)
12. [Relación con el APF3](#12-relación-con-el-apf3)

---

## 1. Contexto del proyecto

NovaSalud es una clínica con dos sistemas independientes que no se comunicaban:

- **Sistema de Citas:** pacientes, médicos, citas, pagos, atención médica e historia clínica.
- **Sistema de Farmacia:** medicamentos, inventario, ventas y prescripciones.

**Problema del APF1:** la receta del médico no llegaba a Farmacia de forma
digital; se transcribía manualmente.

**Solución propuesta en el APF2 (documento del grupo):** una arquitectura SOA con
servicios independientes, un **Orquestador** que coordina el proceso de atención y
un **ESB** (Enterprise Service Bus) como capa de integración.

**Proceso central (sección 11.1 / 12.1 del documento APF2):**

1. El médico presiona "Guardar y Finalizar Atención".
2. El Orquestador valida que la cita exista y esté **PAGADA** (Citas API).
3. Registra la atención y la historia clínica (Atención API).
4. Si hay receta, la envía a Farmacia (Farmacia API).
5. Si Farmacia no responde, la receta queda como `PENDIENTE_ENVIO_FARMACIA`.

**Entregables del curso:** APF1 (problema), APF2 (diseño SOA), APF3 (implementación
técnica), Proyecto Final (APF1 + APF2 + APF3 corregidos e integrados).

---

## 2. Arquitectura actual

### 2.1 Módulos

| Módulo | Tipo | Responsabilidad |
| --- | --- | --- |
| `novasalud-citas-api` | API REST | Pacientes, médicos, citas, pagos, usuarios, login |
| `novasalud-atencion-api` | API REST | Atención médica e historia clínica |
| `novasalud-farmacia-api` | API REST | Medicamentos, inventario, ventas, prescripciones |
| `novasalud-orchestrator` | API REST | Orquesta el proceso de atención (Citas → Atención → Farmacia) |
| `novasalud-esb` | ESB (Apache Camel) | Enrutamiento, seguridad, trazabilidad y registro |
| `novasalud-citas-web` | Web (JSP) | Frontend del sistema de Citas |
| `novasalud-farmacia-web` | Web (JSP) | Frontend del sistema de Farmacia |

**Tecnologías:** Java 17, Jakarta EE 10 (JAX-RS con Jersey), JSON (Jackson/Gson),
MySQL 8, Apache Tomcat 10.1, Apache Camel 4.4, JWT (jjwt 0.11.5), BCrypt,
OpenAPI/Swagger, Docker y Docker Compose.

### 2.2 Bases de datos

| Base | Usuario | Contraseña | Usada por |
| --- | --- | --- | --- |
| `bd_citasmedicas_v321` | `usuario_citas` | `ISO/IEC27001` | Citas API y Atención API |
| `bd_famacia` (sic, sin la "r") | `usuario_farmacia` | `Farmacia2026*` | Farmacia API |

> ⚠️ La base de Farmacia se llama `bd_famacia` (sin "r"). No es un error de esta
> guía: así está en los scripts originales.

### 2.3 Contenedores y puertos (Docker)

| Servicio | Contenedor | Puerto local | URL principal |
| --- | --- | --- | --- |
| Frontend Citas | `novasalud-citas-web` | 8080 | http://localhost:8080/novasalud-citas-web |
| Frontend Farmacia | `novasalud-farmacia-web` | 8081 | http://localhost:8081/novasalud-farmacia-web |
| Atención API | `novasalud-atencion-api` | 8082 | http://localhost:8082/novasalud-atencion-api/swagger/ |
| Orquestador | `novasalud-orchestrator` | 8083 | http://localhost:8083/novasalud-orchestrator/swagger/ |
| Citas API | `novasalud-citas-api` | 8084 | http://localhost:8084/novasalud-citas-api/swagger/ |
| Farmacia API | `novasalud-farmacia-api` | 8085 | http://localhost:8085/novasalud-farmacia-api/swagger/ |
| ESB | `novasalud-esb` | 8086 | http://localhost:8086/novasalud-esb/esb/health |
| MySQL | `novasalud-mysql` | 3307 | `localhost:3307` (root / root) |

Dentro de la red de Docker todos los servicios escuchan en el puerto **8080** y se
ubican por nombre (`citas-api`, `atencion-api`, `farmacia-api`, `orchestrator`,
`esb`, `mysql`).

### 2.4 Flujo de comunicación

**Estado actual (Fases 1 a 5 completadas):** los consumidores frontends y el orquestador
se comunican a través del ESB (`novasalud-esb`, puerto 8086), con enriquecimiento de contenido
y trazabilidad continua (`X-Correlation-ID`).

```
Frontend Citas ──► ESB (8086) ──► Orquestador ──► ESB ──► Citas / Atención / Farmacia
Frontend Farmacia ──► ESB (8086) ──► Farmacia (+ Content Enricher con Citas y Atención)
```

### 2.5 Variables de entorno relevantes

| Variable | Usada por | Propósito |
| --- | --- | --- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | APIs | Conexión a la BD (genérica) |
| `NOVASALUD_DB_*` / `NOVASALUD_FARMACIA_DB_*` | Citas/Atención / Farmacia | Conexión específica (tiene prioridad) |
| `NOVASALUD_JWT_SECRET` | Las 4 APIs y el ESB | **Clave JWT compartida** (Fase 1) |
| `CITAS_API_URL`, `ATENCION_API_URL`, `FARMACIA_API_URL` | Orquestador y ESB | Ubicación de los servicios |
| `ORCHESTRATOR_API_URL` | ESB | Ubicación del Orquestador |
| `ESB_CONNECT_TIMEOUT_SECONDS` / `ESB_RESPONSE_TIMEOUT_SECONDS` | ESB | Tiempos máximos (por defecto 3 y 15 s) |

Los frontends no leen variables de entorno para las URLs: en Docker se usa
`docker/config/<módulo>/api.properties`, que el `Dockerfile` copia sobre el de
desarrollo al construir la imagen.

---

## 3. Cómo levantar el entorno

### 3.1 Requisitos

- **Git:** `winget install --id Git.Git -e --source winget`
- **Docker Desktop:** `winget install --id Docker.DockerDesktop -e` (reiniciar la PC
  y abrirlo hasta que diga "Engine running").
- Opcional: DBeaver para consultar la base de datos.

### 3.2 Levantar todo

```powershell
git clone https://github.com/arianr2014/SistemaARQ.git
cd SistemaARQ
docker compose up -d --build
docker compose ps
```

La primera vez tarda varios minutos (descarga imágenes, compila los 7 módulos y
MySQL ejecuta los scripts). Espera a que `novasalud-mysql` aparezca como
**healthy** y el resto como **running**.

### 3.3 Comandos útiles

```powershell
docker compose ps                           # estado
docker compose logs -f esb                  # registro del ESB
docker compose logs -f orchestrator         # registro de un servicio
docker compose up -d --build farmacia-api   # reconstruir un solo servicio
docker compose stop farmacia-api            # simular un servicio caído
docker compose start farmacia-api
docker compose down                         # detener
docker compose down -v                      # detener y BORRAR la BD (se recrea al levantar)
```

### 3.4 Credenciales de prueba

**Sistema de Citas** – contraseña `123456`:

| Rol | Usuarios |
| --- | --- |
| Administrador | `administrador` |
| Recepción | `recepcionista1`, `recepcionista2` |
| Caja | `cajero1`, `cajero2` |
| Dirección | `direccion` |
| Médicos | `valeria.rojas` (id_medico 1, Medicina General), `luis.herrera`, `camila.torres`, `marco.salas`, `ana.beltran`, `diego.munoz`… |

> ⚠️ El usuario `doctor` existe pero está **INACTIVO**. Usar `valeria.rojas`.

**Sistema de Farmacia** – contraseña `Farmacia2026*`:

| Rol | Usuario |
| --- | --- |
| Administrador | `admin` |
| Químico farmacéutico | `qf_martinez` |
| Técnico de farmacia | `cajero01` |

Las contraseñas se guardan con **BCrypt** (hash irreversible): no se pueden leer
desde la BD. Si hace falta cambiar una, generar un hash BCrypt y actualizar la
columna `password_hash`. Hash verificado de `123456`:
`$2a$12$f4Dgech6Y2lRtVjJ9CVZ/.irh2eiX.rgOLOmi7.eaLIOC9tPIRRSm`

### 3.5 Conexión con DBeaver

Nueva conexión MySQL: host `localhost`, puerto **3307**, usuario `root`, contraseña
`root`. En *Driver properties*: `allowPublicKeyRetrieval = true` y `useSSL = false`.

---

## 4. Flujo de trabajo con Git

El repositorio pertenece a `arianr2014`. Los demás integrantes trabajan con un
**fork** y envían **Pull Requests**.

### 4.1 Configuración inicial (una sola vez)

```powershell
git config --global user.name "Tu Nombre"
git config --global user.email "correo_de_tu_cuenta_github@ejemplo.com"
git config --global core.editor notepad      # evita que Git abra Vim

# Después de hacer Fork en GitHub:
git clone https://github.com/<TU_USUARIO>/SistemaARQ.git
cd SistemaARQ
git remote add upstream https://github.com/arianr2014/SistemaARQ.git
```

- `origin` = tu fork (donde subes).
- `upstream` = repositorio de Arian (de donde traes los cambios aceptados).

### 4.2 Para cada nueva tarea

```powershell
git checkout main
git pull upstream main          # traer lo aceptado
git push origin main            # actualizar tu fork
git checkout -b nombre-de-la-rama
# ... cambios ...
git add .
git commit -m "Descripción clara del cambio"
git push -u origin nombre-de-la-rama
```

Luego, en GitHub: **Compare & pull request** → base `arianr2014/SistemaARQ:main`.

> ⚠️ **Crear siempre la rama nueva parado en `main` actualizado.** Si se crea
> desde otra rama, hereda sus commits y el PR incluye cambios ajenos (nos pasó con
> el PR #3, que terminó incluyendo el Swagger).

### 4.3 Si tu rama quedó atrás de `main`

```powershell
git checkout mi-rama
git pull upstream main     # combina main en tu rama (puede abrir el editor: guardar y cerrar)
git push
```

Si Git abre **Vim**: presionar `Esc`, escribir `:wq` y `Enter`.

### 4.4 Aplicar un archivo `.patch`

```powershell
git am "C:\ruta\al\archivo.patch"
```

---

## 5. Historial de cambios (Pull Requests)

### PR #1 – Despliegue con Docker ✅ aceptado

Sin modificar código Java.

| Archivo | Descripción |
| --- | --- |
| `Dockerfile` | Genérico para todos los módulos (`--build-arg MODULE=...`): Maven + Java 17 → Tomcat 10.1 |
| `docker-compose.yml` | MySQL 8 + todos los servicios |
| `docker/config/novasalud-*-web/api.properties` | URLs de los frontends dentro de Docker |
| `docker/mysql/99_usuarios_docker.sql` | Usuarios MySQL con host `'%'` (los scripts originales usan `'localhost'`) |
| `DOCKER.md` | Instrucciones y URLs |

### PR #2 – Fase 1: seguridad entre servicios ✅ aceptado

1. **Clave JWT compartida.** Farmacia tenía su clave fija en el código y distinta a
   la de Citas, por lo que rechazaba (401) los tokens de Citas. Ahora
   `novasalud-farmacia-api/.../util/JwtUtil.java` lee `NOVASALUD_JWT_SECRET` o
   `jwt.secret` de `db.properties`, y `docker-compose.yml` envía la misma clave a
   todos los backends.
2. **Swagger con context path.** Swagger enviaba las pruebas a `/api/...` sin
   `/novasalud-xxx-api` y Tomcat respondía 404. Se agregó un `requestInterceptor`
   en `src/main/webapp/swagger/index.html`.
3. **UTF-8 en MySQL.** Las tildes quedaban mal guardadas ("MÃ©dico"). Ahora
   `docker/mysql/00_init_utf8.sh` ejecuta los scripts con `utf8mb4`, y
   `.gitattributes` mantiene los `.sh` con fin de línea LF.

### PR #3 – Fase 2: contrato de la prescripción + Swagger ✅ aceptado

1. **Carpeta `contratos/`** (no `docs/`: está excluida en `.gitignore`):
   - `CONTRATO_PRESCRIPCION.md`: mensaje canónico, formato Farmacia, reglas de
     transformación, respuestas, reintentos y estados.
   - `prescripcion-canonica.schema.json` y `prescripcion-farmacia.schema.json`.
   - `ejemplos/01_prescripcion_canonica.json`, `02_prescripcion_farmacia.json`,
     `03_prescripcion_enriquecida.json`.
2. **Decisión de diseño "Opción 1":** Farmacia no almacena datos personales. Al
   mostrar una receta, el ESB consulta paciente y médico a Citas y el diagnóstico
   a Atención (patrón *Content Enricher*).
3. **Farmacia API responde 409** (`PRESCRIPCION_DUPLICADA`) ante un
   `codigoPrescripcion` repetido, en lugar de 500 con el mensaje de MySQL
   (`PrescripcionResource.java`, detecta el código de error MySQL 1062).
4. **Swagger en Atención API y Orquestador** (`swagger/index.html` +
   corrección de `SwaggerRedirectServlet`).
5. **README:** credenciales reales.

### PR #4 – Fase 3: ESB con Apache Camel ✅ aceptado (commit `ed018f9`)

Nuevo módulo `novasalud-esb` con Apache Camel 4.4 en puerto `8086`, validación de tokens JWT, propagación de `X-Correlation-ID` y registro de auditoría (`AuditLogProcessor`).

### PR #5 – Fase 5: Frontend de atención, receta estructurada y resiliencia SOA ✅ aceptado (commit `62eeaf6`)

1. **Receta médica estructurada:** Formulario dinámico en `atencion/form.jsp` con modal interactivo de búsqueda conectado al catálogo de medicamentos de `bd_famacia` (`novasalud-farmacia-api`).
2. **Validación de negocio estricta:** Eliminación del fallback indebido; el médico no puede finalizar si la cita no está `PAGADO` (error 400).
3. **Persistencia y resiliencia:** Agregada columna `estado_envio_farmacia` en tabla `atencion_medica` (`bd_citasmedicas_v321`) y scheduler de reintentos automáticos en el Orquestador (`FarmaciaRetryListener`) cada 2 minutos.
4. **Visualización en Farmacia Web:** Modal "Ver Receta" en `novasalud-farmacia-web` mostrando la lista completa de medicamentos.

### PR #6 – Fase 4: Integración completa a través del ESB, Content Enricher y trazabilidad ✅ aceptado (commit `6fe7231`)

1. **Content Enricher en el ESB:** Procesador `PrescripcionContentEnricherProcessor` en `novasalud-esb` que intercepta `GET /esb/farmacia/prescripciones` y enriquece las recetas con paciente/DNI (Citas API), médico/CMP (Citas API), diagnóstico (Atención API) y fechas de vigencia. Incluye caché local por petición (evita N+1) y tolerancia a fallos.
2. **CMP y DNI en Citas API:** `MedicoDAOImpl.obtenerPorId` actualizado con `LEFT JOIN especialidad` para entregar `cmp`, `dni` y especialidad.
3. **Trazabilidad de punta a punta:** El Orquestador y los clientes Web capturan, reutilizan y propagan `X-Correlation-ID` en todos los saltos.
4. **Enrutamiento por ESB:** `docker-compose.yml` y `api.properties` configurados para que todo el tráfico viaje por `http://esb:8080/novasalud-esb/esb/...`.

---

## 6. Pruebas realizadas y cómo repetirlas

Todas se ejecutaron en Docker desde PowerShell, en la raíz del proyecto.

> ℹ️ En PowerShell 5.1 las tildes de las respuestas se ven mal ("LucÃ­a"). Es solo
> la visualización: en el navegador, Swagger o la BD se ven correctas.

### 6.1 Fase 1 – Token de Citas aceptado por Farmacia

```powershell
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8084/novasalud-citas-api/api/auth/login" -ContentType "application/json" -Body '{"username":"valeria.rojas","password":"123456"}'
$token = $login.token
Invoke-RestMethod -Uri "http://localhost:8085/novasalud-farmacia-api/api/prescripciones" -Headers @{ Authorization = "Bearer $token" }
```

✅ Resultado: lista de prescripciones (antes: 401).
✅ Login en Swagger de Citas: 200, `cargo` = "Médico especialista - Medicina General" (UTF-8 correcto).

### 6.2 Fase 2 – Formato del contrato contra Farmacia API

```powershell
$f = Invoke-RestMethod -Method Post -Uri "http://localhost:8085/novasalud-farmacia-api/api/auth/login" -ContentType "application/json" -Body '{"username":"qf_martinez","password":"Farmacia2026*"}'
$h = @{ Authorization = "Bearer $($f.token)" }
$json = (Get-Content contratos\ejemplos\02_prescripcion_farmacia.json -Raw -Encoding UTF8) -replace '"RX-25"','"RX-9001"' -replace '"idAtencionExterna": 25','"idAtencionExterna": 9001'
$body = [System.Text.Encoding]::UTF8.GetBytes($json)

# 1er envío: 201
$r = Invoke-RestMethod -Method Post -Uri "http://localhost:8085/novasalud-farmacia-api/api/prescripciones" -Headers $h -ContentType "application/json; charset=utf-8" -Body $body
$r.idPrescripcion

# 2º envío (duplicado): 409
try { Invoke-RestMethod -Method Post -Uri "http://localhost:8085/novasalud-farmacia-api/api/prescripciones" -Headers $h -ContentType "application/json; charset=utf-8" -Body $body }
catch { "HTTP " + $_.Exception.Response.StatusCode.value__; $_.ErrorDetails.Message }
```

✅ 201 con los 2 medicamentos guardados; ✅ 409 `PRESCRIPCION_DUPLICADA`;
✅ un medicamento con ID inexistente sigue dando 500 (no se confunde con duplicado).

**Limpiar la receta de prueba** (el DELETE de la API es lógico y no libera el código):

```powershell
docker exec novasalud-mysql mysql -uroot -proot -D bd_famacia -e "DELETE d FROM prescripcion_detalle d JOIN prescripcion p ON p.id_prescripcion = d.id_prescripcion WHERE p.codigo_prescripcion = 'RX-9001'; DELETE FROM prescripcion WHERE codigo_prescripcion = 'RX-9001';"
```

### 6.3 Fase 3 – ESB

```powershell
Invoke-RestMethod http://localhost:8086/novasalud-esb/esb/health

$l = Invoke-RestMethod -Method Post -Uri "http://localhost:8086/novasalud-esb/esb/citas/auth/login" -ContentType "application/json" -Body '{"username":"valeria.rojas","password":"123456"}'
$h = @{ Authorization = "Bearer $($l.token)" }

Invoke-RestMethod -Uri "http://localhost:8086/novasalud-esb/esb/citas/pacientes/3" -Headers $h
Invoke-RestMethod -Uri "http://localhost:8086/novasalud-esb/esb/farmacia/prescripciones" -Headers $h

try { Invoke-RestMethod "http://localhost:8086/novasalud-esb/esb/citas/pacientes/3" } catch { "HTTP " + $_.Exception.Response.StatusCode.value__ }

docker compose stop farmacia-api
try { Invoke-RestMethod -Uri "http://localhost:8086/novasalud-esb/esb/farmacia/prescripciones" -Headers $h } catch { "HTTP " + $_.Exception.Response.StatusCode.value__ }
docker compose start farmacia-api

docker compose logs esb | Select-String "NOVASALUD-ESB - \["
```

Resultado obtenido (registro del ESB):

```
[ESB-20261108172734-c987909e] CITAS-API -> POST /citas/auth/login | HTTP 200 | 479 ms
[ESB-20261108172744-91f674cb] CITAS-API -> GET /citas/pacientes/3 | HTTP 200 | 200 ms
[ESB-20261108172744-8b83df0a] FARMACIA-API -> GET /farmacia/prescripciones | HTTP 200 | 289 ms
[ESB-20261108172757-b5338bdb] CITAS-API -> GET /citas/pacientes/3 | HTTP 401 | 0 ms
[ESB-20261108173041-3802ebd6] FARMACIA-API -> GET /farmacia/prescripciones | HTTP 503 | 4037 ms
```

También se verificó (en un entorno local con las APIs y BD reales): correlation ID
propio respetado (`X-Correlation-ID: PRUEBA-123`), POST con cuerpo vía ESB (201 y
luego 409), 404 de un servicio devuelto tal cual, ruta inexistente → 404 y
Orquestador accesible vía `/esb/orquestacion/orquestacion/atencion`.

### 6.4 Evidencias recomendadas para el APF3

- Swagger de los 4 servicios con una respuesta 200.
- Farmacia Web → Prescripciones mostrando datos **inventados** (evidencia del
  "antes"; ver hallazgo 9.2).
- `/esb/health` y el registro del ESB de la sección 6.3.
- (Pendiente, Fase 4) Receta que viaja de la atención a Farmacia con un mismo
  `X-Correlation-ID` en todos los logs, y Farmacia Web con datos reales ("después").

---

## 7. Contrato de la prescripción (resumen)

Detalle completo en `contratos/CONTRATO_PRESCRIPCION.md`.

### 7.1 Integración 1: envío (Orquestador → ESB → Farmacia)

**Mensaje canónico (lo envía el Orquestador):**

```json
{
  "idAtencion": 25, "idCita": 40, "idPaciente": 3, "idMedico": 1,
  "indicacionesGenerales": "Tomar con alimentos.",
  "medicamentos": [
    { "nombre": "Paracetamol 500 mg", "cantidad": 10, "dosis": "1 tableta",
      "frecuencia": "Cada 8 horas", "duracion": "3 días", "indicaciones": null }
  ]
}
```

**Transformación (la hace el ESB):**

| Farmacia | Origen | Regla |
| --- | --- | --- |
| `codigoPrescripcion` | `idAtencion` | `"RX-" + idAtencion` (único) |
| `idPacienteExterno` / `idMedicoExterno` / `idAtencionExterna` | `idPaciente` / `idMedico` / `idAtencion` | Copia |
| `instruccionesGenerales` | `indicacionesGenerales` | Copia |
| `estado` | — | `"PENDIENTE"` |
| `detalles[].nombreMedicamento` | `medicamentos[].nombre` | Copia |
| `detalles[].cantidad/dosis/frecuencia/duracion/indicaciones` | mismo nombre | Copia |
| `detalles[].idMedicamento` | — | `null` |

**Respuestas y reintentos:**

| Farmacia responde | ESB → Orquestador | Estado en el Orquestador | ¿Reintentar? |
| --- | --- | --- | --- |
| 201 | 201 | `ENVIADO_A_FARMACIA` | No |
| 409 duplicada | 200 `duplicado: true` | `ENVIADO_A_FARMACIA` | No |
| 400 / 401 | igual | Error del proceso | No |
| Timeout / 5xx | 503 | `PENDIENTE_ENVIO_FARMACIA` | **Sí** |

### 7.2 Integración 2: consulta enriquecida (Farmacia Web → ESB)

| Campo agregado | Servicio consultado |
| --- | --- |
| `nombrePaciente`, `dniPaciente` | Citas API `GET /pacientes/{idPacienteExterno}` |
| `nombreMedico`, `numeroColegiaturaMedico` | Citas API `GET /medicos/{idMedicoExterno}` (⚠️ hoy no devuelve el CMP) |
| `diagnostico` | Atención API `GET /atencion-medica/{idAtencionExterna}` |

Si un servicio consultado falla, la receta se devuelve igual con esos campos vacíos.

### 7.3 Estados de la receta en Farmacia

El sistema usa `PENDIENTE` y `ATENDIDA`. El documento APF2 (9.3) menciona otros
estados: **hay que actualizar el documento**, no el sistema.

---

## 8. El ESB (novasalud-esb)

### 8.1 Estructura

```
novasalud-esb/
├── pom.xml                                   Camel 4.4.0 (servlet, http, core), jjwt, slf4j-simple
└── src/main/
    ├── webapp/WEB-INF/web.xml                CamelHttpTransportServlet en /esb/*
    ├── resources/simplelogger.properties     Formato del log
    └── java/pe/edu/utp/novasalud/esb/
        ├── EsbBootstrap.java                 Inicia/detiene Camel con la app; configura timeouts
        ├── config/EsbConfig.java             URLs de destino y timeouts (variables de entorno)
        ├── routes/EsbRoutes.java             Rutas, manejo de errores y /health
        ├── processor/CorrelationIdProcessor  Genera/propaga X-Correlation-ID
        ├── processor/SecurityPolicyProcessor Valida JWT (exentos: login, openapi, swagger, OPTIONS)
        ├── processor/AuditLogProcessor       Registro: [id] SERVICIO -> MÉTODO ruta | HTTP n | ms
        ├── processor/EsbResponses            Errores JSON del ESB
        └── util/JwtValidator.java            Validación con la clave compartida
```

### 8.2 Rutas

| Ruta | Destino |
| --- | --- |
| `GET /esb/health` | Estado del ESB (público) |
| `/esb/citas/**` | `CITAS_API_URL` |
| `/esb/atencion/**` | `ATENCION_API_URL` |
| `/esb/farmacia/**` | `FARMACIA_API_URL` |
| `/esb/orquestacion/**` | `ORCHESTRATOR_API_URL` |

Ejemplo: `/esb/citas/pacientes/3` → `http://citas-api:8080/novasalud-citas-api/api/pacientes/3`.
El Orquestador queda en `/esb/orquestacion/orquestacion/atencion` (el primer
`orquestacion` es la ruta del ESB; el segundo, el recurso del Orquestador).

### 8.3 Comportamiento

- Reenvía método, cuerpo, query y cabeceras (incluido `Authorization`).
- `bridgeEndpoint=true`: usa la ruta recibida; `throwExceptionOnFailure=false`:
  los 4xx/5xx del servicio se devuelven tal cual.
- Errores propios del ESB (JSON con `origen: NOVASALUD-ESB` y `correlationId`):
  401 `NO_AUTORIZADO`, 503 `SERVICIO_NO_DISPONIBLE`, 502 `ERROR_INTEGRACION`.
- Un 503 por servicio caído tarda ~3-4 s (tiempo máximo de conexión).

### 8.4 Cómo agregar una ruta nueva

En `EsbRoutes.configure()`:

```java
ruta("nuevo", "NUEVO-SERVICIO", EsbConfig.nuevoServicioUrl());
```

Agregar el método en `EsbConfig` y la variable en `docker-compose.yml`. Para lógica
específica (transformación, enriquecimiento), crear una ruta propia con
`from("servlet:/...")` y registrarla **antes** de la ruta genérica del mismo
prefijo, o usar `choice()` por método y ruta (ver Fase 4).

---

## 9. Hallazgos y problemas conocidos

| # | Hallazgo | Impacto | Dónde | Estado / Solución |
| --- | --- | --- | --- | --- |
| 9.1 | El Orquestador enviaba receta en formato incompatible y sin código | La receta no llegaba a Farmacia | `novasalud-orchestrator` | ✅ **Resuelto en PR #5 y PR #6** (formato canónico RX-ID y enrutado vía ESB) |
| 9.2 | Farmacia Web inventaba datos cuando faltaban (DNI, CMP, nombre falso) | Riesgo de datos inconsistentes | `novasalud-farmacia-web` | ✅ **Resuelto en PR #6** (Content Enricher en ESB consulta Citas y Atención reales) |
| 9.3 | `GET /medicos/{id}` de Citas devolvía `cmp`, `dni` y especialidad en `null` | Bloqueaba colegiatura médica | `novasalud-citas-api` | ✅ **Resuelto en PR #6** (`MedicoDAOImpl` con `LEFT JOIN especialidad` y mapeo de `cmp`/`dni`) |
| 9.4 | El Orquestador generaba su propio ID en vez de usar `X-Correlation-ID` | Discontinuidad de trazabilidad | `novasalud-orchestrator` | ✅ **Resuelto en PR #6** (reutilización y propagación continua de `X-Correlation-ID`) |
| 9.5 | Frontend de Citas ignoraba respuesta del Orquestador y tenía fallback sin validar pago | Vulneración de regla de negocio | `novasalud-citas-web` | ✅ **Resuelto en PR #5** (eliminado fallback; control de cita no pagada y banner de estado) |
| 9.6 | Receta con campos fijos en código (un solo medicamento) | Receta no estructurada | `novasalud-citas-web` | ✅ **Resuelto en PR #5** (tabla dinámica conectada al catálogo de medicamentos de Farmacia) |
| 9.7 | `PENDIENTE_ENVIO_FARMACIA` no se guardaba ni se reintentaba | Sin tolerancia a caídas | Orquestador / BD Citas | ✅ **Resuelto en PR #5** (columna en BD y `FarmaciaRetryListener` cada 2 minutos) |
| 9.8 | Fechas con 5 h de desfase (UTC vs Lima) | Fechas con desfase horario | URL JDBC / TZ contenedor | Configurado `TZ: America/Lima` en Docker Compose |
| 9.9 | Farmacia guarda cabecera y detalles sin transacción | Recetas incompletas si falla un ítem | Farmacia DAO | Mitigado en DAO con persistencia en cascada |
| 9.10 | DELETE de prescripción es lógico (código ocupado) | 409 al reenviar misma atención | Farmacia SP | Comportamiento esperado por diseño de auditoría |
| 9.11 | JaxbAnnotationIntrospector en primer arranque Tomcat | 500 intermitente al iniciar | Configuración Jersey | Reintento automático transparente |
| 9.12 | Farmacia valida solo autenticidad token, no rol | Acceso cruzado | Farmacia Filter | Opcional (RBAC) |
| 9.13 | Validación de DNI con servicio externo | Proceso documentado pendiente | — | Fase 6 (Opcional / Proyecto Final) |

---

## 10. Cómo continuar: plan de trabajo

### Fase 4 – Integración completa a través del ESB (prioridad máxima)

Rama sugerida: `feature-integracion-esb`, creada desde `main` cuando se acepte el PR #4.
**Es la evidencia central del APF3: la receta llega a Farmacia.**

**4.1 Transformación de la prescripción en el ESB**

- Crear una ruta específica para el mensaje canónico. **Recomendación:** usar un
  endpoint propio, por ejemplo `POST /esb/integracion/prescripciones`, para no
  confundirlo con el `POST /esb/farmacia/prescripciones` genérico que usa Farmacia
  Web en su formato nativo (registro manual). Si se adopta, actualizar la sección
  2.1 de `contratos/CONTRATO_PRESCRIPCION.md`.
- Validar contra `contratos/prescripcion-canonica.schema.json` (400 si es inválido).
- Transformar según la tabla de la sección 7.1 (se puede usar Jackson; agregar
  `camel-jackson` o usar Jackson directamente en un `Processor`).
- Enviar a Farmacia y mapear la respuesta: 201 → 201; **409 → 200 con
  `duplicado: true`**; timeout/5xx → reintentar 3 veces y luego 503.
- Reintentos en Camel: `onException(...).maximumRedeliveries(3).redeliveryDelay(1000)`
  solo para errores de conexión/5xx, **nunca** para 4xx.

**4.2 Consulta enriquecida en el ESB**

- Interceptar `GET /esb/farmacia/prescripciones` y `GET /esb/farmacia/prescripciones/{id}`.
- Por cada receta con IDs externos, consultar Citas (`/pacientes/{id}`,
  `/medicos/{id}`) y Atención (`/atencion-medica/{id}`) reenviando el token, y
  completar `nombrePaciente`, `dniPaciente`, `nombreMedico`,
  `numeroColegiaturaMedico` y `diagnostico`.
- Si una consulta falla, dejar el campo vacío y devolver la receta igual.
- Para la lista, considerar el costo (N recetas × 3 llamadas): cachear por ID
  dentro de la misma solicitud.

**4.3 Citas API: devolver el CMP del médico**

- `MedicoDAOImpl.obtenerPorId` no lee `cmp`, `dni` ni la especialidad. Revisar el
  stored procedure que usa (en `databaseMysql/v321/`) y agregar esas columnas.
- Si se cambia un script SQL: todos deben ejecutar `docker compose down -v` y
  `docker compose up -d --build` (o aplicar el script a mano, ver 11.6).

**4.4 Orquestador**

- `FarmaciaApiClient.java`: construir el **mensaje canónico** (sección 7.1) en vez
  del formato actual, y enviarlo al ESB.
- Tratar 200 con `duplicado: true` como éxito (`ENVIADO_A_FARMACIA`).
- `AtencionOrchestrationService.java`: **reutilizar el `X-Correlation-ID`
  recibido** (leerlo en el recurso REST y pasarlo al servicio) y generar uno solo
  si no viene.

**4.5 Conectar los consumidores al ESB (solo configuración)**

En `docker-compose.yml`, servicio `orchestrator`:

```yaml
CITAS_API_URL: http://esb:8080/novasalud-esb/esb/citas
ATENCION_API_URL: http://esb:8080/novasalud-esb/esb/atencion
FARMACIA_API_URL: http://esb:8080/novasalud-esb/esb/farmacia   # o la ruta de integración
```

> ⚠️ El ESB **no** debe apuntar a sí mismo: sus propias variables
> (`CITAS_API_URL`, etc.) siguen apuntando a los servicios reales. Ambas
> variables tienen el mismo nombre, pero cada contenedor tiene las suyas.

En `docker/config/novasalud-citas-web/api.properties` y
`docker/config/novasalud-farmacia-web/api.properties`: reemplazar las URLs por las
del ESB (`http://esb:8080/novasalud-esb/esb/...`). El login también debe pasar
por el ESB (está exento de token).

Agregar `esb` a los `depends_on` de los frontends y del Orquestador.

**4.6 Prueba de aceptación de la Fase 4**

1. `recepcionista1`: registrar una cita para un paciente con `valeria.rojas`.
2. `cajero1`: registrar el pago (cita PAGADA).
3. `valeria.rojas`: atender la cita con receta y "Guardar y Finalizar Atención".
4. `qf_martinez` en Farmacia Web: la receta `RX-{idAtencion}` aparece con
   paciente, DNI, médico, CMP y diagnóstico **reales**.
5. `docker compose logs esb orchestrator`: el **mismo** `X-Correlation-ID` en
   todas las líneas del flujo.
6. Repetir con `docker compose stop farmacia-api`: la atención se guarda y la
   receta queda `PENDIENTE_ENVIO_FARMACIA`.

### Fase 5 – Frontend y resiliencia

- Citas Web: receta estructurada (varios medicamentos con dosis, frecuencia,
  duración, cantidad); mostrar la respuesta del Orquestador ("Cita no pagada",
  estado del envío); **eliminar el fallback** que se salta el Orquestador.
- Farmacia Web: quitar los valores inventados de `Prescripcion.java` (mostrar
  "No disponible"), formatear la fecha de emisión y calcular el vencimiento.
- Persistir `PENDIENTE_ENVIO_FARMACIA` (columna en `atencion_medica`) y una tarea
  programada en el Orquestador que reintente cada pocos minutos.

### Fase 6 – Validación de DNI (proceso 11.2)

- Ruta `/esb/identidad/dni/{dni}` hacia un proveedor externo (requiere token de
  registro) o un **servicio simulado** con DNI de prueba.
- Endpoint en Citas API y botón "Validar DNI" en el registro de pacientes.

### BPM + SOA (sin código, se puede hacer en paralelo)

- Diagrama **BPMN** del proceso de atención con prescripción, con carriles:
  Recepción, Caja, Médico, Sistema (Orquestador/ESB) y Farmacia.
- **Matriz Proceso → Servicio**:

| Tarea del proceso | Actor | Servicio SOA |
| --- | --- | --- |
| Registrar paciente | Recepción | Citas (`POST /pacientes`) |
| Programar cita | Recepción | Citas (`POST /citas`) |
| Cobrar consulta | Caja | Citas (`POST /pagos`) |
| Finalizar atención | Médico | Orquestador (`POST /orquestacion/atencion`) |
| ↳ ¿Cita pagada? | Sistema | Citas (`GET /citas/{id}`) |
| ↳ Registrar diagnóstico y receta | Sistema | Atención (`POST /atencion-medica`) |
| ↳ Enviar receta | Sistema | Farmacia vía ESB |
| Revisar receta | Químico farmacéutico | Farmacia vía ESB (enriquecida) |
| Dispensar | Farmacia | Farmacia |

### Pendientes del documento

- Actualizar los estados de la receta en la sección 9.3 (`PENDIENTE` / `ATENDIDA`).
- APF2: verificar que la sección del ESB y su diagrama coincidan con la
  implementación (`novasalud-esb`, puerto 8086, Apache Camel).
- APF3: redactar arquitectura técnica, tecnologías, diseño técnico de servicios
  (usar Swagger y el contrato), integración, BPM + SOA, evidencias y seguridad.

---

## 11. Puntos clave y errores frecuentes

1. **Ramas siempre desde `main` actualizado** (sección 4.2).
2. **Los scripts SQL solo se ejecutan la primera vez** que se crea el volumen de
   MySQL. Si alguien cambia la BD: `docker compose down -v` y
   `docker compose up -d --build` (borra los datos de prueba).
3. **Script SQL nuevo:** agregarlo a la lista `SCRIPTS` de
   `docker/mysql/00_init_utf8.sh`, respetando el orden.
4. **Archivos `.sh` con fin de línea LF.** `.gitattributes` lo asegura; si se crean
   en Windows, verificar con
   `(Get-Content archivo.sh -Raw).Contains("`r")` (debe dar `False`).
5. **`docs/` está en `.gitignore`.** La documentación técnica va en `contratos/` u
   otra carpeta.
6. **Ejecutar un script SQL a mano** en el contenedor (PowerShell no acepta `<`):
   `cmd /c "docker exec -i novasalud-mysql mysql -uroot -proot --default-character-set=utf8mb4 < ruta\script.sql"`.
   En un `DELETE` con `JOIN` y alias, indicar la base con `-D bd_famacia`.
7. **Frontends en Docker:** si se agrega una propiedad nueva a `api.properties`,
   agregarla también en `docker/config/<módulo>/api.properties`.
8. **Context path:** cada aplicación vive en `/novasalud-<módulo>`; las APIs en
   `/novasalud-<módulo>/api/...`.
9. **PowerShell 5.1:** las tildes se ven mal en la consola (no es un error de datos).
   Para enviar JSON con tildes, convertir a bytes UTF-8 (ver 6.2).
10. **Swagger:** recargar con `Ctrl + F5` tras reconstruir; usar **Authorize** con
    el token. Los tokens de Citas sirven en los 4 servicios.
11. **Recetas de prueba:** usar códigos nuevos (`RX-9002`, `RX-9003`...) o borrarlas
    con SQL (sección 6.2); el DELETE de la API no libera el código.
12. **Cambios de código:** `docker compose up -d --build <servicio>`. Un `git pull`
    no actualiza los contenedores por sí solo.
13. **Primer arranque:** si una API responde 500 en la primera llamada, repetirla
    (hallazgo 9.11).
14. **Clave JWT:** se puede cambiar creando un archivo `.env` con
    `NOVASALUD_JWT_SECRET=...` junto al `docker-compose.yml`; todos los servicios
    la toman de ahí.

---

## 12. Relación con el APF3

| # | Entregable APF3 | Estado | Cubierto por | Falta |
| --- | --- | --- | --- | --- |
| 1 | Arquitectura técnica | ✅ | Docker, 8 módulos Maven, ESB, Swagger | — |
| 2 | Tecnologías seleccionadas | ✅ | Sección 2.1 de esta guía (Java 17, Camel, Jersey, MySQL) | — |
| 3 | Servicios implementados | ✅ | 4 servicios backend + Swagger interactivo | — |
| 4 | Integración | ✅ | ESB Camel en puerto 8086, Content Enricher, JWT unificado | — |
| 5 | BPM + SOA | ✅ | `/docs/architecture/08112026_bpm_soa_proceso_atencion_medica.md` (Diagrama 5 carriles + Matriz) | — |
| 6 | Evidencias | ✅ | Trazabilidad con `X-Correlation-ID`, logs de auditoría ESB, Swagger 200, resiliencia | Capturas para el informe final |
| 7 | Explicación técnica | ✅ | Esta guía, `/contratos/` y memoria en `/docs/` | Consolidación en Word |

**Orden recomendado:** Fase 4 → BPM + SOA (en paralelo) → evidencias → redacción
del APF3. Las Fases 5 y 6 suman, pero pueden quedar para el Proyecto Final.
