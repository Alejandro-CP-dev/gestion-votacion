<?php
/**
 * generar_token.php — Único endpoint AJAX del sitio.
 *
 * Todo lo demás en este proyecto es formularios normales (POST-Redirect-GET),
 * a propósito: sin JavaScript, todo sigue funcionando. Esta es la excepción
 * porque recargar papeleta.php perdería la opción que la persona ya había
 * marcado. Por eso responde JSON en vez de HTML, y por eso app.js lo llama
 * con fetch() en lugar de un <form> normal.
 *
 * Lo que SÍ se mantiene igual que el resto del sitio: sesión obligatoria,
 * CSRF de sesión, y el backend Java como único que sabe hablar con MySQL.
 */
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/sesion.php';
require_once __DIR__ . '/includes/ApiClient.php';

header('Content-Type: application/json; charset=UTF-8');

function responder(int $estado, array $cuerpo): void
{
    http_response_code($estado);
    echo json_encode($cuerpo, JSON_UNESCAPED_UNICODE);
    exit;
}

if (!haySesion()) {
    responder(401, ['ok' => false, 'mensaje' => 'Inicia sesión para continuar.']);
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    responder(405, ['ok' => false, 'mensaje' => 'Método no permitido.']);
}

$csrf = $_POST['_csrf'] ?? '';
if (empty($_SESSION['csrf']) || !hash_equals($_SESSION['csrf'], (string) $csrf)) {
    responder(403, ['ok' => false, 'mensaje' => 'Tu sesión expiró o la página se recargó. Recarga e intenta de nuevo.']);
}

$encuestaId = (int) ($_POST['encuestaId'] ?? 0);
if ($encuestaId <= 0) {
    responder(400, ['ok' => false, 'mensaje' => 'Votación inválida.']);
}

$usuario = perfil();

$r = ApiClient::post('/tokens/generar-propio', [
    'encuestaId' => $encuestaId,
    'usuarioId'  => (int) ($usuario['id'] ?? 0),
]);

if (ApiClient::ok($r)) {
    responder(200, ['ok' => true, 'token' => $r['cuerpo']['token'] ?? '']);
}

responder($r['codigo'] ?: 500, [
    'ok' => false,
    'mensaje' => ApiClient::mensaje($r, 'No fue posible generar tu token.'),
]);
