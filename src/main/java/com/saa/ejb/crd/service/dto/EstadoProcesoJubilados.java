package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;

/**
 * Una de las dos secciones (seguro / pensiones) del seguimiento de la corrida mensual de
 * jubilados — §4.3 de API-DOS-PROCESOS-MENSUALES-JUBILADOS.md.
 *
 * {@code idOrdenPago} solo aplica a la sección seguro (la orden agregada al proveedor);
 * {@code cruzadoAPrestamos} solo a la sección pensiones. Se dejan en {@code null} en la
 * sección donde no corresponden — el frontend no traduce nada.
 */
public class EstadoProcesoJubilados {

    /** 0 pendiente, 1 generado. */
    private Long estado;

    /** "PENDIENTE" o "GENERADO", resuelto por el backend. */
    private String nombreEstado;

    private LocalDateTime fecha;
    private String usuario;
    private Double total;
    private Long jubilados;

    /** Solo sección seguro: id de la orden agregada al proveedor. */
    private Long idOrdenPago;

    /** Solo sección pensiones: total cruzado a préstamos. */
    private Double cruzadoAPrestamos;

    public EstadoProcesoJubilados() {
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public String getNombreEstado() {
        return nombreEstado;
    }

    public void setNombreEstado(String nombreEstado) {
        this.nombreEstado = nombreEstado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Long getJubilados() {
        return jubilados;
    }

    public void setJubilados(Long jubilados) {
        this.jubilados = jubilados;
    }

    public Long getIdOrdenPago() {
        return idOrdenPago;
    }

    public void setIdOrdenPago(Long idOrdenPago) {
        this.idOrdenPago = idOrdenPago;
    }

    public Double getCruzadoAPrestamos() {
        return cruzadoAPrestamos;
    }

    public void setCruzadoAPrestamos(Double cruzadoAPrestamos) {
        this.cruzadoAPrestamos = cruzadoAPrestamos;
    }
}
