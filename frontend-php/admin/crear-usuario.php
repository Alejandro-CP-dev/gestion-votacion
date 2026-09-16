<?php
/**
 * admin/crear-usuario.php — Alta de un usuario nuevo (estudiante o admin).
 *
 * No hay flujo de "olvidé mi clave" en este sistema (login.php lo dice: se
 * pide en coordinación académica), así que el administrador fija la clave
 * inicial aquí mismo.
 */
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../includes/sesion.php';
require_once __DIR__ . '/../includes/ApiClient.php';

requerirAdmin();

$nombre = '';
$apellido = '';
$correo = '';
$telefono = '';
$rol = 'ESTUDIANTE';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    validarCsrf();

    $nombre = trim($_POST['nombre'] ?? '');
    $apellido = trim($_POST['apellido'] ?? '');
    $correo = trim($_POST['correo'] ?? '');
    $telefono = trim($_POST['telefono'] ?? '');
    $clave = $_POST['clave'] ?? '';
    $rol = in_array($_POST['rol'] ?? '', ['ADMIN', 'ESTUDIANTE'], true) ? $_POST['rol'] : 'ESTUDIANTE';

    if ($nombre === '' || $apellido === '' || $correo === '' || $clave === '') {
        guardarAviso('error', 'Nombre, apellido, correo y clave son obligatorios.');
        irA('admin/crear-usuario.php');
    }

    $payload = [
        'nombre' => $nombre,
        'apellido' => $apellido,
        'correo' => $correo,
        'telefono' => $telefono,
        'clave' => $clave,
        'rol' => $rol,
    ];

    $r = ApiClient::post('/usuarios', $payload);

    if (ApiClient::ok($r)) {
        guardarAviso('exito', 'El usuario "' . $nombre . ' ' . $apellido . '" fue creado.');
        irA('admin/usuarios.php');
    }

    if ($r['codigo'] === 409) {
        guardarAviso('alerta', ApiClient::mensaje($r, 'Ya existe un usuario con ese correo.'));
        irA('admin/crear-usuario.php');
    }

    guardarAviso('error', ApiClient::mensaje($r, 'No fue posible crear el usuario.'));
    irA('admin/crear-usuario.php');
}

$tituloPagina = 'Crear usuario';
$paginaActual = 'usuarios';
$panelAncho = true;
require __DIR__ . '/../includes/cabecera.php';
?>

<h1 class="titulo-panel">Crear usuario</h1>

<div class="panel-tarjeta">
  <form method="post" action="<?= BASE_URL ?>/admin/crear-usuario.php" data-envio-simple>
    <?= campoCsrf() ?>

    <div class="campo">
      <label for="nombre">Nombre</label>
      <input class="entrada" type="text" id="nombre" name="nombre"
             value="<?= e($nombre) ?>" required maxlength="100" autofocus>
    </div>

    <div class="campo">
      <label for="apellido">Apellido</label>
      <input class="entrada" type="text" id="apellido" name="apellido"
             value="<?= e($apellido) ?>" required maxlength="100">
    </div>

    <div class="campo">
      <label for="correo">Correo</label>
      <input class="entrada" type="email" id="correo" name="correo"
             value="<?= e($correo) ?>" required maxlength="150" autocomplete="off">
    </div>

    <div class="campo">
      <label for="telefono">Teléfono (opcional)</label>
      <input class="entrada" type="tel" id="telefono" name="telefono"
             value="<?= e($telefono) ?>" maxlength="20">
    </div>

    <div class="campo">
      <label for="clave">Contraseña inicial</label>
      <input class="entrada" type="password" id="clave" name="clave"
             required minlength="6" autocomplete="new-password">
    </div>

    <div class="campo">
      <label for="rol">Rol</label>
      <select class="entrada" id="rol" name="rol">
        <option value="ESTUDIANTE" <?= $rol === 'ESTUDIANTE' ? 'selected' : '' ?>>Estudiante</option>
        <option value="ADMIN" <?= $rol === 'ADMIN' ? 'selected' : '' ?>>Administrador</option>
      </select>
    </div>

    <div class="acciones">
      <button class="boton boton--principal" type="submit" data-cargando="Creando…">
        <span class="boton__rueda" aria-hidden="true"></span>
        <span data-texto>Crear usuario</span>
      </button>
    </div>
  </form>
</div>

<?php require __DIR__ . '/../includes/pie.php'; ?>
