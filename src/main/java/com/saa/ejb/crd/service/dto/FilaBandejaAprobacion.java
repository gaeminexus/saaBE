package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;

/**
 * Una fila de la bandeja combinada de aprobación de contabilidad: une cobros individuales
 * (CRD.CBCR) y cargas Petro pendientes (CRD.CRAR), SIN modelo común — cada fila trae su
 * {@code tipo} para que la pantalla despache al endpoint correcto de cada uno.
 *
 * @see com.saa.ejb.crd.service.CobroCreditoService#bandejaAprobacion()
 */
public class FilaBandejaAprobacion {

    /** "COBRO_CREDITO" o "CARGA_PETRO". */
    private String tipo;

    /** Código en su propia tabla (CBCR.CBCRCDGO o CRAR.CRARCDGO). */
    private Long id;

    /** Nombre del partícipe (cobro individual) o de la filial (carga Petro). */
    private String descripcion;

    private Double valor;

    private String usuarioRegistro;

    private LocalDateTime fechaRegistro;

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
