<?php
/**
 * logout.php — Cierre de sesión.
 *
 * Solo por POST, con el mismo CSRF que cualquier otro formulario que escribe:
 * cerrar la sesión de otra persona con un enlace GET (o una <img> maliciosa)
 * es exactamente el tipo de escritura que el CSRF existe para evitar.
 */
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/sesion.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    irA('index.php');
}

validarCsrf();
cerrarSesion();
irA('login.php');
