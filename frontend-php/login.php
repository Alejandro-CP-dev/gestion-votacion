<?php
/**
 * login.php — Autenticación contra POST /api/auth/login
 *
 * Aquí no hay una sola línea de PDO ni de mysqli. El taller exige que el
 * frontend no tenga acceso a las credenciales de MySQL, y la única forma de
 * cumplirlo de verdad es que no exista código capaz de conectarse.
 */
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/sesion.php';
require_once __DIR__ . '/includes/ApiClient.php';

// Quien ya entró no vuelve a ver el login.
if (haySesion()) {
    irA('index.php');
}

$error  = null;
$correo = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    validarCsrf();

    $correo = trim($_POST['correo'] ?? '');
    $clave  = $_POST['clave'] ?? '';

    $r = ApiClient::post('/auth/login', ['correo' => $correo, 'clave' => $clave]);

    if (ApiClient::ok($r) && !empty($r['cuerpo']['id'])) {
        guardarPerfil($r['cuerpo']);
        irA('index.php');
    }

    // El backend ya devuelve el mismo mensaje para "correo inexistente" y
    // "clave incorrecta". El frontend no lo desarma: si aquí se distinguiera,
    // se perdería la protección contra enumeración de usuarios que tanto
    // cuidamos en AuthService.
    $error = ApiClient::mensaje($r, 'No fue posible iniciar sesión.');
}

$tituloPagina = 'Iniciar sesión';
require __DIR__ . '/includes/cabecera.php';
?>

<div class="login">

  <aside class="login__panel">
    <div>
      <svg class="login__forma" width="42" height="42" viewBox="0 0 48 48" fill="none"
           stroke="#39A900" stroke-width="2.5" aria-hidden="true">
        <path d="M24 5l16 6v11c0 10-6.8 17.9-16 21-9.2-3.1-16-11-16-21V11z" stroke-linejoin="round"/>
        <path d="M16 24l6 6 11-12" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <h2>SVIS</h2>
      <p>Sistema de Votaciones y Encuestas Institucionales Seguras</p>
    </div>

    <p class="login__lema">
      Tu voto es único, anónimo e irreversible. El sistema no guarda ninguna
      relación entre tu nombre y tu elección.
    </p>
  </aside>

  <div class="login__caja">
    <h1 class="tarjeta__titulo">Bienvenido</h1>
    <p class="tarjeta__desc">Ingresa con tu correo institucional.</p>

    <?php if ($error): ?>
      <!-- El error va en el flujo de la página, no en un toast flotante:
           un mensaje que se desvanece solo obliga a leer contrarreloj. -->
      <div class="aviso aviso--error espacio-arriba" role="alert">
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none"
             stroke="var(--granate)" stroke-width="2" aria-hidden="true">
          <circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16h.01" stroke-linecap="round"/>
        </svg>
        <span><?= e($error) ?></span>
      </div>
    <?php endif; ?>

    <form method="post" action="login.php" class="espacio-arriba">
      <?= campoCsrf() ?>

      <div class="campo">
        <label for="correo">Correo institucional</label>
        <input class="entrada" type="email" id="correo" name="correo"
               value="<?= e($correo) ?>" required autocomplete="username"
               autofocus <?= $error ? 'aria-invalid="true"' : '' ?>>
      </div>

      <div class="campo">
        <label for="clave">Contraseña</label>
        <input class="entrada" type="password" id="clave" name="clave"
               required autocomplete="current-password"
               <?= $error ? 'aria-invalid="true"' : '' ?>>
      </div>

      <button class="boton boton--principal boton--bloque" type="submit">
        Iniciar sesión
      </button>

      <p class="pista espacio-arriba">
        ¿No recuerdas tu contraseña? Solicítala en coordinación académica.
      </p>
    </form>
  </div>
</div>

<?php require __DIR__ . '/includes/pie.php'; ?>
