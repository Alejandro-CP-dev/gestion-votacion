<?php
/**
 * perfil.php — Autoservicio: cada usuario edita sus propios datos.
 *
 * Dos formularios independientes y dos llamadas al backend distintas: los
 * datos personales no requieren la clave actual, cambiar la clave sí. Mezclar
 * ambos en un solo POST obligaría a escribir la clave actual solo para
 * corregir un número de teléfono.
 *
 * El id que viaja al backend es siempre perfil()['id'] — nunca uno leído del
 * formulario — para que nadie pueda editar el perfil de otra persona
 * cambiando un campo oculto. Ver la nota de autorización en UsuarioService.
 */
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/sesion.php';
require_once __DIR__ . '/includes/ApiClient.php';

requerirSesion();

$idUsuario = (int) perfil()['id'];
$accion = $_POST['accion'] ?? '';

if ($_SERVER['REQUEST_METHOD'] === 'POST' && $accion === 'datos') {
    validarCsrf();

    $nombre = trim($_POST['nombre'] ?? '');
    $apellido = trim($_POST['apellido'] ?? '');
    $telefono = trim($_POST['telefono'] ?? '');

    if ($nombre === '' || $apellido === '') {
        guardarAviso('error', 'Nombre y apellido son obligatorios.');
        irA('perfil.php');
    }

    $r = ApiClient::put("/usuarios/{$idUsuario}/perfil", [
        'nombre' => $nombre,
        'apellido' => $apellido,
        'telefono' => $telefono,
    ]);

    if (ApiClient::ok($r)) {
        // Se guarda el mismo perfil, con los campos ya actualizados: así el
        // nombre en la barra/sidebar cambia de inmediato, sin re-login.
        guardarPerfil($r['cuerpo']);
        guardarAviso('exito', 'Tus datos fueron actualizados.');
        irA('perfil.php');
    }

    guardarAviso('error', ApiClient::mensaje($r, 'No fue posible actualizar tus datos.'));
    irA('perfil.php');
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && $accion === 'clave') {
    validarCsrf();

    $claveActual = $_POST['claveActual'] ?? '';
    $claveNueva = $_POST['claveNueva'] ?? '';
    $claveConfirmar = $_POST['claveConfirmar'] ?? '';

    if ($claveActual === '' || $claveNueva === '') {
        guardarAviso('error', 'Escribe tu clave actual y la clave nueva.');
        irA('perfil.php');
    }

    if ($claveNueva !== $claveConfirmar) {
        guardarAviso('error', 'La confirmación no coincide con la clave nueva.');
        irA('perfil.php');
    }

    $r = ApiClient::post("/usuarios/{$idUsuario}/clave", [
        'claveActual' => $claveActual,
        'claveNueva' => $claveNueva,
    ]);

    if (ApiClient::ok($r)) {
        guardarAviso('exito', 'Tu clave fue actualizada.');
        irA('perfil.php');
    }

    guardarAviso('error', ApiClient::mensaje($r, 'No fue posible cambiar tu clave.'));
    irA('perfil.php');
}

$datos = perfil();

$tituloPagina = 'Mi perfil';
$paginaActual = 'perfil';
require __DIR__ . '/includes/cabecera.php';
?>

<h1 class="titulo-panel">Mi perfil</h1>

<div class="panel-tarjeta">
  <h2 class="tarjeta__titulo">Datos personales</h2>
  <form method="post" action="<?= BASE_URL ?>/perfil.php" data-envio-simple>
    <?= campoCsrf() ?>
    <input type="hidden" name="accion" value="datos">

    <div class="campo">
      <label for="correo">Correo</label>
      <input class="entrada" type="email" id="correo" value="<?= e($datos['correo'] ?? '') ?>" disabled>
    </div>

    <div class="campo">
      <label for="nombre">Nombre</label>
      <input class="entrada" type="text" id="nombre" name="nombre"
             value="<?= e($datos['nombre'] ?? '') ?>" required maxlength="100">
    </div>

    <div class="campo">
      <label for="apellido">Apellido</label>
      <input class="entrada" type="text" id="apellido" name="apellido"
             value="<?= e($datos['apellido'] ?? '') ?>" required maxlength="100">
    </div>

    <div class="campo">
      <label for="telefono">Teléfono</label>
      <input class="entrada" type="tel" id="telefono" name="telefono"
             value="<?= e($datos['telefono'] ?? '') ?>" maxlength="20">
    </div>

    <div class="acciones">
      <button class="boton boton--principal" type="submit" data-cargando="Guardando…">
        <span class="boton__rueda" aria-hidden="true"></span>
        <span data-texto>Guardar datos</span>
      </button>
    </div>
  </form>
</div>

<div class="panel-tarjeta espacio-arriba">
  <h2 class="tarjeta__titulo">Cambiar contraseña</h2>
  <form method="post" action="<?= BASE_URL ?>/perfil.php" data-envio-simple>
    <?= campoCsrf() ?>
    <input type="hidden" name="accion" value="clave">

    <div class="campo">
      <label for="claveActual">Contraseña actual</label>
      <input class="entrada" type="password" id="claveActual" name="claveActual"
             required autocomplete="current-password">
    </div>

    <div class="campo">
      <label for="claveNueva">Contraseña nueva</label>
      <input class="entrada" type="password" id="claveNueva" name="claveNueva"
             required minlength="6" autocomplete="new-password">
    </div>

    <div class="campo">
      <label for="claveConfirmar">Confirmar contraseña nueva</label>
      <input class="entrada" type="password" id="claveConfirmar" name="claveConfirmar"
             required minlength="6" autocomplete="new-password">
    </div>

    <div class="acciones">
      <button class="boton boton--principal" type="submit" data-cargando="Cambiando…">
        <span class="boton__rueda" aria-hidden="true"></span>
        <span data-texto>Cambiar contraseña</span>
      </button>
    </div>
  </form>
</div>

<?php require __DIR__ . '/includes/pie.php'; ?>
