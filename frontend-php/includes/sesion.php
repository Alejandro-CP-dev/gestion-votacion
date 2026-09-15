<?php
/**
 * includes/sesion.php — Todo lo que depende de $_SESSION en un solo archivo:
 * identidad, navegación, avisos flash, CSRF y escape de salida.
 *
 * Nada de esto toca la base de datos. "Sesión" aquí es únicamente la sesión
 * PHP del navegador; el backend Java no sabe que existe.
 */

/** ¿Hay alguien autenticado en esta sesión? */
function haySesion(): bool
{
    return !empty($_SESSION['usuario']['id']);
}

/** Corta la ejecución y manda al login si no hay sesión. */
function requerirSesion(): void
{
    if (!haySesion()) {
        guardarAviso('alerta', 'Inicia sesión para continuar.');
        irA('login.php');
    }
}

/** El perfil devuelto por POST /auth/login, tal cual lo guardó guardarPerfil(). */
function perfil(): array
{
    return $_SESSION['usuario'] ?? [];
}

function guardarPerfil(array $usuario): void
{
    $_SESSION['usuario'] = $usuario;
}

/**
 * El campo de rol que distingue estudiante de administrador.
 *
 * Ajusta el nombre del campo y el valor de comparación si el backend usa
 * otros literales — este es el único lugar del frontend que lo sabe.
 */
function esAdmin(): bool
{
    return strtoupper((string) (perfil()['rol'] ?? '')) === 'ADMIN';
}

/** Como requerirSesion(), pero además exige rol de administrador. */
function requerirAdmin(): void
{
    requerirSesion();
    if (!esAdmin()) {
        guardarAviso('error', 'No tienes permisos para acceder a esa sección.');
        irA('votar.php');
    }
}

function cerrarSesion(): void
{
    $_SESSION = [];
    session_destroy();
}

/** Redirección absoluta desde la raíz del sitio + fin de la petición. */
function irA(string $ruta): void
{
    header('Location: ' . BASE_URL . '/' . ltrim($ruta, '/'));
    exit;
}

/**
 * Aviso de una sola lectura: sobrevive exactamente una redirección (el patrón
 * POST-Redirect-GET de todo formulario que escribe) y luego desaparece solo.
 */
function guardarAviso(string $tipo, string $mensaje): void
{
    $_SESSION['aviso'] = ['tipo' => $tipo, 'mensaje' => $mensaje];
}

function tomarAviso(): ?array
{
    if (empty($_SESSION['aviso'])) {
        return null;
    }
    $aviso = $_SESSION['aviso'];
    unset($_SESSION['aviso']);
    return $aviso;
}

/** Escape por defecto para todo lo que se imprime en una plantilla. */
function e($valor): string
{
    return htmlspecialchars((string) $valor, ENT_QUOTES, 'UTF-8');
}

/**
 * El botón de tema claro/oscuro, igual en las tres barras que lo llevan
 * (sidebar del administrador, barra del estudiante, portada pública). Vive
 * aquí y no repetido en cada plantilla para que un cambio de ícono no sean
 * tres ediciones y una que se olvida.
 *
 * Qué ícono se ve es CSS puro (ver .boton-tema en estilos.css); este botón
 * no necesita saber el tema actual, solo existe para que haya algo que
 * hacer clic.
 */
function boton_tema(): string
{
    return '
    <button class="boton-tema" type="button" data-tema-boton aria-label="Cambiar entre tema claro y oscuro">
      <svg class="boton-tema__sol" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
        <circle cx="12" cy="12" r="4"/>
        <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" stroke-linecap="round"/>
      </svg>
      <svg class="boton-tema__luna" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
        <path d="M21 12.8A9 9 0 1111.2 3 7 7 0 0021 12.8z" stroke-linejoin="round"/>
      </svg>
    </button>';
}

/* ==================== CSRF ====================
 * Un token por sesión, no uno por formulario: alcanza para el problema real
 * (envíos cruzados de sitio) y no obliga a regenerar la página en cada tab
 * abierta a la vez.
 */

function campoCsrf(): string
{
    if (empty($_SESSION['csrf'])) {
        $_SESSION['csrf'] = bin2hex(random_bytes(32));
    }
    return '<input type="hidden" name="_csrf" value="' . e($_SESSION['csrf']) . '">';
}

function validarCsrf(): void
{
    $token = $_POST['_csrf'] ?? '';
    if (empty($_SESSION['csrf']) || !hash_equals($_SESSION['csrf'], (string) $token)) {
        guardarAviso('error', 'Tu sesión expiró o la página se recargó. Intenta de nuevo.');
        irA('index.php');
    }
}
