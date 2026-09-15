package co.edu.sena.cimm.votaciones.servlet;

import co.edu.sena.cimm.votaciones.config.AppContext;
import co.edu.sena.cimm.votaciones.dto.LoginRequest;
import co.edu.sena.cimm.votaciones.dto.UsuarioView;
import co.edu.sena.cimm.votaciones.service.AuthService;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * POST /api/auth/login
 *
 * Entrada:  { "correo": "admin@cimm.edu.co", "clave": "admin123" }
 * Salida:   { "id":1, "nombre":"Osman", "apellido":"Aranguren",
 *             "correo":"admin@cimm.edu.co", "rol":"ADMIN" }
 *
 * Mira el tamano del metodo doPost: cuatro lineas. Un servlet traduce HTTP a
 * llamadas de servicio y nada mas. En el momento en que aparezca un "if" de
 * negocio aqui dentro, esa regla quedo en un sitio donde no se puede probar
 * sin levantar Tomcat.
 *
 * Este endpoint existe porque el taller dice que el frontend NO tiene acceso a
 * las credenciales de MySQL. Un login hecho con PDO desde PHP incumpliria esa
 * condicion aunque funcionara perfecto.
 */
@WebServlet("/api/auth/login")
public class AuthServlet extends BaseApiServlet{
    
    private AuthService authService;
    
    /**
     * init() lo llama Tomcat una sola vez al crear el servlet. Pedir la
     * dependencia aqui y no en cada doPost evita resolverla en cada peticion.
     */
    @Override
    public void init() {
        this.authService = AppContext.getAuthService();
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            LoginRequest peticion = leerJson(req, LoginRequest.class);
            UsuarioView perfil = authService.autenticar(peticion);
            escribirJson(resp, 200, perfil);
 
        } catch (Exception ex) {
            // Un solo catch. La traduccion a 400 / 404 / 409 / 500 la hace
            // responderError() en BaseApiServlet, igual para todo el API.
            responderError(resp, ex);
        }
    }
}
