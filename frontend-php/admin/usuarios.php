<?php
/**
 * admin/usuarios.php — Listado de usuarios y activar/desactivar.
 *
 * A diferencia de cerrar una votación, activar/desactivar es reversible: no
 * lleva el modal de confirmación que sí usa admin/gestionar.php para cerrar.
 */
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../includes/sesion.php';
require_once __DIR__ . '/../includes/ApiClient.php';

requerirAdmin();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    validarCsrf();

    $id = (int) ($_POST['usuarioId'] ?? 0);
    $accion = $_POST['accion'] ?? '';

    if ($id <= 0 || !in_array($accion, ['activar', 'desactivar'], true)) {
        guardarAviso('error', 'Acción no reconocida.');
        irA('admin/usuarios.php');
    }

    if ($accion === 'desactivar' && $id === (int) perfil()['id']) {
        guardarAviso('error', 'No puedes desactivar tu propia cuenta.');
        irA('admin/usuarios.php');
    }

    $r = ApiClient::post("/usuarios/{$id}/{$accion}");

    if (ApiClient::ok($r)) {
        guardarAviso('exito', $accion === 'activar' ? 'El usuario quedó activo.' : 'El usuario quedó inactivo.');
        irA('admin/usuarios.php');
    }

    guardarAviso('error', ApiClient::mensaje($r, 'No fue posible actualizar el usuario.'));
    irA('admin/usuarios.php');
}

$usuarios = [];
$falloRed = null;

$r = ApiClient::get('/usuarios');
if (ApiClient::ok($r)) {
    $usuarios = $r['cuerpo'] ?: [];
} else {
    $falloRed = ApiClient::mensaje($r, 'No pudimos consultar los usuarios.');
}

$etiquetaRol = ['ADMIN' => 'Administrador', 'ESTUDIANTE' => 'Estudiante'];
$etiquetaEstado = ['ACTIVO' => 'Activo', 'INACTIVO' => 'Inactivo'];
$claseEstado = ['ACTIVO' => 'insignia--abierta', 'INACTIVO' => 'insignia--cerrada'];

$tituloPagina = 'Usuarios';
$paginaActual = 'usuarios';
$panelAncho = true;
require __DIR__ . '/../includes/cabecera.php';
?>

<div class="panel-cabecera">
  <h1 class="titulo-panel">Usuarios</h1>
  <a class="boton boton--principal" href="<?= BASE_URL ?>/admin/crear-usuario.php">+ Crear usuario</a>
</div>

<?php if ($falloRed): ?>

  <div class="aviso aviso--error" role="alert">
    <svg width="17" height="17" viewBox="0 0 24 24" fill="none"
         stroke="var(--granate)" stroke-width="2" aria-hidden="true">
      <circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16h.01" stroke-linecap="round"/>
    </svg>
    <span><?= e($falloRed) ?> Vuelve a intentarlo en unos minutos.</span>
  </div>

<?php elseif (!$usuarios): ?>

  <div class="panel-tarjeta">
    <div class="vacio">
      <h2>No hay usuarios todavía</h2>
      <p>Crea el primero desde “Crear usuario”.</p>
    </div>
  </div>

<?php else: ?>

  <div class="panel-tarjeta">
    <table class="tabla">
      <thead>
        <tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th></th></tr>
      </thead>
      <tbody>
        <?php foreach ($usuarios as $u):
            $id = (int) $u['id'];
            $rol = strtoupper($u['rol'] ?? '');
            $estado = strtoupper($u['estado'] ?? '');
            $esUnoMismo = $id === (int) perfil()['id'];
        ?>
          <tr>
            <td class="tabla__titulo"><?= e($u['nombre'] . ' ' . $u['apellido']) ?></td>
            <td><?= e($u['correo']) ?></td>
            <td><?= e($etiquetaRol[$rol] ?? $u['rol']) ?></td>
            <td>
              <span class="insignia <?= e($claseEstado[$estado] ?? 'insignia--cerrada') ?>">
                <?= e($etiquetaEstado[$estado] ?? $u['estado']) ?>
              </span>
            </td>
            <td class="tabla__acciones">
              <?php if ($esUnoMismo): ?>
                <span class="pista">Tu cuenta</span>
              <?php elseif ($estado === 'ACTIVO'): ?>
                <form method="post" action="<?= BASE_URL ?>/admin/usuarios.php" data-envio-simple>
                  <?= campoCsrf() ?>
                  <input type="hidden" name="usuarioId" value="<?= $id ?>">
                  <input type="hidden" name="accion" value="desactivar">
                  <button class="boton boton--plano" type="submit" data-cargando="Desactivando…">
                    <span class="boton__rueda" aria-hidden="true"></span>
                    <span data-texto>Desactivar</span>
                  </button>
                </form>
              <?php else: ?>
                <form method="post" action="<?= BASE_URL ?>/admin/usuarios.php" data-envio-simple>
                  <?= campoCsrf() ?>
                  <input type="hidden" name="usuarioId" value="<?= $id ?>">
                  <input type="hidden" name="accion" value="activar">
                  <button class="boton boton--principal" type="submit" data-cargando="Activando…">
                    <span class="boton__rueda" aria-hidden="true"></span>
                    <span data-texto>Activar</span>
                  </button>
                </form>
              <?php endif; ?>
            </td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>

<?php endif; ?>

<?php require __DIR__ . '/../includes/pie.php'; ?>
