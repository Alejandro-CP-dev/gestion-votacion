package co.edu.sena.cimm.votaciones.servlet;


import co.edu.sena.cimm.votaciones.repository.Database;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *GET/api/salud
 * 
 * Responde si esl WAR quedo desplegado y si la base de datos contesta.
 * 
 * 
 * @author Jhon Cardenas
 */
@WebServlet("/api/salud")
public class SaludServlet extends BaseApiServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("servicio", "SVIS API");
        estado.put("servletFunciona", true);
        
        try (Connection conn = Database.getConnection()){
            PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM Usuario");
            ResultSet rs = ps.executeQuery();
            
            rs.next();
            estado.put("baseDatos", "CONECTADA");
            estado.put("usuariosRegistrados", rs.getLong(1));
            estado.put("autoCommitPorDefecto", conn.getAutoCommit());
            escribirJson(resp, 200, estado);
            
        } catch (Exception e) {
            estado.put("baseDatos","SIN CONEXION");
            estado.put("detalle", e.getMessage());
            escribirJson(resp, 503, estado);
        }
    }
}
