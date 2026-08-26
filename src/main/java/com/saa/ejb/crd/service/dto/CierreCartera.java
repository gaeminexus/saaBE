package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Una corrida del cierre mensual de cartera, completa: cabecera, los seis sub-procesos con
 * sus líneas, el snapshot de bandas y los controles.
 *
 * Es la MISMA estructura que devuelven previsualizar, ejecutar y consultar. Lo único que
 * cambia entre las tres es si {@code idCorrida} y los {@code idAsiento} vienen informados:
 * en la previsualización no hay nada grabado.
 */
public class CierreCartera {

    /** Código de la corrida (CRD.CRCT). Nulo en la previsualización. */
    private Long idCorrida;

    /** Código de la empresa (SCP.PJRQ). */
    private Long idEmpresa;

    /** Año del mes CERRADO. */
    private Long anio;

    /** Mes CERRADO, 1 a 12. */
    private Long mes;

    /** Fecha de corte: último día del mes cerrado. */
    private LocalDate fechaCorte;

    /** Fecha de proceso: primer día del mes siguiente. */
    private LocalDate fechaProceso;

    /**
     * Fecha hasta la que factura la apertura: último día del mes que se ABRE, es decir el
     * mes de {@code fechaProceso}. No es {@code fechaCorte}.
     */
    private LocalDate fechaCorteApertura;

    /** Estado operativo: 1 PREPARADA, 2 EJECUTADA, 3 REVERSADA. Nulo en la previsualización. */
    private Long idEstado;

    /** Etiqueta del estado. */
    private String nombreEstado;

    /** Los seis sub-procesos, en orden de ejecución. */
    private List<SubProcesoCierre> subProcesos = new ArrayList<SubProcesoCierre>();

    /** Snapshot de la distribución de capital por banda que deja esta corrida. */
    private List<BandaSnapshotCierre> snapshot = new ArrayList<BandaSnapshotCierre>();

    /**
     * Desviaciones contra el snapshot de la corrida anterior. Lista VACÍA si es la primera
     * corrida o si todo coincide. Ver {@link DesviacionBandaCierre}.
     */
    private List<DesviacionBandaCierre> desviaciones = new ArrayList<DesviacionBandaCierre>();

    /** Suma de las desviaciones, en valor absoluto. Cero es lo esperado cuando no hubo pagos. */
    private Double totalDesviacion;

    /** Capital total de la cartera a la fecha de corte, según la distribución nueva. */
    private Double capitalTotal;

    /** Advertencias que el usuario debe leer antes de ejecutar. Lista VACÍA si no hay. */
    private List<String> advertencias = new ArrayList<String>();

    /**
     * Control de archivo Petro del mes (decisión D13). Se calcula en previsualizar y en
     * ejecutar; en {@code consultar} viene NULO, porque no es un dato guardado sino una
     * comprobación del momento.
     */
    private ControlArchivoPetro controlArchivoPetro;

    /**
     * De dónde sale el importe de aportes del neteo. Igual que el control: se calcula, no
     * se guarda, así que en {@code consultar} viene NULO.
     */
    private DesgloseAportesCierre desgloseAportes;

    public CierreCartera() {
    }

    public Long getIdCorrida() {
        return idCorrida;
    }

    public void setIdCorrida(Long idCorrida) {
        this.idCorrida = idCorrida;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Long getAnio() {
        return anio;
    }

    public void setAnio(Long anio) {
        this.anio = anio;
    }

    public Long getMes() {
        return mes;
    }

    public void setMes(Long mes) {
        this.mes = mes;
    }

    public LocalDate getFechaCorte() {
        return fechaCorte;
    }

    public void setFechaCorte(LocalDate fechaCorte) {
        this.fechaCorte = fechaCorte;
    }

    public LocalDate getFechaProceso() {
        return fechaProceso;
    }

    public void setFechaProceso(LocalDate fechaProceso) {
        this.fechaProceso = fechaProceso;
    }

    public LocalDate getFechaCorteApertura() {
        return fechaCorteApertura;
    }

    public void setFechaCorteApertura(LocalDate fechaCorteApertura) {
        this.fechaCorteApertura = fechaCorteApertura;
    }

    public Long getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
    }

    public String getNombreEstado() {
        return nombreEstado;
    }

    public void setNombreEstado(String nombreEstado) {
        this.nombreEstado = nombreEstado;
    }

    public List<SubProcesoCierre> getSubProcesos() {
        return subProcesos;
    }

    public void setSubProcesos(List<SubProcesoCierre> subProcesos) {
        this.subProcesos = subProcesos;
    }

    public List<BandaSnapshotCierre> getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(List<BandaSnapshotCierre> snapshot) {
        this.snapshot = snapshot;
    }

    public List<DesviacionBandaCierre> getDesviaciones() {
        return desviaciones;
    }

    public void setDesviaciones(List<DesviacionBandaCierre> desviaciones) {
        this.desviaciones = desviaciones;
    }

    public Double getTotalDesviacion() {
        return totalDesviacion;
    }

    public void setTotalDesviacion(Double totalDesviacion) {
        this.totalDesviacion = totalDesviacion;
    }

    public Double getCapitalTotal() {
        return capitalTotal;
    }

    public void setCapitalTotal(Double capitalTotal) {
        this.capitalTotal = capitalTotal;
    }

    public List<String> getAdvertencias() {
        return advertencias;
    }

    public void setAdvertencias(List<String> advertencias) {
        this.advertencias = advertencias;
    }

    public ControlArchivoPetro getControlArchivoPetro() {
        return controlArchivoPetro;
    }

    public void setControlArchivoPetro(ControlArchivoPetro controlArchivoPetro) {
        this.controlArchivoPetro = controlArchivoPetro;
    }

    public DesgloseAportesCierre getDesgloseAportes() {
        return desgloseAportes;
    }

    public void setDesgloseAportes(DesgloseAportesCierre desgloseAportes) {
        this.desgloseAportes = desgloseAportes;
    }
}
