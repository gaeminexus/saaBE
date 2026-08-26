package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Un sub-proceso del cierre de cartera con sus líneas y sus totales: lo que contabilidad
 * revisa antes de autorizar, y lo que queda registrado después de ejecutar.
 *
 * Un sub-proceso <b>sin líneas</b> (por ejemplo, un mes sin ninguna cuota que venza) no es
 * un error: sale con la lista vacía, {@code totalDebe} y {@code totalHaber} en cero y
 * {@code omitido} en {@code true}. La ejecución NO genera asiento para él — un asiento sin
 * líneas no cuadra y no aporta nada.
 */
public class SubProcesoCierre {

    /** Código del sub-proceso. Ver {@link com.saa.rubros.SubProcesoCierreCartera}. */
    private Long subProceso;

    /** Nombre legible del sub-proceso, p.ej. "Asiento de vencidos". */
    private String nombre;

    /** Referencia al sub-proceso en la pizarra del levantamiento: "①", "②", "①.1"… */
    private String referencia;

    /** Fecha contable con la que se genera el asiento. */
    private LocalDate fecha;

    /** Glosa del asiento. */
    private String glosa;

    /** Líneas del asiento. */
    private List<LineaAsientoCierre> lineas = new ArrayList<LineaAsientoCierre>();

    /** Suma del DEBE. */
    private Double totalDebe;

    /** Suma del HABER. Debe igualar a {@code totalDebe}. */
    private Double totalHaber;

    /** {@code true} si el sub-proceso no tiene nada que contabilizar este mes. */
    private Boolean omitido;

    /** Motivo por el que se omite, cuando {@code omitido} es {@code true}. */
    private String motivoOmision;

    /** Código del asiento generado (CNT.ASNT). Nulo en la previsualización. */
    private Long idAsiento;

    /** Número del asiento generado. Nulo en la previsualización. */
    private String numeroAsiento;

    public SubProcesoCierre() {
    }

    public Long getSubProceso() {
        return subProceso;
    }

    public void setSubProceso(Long subProceso) {
        this.subProceso = subProceso;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getGlosa() {
        return glosa;
    }

    public void setGlosa(String glosa) {
        this.glosa = glosa;
    }

    public List<LineaAsientoCierre> getLineas() {
        return lineas;
    }

    public void setLineas(List<LineaAsientoCierre> lineas) {
        this.lineas = lineas;
    }

    public Double getTotalDebe() {
        return totalDebe;
    }

    public void setTotalDebe(Double totalDebe) {
        this.totalDebe = totalDebe;
    }

    public Double getTotalHaber() {
        return totalHaber;
    }

    public void setTotalHaber(Double totalHaber) {
        this.totalHaber = totalHaber;
    }

    public Boolean getOmitido() {
        return omitido;
    }

    public void setOmitido(Boolean omitido) {
        this.omitido = omitido;
    }

    public String getMotivoOmision() {
        return motivoOmision;
    }

    public void setMotivoOmision(String motivoOmision) {
        this.motivoOmision = motivoOmision;
    }

    public Long getIdAsiento() {
        return idAsiento;
    }

    public void setIdAsiento(Long idAsiento) {
        this.idAsiento = idAsiento;
    }

    public String getNumeroAsiento() {
        return numeroAsiento;
    }

    public void setNumeroAsiento(String numeroAsiento) {
        this.numeroAsiento = numeroAsiento;
    }
}
