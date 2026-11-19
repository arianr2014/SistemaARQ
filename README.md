# Sistema Clínico y Farmacéutico NovaSalud - V3.2.1

## 1. Descripción General

El **Sistema NovaSalud V3.2.1** es una solución empresarial de salud desarrollada en **Java 17 / Jakarta EE 10** bajo una **Arquitectura Orientada a Servicios (SOA) Desacoplada**.

El sistema distribuye sus capacidades de negocio en microservicios independientes y coordina el flujo clínico transaccional (Cita $\rightarrow$ Consulta Médica $\rightarrow$ Prescripción $\rightarrow$ Farmacia) mediante un servicio **Orquestador REST SOA** (`novasalud-orchestrator`), aplicando el patrón **Database-per-Service** con dos bases de datos MySQL aisladas (`bd_citasmedicas_v321` y `bd_famacia`).

---

## 2. Tecnologías Utilizadas

- **Lenguaje / JDK:** Java 17
- **Plataforma Web:** Jakarta EE 10 (Servlet API 6.0, JAX-RS / Jakarta REST 3.1)
- **Vistas Web:** JSP 3.0 / JSTL 3.0 / Bootstrap 5.3 / Bootstrap Icons
- **Seguridad & Tokens:** JSON Web Tokens (JJWT 0.11.5), BCrypt (`jbcrypt 0.4`), Tokens anti-CSRF, Control de Sesión Remoto (`session_version`), Filtros CORS
- **Pool de Conexiones JDBC:** HikariCP 5.0.1
- **Persistencia & BD:** JDBC nativo con Stored Procedures, MySQL Connector/J 8.0.33, MySQL 8.0
- **Formato de Comunicación Inter-Servicios:** JSON (`com.google.code.gson:gson:2.10.1`)
- **Cliente HTTP REST:** `java.net.http.HttpClient` (Nativo Java 11+)
- **Documentación de APIs:** OpenAPI 3.0 / Swagger UI v5
- **Gestor de Construcción:** Apache Maven Multi-Módulo
- **Servidor de Aplicaciones:** Apache Tomcat 10.1+ (compatible con Jakarta EE 10)

---

## 3. Arquitectura del Proyecto (Módulos Maven)

El proyecto está administrado por un POM Padre (`novasalud-parent`) compuesto por **6 módulos independientes**:

```text
SistemaARQ/
├── pom.xml                             # POM Padre Multi-Módulo Maven
├── databaseMysql/                      # Scripts SQL Oficiales de las Bases de Datos
│   ├── v321/                           # Scripts de bd_citasmedicas_v321
│   ├── Farmacia/                       # Scripts de bd_famacia
│   └── PASOS_DESPLIEGUE_BASES_DATOS.md # Guía unificada de despliegue de BD
├── novasalud-citas-web/                # Frontend Web MVC para Citas y Atención Clínica (JSP/Servlets)
├── novasalud-citas-api/                # Microservicio REST para Pacientes, Médicos, Citas y Pagos
├── novasalud-atencion-api/             # Microservicio REST para Registro Clínico e Historias Clínicas
├── novasalud-orchestrator/             # Servicio Orquestador REST SOA (Coordinación transaccional)
├── novasalud-farmacia-api/             # Microservicio REST para Prescripciones, Inventario y Medicamentos
└── novasalud-farmacia-web/             # Frontend Web MVC para Dispensación y Punto de Venta
```

### Topología y Puertos de Comunicación:

| Módulo | Tipo | Puerto por Defecto | Context Path Base | Base de Datos Asociada |
| :--- | :---: | :---: | :--- | :--- |
| **`novasalud-citas-web`** | Frontend MVC | `:8080` | `/novasalud-citas-web` | Consume APIs REST |
| **`novasalud-citas-api`** | Microservicio REST | `:8080` | `/novasalud-citas-api/api` | `bd_citasmedicas_v321` |
| **`novasalud-atencion-api`** | Microservicio REST | `:8082` | `/novasalud-atencion-api/api` | `bd_citasmedicas_v321` |
| **`novasalud-orchestrator`** | Orquestador SOA | `:8083` | `/novasalud-orchestrator/api` | *Stateless (Sin BD)* |
| **`novasalud-farmacia-api`** | Microservicio REST | `:8081` | `/novasalud-farmacia-api/api` | `bd_famacia` |
| **`novasalud-farmacia-web`** | Frontend MVC | `:8081` | `/novasalud-farmacia-web` | Consume APIs REST |

---

## 4. Flujo de Orquestación SOA (`novasalud-orchestrator`)

Al atender una consulta médica, el frontend invoca al Orquestador (`POST /api/orquestacion/atencion`), el cual ejecuta el siguiente flujo coordinado:

1. **Generación de Trazabilidad:** Genera un encabezado `X-Correlation-ID` que se propaga a todos los microservicios.
2. **Paso 1 (Validar Cita y Pago):** Llama a `novasalud-citas-api` (`GET /api/citas/{idCita}`). Valida que la cita exista y su estado de pago sea estrictamente `PAGADO`.
3. **Paso 2 (Registrar Atención):** Llama a `novasalud-atencion-api` (`POST /api/atencion-medica`) para persistir el acto médico (diagnóstico CIE-10, síntomas, receta).
4. **Paso 3 (Transferir Prescripción con Resiliencia):** Si la atención incluye receta, transfiere los medicamentos a `novasalud-farmacia-api` (`POST /api/farmacia/prescripciones`). Si Farmacia no responde o presenta timeout, aplica fallback asignando el estado `PENDIENTE_ENVIO_FARMACIA` sin abortar la atención clínica.

---

## 5. Bases de Datos (Database-per-Service)

El sistema requiere dos bases de datos MySQL independientes:

1. **`bd_citasmedicas_v321`** (Dominio de Citas y Atención Clínica):
   * Usuario: `usuario_citas`
   * Contraseña: `ISO/IEC27001`
   * URL JDBC: `jdbc:mysql://localhost:3306/bd_citasmedicas_v321?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
2. **`bd_famacia`** (Dominio de Farmacia y Medicamentos):
   * Usuario: `usuario_farmacia`
   * Contraseña: `Farmacia2026*`
   * URL JDBC: `jdbc:mysql://localhost:3306/bd_famacia?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`

> Para consultar la guía detallada de ejecución y orden de scripts SQL, revisa el archivo:  
> 👉 [databaseMysql/PASOS_DESPLIEGUE_BASES_DATOS.md](databaseMysql/PASOS_DESPLIEGUE_BASES_DATOS.md)

---

## 6. Archivos de Configuración por Proyecto (`api.properties` y `db.properties`)

Cada módulo gestiona sus conexiones a base de datos y endpoints de servicios externos de forma desacoplada en su carpeta `src/main/resources/`:

### 6.1. `novasalud-citas-web` (`src/main/resources/api.properties`)
Configura las URLs base de los microservicios que consume el cliente HTTP (`ApiClient.java`) del frontend de citas:
```properties
# URL Base para Citas, Pacientes, Médicos, Horarios, Especialidades, Tarifas
citas.api.url=http://localhost:8080/novasalud-citas-api/api

# URL Base para Atención Médica e Historias Clínicas
atencion.api.url=http://localhost:8082/novasalud-atencion-api/api

# URL Base para Orquestador SOA
orchestrator.api.url=http://localhost:8083/novasalud-orchestrator/api

# Fallback Legacy
api.base.url=http://localhost:8080/novasalud-citas-api/api
```

---

### 6.2. `novasalud-citas-api` (`src/main/resources/db.properties`)
Configura la conexión a la base de datos de Citas, el pool HikariCP y la firma de tokens JWT:
```properties
# Configuración de Base de Datos - NovaSalud Backend API
db.url=jdbc:mysql://localhost:3306/bd_citasmedicas_v321?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=usuario_citas
db.password=ISO/IEC27001
db.driver=com.mysql.cj.jdbc.Driver

# Parámetros de Configuración del Pool HikariCP
db.pool.maximumPoolSize=10
db.pool.minimumIdle=2
db.pool.idleTimeout=30000
db.pool.connectionTimeout=10000
db.pool.maxLifetime=1800000

# Clave Secreta para Firma de Tokens JWT (256-bit HMAC Secret)
jwt.secret=NovaSaludSystemSecretKeyHMAC256BitsLongForSecureJwtTokenSigning2026UTP
```

---

### 6.3. `novasalud-atencion-api` (`src/main/resources/db.properties`)
Configura la conexión a la base de datos para atención clínica, pool HikariCP y validación JWT:
```properties
# Configuración de Base de Datos - NovaSalud Atención API
db.url=jdbc:mysql://localhost:3306/bd_citasmedicas_v321?useSSL=false&allowPublicKeyRetrieval=true&noAccessToProcedureBodies=true&serverTimezone=UTC
db.user=usuario_citas
db.password=ISO/IEC27001
db.driver=com.mysql.cj.jdbc.Driver

# Parámetros de Configuración del Pool HikariCP
db.pool.maximumPoolSize=10
db.pool.minimumIdle=2
db.pool.idleTimeout=30000
db.pool.connectionTimeout=10000
db.pool.maxLifetime=1800000

# Clave Secreta para Firma de Tokens JWT (256-bit HMAC Secret)
jwt.secret=NovaSaludSystemSecretKeyHMAC256BitsLongForSecureJwtTokenSigning2026UTP
```

---

### 6.4. `novasalud-orchestrator` (`src/main/resources/orchestrator.properties`)
Configura los endpoints de los 3 microservicios coordinados por el orquestador stateless:
```properties
# Configuración de URLs de Microservicios Backend para NovaSalud Orchestrator
citas.api.url=http://localhost:8080/novasalud-citas-api/api
atencion.api.url=http://localhost:8082/novasalud-atencion-api/api
farmacia.api.url=http://localhost:8081/novasalud-farmacia-api/api

# Clave Secreta JWT compartida
jwt.secret=NovaSaludSystemSecretKeyHMAC256BitsLongForSecureJwtTokenSigning2026UTP
```

---

### 6.5. `novasalud-farmacia-api` (`src/main/resources/db.properties`)
Configura la base de datos aislada de farmacia (`bd_famacia`), pool HikariCP y clave JWT propia:
```properties
# Configuración de Base de Datos - NovaSalud Farmacia Backend API
db.url=jdbc:mysql://localhost:3306/bd_famacia?useSSL=false&allowPublicKeyRetrieval=true&noAccessToProcedureBodies=true&serverTimezone=UTC
db.user=usuario_farmacia
db.password=Farmacia2026*
db.driver=com.mysql.cj.jdbc.Driver

# Parámetros de Configuración del Pool HikariCP
db.pool.maximumPoolSize=10
db.pool.minimumIdle=2
db.pool.idleTimeout=30000
db.pool.connectionTimeout=10000
db.pool.maxLifetime=1800000

# Clave Secreta para Firma de Tokens JWT (256-bit HMAC Secret)
jwt.secret=NovaSaludFarmaciaSystemSecretKeyHMAC256BitsLongForSecureJwtTokenSigning2026UTP
```

---

### 6.6. `novasalud-farmacia-web` (`src/main/resources/api.properties`)
Configura la ruta de la API REST de Farmacia consumida por la interfaz web de farmacia:
```properties
# URL Base de la API Backend de Farmacia (novasalud-farmacia-api)
api.base.url=http://localhost:8080/novasalud-farmacia-api/api
```

---

## 7. Credenciales de Prueba del Sistema

### Usuarios de Acceso Web:

**Sistema de Citas** (http://localhost:8080/novasalud-citas-web) – contraseña `123456`:
- **Administrador:** `administrador` (Rol: `ADMIN`)
- **Recepcionistas:** `recepcionista1`, `recepcionista2` (Rol: `RECEPCIONISTA`)
- **Cajeros:** `cajero1`, `cajero2` (Rol: `CAJERO`)
- **Dirección:** `direccion` (Rol: `DIRECCION`)
- **Médicos:** `valeria.rojas`, `luis.herrera`, `camila.torres`, `marco.salas`, `ana.beltran`, `diego.munoz`, entre otros (Rol: `DOCTOR`)

> El usuario `doctor` existe en la base de datos pero está **INACTIVO**; para pruebas use un médico activo, por ejemplo `valeria.rojas`.

**Sistema de Farmacia** (http://localhost:8081/novasalud-farmacia-web) – contraseña `Farmacia2026*`:
- **Administrador:** `admin` (Rol: `ADMINISTRADOR`)
- **Químico Farmacéutico:** `qf_martinez` (Rol: `QUIMICO_FARMACEUTICO`)
- **Técnico de Farmacia:** `cajero01` (Rol: `TECNICO_FARMACIA`)

---

## 8. Compilación, Instalación y Puesta en Marcha

### 8.1. Requisitos Previos
* **Java Development Kit (JDK):** Versión 17 o superior (`java -version`).
* **Apache Maven:** Versión 3.8 o superior (`mvn -version`).
* **MySQL Server:** Versión 8.0 o superior en puerto 3306.
* **Apache Tomcat:** Versión 10.1+ (con soporte para Jakarta EE 10 / Servlet 6.0).

---

### 8.2. Paso 1: Despliegue de Bases de Datos en MySQL

Ejecutar los scripts oficiales en el orden establecido:

```bash
# 1. Base de Datos Citas Médicas
mysql -u root -p < "databaseMysql/v321/00_instalacion_completa_bd_citasmedicas_v321.sql"
mysql -u usuario_citas -pISO/IEC27001 < "databaseMysql/v321/01_stored_procedures_completos_v321.sql"

# 2. Base de Datos Farmacia
mysql -u root -p < "databaseMysql/Farmacia/00_bd_farmacia_v1.sql"
mysql -u root -p < "databaseMysql/Farmacia/01_bd_usuario.sql"
mysql -u usuario_farmacia -pFarmacia2026* < "databaseMysql/Farmacia/02_insert_data_farmacia_v1.sql"
mysql -u usuario_farmacia -pFarmacia2026* < "databaseMysql/Farmacia/03_bd_farmacia_sp_v1.sql"
```

---

### 8.3. Paso 2: Compilación de Todos los Módulos con Maven

Desde la raíz del proyecto (`SistemaARQ`), ejecutar el comando Maven multi-módulo para compilar y empaquetar todos los proyectos en un solo paso:

```bash
mvn clean package -DskipTests
```

Este comando generará los artefactos `.war` en las carpetas `target/` de cada módulo:
* `novasalud-citas-web/target/novasalud-citas-web.war`
* `novasalud-citas-api/target/novasalud-citas-api.war`
* `novasalud-atencion-api/target/novasalud-atencion-api.war`
* `novasalud-orchestrator/target/novasalud-orchestrator.war`
* `novasalud-farmacia-api/target/novasalud-farmacia-api.war`
* `novasalud-farmacia-web/target/novasalud-farmacia-web.war`

*(Si deseas compilar un módulo en específico, simplemente ingresa a su carpeta y ejecuta `mvn clean package -DskipTests`).*

---

### 8.4. Paso 3: Despliegue en Apache Tomcat 10.1+

1. Iniciar Apache Tomcat.
2. Copiar los archivos `.war` generados a la carpeta `webapps/` de tu instalación de Tomcat.
3. Tomcat desplegará automáticamente las aplicaciones en sus respectivos contextos.

---

### 8.5. Paso 4: Acceso y Verificación del Sistema

Una vez desplegados los módulos, puedes acceder a través de tu navegador web:

* **Módulo de Citas y Atención Médica Web:**  
  👉 `http://localhost:8080/novasalud-citas-web/login`
* **Módulo de Farmacia Web:**  
  👉 `http://localhost:8080/novasalud-farmacia-web/login` (o en su puerto configurado)
* **Documentación Interactiva Swagger UI (Citas API):**  
  👉 `http://localhost:8080/novasalud-citas-api/swagger`
* **Endpoint del Orquestador SOA:**  
  👉 `http://localhost:8083/novasalud-orchestrator/api/orquestacion/atencion`


1. Portal de Citas Médicas (novasalud-citas-web)

🔗 URL: http://localhost:8080/novasalud-citas-web/
🔑 Contraseña universal para todos los usuarios: 123456

Rol	Usuario	Contraseña	Uso sugerido
Administrador General	administrador	123456	Gestión completa de catálogos y usuarios.
Médico General	doctor	123456	Atención de consultas y prescripción médica.
Médica Especialista	valeria.rojas	123456	Dra. Valeria Rojas (Medicina General).
Recepcionista (Mañana)	recepcionista1	123456	Agendamiento y registro de pacientes.
Recepcionista (Tarde)	recepcionista2	123456	Agendamiento de citas.
Cajero	cajero1	123456	Cobro de citas médicas en caja.
2. Portal de Farmacia (novasalud-farmacia-web)

🔗 URL: http://localhost:8081/novasalud-farmacia-web/
🔑 Contraseña universal para farmacia: Farmacia2026*

Rol	Usuario	Contraseña	Uso sugerido
admin Farmacia	admin	Farmacia2026*	Gestión de inventario, proveedores y lotes.
Químico Farmacéutico	qf_martinez	Farmacia2026*	Revisión y dispensación de recetas médicas.
Técnico / Cajero	cajero01	Farmacia2026*