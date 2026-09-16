<?php
/**
 * papeleta.php — La pantalla donde se emite el voto.
 *
 * DOS DECISIONES QUE VALE LA PENA PODER DEFENDER:
 *
 * 1. La encuesta se carga desde GET /api/encuestas/activas, no desde
 *    /api/encuestas/{id}. No es por comodidad: el endpoint público solo
 *    devuelve las ACTIVAS, así que si alguien escribe a mano el id de una
 *    encuesta cerrada, simplemente no la encuentra. La restricción la impone
 *    el backend, no un "if" de esta página.
 *
 * 2. Patrón POST-Redirect-GET en todas las salidas. Sin él, un F5 después de
 *    votar reenvía el formulario, el backend responde 409 correctamente, y el
 *    votante ve un error aunque su voto SÍ quedó registrado.
 */
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/sesion.php';
require_once __DIR__ . '/includes/ApiClient.php';

requerirSesion();
$usuario = perfil();

$id = (int) ($_POST['encuestaId'] ?? $_GET['id'] ?? 0);
if ($id <= 0) {
    irA('votar.php');
}

/* ---------- Cargar la votación ---------- */
$r = ApiClient::get('/encuestas/activas');

if (!ApiClient::ok($r)) {
    guardarAviso('error', ApiClient::mensaje($r, 'No pudimos cargar la votación.'));
    irA('votar.php');
}

$encuesta = null;
foreach (($r['cuerpo'] ?: []) as $e) {
    if ((int) $e['id'] === $id) {
        $encuesta = $e;
        break;
    }
}

if (!$encuesta) {
    guardarAviso('alerta', 'Esa votación no está abierta en este momento.');
    irA('votar.php');
}

/* ---------- ¿Puede votar? ----------
 * Ya no bloquea a quien no tiene token todavía: puede generarlo aquí mismo
 * con el botón "Generar mi token" (ver más abajo). Solo se bloquea a quien
 * ya votó, porque eso sí es definitivo.
 */
$est = ApiClient::get('/tokens/estado',
    ['encuestaId' => $id, 'usuarioId' => $usuario['id']], true);

$tieneToken = false;
if (ApiClient::ok($est)) {
    if (!empty($est['cuerpo']['yaVoto'])) {
        guardarAviso('alerta', 'Ya emitiste tu voto en esta votación.');
        irA('votar.php');
    }
    $tieneToken = !empty($est['cuerpo']['tieneToken']);
}

/**
 * Si el administrador ya generó el padrón con el token de esta persona (o
 * ella ya lo había generado antes), se lo mostramos aquí mismo: no tiene por
 * qué haberlo recibido en papel para poder votar. Es la misma respuesta que
 * antes solo veía el administrador en el momento de generar el padrón, ahora
 * descifrada bajo demanda para el dueño del token.
 *
 * Si no llega nada (expiró, por ejemplo) simplemente no se muestra: no es un
 * error, tieneToken ya dice que la persona está habilitada.
 */
$miToken = null;
if ($tieneToken) {
    $rt = ApiClient::get('/tokens/mio',
        ['encuestaId' => $id, 'usuarioId' => $usuario['id']], true);

    if (ApiClient::ok($rt)) {
        $miToken = $rt['cuerpo']['token'] ?? null;
    }
}

/* ---------- Emisión ---------- */
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    validarCsrf();

    $opcionId = (int) ($_POST['opcionId'] ?? 0);
    $token    = trim($_POST['token'] ?? '');

    $res = ApiClient::post('/votos/emitir', ['token' => $token, 'opcionId' => $opcionId]);

    if (ApiClient::ok($res) && !empty($res['cuerpo']['codigoRecibo'])) {

        // El comprobante vive en la sesión y en ningún otro lado.
        // No hay endpoint para recuperarlo después, y es a propósito: si el
        // sistema pudiera devolverte tu recibo a partir de tu identidad,
        // existiría un vínculo entre persona y voto.
        $_SESSION['comprobante'][$id] = [
            'codigo' => $res['cuerpo']['codigoRecibo'],
            'titulo' => $encuesta['titulo'],
            'fecha'  => date('d/m/Y H:i'),
        ];
        irA('comprobante.php?id=' . $id);
    }

    if ($res['codigo'] === 409) {
        // No es un fallo: es el sistema cumpliendo la Regla 3. Por eso el
        // aviso es de tipo "alerta" y no de tipo "error", y el texto no
        // menciona códigos HTTP ni excepciones.
        guardarAviso('alerta', ApiClient::mensaje($res, 'Este token ya fue utilizado.'));
        irA('votar.php');
    }

    guardarAviso('error', ApiClient::mensaje($res, 'No fue posible registrar tu voto.'));
    irA('papeleta.php?id=' . $id);
}

$tituloPagina = $encuesta['titulo'];
$paginaActual = 'votar';
require __DIR__ . '/includes/cabecera.php';
?>

<a class="enlace-atras" href="<?= BASE_URL ?>/votar.php">
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
       stroke-width="2" aria-hidden="true">
    <path d="M15 6l-6 6 6 6" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>
  Volver a mis votaciones
</a>

<div class="hoja">
  <div class="encabezado">
    <h1><?= e($encuesta['titulo']) ?></h1>
    <?php if (!empty($encuesta['descripcion'])): ?>
      <p><?= e($encuesta['descripcion']) ?></p>
    <?php endif; ?>
  </div>

  <form class="paso es-visible" method="post" action="papeleta.php"
        data-confirmar="telonVoto">

    <?= campoCsrf() ?>
    <input type="hidden" name="encuestaId" value="<?= $id ?>">

    <!-- ===== Opciones ===== -->
    <fieldset>
      <legend>Selecciona una opción</legend>

      <?php foreach ($encuesta['opciones'] as $op): ?>
        <label class="opcion">
          <input type="radio" name="opcionId" value="<?= (int) $op['id'] ?>"
                 data-etiqueta="<?= e($op['textoOpcion']) ?>" required>
          <span class="marca" aria-hidden="true"></span>
          <span>
            <span class="opcion__texto"><?= e($op['textoOpcion']) ?></span>
            <span class="opcion__nota">Tarjetón N.° <?= (int) $op['orden'] ?></span>
          </span>
        </label>
      <?php endforeach; ?>
    </fieldset>

    <?php if ($tieneToken && $miToken): ?>
    <!-- ===== Ver mi token =====
         El administrador ya generó el token de esta persona en el padrón.
         Queda oculto detrás de <details>: es un widget nativo del navegador
         (se puede abrir sin nada de JavaScript), así que no hace falta
         replicar en app.js lo que el propio HTML ya sabe hacer. La razón de
         no mostrarlo directo es que la pantalla puede estar siendo mirada
         por alguien más; que cada quien decida cuándo revelarlo. -->
    <div class="campo espacio-arriba">
      <div class="campo__rotulo">
        <label>Tu token</label>
        <span>Asignado para esta votación</span>
      </div>

      <details class="detalle-token">
        <summary class="boton boton--plano">Ver mi token</summary>
        <div class="codigo espacio-arriba" data-resultado-token>
          <code id="miTokenAsignado"><?= e($miToken) ?></code>
          <button class="copiar" type="button" data-copiar="miTokenAsignado">Copiar</button>
        </div>
      </details>

      <p class="pista">
        Cópialo y pégalo abajo para emitir tu voto. Solo puede usarse una vez.
      </p>
    </div>
    <?php elseif (!$tieneToken): ?>
    <!-- ===== Generar mi token =====
         Solo aparece si la persona todavia no tiene uno para esta votacion.
         Con JS, queda oculto hasta que se elige una opcion (ver app.js);
         sin JS, se ve siempre, porque la version sin JavaScript nunca debe
         ser la mas restrictiva. -->
    <div class="campo espacio-arriba" data-generar-token data-encuesta-id="<?= $id ?>">
      <div class="campo__rotulo">
        <label>Tu token</label>
        <span>Se genera una sola vez</span>
      </div>

      <button class="boton boton--plano" type="button" data-boton-generar>
        Generar mi token
      </button>

      <div class="codigo espacio-arriba" data-resultado-token hidden>
        <code id="tokenGenerado"></code>
        <button class="copiar" type="button" data-copiar="tokenGenerado">Copiar</button>
      </div>

      <p class="pista es-error" data-error-token hidden></p>

      <p class="pista">
        Cópialo y pégalo abajo para emitir tu voto. No podrás generar otro
        para esta votación.
      </p>
    </div>
    <?php endif; ?>

    <!-- ===== Token ===== -->
    <div class="campo espacio-arriba">
      <div class="campo__rotulo">
        <label for="celda0">Token de votación</label>
        <span>10 caracteres</span>
      </div>

      <!--
        El token NO se enmascara. Una contraseña se oculta porque la persona
        la sabe de memoria; aquí la está transcribiendo desde un papel, y si
        no ve lo que escribió no puede verificarlo. Ocultarlo no añadiría
        seguridad y sí añadiría votos fallidos.
      -->
      <div class="token" data-token data-destino="tokenValor"
           data-pista="pistaToken" data-boton="btnEmitir">
        <input id="celda0" data-celda maxlength="1" autocomplete="off" aria-label="Carácter 1">
        <input data-celda maxlength="1" autocomplete="off" aria-label="Carácter 2">
        <input data-celda maxlength="1" autocomplete="off" aria-label="Carácter 3">
        <input data-celda maxlength="1" autocomplete="off" aria-label="Carácter 4">
        <input data-celda maxlength="1" autocomplete="off" aria-label="Carácter 5">
        <span class="token__sep" aria-hidden="true">–</span>
        <input data-celda maxlength="1" autocomplete="off" aria-label="Carácter 6">
        <input data-celda maxlength="1" autocomplete="off" aria-label="Carácter 7">
        <input data-celda maxlength="1" autocomplete="off" aria-label="Carácter 8">
        <input data-celda maxlength="1" autocomplete="off" aria-label="Carácter 9">
        <input data-celda maxlength="1" autocomplete="off" aria-label="Carácter 10">
      </div>

      <input type="hidden" name="token" id="tokenValor">

      <noscript>
        <!-- Sin JavaScript las celdas no se pueden unir, así que se ofrece un
             campo normal. El name duplicado no colisiona: cuando el navegador
             tiene scripting activo, el contenido de noscript no se convierte
             en elementos del DOM. -->
        <input class="entrada" type="text" name="token" maxlength="12"
               placeholder="ABCDE-FGHJK" autocomplete="off">
      </noscript>

      <p class="pista" id="pistaToken">
        Escribe el código impreso en tu volante. Puedes pegarlo completo.
      </p>
    </div>

    <!-- ===== Aviso ===== -->
    <div class="aviso espacio-arriba">
      <svg width="17" height="17" viewBox="0 0 24 24" fill="none"
           stroke="var(--verde-t)" stroke-width="2" aria-hidden="true">
        <circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01" stroke-linecap="round"/>
      </svg>
      <span>
        <b>Tu voto es anónimo y solo se emite una vez.</b> El sistema no guarda
        ninguna relación entre tu nombre y tu elección, y tu token quedará anulado.
      </span>
    </div>

    <div class="acciones">
      <button class="boton boton--grave" type="submit" id="btnEmitir"
              data-cargando="Registrando tu voto…">
        <span class="boton__rueda" aria-hidden="true"></span>
        <span data-texto>Emitir mi voto</span>
      </button>
    </div>
  </form>
</div>

<!-- ===== Confirmación ===== -->
<div class="telon" id="telonVoto">
  <div class="modal" role="dialog" aria-modal="true" aria-labelledby="tituloConfirmar">
    <h2 id="tituloConfirmar">Confirma tu voto</h2>
    <p class="tarjeta__desc">Vas a votar por</p>

    <div class="modal__elegida" data-eco></div>

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
      <button class="boton boton--grave boton--bloque" type="button" data-aceptar>Sí, votar</button>
    </div>
  </div>
</div>

<?php require __DIR__ . '/includes/pie.php'; ?>
