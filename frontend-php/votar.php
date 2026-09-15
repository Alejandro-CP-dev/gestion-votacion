<?php
/**
 * votar.php — Las votaciones abiertas y el estado del estudiante en cada una.
 *
 * Tres estados posibles por votación:
 *   habilitado  -> tiene token sin usar, puede votar
 *   ya votó     -> su token está USADO
 *   sin padrón  -> no recibió token para esta votación
 *
 * Ojo con el "ya votó": el backend sabe QUE la persona votó, nunca POR QUÉ
 * opción. Esa distinción es la que permite bloquear la papeleta sin romper el
 * secreto del voto.
 */
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/sesion.php';
require_once __DIR__ . '/includes/ApiClient.php';

requerirSesion();
$usuario = perfil();

$activas = [];
$falloRed = null;

$r = ApiClient::get('/encuestas/activas');

if (ApiClient::ok($r)) {
    $activas = $r['cuerpo'] ?: [];
} else {
    $falloRed = ApiClient::mensaje($r, 'No pudimos consultar las votaciones.');
}

/**
 * Una consulta de estado por votación.
 *
 * Son N+1 llamadas HTTP y lo sé. Con dos o tres votaciones abiertas a la vez
 * es irrelevante, y la alternativa sería un endpoint nuevo que devuelva el
 * estado del votante para todas de golpe. Si algún día hay veinte elecciones
 * simultáneas, ese endpoint es el arreglo; hoy sería complejidad sin motivo.
 */
$estados = [];
foreach ($activas as $e) {
    $res = ApiClient::get('/tokens/estado',
        ['encuestaId' => $e['id'], 'usuarioId' => $usuario['id']], true);

    $estados[$e['id']] = ApiClient::ok($res)
        ? $res['cuerpo']
        : ['tieneToken' => false, 'yaVoto' => false];
}

$tituloPagina = 'Mis votaciones';
$paginaActual = 'votar';
require __DIR__ . '/includes/cabecera.php';
?>

<div class="cabecera-pagina">
  <h1>Votaciones abiertas</h1>
  <p>Participa en las elecciones institucionales en las que estás habilitado.</p>
</div>

<?php if ($falloRed): ?>

  <div class="lista">
    <div class="aviso aviso--error" role="alert">
      <svg width="17" height="17" viewBox="0 0 24 24" fill="none"
           stroke="var(--granate)" stroke-width="2" aria-hidden="true">
        <circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16h.01" stroke-linecap="round"/>
      </svg>
      <span><?= e($falloRed) ?> Vuelve a intentarlo en unos minutos.</span>
    </div>
  </div>

<?php elseif (!$activas): ?>

  <div class="hoja">
    <div class="vacio">
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none"
           stroke="var(--apagado)" stroke-width="1.5" aria-hidden="true">
        <path d="M5 9h14l-1.5 11h-11z" stroke-linejoin="round"/>
        <path d="M9 9V6a3 3 0 016 0v3" stroke-linecap="round"/>
      </svg>
      <h2>No hay votaciones abiertas</h2>
      <p>Cuando el centro abra una elección en la que estés habilitado, aparecerá aquí.</p>
    </div>
  </div>

<?php else: ?>

  <div class="lista">
  <?php foreach ($activas as $encuesta):
      $id      = (int) $encuesta['id'];
      $estado  = $estados[$id];
      $yaVoto  = !empty($estado['yaVoto']);
      $tiene   = !empty($estado['tieneToken']);
      $apagada = $yaVoto || !$tiene;
      $recibo  = $_SESSION['comprobante'][$id] ?? null;
  ?>
    <article class="tarjeta<?= $apagada ? ' tarjeta--apagada' : '' ?>">

      <div class="tarjeta__cabeza">
        <h2 class="tarjeta__titulo"><?= e($encuesta['titulo']) ?></h2>

        <?php if ($yaVoto): ?>
          <span class="insignia insignia--votada">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" stroke-width="3" aria-hidden="true">
              <path d="M5 13l4 4L19 7" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            Ya votaste
          </span>
        <?php elseif (!$tiene): ?>
          <span class="insignia insignia--cerrada">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" stroke-width="2" aria-hidden="true">
              <rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 018 0v3"/>
            </svg>
            Sin habilitar
          </span>
        <?php else: ?>
          <span class="insignia insignia--abierta">Abierta</span>
        <?php endif; ?>
      </div>

      <?php if (!empty($encuesta['descripcion'])): ?>
        <p class="tarjeta__desc"><?= e($encuesta['descripcion']) ?></p>
      <?php endif; ?>

      <div class="tarjeta__pie">
        <?php if ($yaVoto): ?>

          <span class="dato">Tu voto quedó registrado y tu token fue anulado.</span>
          <?php if ($recibo): ?>
            <a class="boton boton--plano" href="<?= BASE_URL ?>/comprobante.php?id=<?= $id ?>">
              Ver mi comprobante
            </a>
          <?php endif; ?>

        <?php elseif (!$tiene): ?>

          <span class="dato">
            No recibiste token para esta votación. Consulta en coordinación académica.
          </span>

        <?php else: ?>

          <a class="boton boton--principal" href="<?= BASE_URL ?>/papeleta.php?id=<?= $id ?>">
            Votar ahora
          </a>
          <span class="dato"><?= count($encuesta['opciones']) ?> opciones</span>

        <?php endif; ?>
      </div>
    </article>
  <?php endforeach; ?>
  </div>

<?php endif; ?>

<?php require __DIR__ . '/includes/pie.php'; ?>
