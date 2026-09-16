<?php
/**
 * includes/cabecera.php — Encabezado común a toda página ya autenticada.
 * login.php es la única página que NO la incluye: antes de tener sesión no
 * hay barra de navegación que mostrar.
 *
 * Variables que la página debe definir antes de este require:
 *   $tituloPagina  (string)  título de la pestaña y de la barra.
 *   $paginaActual  (string)  clave del enlace a marcar con aria-current.
 *   $panelAncho    (bool)    opcional; true = panel de 1080px (administrador).
 */
$usuarioActual = perfil();
$esAdminActual = esAdmin();
$avisoActual   = tomarAviso();
$paginaActual  = $paginaActual ?? '';
$avisoHtml = $avisoActual ? '
    <div class="aviso aviso--pagina aviso--' . e($avisoActual['tipo']) . '"
         role="' . ($avisoActual['tipo'] === 'error' ? 'alert' : 'status') . '">
      <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor"
           stroke-width="2" aria-hidden="true">
        <circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16h.01" stroke-linecap="round"/>
      </svg>
      <span>' . e($avisoActual['mensaje']) . '</span>
    </div>' : '';
?><!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title><?= e($tituloPagina ?? 'SVIS') ?> · SVIS</title>
<!-- Aplica el tema guardado ANTES del primer pintado: si esto fuera parte
     de app.js (que carga al final del body), habría un parpadeo de claro
     a oscuro en cada carga para quien ya eligió oscuro. -->
<script>(function(){try{var t=localStorage.getItem('svis-tema');if(t)document.documentElement.setAttribute('data-tema',t);}catch(e){}})();</script>
<link rel="stylesheet" href="<?= BASE_URL ?>/tokens.css">
<link rel="stylesheet" href="<?= BASE_URL ?>/estilos.css">
</head>
<body>
<a class="salto" href="#contenido">Ir al contenido</a>

<?php if ($esAdminActual): ?>
<!-- El administrador usa un layout de sidebar (aparte del navbar del
     estudiante) porque son roles con tareas distintas: cinco pantallas de
     gestión pesan más como menú lateral fijo que como barra superior. -->
<div class="panel-app">

  <aside class="panel-lateral" id="panelLateral">
    <a class="panel-lateral__marca" href="<?= BASE_URL ?>/admin/dashboard.php">
      <svg width="26" height="26" viewBox="0 0 48 48" fill="none" stroke="currentColor"
           stroke-width="2.5" aria-hidden="true">
        <path d="M24 5l16 6v11c0 10-6.8 17.9-16 21-9.2-3.1-16-11-16-21V11z" stroke-linejoin="round"/>
        <path d="M16 24l6 6 11-12" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <span>
        <b>SVIS</b>
        <span>Panel administrador</span>
      </span>
    </a>

    <nav class="panel-lateral__nav" aria-label="Principal">
      <a class="panel-lateral__enlace" href="<?= BASE_URL ?>/admin/dashboard.php"
         <?= $paginaActual === 'dashboard' ? 'aria-current="page"' : '' ?>>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/>
          <rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>
        </svg>
        Dashboard
      </a>
      <a class="panel-lateral__enlace" href="<?= BASE_URL ?>/admin/crear.php"
         <?= $paginaActual === 'crear' ? 'aria-current="page"' : '' ?>>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <circle cx="12" cy="12" r="9"/><path d="M12 8v8M8 12h8" stroke-linecap="round"/>
        </svg>
        Crear votación
      </a>
      <a class="panel-lateral__enlace" href="<?= BASE_URL ?>/admin/padron.php"
         <?= $paginaActual === 'padron' ? 'aria-current="page"' : '' ?>>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <circle cx="9" cy="8" r="3"/><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6" stroke-linecap="round"/>
          <path d="M16 8a3 3 0 100-6M21 20c0-2.8-1.8-5.1-4.3-5.8" stroke-linecap="round"/>
        </svg>
        Generar padrón
      </a>
      <a class="panel-lateral__enlace" href="<?= BASE_URL ?>/admin/gestionar.php"
         <?= $paginaActual === 'gestionar' ? 'aria-current="page"' : '' ?>>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <line x1="4" y1="6" x2="20" y2="6" stroke-linecap="round"/><circle cx="9" cy="6" r="2"/>
          <line x1="4" y1="12" x2="20" y2="12" stroke-linecap="round"/><circle cx="15" cy="12" r="2"/>
          <line x1="4" y1="18" x2="20" y2="18" stroke-linecap="round"/><circle cx="9" cy="18" r="2"/>
        </svg>
        Gestionar
      </a>
      <a class="panel-lateral__enlace" href="<?= BASE_URL ?>/admin/resultados.php"
         <?= $paginaActual === 'resultados' ? 'aria-current="page"' : '' ?>>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="M4 20V10M12 20V4M20 20v-7" stroke-linecap="round"/><path d="M2 20h20" stroke-linecap="round"/>
        </svg>
        Resultados
      </a>
      <a class="panel-lateral__enlace" href="<?= BASE_URL ?>/admin/usuarios.php"
         <?= $paginaActual === 'usuarios' ? 'aria-current="page"' : '' ?>>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <circle cx="9" cy="8" r="3"/><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6" stroke-linecap="round"/>
          <path d="M16 8a3 3 0 100-6M21 20c0-2.8-1.8-5.1-4.3-5.8" stroke-linecap="round"/>
        </svg>
        Usuarios
      </a>
    </nav>

    <div class="panel-lateral__pie">
      <div class="panel-lateral__perfil">
        <a class="avatar" aria-label="Mi perfil" href="<?= BASE_URL ?>/perfil.php"><?= e(mb_strtoupper(mb_substr($usuarioActual['nombre'] ?? $usuarioActual['correo'] ?? '?', 0, 1))) ?></a>
        <a class="panel-lateral__quien" href="<?= BASE_URL ?>/perfil.php">
          <b><?= e($usuarioActual['nombre'] ?? $usuarioActual['correo'] ?? '') ?></b>
          <small>Administrador · Mi perfil</small>
        </a>
        <?= boton_tema() ?>
      </div>
      <form method="post" action="<?= BASE_URL ?>/logout.php">
        <?= campoCsrf() ?>
        <button class="boton boton--texto boton--bloque" type="submit">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M16 17l5-5-5-5M21 12H9" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          Cerrar sesión
        </button>
      </form>
    </div>
  </aside>

  <div class="panel-lateral__fondo" data-cerrar-lateral></div>

  <div class="panel-columna">
    <header class="panel-topbar">
      <button class="panel-topbar__menu" type="button" data-abrir-lateral
              aria-controls="panelLateral" aria-label="Abrir menú">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="M4 7h16M4 12h16M4 17h16" stroke-linecap="round"/>
        </svg>
      </button>
      <h1 class="panel-topbar__titulo"><?= e($tituloPagina ?? 'SVIS') ?></h1>
    </header>

    <main id="contenido" class="contenido contenido--panel">
      <?= $avisoHtml ?>

<?php else: ?>

<header class="barra">
  <div class="barra__interior">
    <a class="barra__marca" href="<?= BASE_URL ?>/index.php"><b>SVIS</b> · Votaciones institucionales</a>

    <?php if (haySesion()): ?>
      <nav class="barra__nav" aria-label="Principal">
        <a href="<?= BASE_URL ?>/votar.php" <?= $paginaActual === 'votar' ? 'aria-current="page"' : '' ?>>Mis votaciones</a>
        <a href="<?= BASE_URL ?>/perfil.php" <?= $paginaActual === 'perfil' ? 'aria-current="page"' : '' ?>>Mi perfil</a>
      </nav>

      <span class="sesion"><?= e($usuarioActual['nombre'] ?? $usuarioActual['correo'] ?? '') ?></span>
    <?php endif; ?>

    <?= boton_tema() ?>

    <?php if (haySesion()): ?>
      <form method="post" action="<?= BASE_URL ?>/logout.php">
        <?= campoCsrf() ?>
        <button class="boton boton--texto" type="submit">Cerrar sesión</button>
      </form>
    <?php endif; ?>
  </div>
</header>

<main id="contenido" class="contenido<?= !empty($panelAncho) ? ' contenido--panel' : '' ?>">
  <?= $avisoHtml ?>

<?php endif; ?>
