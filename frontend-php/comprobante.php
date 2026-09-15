<?php
/**
 * comprobante.php — El recibo anónimo del voto.
 *
 * El comprobante se lee de la sesión. No existe un endpoint para recuperarlo,
 * y eso no es una funcionalidad pendiente: si el sistema pudiera devolverte tu
 * recibo a partir de tu identidad, existiría un vínculo entre persona y voto
 * guardado en alguna parte. La tabla Comprobante no tiene UsuarioId
 * precisamente para que ese vínculo no exista.
 *
 * Consecuencia que el votante debe entender: si cierra sesión sin copiar el
 * código, lo pierde. Por eso la pantalla lo dice en lugar de dar por hecho que
 * podrá volver.
 */
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/sesion.php';

requerirSesion();

$id      = (int) ($_GET['id'] ?? 0);
$recibo  = $_SESSION['comprobante'][$id] ?? null;

if (!$recibo) {
    guardarAviso('alerta', 'No hay un comprobante disponible en esta sesión.');
    irA('votar.php');
}

$tituloPagina = 'Comprobante';
$paginaActual = 'votar';
require __DIR__ . '/includes/cabecera.php';
?>

<div class="hoja">
  <div class="paso es-visible centrado">

    <!-- La única animación de toda la aplicación, en el único momento que la
         merece. Se desactiva sola si el sistema pide reducir movimiento. -->
    <svg class="sello" viewBox="0 0 80 80" aria-hidden="true">
      <circle cx="40" cy="40" r="36"/>
      <path d="M26 41l10 10 19-20"/>
    </svg>

    <h1>Tu voto fue registrado</h1>
    <p>Guarda este código. Confirma que tu voto se contó, sin revelar por quién votaste.</p>

    <div class="codigo">
      <code id="codigoRecibo"><?= e($recibo['codigo']) ?></code>
      <button class="copiar" type="button" data-copiar="codigoRecibo">Copiar</button>
    </div>

    <dl class="resumen">
      <div class="resumen__fila">
        <dt>Votación</dt>
        <dd><?= e($recibo['titulo']) ?></dd>
      </div>
      <div class="resumen__fila">
        <dt>Registrado el</dt>
        <dd><?= e($recibo['fecha']) ?></dd>
      </div>
    </dl>

    <!-- Fíjate en lo que NO aparece en esta pantalla: la opción que eligió.
         El votante la conoce; el sistema no tiene por qué repetírsela ni
         hacerla viajar por la red otra vez. -->

    <div class="aviso aviso--alerta espacio-arriba">
      <svg width="17" height="17" viewBox="0 0 24 24" fill="none"
           stroke="var(--ambar)" stroke-width="2" aria-hidden="true">
        <path d="M12 3l9 16H3z" stroke-linejoin="round"/>
        <path d="M12 10v4M12 17h.01" stroke-linecap="round"/>
      </svg>
      <span>
        Cópialo antes de cerrar sesión. El sistema no puede volver a mostrártelo:
        no guarda ninguna relación entre tu identidad y este comprobante.
      </span>
    </div>

    <div class="acciones acciones--centro">
      <a class="boton boton--plano" href="<?= BASE_URL ?>/votar.php">
        Volver a mis votaciones
      </a>
    </div>
  </div>
</div>

<?php require __DIR__ . '/includes/pie.php'; ?>
