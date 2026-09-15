# SVIS — Sistema de Votaciones y Encuestas Institucionales Seguras

Sistema de votación electrónica para el Centro de Formación SENA: un token de
un solo uso por estudiante y por votación, sin que el sistema guarde ninguna
relación entre la identidad de quien vota y la opción elegida.

## Arquitectura

Dos aplicaciones independientes que solo se hablan por HTTP:

```
frontend-php/   PHP puro. Sirve las pantallas y llama al backend.
                NO tiene acceso a MySQL: ni PDO ni mysqli en ningún archivo.
backend-java/   API REST en Servlets (sin framework). El único que conoce
                las credenciales de la base de datos.
MySQL           Una sola base, SvisDb (ver backend-java/.../schema.sql).
```

El frontend nunca puede filtrar credenciales de base de datos porque
sencillamente no tiene código capaz de conectarse a MySQL. Las peticiones de
administración van con una clave compartida (`X-Svis-Key`) que solo conoce el
servidor PHP; el voto en sí (`POST /api/votos/emitir`) es público a propósito,
porque su credencial real es el token OTP.

## Tecnologías

- **Backend:** Java 17, Servlets 4.0 (namespace `javax.*`, requiere **Tomcat 9**
  — Tomcat 10+ usa `jakarta.*` y no lo carga), Gson, HikariCP, MySQL
  Connector/J, jBCrypt. Empaquetado con Maven (`mvn package` genera
  `svis-api.war`).
- **Frontend:** PHP 8 sin frameworks ni Composer. Un único script JS
  (`js/app.js`), progresivo: cada widget se activa por atributos `data-*` y
  tiene una salida funcional sin JavaScript.
- **Base de datos:** MySQL / MariaDB.

## Puesta en marcha

### 1. Base de datos

```sql
-- Crear la base y cargar el esquema + datos semilla:
mysql -u root -p < backend-java/src/main/resources/schema.sql
```

Usuarios de prueba que trae el esquema:

| Correo                 | Clave      | Rol        |
|-------------------------|-----------|------------|
| admin@cimm.edu.co        | admin123  | ADMIN      |
| jhon@cimm.edu.co          | sena2026  | ESTUDIANTE |
| laura@cimm.edu.co         | sena2026  | ESTUDIANTE |
| andres@cimm.edu.co        | sena2026  | ESTUDIANTE |

### 2. Backend (Tomcat 9)

```bash
cp backend-java/src/main/resources/db.properties.example \
   backend-java/src/main/resources/db.properties
```

Edita `db.properties`: credenciales de tu MySQL y **genera tu propia
`api.key`** (por ejemplo `openssl rand -base64 24`). Ese archivo no se sube a
git — cada quien tiene el suyo.

```bash
cd backend-java
mvn clean package
```

Despliega `target/svis-api.war` en la raíz de un **Tomcat 9** (cópialo como
`webapps/ROOT.war`). Verifica con:

```
GET http://localhost:8080/api/salud
```

### 3. Frontend (Apache / XAMPP)

Sirve la carpeta `frontend-php/` con Apache (por ejemplo,
`C:\xampp\htdocs\<tu-carpeta>\frontend-php`).

```bash
cp frontend-php/config.local.php.example frontend-php/config.local.php
```

Edita `config.local.php`:

- `BASE_URL`: vacío si el frontend vive en la raíz del sitio, o algo como
  `/gestion-votacion/frontend-php` si vive en un subdirectorio.
- `API_BASE_URL`: la URL de tu Tomcat (por defecto `http://localhost:8080/api`).
- `API_KEY`: **la misma clave** que pusiste en `api.key` de `db.properties`.

Sin esa clave configurada, el backend bloquea todas las rutas de
administración a propósito (falla cerrado).

## Ciclo de vida de una votación

```
CREADA ──(generar padrón + abrir)──> ACTIVA ──(cerrar)──> CERRADA
```

- El padrón (los tokens) se genera **antes** de abrir la votación — es una
  regla de negocio, no un detalle técnico: si se pudieran emitir tokens con
  la votación ya abierta, el administrador podría agregar votantes a mitad
  del conteo.
- Cada estudiante también puede generar su propio token desde la papeleta,
  una vez que la votación ya está abierta (`POST /api/tokens/generar-propio`),
  sin esperar a que el administrador arme un padrón masivo.
- Un token es de un solo uso. La base de datos solo guarda su hash SHA-256;
  el texto en claro vive únicamente en la respuesta HTTP que lo generó.

## Roles

- **Administrador:** crea votaciones, genera el padrón, abre/cierra, ve
  resultados agregados (nunca quién votó por qué).
- **Estudiante:** vota una sola vez por votación abierta en la que esté
  habilitado, y recibe un comprobante que confirma el voto sin revelar la
  opción elegida.

## Estructura del repositorio

```
backend-java/
  src/main/java/co/edu/sena/cimm/votaciones/
    servlet/     Endpoints REST (uno por recurso)
    service/     Reglas de negocio
    repository/  Acceso a MySQL (JDBC + HikariCP)
    dto/         Lo que entra y sale por HTTP
    model/       Lo que refleja la base de datos
frontend-php/
  admin/         Pantallas del administrador
  includes/      Sesión, cliente HTTP hacia el backend, layout compartido
  *.php          Pantallas del estudiante (login, votar, papeleta, comprobante)
```
