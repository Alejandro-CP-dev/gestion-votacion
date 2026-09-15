<?php
/**
 * admin/resultados.php — Conteos por opción.
 *
 * Esta es, a propósito, la única pantalla de todo el sistema que muestra
 * cuántos votos tiene cada opción. papeleta.php jamás lo hace (el estudiante
 * no debe poder inferir el resultado mientras vota), y comprobante.php
 * tampoco (el recibo no delata la elección). Aquí sí, porque es exactamente
 * la razón de ser de esta pantalla: es solo para el administrador, después
 * de que la votación existe.
 */
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../includes/sesion.php';
require_once __DIR__ . '/../includes/ApiClient.php';

requerirAdmin();

$id = (int) ($_GET['id'] ?? 0);

$votaciones = [];
$falloRed = null;
$rLista = ApiClient::get('/encuestas');
if (ApiClient::ok($rLista)) {
    // Antes de abrirse no hay nada que contar todavía.
    $votaciones = array_values(array_filter($rLista['cuerpo'] ?: [],
        fn($v) => strtoupper($v['estado'] ?? '') !== 'CREADA'));
} else {
    $falloRed = ApiClient::mensaje($rLista, 'No pudimos consultar las votaciones.');
}

$resultado = null;
if ($id > 0) {
    $r = ApiClient::get("/encuestas/{$id}/resultados");
    if (ApiClient::ok($r)) {
        $resultado = $r['cuerpo'];
    } else {
        guardarAviso('error', ApiClient::mensaje($r, 'No pudimos cargar los resultados de esa votación.'));
        irA('admin/resultados.php');
    }
}

$tituloPagina = 'Resultados';
$paginaActual = 'resultados';
$panelAncho = true;
require __DIR__ . '/../includes/cabecera.php';
?>

<h1 class="titulo-panel">Resultados</h1>

<?php if ($falloRed): ?>

  <div class="aviso aviso--error" role="alert">
    <svg width="17" height="17" viewBox="0 0 24 24" fill="none"
         stroke="var(--granate)" stroke-width="2" aria-hidden="true">
      <circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16h.01" stroke-linecap="round"/>
    </svg>
    <span><?= e($falloRed) ?> Vuelve a intentarlo en unos minutos.</span>
  </div>

<?php elseif ($resultado): ?>

  <a class="enlace-atras" href="<?= BASE_URL ?>/admin/resultados.php">
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         stroke-width="2" aria-hidden="true">
      <path d="M15 6l-6 6 6 6" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
    Todas las votaciones
  </a>

  <div class="panel-tarjeta">
    <div class="panel-cabecera">
      <h2 class="panel-seccion"><?= e($resultado['titulo'] ?? '') ?></h2>
      <span class="dato"><?= (int) ($resultado['totalVotos'] ?? 0) ?> votos emitidos</span>
    </div>

    <?php
    $opcionesR = $resultado['opciones'] ?? [];
    $total = (int) ($resultado['totalVotos'] ?? 0);
    $maximo = $total > 0 ? max(array_column($opcionesR, 'votos')) : -1;
    ?>

    <?php if (!$opcionesR): ?>
      <div class="vacio">
        <h2>Sin votos todavía</h2>
        <p>Los resultados aparecerán aquí a medida que se emitan votos.</p>
      </div>
    <?php else: ?>
      <?php foreach ($opcionesR as $op):
          $votos = (int) ($op['votos'] ?? 0);
          $pct = $total > 0 ? round($votos / $total * 100) : 0;
          $ganadora = $total > 0 && $votos === $maximo;
      ?>
        <div class="resultado<?= $ganadora ? ' resultado--ganadora' : '' ?>">
          <div class="resultado__cabeza">
            <b><?= e($op['textoOpcion']) ?></b>
            <span><?= $votos ?> voto<?= $votos === 1 ? '' : 's' ?> · <?= $pct ?>%</span>
          </div>
          <progress class="barra-avance" value="<?= $pct ?>" max="100"></progress>
        </div>
      <?php endforeach; ?>
    <?php endif; ?>
  </div>

<?php else: ?>

  <div class="panel-tarjeta">
    <?php if (!$votaciones): ?>
      <div class="vacio">
        <h2>Todavía no hay resultados</h2>
        <p>Aparecerán aquí en cuanto una votación se abra y reciba votos.</p>
      </div>
    <?php else: ?>
      <table class="tabla">
        <thead><tr><th>Votación</th><th>Estado</th><th></th></tr></thead>
        <tbody>
          <?php foreach ($votaciones as $v): ?>
            <tr>
              <td class="tabla__titulo"><?= e($v['titulo']) ?></td>
              <td>
                <span class="insignia <?= strtoupper($v['estado']) === 'ABIERTA' ? 'insignia--abierta' : 'insignia--cerrada' ?>">
                  <?= strtoupper($v['estado']) === 'ABIERTA' ? 'Abierta' : 'Cerrada' ?>
                </span>
              </td>
              <td class="tabla__acciones">
                <a class="boton boton--plano" href="<?= BASE_URL ?>/admin/resultados.php?id=<?= (int) $v['id'] ?>">Ver resultados</a>
              </td>
            </tr>
          <?php endforeach; ?>
        </tbody>
      </table>
    <?php endif; ?>
  </div>

<?php endif; ?>

<?php require __DIR__ . '/../includes/pie.php'; ?>
