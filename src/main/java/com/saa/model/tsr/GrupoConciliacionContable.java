/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.GRCC.
 * Entity GrupoConciliacionContable.
 * Un "grupo de conciliación" conecta N filas de {@link DetalleExtractoBancario}
 * con M filas de {@link com.saa.model.cnt.DetalleAsiento} (N y M pueden ser
 * 1 - el caso más común - o más de 1 en cualquiera de los dos lados: un solo
 * depósito bancario puede liquidar varias líneas contables, o una sola línea
 * contable puede repartirse contra varios movimientos bancarios). Las filas
 * concretas que pertenecen a un grupo viven en las tablas de enlace
 * {@link GrupoConciliacionExtracto} y {@link GrupoConciliacionAsiento}, no
 * aquí - esta fila es solo la cabecera/resumen del match.</p>
 * <p>Para que un grupo sea válido, TANTO el monto como la fecha deben
 * cuadrar entre los dos lados: valorExtracto debe igualar valorAsiento
 * (dentro de una tolerancia de redondeo), Y la fecha más temprana y la más
 * tardía entre TODOS los ítems del grupo (de ambos lados) no pueden diferir
 * en más de {@code toleranciaDiasAplicada} días - un monto que cuadra con
 * fechas muy separadas no es una conciliación válida.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "GRCC", schema = "TSR")
@NamedQueries({
    @NamedQuery(name = "GrupoConciliacionContableAll", query = "select e from GrupoConciliacionContable e"),
    @NamedQuery(name = "GrupoConciliacionContableId",
        query = "select e from GrupoConciliacionContable e where e.codigo = :id")
})
public class GrupoConciliacionContable implements Serializable {

    /**
     * Código / PK autoincrementable.
     */
    @Basic
    @Id
    @Column(name = "GRCCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * FK a la cabecera ConciliacionContable (cuenta bancaria + período) a la
     * que pertenece este grupo.
     */
    @ManyToOne
    @JoinColumn(name = "CNCTCDGO", nullable = false)
    private ConciliacionContable conciliacionContable;

    /**
     * Suma de los valores del lado bancario (DetalleExtractoBancario) del
     * grupo. Débitos y créditos ya combinados con signo segun corresponda.
     */
    @Basic
    @Column(name = "GRCCVLEX")
    private Double valorExtracto;

    /**
     * Suma de los valores del lado contable (DetalleAsiento, debe-haber) del
     * grupo.
     */
    @Basic
    @Column(name = "GRCCVLAS")
    private Double valorAsiento;

    /**
     * valorExtracto - valorAsiento. Debe ser ~0 (dentro de tolerancia
     * monetaria) para que el grupo se considere balanceado.
     */
    @Basic
    @Column(name = "GRCCDIFF")
    private Double diferencia;

    /**
     * Fecha más temprana entre todos los ítems del grupo (ambos lados).
     */
    @Basic
    @Column(name = "GRCCFCMN")
    private LocalDate fechaMinima;

    /**
     * Fecha más tardía entre todos los ítems del grupo (ambos lados).
     */
    @Basic
    @Column(name = "GRCCFCMX")
    private LocalDate fechaMaxima;

    /**
     * Días de tolerancia (Rubros.ASP_TOLERANCIA_DIAS_CONCILIACION_CONTABLE)
     * vigentes al momento de crear este grupo - se guarda como snapshot para
     * que un cambio futuro del parámetro no altere retroactivamente la
     * validez de conciliaciones ya hechas.
     */
    @Basic
    @Column(name = "GRCCTOLD")
    private Long toleranciaDiasAplicada;

    /**
     * Usuario que concilió el grupo.
     */
    @Basic
    @Column(name = "GRCCUSCN", length = 50)
    private String usuarioConcilia;

    /**
     * Fecha y hora en que se concilió el grupo.
     */
    @Basic
    @Column(name = "GRCCFCCN")
    private LocalDateTime fechaConciliacion;

    /**
     * Observaciones libres (ej. motivo de una diferencia aceptada).
     */
    @Basic
    @Column(name = "GRCCOBSR", length = 500)
    private String observaciones;

    /**
     * Estado del grupo. 1 = Activo (vigente), 0 = Deshecho (el usuario separó
     * el grupo y sus ítems volvieron al pool de pendientes).
     */
    @Basic
    @Column(name = "GRCCESTD")
    private Long estado;

    // -------------------------------------------------------------------------
    // GETTERS Y SETTERS
    // -------------------------------------------------------------------------

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public ConciliacionContable getConciliacionContable() {
        return conciliacionContable;
    }

    public void setConciliacionContable(ConciliacionContable conciliacionContable) {
        this.conciliacionContable = conciliacionContable;
    }

    public Double getValorExtracto() {
        return valorExtracto;
    }

    public void setValorExtracto(Double valorExtracto) {
        this.valorExtracto = valorExtracto;
    }

    public Double getValorAsiento() {
        return valorAsiento;
    }

    public void setValorAsiento(Double valorAsiento) {
        this.valorAsiento = valorAsiento;
    }

    public Double getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(Double diferencia) {
        this.diferencia = diferencia;
    }

    public LocalDate getFechaMinima() {
        return fechaMinima;
    }

    public void setFechaMinima(LocalDate fechaMinima) {
        this.fechaMinima = fechaMinima;
    }

    public LocalDate getFechaMaxima() {
        return fechaMaxima;
    }

    public void setFechaMaxima(LocalDate fechaMaxima) {
        this.fechaMaxima = fechaMaxima;
    }

    public Long getToleranciaDiasAplicada() {
        return toleranciaDiasAplicada;
    }

    public void setToleranciaDiasAplicada(Long toleranciaDiasAplicada) {
        this.toleranciaDiasAplicada = toleranciaDiasAplicada;
    }

    public String getUsuarioConcilia() {
        return usuarioConcilia;
    }

    public void setUsuarioConcilia(String usuarioConcilia) {
        this.usuarioConcilia = usuarioConcilia;
    }

    public LocalDateTime getFechaConciliacion() {
        return fechaConciliacion;
    }

    public void setFechaConciliacion(LocalDateTime fechaConciliacion) {
        this.fechaConciliacion = fechaConciliacion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
