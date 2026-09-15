/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package co.edu.sena.cimm.votaciones.model;

/**
 *
 * @author Usuario
 */
public class Usuario {

    private int Id;
    private String Nombre;
    private String Apellido;
    private String Correo;
    private String Telefono;
    private String Clave;
    private RolUsuario Rol;
    private EstadoUsuario Estado;

    public Usuario() {
    }

    public Usuario(int Id, String Nombre, String Apellido, String Correo, String Telefono, String Clave, RolUsuario Rol, EstadoUsuario Estado) {
        this.Id = Id;
        this.Nombre = Nombre;
        this.Apellido = Apellido;
        this.Correo = Correo;
        this.Telefono = Telefono;
        this.Clave = Clave;
        this.Rol = Rol;
        this.Estado = Estado;
    }

    public int getId() {
        return Id;
    }

    public void setId(int Id) {
        this.Id = Id;
    }

    public String getNombre() {
        return Nombre;
    }

    public void setNombre(String Nombre) {
        this.Nombre = Nombre;
    }

    public String getApellido() {
        return Apellido;
    }

    public void setApellido(String Apellido) {
        this.Apellido = Apellido;
    }

    public String getCorreo() {
        return Correo;
    }

    public void setCorreo(String Correo) {
        this.Correo = Correo;
    }

    public String getTelefono() {
        return Telefono;
    }

    public void setTelefono(String Telefono) {
        this.Telefono = Telefono;
    }

    public String getClave() {
        return Clave;
    }

    public void setClave(String Clave) {
        this.Clave = Clave;
    }

    public RolUsuario getRol() {
        return Rol;
    }

    public void setRol(RolUsuario Rol) {
        this.Rol = Rol;
    }

    public EstadoUsuario getEstado() {
        return Estado;
    }

    public void setEstado(EstadoUsuario Estado) {
        this.Estado = Estado;
    }

}
