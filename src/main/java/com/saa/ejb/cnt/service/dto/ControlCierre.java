package com.saa.ejb.cnt.service.dto;

import java.io.Serializable;

/**
 * Pase a producción (B-1..B-3) — un control del semáforo de cierre de mes
 * ({@code GET /prdo/controlCierre/{idEmpresa}/{anio}/{mes}}). Solo lectura.
 *
 * <p>{@code valor} es lo que hay (normalmente la contabilidad) y {@code esperado} contra qué se compara;
 * {@code diferencia = valor − esperado}. Los dos pueden ser nulos cuando el control no es numérico o no se
 * pudo medir (por ejemplo, sin extracto o sin G generadas): en ese caso el estado es ADVERTENCIA y el
 * motivo va en {@code detalle}. docs/logica-negocio/cnt/CONTROL-CIERRE-MES.md.</p>
 */
@SuppressWarnings("serial")
public class ControlCierre implements Serializable {

    public static final String OK = "OK";
    public static final String ADVERTENCIA = "ADVERTENCIA";
    public static final String ERROR = "ERROR";

    /** Grupo del control: PUENTES, ECUACION, CARTERA, BANCOS, ASIENTOS, B17_G. */
    private String grupo;
    /** Código estable del control (lo puede leer la pantalla). */
    private String codigo;
    private String descripcion;
    private Double valor;
    private Double esperado;
    private Double diferencia;
    /** OK / ADVERTENCIA / ERROR. */
    private String estado;
    private String detalle;

    public ControlCierre() {
    }

    public ControlCierre(String grupo, String codigo, String descripcion) {
        this.grupo = grupo;
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public Double getEsperado() { return esperado; }
    public void setEsperado(Double esperado) { this.esperado = esperado; }
    public Double getDiferencia() { return diferencia; }
    public void setDiferencia(Double diferencia) { this.diferencia = diferencia; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
