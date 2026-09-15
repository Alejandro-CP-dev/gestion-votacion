/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package co.edu.sena.cimm.votaciones.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;

/**
 * Proveedor unico del pool de conexiones a MySQL.
 *
 * POR QUE UN POOL Y NO DriverManager.getConnection() EN CADA CONSULTA:
 * abrir una conexion TCP a MySQL tarda entre 20 y 100 ms. En la prueba de
 * concurrencia del taller se disparan dos peticiones al mismo tiempo; si cada
 * una abre su conexion, buena parte del tiempo medido es el saludo con MySQL
 * y no el bloqueo de fila que queremos demostrar. El pool las tiene listas.
 *
 * ORDEN DE PRIORIDAD DE LA CONFIGURACION:
 *   1. Variables de entorno (SVIS_DB_URL, SVIS_DB_USER, SVIS_DB_PASSWORD)
 *   2. Archivo db.properties del classpath
 *   3. Valores por defecto de este archivo
 *
 * Esa jerarquia permite que cada integrante del equipo tenga su propia clave
 * de MySQL sin editar (ni subir a Git) el mismo archivo una y otra vez.
 */
public final class Database {

    /**
     * "vlatile" + doble revision: garantiza que si dos peticiones llegan al
     * mismo tiempo cuendo el pool aun no existe, se construya Una sola vez.
     */
    private static volatile DataSource dataSource;

    public Database() {
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (Database.class) {
                if (dataSource == null) {
                    dataSource = construir();
                }
            }
        }
        return dataSource;
    }

    /**
     * Entrega una conexion del pool. Usarla SIEMPRE dentro de
     * try-with-resources:
     *
     * try (Connection c = Database.getConnection()) { ... }
     *
     * El close() no cierra la conexion, la devuelve al pool. Si se olvida el
     * try-with-resources, el pool se agota a las 5 peticiones y la aplicacion
     * se cuelga esperando.
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    private static DataSource construir() {
        Properties p = cargarPropiedades();

        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
        cfg.setJdbcUrl(valor(p, "db.url", "SVIS_DB_URL",
                "jdbc:mysql://localhost:3306/SvisDb"
                + "?serverTimezone=America/Bogota&useSSL=false&allowPublicKeyRetrieval=true"));
        cfg.setUsername(valor(p, "db.user", "SVIS_DB_USER", "root"));
        cfg.setPassword(valor(p, "db.password", "SVIS_DB_PASSWORD", ""));
        cfg.setMaximumPoolSize(Integer.parseInt(valor(p, "db.poolSize", "SVIS_DB_POOL", "10")));
        cfg.setPoolName("svis-pool");

        cfg.setConnectionTimeout(10000); // si en 10 segundos no hay conexion, da falla

        System.out.println("[SVIS] Pool de conexiones iniciado hacia " + cfg.getJdbcUrl());
        return new HikariDataSource(cfg);
    }

    private static Properties cargarPropiedades() {
        Properties p = new Properties();
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream("db.properties")) {

            if (in != null) {
                p.load(in);
            }

        } catch (IOException e) {

            System.out.println("[SBIS] No se encontro db.properties, se usaran los valores por defecto");
        }
        return p;
    }
    
    private static String valor(Properties p, String clave, String variableEntorno, String porDefecto){
        String valorVariable = System.getenv(variableEntorno);
        if (valorVariable == null || valorVariable.trim().isEmpty()) {
            valorVariable = p.getProperty(clave);
        }
        if (valorVariable == null || valorVariable.trim().isEmpty()) {
            valorVariable = porDefecto;
        }
        return valorVariable.trim();
    }
}
