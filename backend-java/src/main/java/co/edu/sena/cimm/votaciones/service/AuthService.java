package co.edu.sena.cimm.votaciones.service;

import co.edu.sena.cimm.votaciones.dto.LoginRequest;
import co.edu.sena.cimm.votaciones.dto.NegocioException;
import co.edu.sena.cimm.votaciones.dto.UsuarioView;
import co.edu.sena.cimm.votaciones.model.EstadoUsuario;
import co.edu.sena.cimm.votaciones.model.Usuario;
import co.edu.sena.cimm.votaciones.repository.UsuarioRepository;
import java.util.Optional;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Reglas de negocio de la autenticacion.
 *
 * Fijate en lo que NO hay en esta clase: ni una linea de SQL, ni un import de
 * java.sql, ni una mencion a HttpServletRequest. No sabe de donde vienen los
 * datos ni a donde van. Esa es toda la razon de existir de la capa de servicio,
 * y es lo que permite responder "y si manana cambian de MySQL a PostgreSQL?"
 * con un "no toco esta clase".
 */
public class AuthService {
    private final UsuarioRepository usuarioRepo;

    public AuthService(UsuarioRepository usuarioRepo) {
        this.usuarioRepo = usuarioRepo;
    }
    
    public UsuarioView autenticar(LoginRequest peticion){
        
        if (peticion == null
                || peticion.correo == null || peticion.correo.trim().isEmpty()
                || peticion.clave == null || peticion.clave.trim().isEmpty()) {
            throw new NegocioException("El correo y clave son obligatorios");
        }

        String correo = peticion.correo.trim().toLowerCase();
        Optional<Usuario> encontrado = usuarioRepo.buscarPorCorreo(correo);

        if (!encontrado.isPresent()) {
            throw new NegocioException("Correo o claves incorrectos");
        }

        Usuario usuario = encontrado.get();

        if (!claveCoincide(peticion.clave, usuario.getClave())) {
            throw new NegocioException("Correo o clave incorrectos");
        }
        
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new NegocioException("Su cuenta esta inactiva. Consulte con el administrador");
        }
        
        // Se devuelve el DTO, no el Usuario: el hash de la clave no sale de aqui.
        return UsuarioView.desde(usuario);
    }
    
    
    /**
     * Verifica la clave contra el hash BCrypt guardado.
     *
     * BCrypt vuelve a hashear la clave usando la sal que viene incrustada en el
     * propio hash y compara. Por eso nunca se hashea la clave por separado para
     * luego compararla con "equals": cada hash de BCrypt lleva su propia sal y
     * dos hashes de la misma clave son distintos entre si.
     *
     * Lo del prefijo: PHP con password_hash() genera hashes "$2y$...", Java con
     * jBCrypt genera "$2a$...". Es el mismo algoritmo; el prefijo 2y solo marca
     * una correccion de 2011 en la implementacion en C. Normalizarlo evita
     * perder una tarde si algun usuario queda creado desde PHP.
     */
    private boolean claveCoincide(String claveEnTextoPlano, String hashGuardado) {
        if (hashGuardado == null || hashGuardado.length() < 4) {
            return false;
        }
        String hash = hashGuardado.startsWith("$2y$")
                ? "$2a$" + hashGuardado.substring(4)
                : hashGuardado;
 
        return BCrypt.checkpw(claveEnTextoPlano, hash);
    }
    
    
}
