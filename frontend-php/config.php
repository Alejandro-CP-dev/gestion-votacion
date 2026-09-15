<?php
/**
 * config.php — Arranque de cada página: sesión PHP y constantes del sitio.
 *
 * Este archivo NUNCA abre una conexión a MySQL. El backend Java es el único
 * que conoce las credenciales de base de datos; el frontend solo le habla
 * por HTTP a través de includes/ApiClient.php.
 */

if (session_status() === PHP_SESSION_NONE) {
    session_set_cookie_params([
        'httponly' => true,
        'samesite' => 'Lax',
    ]);
    session_start();
}

// Valores especificos de esta maquina (ruta, puerto del backend, clave
// compartida). Viven en config.local.php, que NO se sube a git — cada quien
// tiene el suyo. Copia config.local.php.example para crear el tuyo.
if (file_exists(__DIR__ . '/config.local.php')) {
    require __DIR__ . '/config.local.php';
}

// Ruta base de la aplicación, relativa al dominio. Vacía si el frontend vive
// en la raíz del sitio; algo como '/svis' si vive en un subdirectorio. Se usa
// como prefijo absoluto en enlaces y redirecciones para que no importe desde
// qué profundidad de carpeta se genere el enlace (p. ej. admin/dashboard.php).
if (!defined('BASE_URL')) {
    define('BASE_URL', '');
}

// URL base del backend Java (Servlets + Maven, desplegado en Tomcat).
// Ajusta el puerto y el contexto a los reales de tu despliegue.
if (!defined('API_BASE_URL')) {
    define('API_BASE_URL', 'http://localhost:8080/api');
}

// Clave compartida que exige ApiKeyFilter del backend en los endpoints de
// administracion (cabecera X-Svis-Key). Debe coincidir con api.key en
// backend-java/src/main/resources/db.properties. Vacia por defecto: sin
// clave configurada, el backend bloquea todo lo de administracion (falla
// cerrado, a proposito).
if (!defined('API_KEY')) {
    define('API_KEY', '');
}
