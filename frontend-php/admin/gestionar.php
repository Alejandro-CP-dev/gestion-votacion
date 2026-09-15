<?php
/**
 * admin/gestionar.php — El ciclo de vida de cada votación: CREADA → ABIERTA → CERRADA.
 *
 * Abrir y cerrar son las dos únicas transiciones, y ambas son escrituras:
 * pasan por POST-Redirect-GET igual que emitir un voto. Cerrar además pide
 * confirmación explícita, porque una vez cerrada una votación no hay forma
 * de reabrirla — es la misma clase de acción irreversible que en papeleta.php
 * lleva boton--grave y un modal, así que aquí se trata igual.
 */
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../includes/sesion.php';
require_once __DIR__ . '/../includes/ApiClient.php';

requerirAdmin();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    validarCsrf();

    $id = (int) ($_POST['encuestaId'] ?? 0);
    $accion = $_POST['accion'] ?? '';

    if ($id <= 0 || !in_array($accion, ['abrir', 'cerrar'], true)) {
        guardarAviso('error', 'Acción no reconocida.');
        irA('admin/gestionar.php');
    }

    $r = ApiClient::post("/encuestas/{$id}/{$accion}");

    if (ApiClient::ok($r)) {
        guardarAviso('exito', $accion === 'abrir' ? 'La votación quedó abierta.' : 'La votación quedó cerrada.');
        irA('admin/gestionar.php');
    }

    if ($r['codigo'] === 409) {
        // P. ej. cerrar una votación que ya está cerrada, o abrir una sin
        // padrón generado: una regla de negocio respondiendo, no un error.
        guardarAviso('alerta', ApiClient::mensaje($r, 'Esa votación no puede cambiar de estado ahora mismo.'));
        irA('admin/gestionar.php');
    }

    guardarAviso('error', ApiClient::mensaje($r, 'No fue posible actualizar la votación.'));
    irA('admin/gestionar.php');
}

$votaciones = [];
$falloRed = null;

$r = ApiClient::get('/encuestas');
if (ApiClient::ok($r)) {
    $votaciones = $r['cuerpo'] ?: [];
} else {
    $falloRed = ApiClient::mensaje($r, 'No pudimos consultar las votaciones.');
}

$etiquetaEstado = ['CREADA' => 'Creada', 'ABIERTA' => 'Abierta', 'CERRADA' => 'Cerrada'];
$claseEstado = ['CREADA' => 'insignia--creada', 'ABIERTA' => 'insignia--abierta', 'CERRADA' => 'insignia--cerrada'];

$tituloPagina = 'Gestionar votaciones';
$paginaActual = 'gestionar';
$panelAncho = true;
require __DIR__ . '/../includes/cabecera.php';
?>

<h1 class="titulo-panel">Gestionar votaciones</h1>

<?php if ($falloRed): ?>

  <div class="aviso aviso--error" role="alert">
    <svg width="17" height="17" viewBox="0 0 24 24" fill="none"
         stroke="var(--granate)" stroke-width="2" aria-hidden="true">
      <circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16h.01" stroke-linecap="round"/>
    </svg>
    <span><?= e($falloRed) ?> Vuelve a intentarlo en unos minutos.</span>
  </div>

<?php elseif (!$votaciones): ?>

  <div class="panel-tarjeta">
    <div class="vacio">
      <h2>No hay votaciones todavía</h2>
      <p>Crea la primera desde “Crear votación”.</p>
    </div>
  </div>

<?php else: ?>

  <div class="panel-tarjeta">
    <table class="tabla">
      <thead>
        <tr><th>Votación</th><th>Estado</th><th>Opciones</th><th></th></tr>
      </thead>
      <tbody>
        <?php foreach ($votaciones as $v):
            $id = (int) $v['id'];
            $estado = strtoupper($v['estado'] ?? '');
        ?>
          <tr>
            <td class="tabla__titulo"><?= e($v['titulo']) ?></td>
            <td>
              <span class="insignia <?= e($claseEstado[$estado] ?? 'insignia--cerrada') ?>">
                <?= e($etiquetaEstado[$estado] ?? $v['estado']) ?>
              </span>
            </td>
            <td><?= count($v['opciones'] ?? []) ?></td>
            <td class="tabla__acciones">

              <?php if ($estado === 'CREADA'): ?>
                <a class="boton boton--plano" href="<?= BASE_URL ?>/admin/padron.php">Generar padrón</a>
                <form method="post" action="<?= BASE_URL ?>/admin/gestionar.php" data-envio-simple>
                  <?= campoCsrf() ?>
                  <input type="hidden" name="encuestaId" value="<?= $id ?>">
                  <input type="hidden" name="accion" value="abrir">
                  <button class="boton boton--principal" type="submit" data-cargando="Abriendo…">
                    <span class="boton__rueda" aria-hidden="true"></span>
                    <span data-texto>Abrir</span>
                  </button>
                </form>

              <?php elseif ($estado === 'ABIERTA'): ?>
                <form method="post" action="<?= BASE_URL ?>/admin/gestionar.php" data-confirmar="telonCerrar<?= $id ?>">
                  <?= campoCsrf() ?>
                  <input type="hidden" name="encuestaId" value="<?= $id ?>">
                  <input type="hidden" name="accion" value="cerrar">
                  <button class="boton boton--grave" type="submit" data-cargando="Cerrando…">
                    <span class="boton__rueda" aria-hidden="true"></span>
                    <span data-texto>Cerrar</span>
                  </button>
                </form>

              <?php else: ?>
                <a class="boton boton--plano" href="<?= BASE_URL ?>/admin/resultados.php?id=<?= $id ?>">Resultados</a>
              <?php endif; ?>

            </td>
          </tr>

        <?php endforeach; ?>
      </tbody>
    </table>
  </div>

  <!-- Los modales de confirmación van fuera de la tabla: un <div> no puede
       ser hijo directo de <tbody> entre filas, y son fixed sobre toda la
       página de todas formas. -->
  <?php foreach ($votaciones as $v):
      $id = (int) $v['id'];
      if (strtoupper($v['estado'] ?? '') !== 'ABIERTA') continue;
  ?>
    <div class="telon" id="telonCerrar<?= $id ?>">
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="tituloCerrar<?= $id ?>">
        <h2 id="tituloCerrar<?= $id ?>">Cerrar "<?= e($v['titulo']) ?>"</h2>
        <p class="tarjeta__desc">
          Nadie podrá votar en esta elección después de cerrarla, y no hay forma de reabrirla.
        </p>
        <div class="aviso aviso--alerta">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none"
               stroke="var(--ambar)" stroke-width="2" aria-hidden="true">
            <path d="M12 3l9 16H3z" stroke-linejoin="round"/>
            <path d="M12 10v4M12 17h.01" stroke-linecap="round"/>
          </svg>
          <span>Esta acción no se puede deshacer.</span>
        </div>
        <div class="acciones">
          <button class="boton boton--plano boton--bloque" type="button" data-cancelar>Cancelar</button>
          <button class="boton boton--grave boton--bloque" type="button" data-aceptar>Sí, cerrar</button>
        </div>
      </div>
    </div>
  <?php endforeach; ?>

<?php endif; ?>

<?php require __DIR__ . '/../includes/pie.php'; ?>
