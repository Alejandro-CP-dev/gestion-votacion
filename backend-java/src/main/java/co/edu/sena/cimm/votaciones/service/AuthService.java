package co.edu.sena.cimm.votaciones.service;

import co.edu.sena.cimm.votaciones.dto.LoginRequest;
import co.edu.sena.cimm.votaciones.dto.NegocioException;
import co.edu.sena.cimm.votaciones.dto.UsuarioView;
import co.edu.sena.cimm.votaciones.model.EstadoUsuario;
import co.edu.sena.cimm.votaciones.model.Usuario;
import co.edu.sena.cimm.votaciones.repository.UsuarioRepository;
import co.edu.sena.cimm.votaciones.util.ClaveUtil;
import java.util.Optional;

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
    
    
    private boolean claveCoincide(String claveEnTextoPlano, String hashGuardado) {
        return ClaveUtil.coincide(claveEnTextoPlano, hashGuardado);
    }
    
    
}
