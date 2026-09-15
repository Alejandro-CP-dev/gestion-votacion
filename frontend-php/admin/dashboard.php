<?php
/**
 * admin/dashboard.php — Vista general: cuántas votaciones hay y en qué estado.
 *
 * Solo lectura. GET /encuestas devuelve TODAS las votaciones (a diferencia de
 * /encuestas/activas, que usa el estudiante y solo trae las abiertas) — es la
 * misma razón por la que papeleta.php no puede usarse para leer una encuesta
 * cerrada: aquí es al revés, el administrador sí necesita ver las tres fases.
 */
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../includes/sesion.php';
require_once __DIR__ . '/../includes/ApiClient.php';

requerirAdmin();

$votaciones = [];
$falloRed = null;

$r = ApiClient::get('/encuestas');
if (ApiClient::ok($r)) {
    $votaciones = $r['cuerpo'] ?: [];
} else {
    $falloRed = ApiClient::mensaje($r, 'No pudimos consultar las votaciones.');
}

$totales = ['CREADA' => 0, 'ABIERTA' => 0, 'CERRADA' => 0];
foreach ($votaciones as $v) {
    $estado = strtoupper($v['estado'] ?? '');
    if (isset($totales[$estado])) {
        $totales[$estado]++;
    }
}

$recientes = array_slice($votaciones, 0, 6);

$etiquetaEstado = ['CREADA' => 'Creada', 'ABIERTA' => 'Abierta', 'CERRADA' => 'Cerrada'];
$claseEstado = ['CREADA' => 'insignia--creada', 'ABIERTA' => 'insignia--abierta', 'CERRADA' => 'insignia--cerrada'];

$tituloPagina = 'Panel del administrador';
$paginaActual = 'dashboard';
$panelAncho = true;
require __DIR__ . '/../includes/cabecera.php';
?>

<div class="panel-cabecera">
  <h1 class="titulo-panel">Panel del administrador</h1>
  <a class="boton boton--principal" href="<?= BASE_URL ?>/admin/crear.php">Nueva votación</a>
</div>

<?php if ($falloRed): ?>

  <div class="aviso aviso--error" role="alert">
    <svg width="17" height="17" viewBox="0 0 24 24" fill="none"
         stroke="var(--granate)" stroke-width="2" aria-hidden="true">
      <circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16h.01" stroke-linecap="round"/>
    </svg>
    <span><?= e($falloRed) ?> Vuelve a intentarlo en unos minutos.</span>
  </div>

<?php else: ?>

  <div class="panel-resumen">
    <div class="estadistica">
      <div class="estadistica__numero"><?= count($votaciones) ?></div>
      <div class="estadistica__etiqueta">Votaciones en total</div>
    </div>
    <div class="estadistica">
      <div class="estadistica__numero"><?= $totales['ABIERTA'] ?></div>
      <div class="estadistica__etiqueta">Abiertas ahora mismo</div>
    </div>
    <div class="estadistica">
      <div class="estadistica__numero"><?= $totales['CREADA'] ?></div>
      <div class="estadistica__etiqueta">Creadas, sin abrir</div>
    </div>
    <div class="estadistica">
      <div class="estadistica__numero"><?= $totales['CERRADA'] ?></div>
      <div class="estadistica__etiqueta">Cerradas</div>
    </div>
  </div>

  <div class="panel-tarjeta">
    <h2 class="panel-seccion">Últimas votaciones</h2>
    <?php if (!$votaciones): ?>
      <div class="vacio">
        <h2>Todavía no hay votaciones</h2>
        <p>Crea la primera desde “Nueva votación”.</p>
      </div>
    <?php else: ?>
      <table class="tabla">
        <thead>
          <tr>
            <th>Votación</th>
            <th>Estado</th>
            <th>Opciones</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <?php foreach ($recientes as $v): $estado = strtoupper($v['estado'] ?? ''); ?>
            <tr>
              <td class="tabla__titulo"><?= e($v['titulo']) ?></td>
              <td>
                <span class="insignia <?= e($claseEstado[$estado] ?? 'insignia--cerrada') ?>">
                  <?= e($etiquetaEstado[$estado] ?? $v['estado']) ?>
                </span>
              </td>
              <td><?= count($v['opciones'] ?? []) ?></td>
              <td class="tabla__acciones">
                <a class="boton boton--plano" href="<?= BASE_URL ?>/admin/gestionar.php">Gestionar</a>
                <?php if ($estado !== 'CREADA'): ?>
                  <a class="boton boton--plano" href="<?= BASE_URL ?>/admin/resultados.php?id=<?= (int) $v['id'] ?>">Resultados</a>
                <?php endif; ?>
              </td>
            </tr>
          <?php endforeach; ?>
        </tbody>
      </table>
    <?php endif; ?>
  </div>

<?php endif; ?>

<?php require __DIR__ . '/../includes/pie.php'; ?>
