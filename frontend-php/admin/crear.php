<?php
/**
 * admin/crear.php — Alta de una votación nueva.
 *
 * La votación nace en estado CREADA: sin votos posibles todavía. Abrirla es
 * una decisión aparte, deliberada, que se toma en admin/gestionar.php — así
 * el administrador puede generar el padrón con calma antes de que cualquier
 * estudiante pueda votar.
 */
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../includes/sesion.php';
require_once __DIR__ . '/../includes/ApiClient.php';

requerirAdmin();

$titulo = '';
$descripcion = '';
$opciones = ['', ''];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    validarCsrf();

    $titulo = trim($_POST['titulo'] ?? '');
    $descripcion = trim($_POST['descripcion'] ?? '');
    $opcionesEnviadas = array_map('trim', $_POST['opciones'] ?? []);
    $opcionesValidas = array_values(array_filter($opcionesEnviadas, fn($o) => $o !== ''));

    if ($titulo === '' || count($opcionesValidas) < 2) {
        guardarAviso('error', 'La votación necesita un título y al menos dos opciones.');
        irA('admin/crear.php');
    }

    $payload = [
        'titulo' => $titulo,
        'descripcion' => $descripcion,
        'creadoPor' => (int) perfil()['id'],
        'opciones' => $opcionesValidas,
    ];

    $r = ApiClient::post('/encuestas', $payload);

    if (ApiClient::ok($r)) {
        guardarAviso('exito', 'La votación "' . $titulo . '" fue creada. Ya puedes generar su padrón.');
        irA('admin/gestionar.php');
    }

    if ($r['codigo'] === 409) {
        // Ya existe una votación con ese título, o alguna otra regla de
        // negocio del backend: no es un fallo del sistema, es una respuesta.
        guardarAviso('alerta', ApiClient::mensaje($r, 'Ya existe una votación con esos datos.'));
        irA('admin/crear.php');
    }

    guardarAviso('error', ApiClient::mensaje($r, 'No fue posible crear la votación.'));
    irA('admin/crear.php');
}

$tituloPagina = 'Crear votación';
$paginaActual = 'crear';
$panelAncho = true;
require __DIR__ . '/../includes/cabecera.php';
?>

<h1 class="titulo-panel">Crear votación</h1>

<div class="panel-tarjeta">
  <form method="post" action="<?= BASE_URL ?>/admin/crear.php" data-envio-simple>
    <?= campoCsrf() ?>

    <div class="campo">
      <label for="titulo">Título</label>
      <input class="entrada" type="text" id="titulo" name="titulo"
             value="<?= e($titulo) ?>" required maxlength="150" autofocus>
    </div>

    <div class="campo">
      <label for="descripcion">Descripción (opcional)</label>
      <textarea class="entrada" id="descripcion" name="descripcion" rows="3"
                maxlength="500"><?= e($descripcion) ?></textarea>
    </div>

    <fieldset class="espacio-arriba">
      <legend>Opciones</legend>

      <div data-opciones data-minimo="2">
        <?php foreach ($opciones as $indice => $valor): ?>
          <div class="fila-opcion" data-fila-opcion>
            <div class="campo">
              <label><span data-numero-opcion>Opción <?= $indice + 1 ?></span>
                <input class="entrada" type="text" name="opciones[]" value="<?= e($valor) ?>"
                       maxlength="150" required>
              </label>
            </div>
            <button class="boton boton--plano boton--icono" type="button"
                    data-quitar-opcion hidden aria-label="Quitar esta opción">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" aria-hidden="true">
                <path d="M6 6l12 12M18 6L6 18" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
        <?php endforeach; ?>
      </div>

      <button class="boton boton--plano" type="button" data-agregar-opcion>
        + Agregar opción
      </button>
    </fieldset>

    <div class="acciones">
      <button class="boton boton--principal" type="submit" data-cargando="Creando…">
        <span class="boton__rueda" aria-hidden="true"></span>
        <span data-texto>Crear votación</span>
      </button>
    </div>
  </form>
</div>

<?php require __DIR__ . '/../includes/pie.php'; ?>
