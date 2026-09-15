<?php
/**
 * admin/padron.php — Generar el padrón: emitir un token por estudiante para
 * una votación puntual.
 *
 * El padrón generado se muestra una vez, igual que el comprobante del
 * estudiante en comprobante.php, y por la misma razón: no hay endpoint para
 * volver a listar los tokens de una votación, porque un token es la única
 * prueba de que alguien puede votar y reimprimirlo a la ligera es un riesgo
 * de seguridad, no una comodidad. Si el admin necesita verlo de nuevo dentro
 * de la misma sesión, aquí queda disponible; si cierra sesión, se pierde y
 * hay que generarlo otra vez (el backend decide si eso reemplaza tokens
 * previos o falla con 409 por padrón ya existente).
 */
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../includes/sesion.php';
require_once __DIR__ . '/../includes/ApiClient.php';

requerirAdmin();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    validarCsrf();

    $encuestaId = (int) ($_POST['encuestaId'] ?? 0);
    $usuarioIds = array_map('intval', $_POST['usuarios'] ?? []);

    if ($encuestaId <= 0 || !$usuarioIds) {
        guardarAviso('error', 'Elige una votación y al menos un estudiante.');
        irA('admin/padron.php');
    }

    $r = ApiClient::post('/tokens/generar', ['encuestaId' => $encuestaId, 'usuarioIds' => $usuarioIds]);

    if (ApiClient::ok($r)) {
        // Vive solo en la sesión, como el comprobante del estudiante: no hay
        // otro lugar del sistema donde este listado de tokens vuelva a existir.
        $_SESSION['padronGenerado'][$encuestaId] = $r['cuerpo']['tokens'] ?? [];
        guardarAviso('exito', 'Padrón generado para ' . count($usuarioIds) . ' estudiante(s).');
        irA('admin/padron.php?generado=' . $encuestaId);
    }

    if ($r['codigo'] === 409) {
        guardarAviso('alerta', ApiClient::mensaje($r, 'Ya existe un padrón para esta votación.'));
        irA('admin/padron.php');
    }

    guardarAviso('error', ApiClient::mensaje($r, 'No fue posible generar el padrón.'));
    irA('admin/padron.php');
}

/* ---------- Datos para la pantalla ---------- */

$encuestas = [];
$r = ApiClient::get('/encuestas');
if (ApiClient::ok($r)) {
    $encuestas = array_values(array_filter($r['cuerpo'] ?: [],
        fn($v) => strtoupper($v['estado'] ?? '') !== 'CERRADA'));
}

$estudiantes = [];
$rEst = ApiClient::get('/usuarios', ['rol' => 'ESTUDIANTE']);
if (ApiClient::ok($rEst)) {
    $estudiantes = $rEst['cuerpo'] ?: [];
}

$generadoId = (int) ($_GET['generado'] ?? 0);
$padronGenerado = $generadoId ? ($_SESSION['padronGenerado'][$generadoId] ?? null) : null;
$encuestaGenerada = null;
foreach ($encuestas as $v) {
    if ((int) $v['id'] === $generadoId) {
        $encuestaGenerada = $v;
        break;
    }
}

$tituloPagina = 'Generar padrón';
$paginaActual = 'padron';
$panelAncho = true;
require __DIR__ . '/../includes/cabecera.php';
?>

<h1 class="titulo-panel">Generar padrón</h1>

<?php if ($padronGenerado !== null): ?>

  <div class="panel-tarjeta">
    <div class="panel-cabecera">
      <h2 class="panel-seccion">
        Tokens generados<?= $encuestaGenerada ? ' — ' . e($encuestaGenerada['titulo']) : '' ?>
      </h2>
      <button class="boton boton--plano" type="button" data-imprimir>Imprimir</button>
    </div>

    <?php if (!$padronGenerado): ?>
      <div class="vacio">
        <h2>No hay tokens que mostrar</h2>
        <p>Genera un padrón nuevo desde el formulario de abajo.</p>
      </div>
    <?php else: ?>
      <table class="tabla">
        <thead>
          <tr><th>Estudiante</th><th>Correo</th><th>Token</th></tr>
        </thead>
        <tbody>
          <?php foreach ($padronGenerado as $item): ?>
            <tr>
              <td><?= e($item['nombre'] ?? '') ?></td>
              <td><?= e($item['correo'] ?? '') ?></td>
              <td><code class="token-codigo"><?= e($item['token'] ?? '') ?></code></td>
            </tr>
          <?php endforeach; ?>
        </tbody>
      </table>
    <?php endif; ?>
  </div>

<?php endif; ?>

<div class="panel-tarjeta">
  <h2 class="panel-seccion">Nuevo padrón</h2>

  <?php if (!$encuestas || !$estudiantes): ?>
    <div class="vacio">
      <h2>Falta información para generar un padrón</h2>
      <p>
        <?= !$encuestas ? 'No hay votaciones disponibles (crea una primero).' : 'No hay estudiantes registrados.' ?>
      </p>
    </div>
  <?php else: ?>
    <form method="post" action="<?= BASE_URL ?>/admin/padron.php" data-envio-simple>
      <?= campoCsrf() ?>

      <div class="campo">
        <label for="encuestaId">Votación</label>
        <select class="entrada" id="encuestaId" name="encuestaId" required>
          <option value="">Selecciona una votación</option>
          <?php foreach ($encuestas as $v): ?>
            <option value="<?= (int) $v['id'] ?>"><?= e($v['titulo']) ?></option>
          <?php endforeach; ?>
        </select>
      </div>

      <div class="campo espacio-arriba" data-verificacion>
        <div class="campo__rotulo">
          <label>Estudiantes</label>
          <span><span data-contador-seleccion>0</span> de <?= count($estudiantes) ?> seleccionados</span>
        </div>

        <div class="verificacion-lista">
          <div class="verificacion-cabecera">
            <label>
              <input type="checkbox" data-marcar-todos>
              Seleccionar todos
            </label>
          </div>
          <?php foreach ($estudiantes as $est): ?>
            <label class="verificacion">
              <input type="checkbox" name="usuarios[]" value="<?= (int) $est['id'] ?>" data-marcable>
              <span><?= e($est['nombre'] ?? $est['correo']) ?></span>
            </label>
          <?php endforeach; ?>
        </div>
      </div>

      <div class="acciones">
        <button class="boton boton--principal" type="submit" data-cargando="Generando…">
          <span class="boton__rueda" aria-hidden="true"></span>
          <span data-texto>Generar padrón</span>
        </button>
      </div>
    </form>
  <?php endif; ?>
</div>

<?php require __DIR__ . '/../includes/pie.php'; ?>
