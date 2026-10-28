package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de la carga de un archivo de asientos manuales. Si {@link #exito} es false el archivo se rechazo
 * completo y {@link #errores} trae todas las observaciones con su numero de linea; si es true, {@link #asientos}
 * trae lo que se registro. POJO de transporte, sin @Entity (precedente: {@link RespuestaBalance}).
 * docs/logica-negocio/cnt/CARGADOR-ASIENTOS-MANUALES.md.
 */
@SuppressWarnings("serial")
public class ResultadoCargaAsientosManuales implements Serializable {

    private boolean exito;

    private String nombreArchivo;

    /** Lineas del archivo sin contar la cabecera ni las vacias. */
    private int lineasLeidas;

    /** Asientos distintos (numero_asiento) encontrados en el archivo. */
    private int asientosLeidos;

    /** Tipo de asiento con el que se registro (o se iba a registrar). */
    private String tipoAsiento;

    private List<ErrorLinea> errores = new ArrayList<>();

    private List<AsientoCreado> asientos = new ArrayList<>();

    /** Un error de validacion. {@code linea} 0 = error del archivo o de la peticion, no de una linea. */
    public static class ErrorLinea implements Serializable {

        private int linea;

        private String numeroAsiento;

        private String mensaje;

        public ErrorLinea() {
        }

        public ErrorLinea(int linea, String numeroAsiento, String mensaje) {
            this.linea = linea;
            this.numeroAsiento = numeroAsiento;
            this.mensaje = mensaje;
        }

        public int getLinea() {
            return linea;
        }

        public void setLinea(int linea) {
            this.linea = linea;
        }

        public String getNumeroAsiento() {
            return numeroAsiento;
        }

        public void setNumeroAsiento(String numeroAsiento) {
            this.numeroAsiento = numeroAsiento;
        }

        public String getMensaje() {
            return mensaje;
        }

        public void setMensaje(String mensaje) {
            this.mensaje = mensaje;
        }
    }

    /** Un asiento registrado. */
    public static class AsientoCreado implements Serializable {

        /** ASNTCDGO. */
        private Long idAsiento;

        /** ASNTNMAL, p. ej. DIA-2025-06-0001. */
        private String numeroAlterno;

        /** numero_asiento del archivo del cliente. */
        private String numeroAsientoCliente;

        private LocalDate fecha;

        private int lineas;

        private double debe;

        private double haber;

        public Long getIdAsiento() {
            return idAsiento;
        }

        public void setIdAsiento(Long idAsiento) {
            this.idAsiento = idAsiento;
        }

        public String getNumeroAlterno() {
            return numeroAlterno;
        }

        public void setNumeroAlterno(String numeroAlterno) {
            this.numeroAlterno = numeroAlterno;
        }

        public String getNumeroAsientoCliente() {
            return numeroAsientoCliente;
        }

        public void setNumeroAsientoCliente(String numeroAsientoCliente) {
            this.numeroAsientoCliente = numeroAsientoCliente;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public void setFecha(LocalDate fecha) {
            this.fecha = fecha;
        }

        public int getLineas() {
            return lineas;
        }

        public void setLineas(int lineas) {
            this.lineas = lineas;
        }

        public double getDebe() {
            return debe;
        }

        public void setDebe(double debe) {
            this.debe = debe;
        }

        public double getHaber() {
            return haber;
        }

        public void setHaber(double haber) {
            this.haber = haber;
        }
    }

    public boolean isExito() {
        return exito;
    }

    public void setExito(boolean exito) {
        this.exito = exito;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public int getLineasLeidas() {
        return lineasLeidas;
    }

    public void setLineasLeidas(int lineasLeidas) {
        this.lineasLeidas = lineasLeidas;
    }

    public int getAsientosLeidos() {
        return asientosLeidos;
    }

    public void setAsientosLeidos(int asientosLeidos) {
        this.asientosLeidos = asientosLeidos;
    }

    public String getTipoAsiento() {
        return tipoAsiento;
    }

    public void setTipoAsiento(String tipoAsiento) {
        this.tipoAsiento = tipoAsiento;
    }

    public List<ErrorLinea> getErrores() {
        return errores;
    }

    public void setErrores(List<ErrorLinea> errores) {
        this.errores = errores;
    }

    public List<AsientoCreado> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<AsientoCreado> asientos) {
        this.asientos = asientos;
    }

}
