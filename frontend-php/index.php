<?php
/**
 * index.php — Portada pública del sitio.
 *
 * Es, junto con login.php, la única página que se puede ver sin sesión
 * iniciada: por eso no incluye includes/cabecera.php (esa barra asume un
 * usuario autenticado). Si ya hay sesión, el botón principal lleva directo
 * a donde esa persona trabaja: el panel de administrador o "Mis votaciones".
 */
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/sesion.php';

$conSesion = haySesion();
$destino   = $conSesion ? (esAdmin() ? 'admin/dashboard.php' : 'votar.php') : 'login.php';
$textoCta  = $conSesion ? (esAdmin() ? 'Ir al panel de administración' : 'Ir a mis votaciones') : 'Iniciar sesión';

$aviso = tomarAviso();
?><!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>SVIS — Votaciones institucionales</title>
<script>(function(){try{var t=localStorage.getItem('svis-tema');if(t)document.documentElement.setAttribute('data-tema',t);}catch(e){}})();</script>
<link rel="stylesheet" href="<?= BASE_URL ?>/tokens.css">
<link rel="stylesheet" href="<?= BASE_URL ?>/estilos.css">
</head>
<body>
<a class="salto" href="#contenido">Ir al contenido</a>

<header class="barra portada__barra">
  <div class="barra__interior">
    <a class="barra__marca" href="<?= BASE_URL ?>/index.php"><b>SVIS</b> · Votaciones institucionales</a>
    <div class="portada__acciones">
      <?= boton_tema() ?>
      <a class="boton boton--plano" href="<?= BASE_URL ?>/<?= $destino ?>"><?= e($textoCta) ?></a>
    </div>
  </div>
</header>

<main id="contenido">

  <?php if ($aviso): ?>
    <div class="aviso aviso--pagina aviso--<?= e($aviso['tipo']) ?>"
         role="<?= $aviso['tipo'] === 'error' ? 'alert' : 'status' ?>">
      <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor"
           stroke-width="2" aria-hidden="true">
        <circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16h.01" stroke-linecap="round"/>
      </svg>
      <span><?= e($aviso['mensaje']) ?></span>
    </div>
  <?php endif; ?>

  <section class="portada__hero">
    <span class="portada__insignia">Centro de Formación SENA</span>
    <h1>Tu voto, único, anónimo y seguro.</h1>
    <p>
      SVIS es el sistema de votaciones y encuestas institucionales del Centro de
      Formación SENA. Cada estudiante habilitado recibe un token de un solo uso
      para participar, sin que el sistema guarde ninguna relación entre su
      identidad y la opción elegida.
    </p>
    <a class="boton boton--principal" href="<?= BASE_URL ?>/<?= $destino ?>"><?= e($textoCta) ?></a>
  </section>

  <section class="portada__caracteristicas">
    <div class="tarjeta">
      <span class="portada__icono" aria-hidden="true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M5 13l4 4L19 7" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </span>
      <b class="tarjeta__titulo">Voto único</b>
      <p class="tarjeta__desc">
        Cada token sirve una sola vez. En cuanto se emite un voto, queda anulado
        y no se puede volver a usar.
      </p>
    </div>

    <div class="tarjeta">
      <span class="portada__icono" aria-hidden="true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 3l7 3v6c0 5-3.4 9-7 10-3.6-1-7-5-7-10V6z" stroke-linejoin="round"/>
        </svg>
      </span>
      <b class="tarjeta__titulo">Anónimo</b>
      <p class="tarjeta__desc">
        Ningún registro conecta a la persona con la opción que eligió. El
        comprobante confirma el voto, no revela por quién se votó.
      </p>
    </div>

    <div class="tarjeta">
      <span class="portada__icono" aria-hidden="true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 018 0v3"/>
        </svg>
      </span>
      <b class="tarjeta__titulo">Seguro</b>
      <p class="tarjeta__desc">
        Los tokens se validan contra la base de datos institucional y cada
        elección sigue un ciclo controlado: creada, abierta y cerrada.
      </p>
    </div>
  </section>

</main>

<footer class="pie">
  <span>SVIS — Sistema de Votaciones y Encuestas Institucionales Seguras</span>
  <span>Centro de Formación SENA</span>
</footer>

<script src="<?= BASE_URL ?>/js/app.js"></script>
</body>
</html>
