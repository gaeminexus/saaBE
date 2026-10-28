package com.saa.model.cnt;

import java.io.Serializable;

/**
 * Peticion de {@code POST /rest/asnt/cargarManuales}: un archivo con el formato de la plantilla de asientos
 * manuales (fecha;numero_asiento;glosa;cuenta;nombre_cuenta;debe;haber;documento_respaldo;centro_costo) enviado
 * como texto. POJO de transporte, sin @Entity (precedente: {@link ResultadoCargaAsientosManuales}).
 * docs/logica-negocio/cnt/CARGADOR-ASIENTOS-MANUALES.md.
 */
@SuppressWarnings("serial")
public class SolicitudCargaAsientosManuales implements Serializable {

    /** Empresa contable (PJRQCDGO). Obligatorio. */
    private Long idEmpresa;

    /** Usuario que registra los asientos (ASNTUSRO). Obligatorio. */
    private String usuario;

    /** Nombre del archivo que se carga; va en la observacion de cada asiento (trazabilidad e idempotencia). */
    private String nombreArchivo;

    /**
     * Codigo alterno del tipo de asiento (CNT.PLNT.PLNTCDAL). Nulo o 0 = tipo por nombre
     * ({@link #nombreTipoAsiento}, por defecto DIARIO). 10 = APERTURA ({@code TipoAsientos.APERTURA}).
     */
    private Integer codigoAltTipoAsiento;

    /**
     * Nombre del tipo de asiento (PLNTNMBR), sin distinguir mayusculas. Resuelve el tipo cuando el alterno es
     * nulo/0 (DIARIO, INGRESO y EGRESO comparten el 0) o cuando varios tipos comparten el alterno dado.
     */
    private String nombreTipoAsiento;

    /** Contenido completo del archivo (CSV separado por ';', UTF-8), con su linea de cabecera. */
    private String contenido;

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public Integer getCodigoAltTipoAsiento() {
        return codigoAltTipoAsiento;
    }

    public void setCodigoAltTipoAsiento(Integer codigoAltTipoAsiento) {
        this.codigoAltTipoAsiento = codigoAltTipoAsiento;
    }

    public String getNombreTipoAsiento() {
        return nombreTipoAsiento;
    }

    public void setNombreTipoAsiento(String nombreTipoAsiento) {
        this.nombreTipoAsiento = nombreTipoAsiento;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

}
