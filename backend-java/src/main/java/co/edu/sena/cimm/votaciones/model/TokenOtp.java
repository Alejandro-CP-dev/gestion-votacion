/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package co.edu.sena.cimm.votaciones.model;

import java.time.LocalDateTime;

/**
 *
 * @author Usuario
 */
public class TokenOtp {
    
    private long id;
    private int EncuestaId;
    private int UsuarioId;
    private String TokenHash;
    private String TokenCifrado;
    private EstadoToken Estado;
    private LocalDateTime fechaExpiracion;
    private LocalDateTime UsadoEn;

    public TokenOtp() {
    }

    public TokenOtp(long id, int EncuestaId, int UsuarioId, String TokenHash, EstadoToken Estado, LocalDateTime fechaExpiracion, LocalDateTime UsadoEn) {
        this.id = id;
        this.EncuestaId = EncuestaId;
        this.UsuarioId = UsuarioId;
        this.TokenHash = TokenHash;
        this.Estado = Estado;
        this.fechaExpiracion = fechaExpiracion;
        this.UsadoEn = UsadoEn;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getEncuestaId() {
        return EncuestaId;
    }

    public void setEncuestaId(int EncuestaId) {
        this.EncuestaId = EncuestaId;
    }

    public int getUsuarioId() {
        return UsuarioId;
    }

    public void setUsuarioId(int UsuarioId) {
        this.UsuarioId = UsuarioId;
    }

    public String getTokenHash() {
        return TokenHash;
    }

    public void setTokenHash(String TokenHash) {
        this.TokenHash = TokenHash;
    }

    public String getTokenCifrado() {
        return TokenCifrado;
    }

    public void setTokenCifrado(String TokenCifrado) {
        this.TokenCifrado = TokenCifrado;
    }

    public EstadoToken getEstado() {
        return Estado;
    }

    public void setEstado(EstadoToken Estado) {
        this.Estado = Estado;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public LocalDateTime getUsadoEn() {
        return UsadoEn;
    }

    public void setUsadoEn(LocalDateTime UsadoEn) {
        this.UsadoEn = UsadoEn;
    }
    
    
}
