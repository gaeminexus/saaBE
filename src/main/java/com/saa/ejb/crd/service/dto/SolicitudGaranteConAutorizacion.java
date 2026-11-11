package com.saa.ejb.crd.service.dto;

import java.io.InputStream;

/**
 * Cuerpo de {@code GarantePrestamoService.crearConAutorizacion}. Lo arma {@code GarantePrestamoRest} a partir de un
 * multipart — no viaja como JSON. Garante de un préstamo ya otorgado: exige la autorización firmada del garante.
 * Ver DISENO-PAGO-CON-APORTES-DEL-GARANTE.md §G4.
 */
public class SolicitudGaranteConAutorizacion {

    private Long idPrestamo;

    /** Partícipe garante (CRD.ENTD). */
    private Long idEntidad;

    private InputStream archivo;

    /** Nombre original del archivo, ya decodificado (URLDecoder, UTF-8) por el REST. */
    private String nombreArchivo;

    /** Quien registra; tiene que existir en el sistema. */
    private String usuario;

    public SolicitudGaranteConAutorizacion() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public InputStream getArchivo() {
        return archivo;
    }

    public void setArchivo(InputStream archivo) {
        this.archivo = archivo;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
}
