package com.saa.model.crd;

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
 * Representa la tabla AVPC (AfectacionValoresParticipeCarga).
 * 
 * Esta tabla registra las afectaciones MANUALES de valores cuando hay novedades
 * en el procesamiento de archivos Petrocomercial.
 * 
 * PROPÓSITO:
 * Cuando el procesamiento automático detecta novedades (préstamo no encontrado,
 * múltiples préstamos, montos inconsistentes, etc.), el sistema NO puede determinar
 * automáticamente cómo distribuir los valores del archivo.
 * 
 * En estos casos, el FRONTEND permite al usuario especificar:
 * - A qué préstamo y cuota específica afectar
 * - Qué valores exactos aplicar (capital, interés, desgravamen)
 * - Las diferencias entre lo esperado y lo real
 * 
 * FLUJO:
 * 1. FASE 2 procesa archivo y detecta novedades → guarda en NovedadParticipeCarga
 * 2. Frontend muestra novedades al usuario
 * 3. Usuario indica manualmente cómo afectar → guarda en AfectacionValoresParticipeCarga
 * 4. FASE 2 reprocesa usando esta tabla como referencia
 * 
 * @author Sistema SAA
 * @since 2026-03-25
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "AVPC", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "AfectacionValoresParticipeCargaAll", query = "select e from AfectacionValoresParticipeCarga e"),
    @NamedQuery(name = "AfectacionValoresParticipeCargaId", query = "select e from AfectacionValoresParticipeCarga e where e.codigo = :id"),
    @NamedQuery(name = "AfectacionValoresParticipeCargaByNovedad", query = "select e from AfectacionValoresParticipeCarga e where e.novedadParticipeCarga.codigo = :codigoNovedad")
})
public class AfectacionValoresParticipeCarga implements Serializable {

    // ============================================================
    // PK
    // ============================================================
    
    @Id
    @Basic
    @Column(name = "AVPCCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    // ============================================================
    // RELACIONES (FKs)
    // ============================================================
    
    /** FK - Novedad Partícipe Carga (tabla padre) */
    @ManyToOne
    @JoinColumn(name = "NVPCCDGO", referencedColumnName = "NVPCCDGO")
    private NovedadParticipeCarga novedadParticipeCarga;

    /** FK - Préstamo al que se afecta */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    /** FK - Detalle Préstamo (cuota) a afectar */
    @ManyToOne
    @JoinColumn(name = "DTPRCDGO", referencedColumnName = "DTPRCDGO")
    private DetallePrestamo detallePrestamo;

    // ============================================================
    // VALORES DE LA CUOTA ORIGINAL
    // ============================================================
    
    /** Valor total de la cuota original */
    @Basic
    @Column(name = "AVPCVLCT")
    private Double valorCuotaOriginal;

    /** Capital de la cuota original */
    @Basic
    @Column(name = "AVPCVLCP")
    private Double capitalCuotaOriginal;

    /** Interés de la cuota original */
    @Basic
    @Column(name = "AVPCVLIN")
    private Double interesCuotaOriginal;

    /** Desgravamen de la cuota original */
    @Basic
    @Column(name = "AVPCVLDG")
    private Double desgravamenCuotaOriginal;

    // ============================================================
    // VALORES A AFECTAR (indicados por el usuario)
    // ============================================================
    
    /** Valor total a afectar (requerido) */
    @Basic
    @Column(name = "AVPCVAFA")
    private Double valorAfectar;

    /** Capital a afectar */
    @Basic
    @Column(name = "AVPCCPAF")
    private Double capitalAfectar;

    /** Interés a afectar */
    @Basic
    @Column(name = "AVPCINAF")
    private Double interesAfectar;

    /** Desgravamen a afectar */
    @Basic
    @Column(name = "AVPCDGAF")
    private Double desgravamenAfectar;

    // ============================================================
    // DIFERENCIAS CALCULADAS
    // ============================================================
    
    /** Diferencia total (valor cuota - valor a afectar) */
    @Basic
    @Column(name = "AVPCDFTL")
    private Double diferenciaTotal;

    /** Diferencia en capital */
    @Basic
    @Column(name = "AVPCDFCP")
    private Double diferenciaCapital;

    /** Diferencia en interés */
    @Basic
    @Column(name = "AVPCDFIN")
    private Double diferenciaInteres;

    /** Diferencia en desgravamen */
    @Basic
    @Column(name = "AVPCDFDG")
    private Double diferenciaDesgravamen;

    // ============================================================
    // AUDITORÍA
    // ============================================================
    
    /** Fecha en que se registró la afectación */
    @Basic
    @Column(name = "AVPCFCAF")
    private LocalDate fechaAfectacion;

    /** Usuario que registró la afectación */
    @Basic
    @Column(name = "AVPCUSAR", length = 50)
    private String usuarioRegistro;

    /** Fecha de creación del registro (timestamp automático) */
    @Basic
    @Column(name = "AVPCFCRG")
    private LocalDateTime fechaCreacionRegistro;

    // ============================================================
    // OBSERVACIONES Y ESTADO
    // ============================================================
    
    /** Observaciones adicionales del usuario */
    @Basic
    @Column(name = "AVPCOBSR", length = 1000)
    private String observaciones;

    /** Estado del registro (1=ACTIVO) */
    @Basic
    @Column(name = "AVPCESTD")
    private Long estado;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================
    
    public AfectacionValoresParticipeCarga() {
        this.estado = 1L; // ACTIVO por defecto
        this.fechaCreacionRegistro = LocalDateTime.now();
    }

    // ============================================================
    // GETTERS Y SETTERS
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public NovedadParticipeCarga getNovedadParticipeCarga() {
        return novedadParticipeCarga;
    }

    public void setNovedadParticipeCarga(NovedadParticipeCarga novedadParticipeCarga) {
        this.novedadParticipeCarga = novedadParticipeCarga;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
    }

    public DetallePrestamo getDetallePrestamo() {
        return detallePrestamo;
    }

    public void setDetallePrestamo(DetallePrestamo detallePrestamo) {
        this.detallePrestamo = detallePrestamo;
    }

    public Double getValorCuotaOriginal() {
        return valorCuotaOriginal;
    }

    public void setValorCuotaOriginal(Double valorCuotaOriginal) {
        this.valorCuotaOriginal = valorCuotaOriginal;
    }

    public Double getCapitalCuotaOriginal() {
        return capitalCuotaOriginal;
    }

    public void setCapitalCuotaOriginal(Double capitalCuotaOriginal) {
        this.capitalCuotaOriginal = capitalCuotaOriginal;
    }

    public Double getInteresCuotaOriginal() {
        return interesCuotaOriginal;
    }

    public void setInteresCuotaOriginal(Double interesCuotaOriginal) {
        this.interesCuotaOriginal = interesCuotaOriginal;
    }

    public Double getDesgravamenCuotaOriginal() {
        return desgravamenCuotaOriginal;
    }

    public void setDesgravamenCuotaOriginal(Double desgravamenCuotaOriginal) {
        this.desgravamenCuotaOriginal = desgravamenCuotaOriginal;
    }

    public Double getValorAfectar() {
        return valorAfectar;
    }

    public void setValorAfectar(Double valorAfectar) {
        this.valorAfectar = valorAfectar;
    }

    public Double getCapitalAfectar() {
        return capitalAfectar;
    }

    public void setCapitalAfectar(Double capitalAfectar) {
        this.capitalAfectar = capitalAfectar;
    }

    public Double getInteresAfectar() {
        return interesAfectar;
    }

    public void setInteresAfectar(Double interesAfectar) {
        this.interesAfectar = interesAfectar;
    }

    public Double getDesgravamenAfectar() {
        return desgravamenAfectar;
    }

    public void setDesgravamenAfectar(Double desgravamenAfectar) {
        this.desgravamenAfectar = desgravamenAfectar;
    }

    public Double getDiferenciaTotal() {
        return diferenciaTotal;
    }

    public void setDiferenciaTotal(Double diferenciaTotal) {
        this.diferenciaTotal = diferenciaTotal;
    }

    public Double getDiferenciaCapital() {
        return diferenciaCapital;
    }

    public void setDiferenciaCapital(Double diferenciaCapital) {
        this.diferenciaCapital = diferenciaCapital;
    }

    public Double getDiferenciaInteres() {
        return diferenciaInteres;
    }

    public void setDiferenciaInteres(Double diferenciaInteres) {
        this.diferenciaInteres = diferenciaInteres;
    }

    public Double getDiferenciaDesgravamen() {
        return diferenciaDesgravamen;
    }

    public void setDiferenciaDesgravamen(Double diferenciaDesgravamen) {
        this.diferenciaDesgravamen = diferenciaDesgravamen;
    }

    public LocalDate getFechaAfectacion() {
        return fechaAfectacion;
    }

    public void setFechaAfectacion(LocalDate fechaAfectacion) {
        this.fechaAfectacion = fechaAfectacion;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public LocalDateTime getFechaCreacionRegistro() {
        return fechaCreacionRegistro;
    }

    public void setFechaCreacionRegistro(LocalDateTime fechaCreacionRegistro) {
        this.fechaCreacionRegistro = fechaCreacionRegistro;
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

    // ============================================================
    // MÉTODOS DE UTILIDAD
    // ============================================================

    /**
     * Calcula automáticamente las diferencias entre valores originales y a afectar
     */
    public void calcularDiferencias() {
        if (valorCuotaOriginal != null && valorAfectar != null) {
            this.diferenciaTotal = valorCuotaOriginal - valorAfectar;
        }
        
        if (capitalCuotaOriginal != null && capitalAfectar != null) {
            this.diferenciaCapital = capitalCuotaOriginal - capitalAfectar;
        }
        
        if (interesCuotaOriginal != null && interesAfectar != null) {
            this.diferenciaInteres = interesCuotaOriginal - interesAfectar;
        }
        
        if (desgravamenCuotaOriginal != null && desgravamenAfectar != null) {
            this.diferenciaDesgravamen = desgravamenCuotaOriginal - desgravamenAfectar;
        }
    }

    @Override
    public String toString() {
        return "AfectacionValoresParticipeCarga{" +
                "codigo=" + codigo +
                ", prestamo=" + (prestamo != null ? prestamo.getCodigo() : null) +
                ", cuota=" + (detallePrestamo != null ? detallePrestamo.getCodigo() : null) +
                ", valorAfectar=" + valorAfectar +
                ", usuarioRegistro='" + usuarioRegistro + '\'' +
                '}';
    }
}
