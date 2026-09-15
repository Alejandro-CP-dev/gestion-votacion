<?php
/**
 * includes/ApiClient.php — El único archivo del frontend que hace una
 * petición HTTP hacia el backend Java.
 *
 * Nada de PDO, nada de mysqli, en ningún otro archivo de este proyecto: si el
 * frontend no tiene código capaz de hablar SQL, no puede filtrar credenciales
 * de base de datos aunque quisiera. Toda página que necesite datos pasa por
 * aquí, y aquí es donde se ajusta la URL del backend si cambia.
 */
class ApiClient
{
    private const TIMEOUT = 8;

    public static function get(string $ruta, ?array $query = null, bool $silencioso = false): array
    {
        $url = API_BASE_URL . $ruta;
        if ($query) {
            $url .= '?' . http_build_query($query);
        }
        return self::enviar('GET', $url, null, $silencioso);
    }

    public static function post(string $ruta, array $datos = [], bool $silencioso = false): array
    {
        return self::enviar('POST', API_BASE_URL . $ruta, $datos, $silencioso);
    }

    public static function put(string $ruta, array $datos = [], bool $silencioso = false): array
    {
        return self::enviar('PUT', API_BASE_URL . $ruta, $datos, $silencioso);
    }

    /** 2xx y hubo respuesta del backend. Un fallo de red (codigo 0) nunca es "ok". */
    public static function ok(array $respuesta): bool
    {
        return $respuesta['codigo'] >= 200 && $respuesta['codigo'] < 300;
    }

    /**
     * El mensaje que el backend puso en el cuerpo, o uno genérico si no hay
     * ninguno (fallo de red, cuerpo vacío, cuerpo sin esa forma).
     */
    public static function mensaje(array $respuesta, string $porDefecto): string
    {
        $cuerpo = $respuesta['cuerpo'] ?? null;
        if (is_array($cuerpo) && !empty($cuerpo['mensaje'])) {
            return (string) $cuerpo['mensaje'];
        }
        if (is_array($cuerpo) && !empty($cuerpo['error'])) {
            return (string) $cuerpo['error'];
        }
        return $porDefecto;
    }

    private static function enviar(string $metodo, string $url, ?array $datos, bool $silencioso): array
    {
        $ch = curl_init($url);

        $cabeceras = ['Accept: application/json', 'X-Svis-Key: ' . API_KEY];
        $opciones = [
            CURLOPT_CUSTOMREQUEST  => $metodo,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CONNECTTIMEOUT => self::TIMEOUT,
            CURLOPT_TIMEOUT        => self::TIMEOUT,
        ];

        if ($datos !== null) {
            $cabeceras[] = 'Content-Type: application/json';
            $opciones[CURLOPT_POSTFIELDS] = json_encode($datos, JSON_UNESCAPED_UNICODE);
        }

        $opciones[CURLOPT_HTTPHEADER] = $cabeceras;
        curl_setopt_array($ch, $opciones);

        $crudo = curl_exec($ch);

        if ($crudo === false) {
            // No es un 4xx ni un 5xx: es que el backend no contestó. El código
            // 0 no existe en HTTP, así que ApiClient::ok() jamás lo confunde
            // con un éxito ni con un error de negocio del backend.
            if (!$silencioso) {
                error_log('ApiClient: fallo de red hacia ' . $url . ' — ' . curl_error($ch));
            }
            curl_close($ch);
            return ['codigo' => 0, 'cuerpo' => null];
        }

        $codigo = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        return ['codigo' => $codigo, 'cuerpo' => json_decode($crudo, true)];
    }
}
