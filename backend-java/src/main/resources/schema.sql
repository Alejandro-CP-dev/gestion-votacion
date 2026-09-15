-- =====================================================================
--  SVIS - Sistema de Votaciones y Encuestas Institucionales Seguras
--  Motor requerido: MySQL 8.0.16 o superior
--
--  Dos decisiones que hay que poder defender en la sustentacion:
--   1) ENGINE=InnoDB explicito en TODAS las tablas. MyISAM no soporta
--      transacciones ni "SELECT ... FOR UPDATE", asi que las Reglas 2 y 3
--      del taller serian imposibles de cumplir.
--   2) El voto NO se guarda como fila. Solo se incrementa un contador en
--      Opcion. Si no existe la fila del voto, no hay nada que rastrear:
--      el anonimato queda garantizado por el diseno, no por una promesa.
--
--  Ejecutar:  mysql -u root -p < schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS SvisDb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE SvisDb;

-- Borrado en orden inverso a las llaves foraneas
DROP TABLE IF EXISTS Comprobante;
DROP TABLE IF EXISTS TokenOtp;
DROP TABLE IF EXISTS Opcion;
DROP TABLE IF EXISTS Encuesta;
DROP TABLE IF EXISTS Usuario;


-- =========================================================
-- 1. USUARIO  (padron institucional)
-- =========================================================
CREATE TABLE Usuario (
    Id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    Nombre        VARCHAR(100) NOT NULL,
    Apellido      VARCHAR(100) NOT NULL,
    Correo        VARCHAR(150) NOT NULL,
    Telefono      VARCHAR(20)  NULL,
    -- Hash BCrypt ($2a$...). Nunca la clave en texto plano.
    Clave         VARCHAR(255) NOT NULL,
    Rol           ENUM('ADMIN', 'ESTUDIANTE') NOT NULL DEFAULT 'ESTUDIANTE',
    Estado        ENUM('ACTIVO', 'INACTIVO')  NOT NULL DEFAULT 'ACTIVO',
    CreadoEn      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ActualizadoEn TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT UqUsuarioCorreo UNIQUE (Correo)
) ENGINE=InnoDB;


-- =========================================================
-- 2. ENCUESTA / VOTACION
-- =========================================================
CREATE TABLE Encuesta (
    Id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    Titulo        VARCHAR(200) NOT NULL,
    Descripcion   TEXT NULL,
    Estado        ENUM('CREADA', 'ACTIVA', 'CERRADA') NOT NULL DEFAULT 'CREADA',
    CreadoPor     INT UNSIGNED NOT NULL,
    CreadoEn      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ActualizadoEn TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT FkEncuestaCreadoPor
        FOREIGN KEY (CreadoPor) REFERENCES Usuario(Id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    INDEX IxEncuestaEstado (Estado)
) ENGINE=InnoDB;


-- =========================================================
-- 3. OPCION  (REGLA 4: aqui NO hay UsuarioId ni TokenId)
-- =========================================================
CREATE TABLE Opcion (
    Id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    EncuestaId  INT UNSIGNED NOT NULL,
    TextoOpcion VARCHAR(255) NOT NULL,
    Orden       INT UNSIGNED NOT NULL DEFAULT 1,
    -- Unico registro del voto en toda la base de datos: un numero.
    ConteoVotos BIGINT UNSIGNED NOT NULL DEFAULT 0,
    CreadoEn    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT FkOpcionEncuesta
        FOREIGN KEY (EncuestaId) REFERENCES Encuesta(Id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT ChkOpcionOrden CHECK (Orden > 0),

    CONSTRAINT UqOpcionEncuestaOrden UNIQUE (EncuestaId, Orden),

    INDEX IxOpcionEncuesta (EncuestaId)
) ENGINE=InnoDB;


-- =========================================================
-- 4. TOKEN OTP  (padron de credenciales de votacion)
-- =========================================================
CREATE TABLE TokenOtp (
    Id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    EncuestaId      INT UNSIGNED NOT NULL,
    UsuarioId       INT UNSIGNED NOT NULL,
    -- SHA-256 en hexadecimal del token. El token en claro se le entrega
    -- al estudiante una sola vez y jamas se persiste.
    TokenHash       CHAR(64) NOT NULL,
    Estado          ENUM('DISPONIBLE', 'USADO') NOT NULL DEFAULT 'DISPONIBLE',
    FechaExpiracion DATETIME NOT NULL,
    UsadoEn         DATETIME NULL,
    CreadoEn        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT FkTokenOtpEncuesta
        FOREIGN KEY (EncuestaId) REFERENCES Encuesta(Id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT FkTokenOtpUsuario
        FOREIGN KEY (UsuarioId) REFERENCES Usuario(Id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    -- REGLA 1: un unico token por usuario y encuesta.
    CONSTRAINT UqTokenOtpEncuestaUsuario UNIQUE (EncuestaId, UsuarioId),

    -- Indice unico sobre el hash: es la columna por la que se busca y se
    -- bloquea la fila al votar. Sin este indice, "FOR UPDATE" escalaria a
    -- bloqueo de tabla y la Regla 3 se cumpliria por accidente, no por diseno.
    CONSTRAINT UqTokenOtpTokenHash UNIQUE (TokenHash),

    INDEX IxTokenOtpEncuestaEstado (EncuestaId, Estado)
) ENGINE=InnoDB;


-- =========================================================
-- 5. COMPROBANTE  (recibo anonimo del votante)
--    Ojo: no guarda UsuarioId, ni TokenId, ni OpcionId.
--    El codigo es SHA-256 de un UUID aleatorio generado en el momento.
--    NO se usa el hash del token como recibo: eso permitiria cruzar
--    Comprobante con TokenOtp y saber quien recibio cual recibo.
-- =========================================================
CREATE TABLE Comprobante (
    Id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    EncuestaId   INT UNSIGNED NOT NULL,
    CodigoRecibo CHAR(64) NOT NULL,
    CreadoEn     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT FkComprobanteEncuesta
        FOREIGN KEY (EncuestaId) REFERENCES Encuesta(Id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT UqComprobanteCodigo UNIQUE (CodigoRecibo)
) ENGINE=InnoDB;


-- =====================================================================
--  Datos semilla
--  Clave de ambos usuarios: admin123 / sena2026  (hash BCrypt $2a$)
-- =====================================================================
INSERT INTO Usuario (Nombre, Apellido, Correo, Clave, Rol) VALUES
('Osman',  'Aranguren', 'admin@cimm.edu.co',
 '$2a$10$M.dbPC/n6SIC2Ty.xGIcl.gq2tCH0QupqC1SszbO25s4fbxDhaPZS', 'ADMIN'),
('Jhon',   'Aprendiz',  'jhon@cimm.edu.co',
 '$2a$10$c6LQLjl9pUxL6dE80iYbfuovwO5Ori47W7ogMRnbiJ2ugb7eiUyqy', 'ESTUDIANTE'),
('Laura',  'Aprendiz',  'laura@cimm.edu.co',
 '$2a$10$c6LQLjl9pUxL6dE80iYbfuovwO5Ori47W7ogMRnbiJ2ugb7eiUyqy', 'ESTUDIANTE'),
('Andres', 'Aprendiz',  'andres@cimm.edu.co',
 '$2a$10$c6LQLjl9pUxL6dE80iYbfuovwO5Ori47W7ogMRnbiJ2ugb7eiUyqy', 'ESTUDIANTE');

INSERT INTO Encuesta (Titulo, Descripcion, Estado, CreadoPor) VALUES
('Eleccion de Representante de Aprendices 2026',
 'Vota por el aprendiz que representara al Centro ante el Consejo Academico.',
 'ACTIVA', 1);

INSERT INTO Opcion (EncuestaId, TextoOpcion, Orden) VALUES
(1, 'Laura Aprendiz - Ficha 2758412',  1),
(1, 'Andres Aprendiz - Ficha 2758412', 2),
(1, 'Voto en blanco',                  3);