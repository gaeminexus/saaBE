package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Planilla de control del IESS de un periodo.
 *
 * <p>POJO de transporte, sin @Entity. <b>No es la planilla oficial</b>: la planilla la
 * genera el IESS a partir de la historia laboral. Esta es la nuestra, para enfrentarla
 * contra la del portal <b>antes de pagar</b> y ver si las novedades del mes surtieron
 * efecto.</p>
 *
 * <p>Reproduce el comprobante completo, no solo la suma de aportes: al 20,60 % de la masa
 * se le anaden la contribucion CCC del 1 % --que se calcula sobre la suma de sueldos y no
 * sobre cada uno, por eso no aparece en ningun renglon-- y el seguro de salud de tiempo
 * parcial. Con todo lo demas en cero, el total es el 21,60 % de la masa.</p>
 *
 * <p>Lo que este control puede destapar es exactamente lo que paso en marzo de 2026: la
 * planilla del portal traia dos personas que ya no estaban, 208,22 de mas, porque nadie
 * habia registrado el aviso de salida. Contra nuestra planilla la diferencia se ve antes
 * de transferir, no despues.</p>
 */
@SuppressWarnings("serial")
public class PlanillaControlIess implements Serializable {

    private Long idPeriodo;

    private Integer anio;

    private Integer mes;

    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    /** Una fila por afiliado con nomina en el periodo. */
    private List<LineaPlanillaControlIess> lineas = new ArrayList<LineaPlanillaControlIess>();

    private Integer numeroAfiliados;

    /** Suma de las bases imponibles: la masa salarial declarada. */
    private Double masaSalarial;

    private Double totalAportePersonal;

    private Double totalAportePatronal;

    /** Suma de los aportes: el 20,60 % de la masa. */
    private Double totalAportes;

    /** Contribucion CCC, 1 % de la masa salarial. */
    private Double contribucionCcc;

    /** Seguro de salud de los afiliados a jornada parcial. */
    private Double totalSeguroTiempoParcial;

    /** Lo que habria que pagar: aportes + CCC + seguro de tiempo parcial. */
    private Double totalComprobante;

    /**
     * Avisos que no bloquean pero que hay que leer antes de dar la planilla por buena.
     *
     * <p>Aqui van las novedades del periodo que siguen sin reportar: si quedan, la planilla
     * que genere el portal <b>no</b> va a coincidir con esta, y la diferencia sera
     * justamente la que esas novedades habrian corregido.</p>
     */
    private List<String> avisos = new ArrayList<String>();

    public Long getIdPeriodo() {
        return idPeriodo;
    }

    public void setIdPeriodo(Long idPeriodo) {
        this.idPeriodo = idPeriodo;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public List<LineaPlanillaControlIess> getLineas() {
        return lineas;
    }

    public void setLineas(List<LineaPlanillaControlIess> lineas) {
        this.lineas = lineas;
    }

    public Integer getNumeroAfiliados() {
        return numeroAfiliados;
    }

    public void setNumeroAfiliados(Integer numeroAfiliados) {
        this.numeroAfiliados = numeroAfiliados;
    }

    public Double getMasaSalarial() {
        return masaSalarial;
    }

    public void setMasaSalarial(Double masaSalarial) {
        this.masaSalarial = masaSalarial;
    }

    public Double getTotalAportePersonal() {
        return totalAportePersonal;
    }

    public void setTotalAportePersonal(Double totalAportePersonal) {
        this.totalAportePersonal = totalAportePersonal;
    }

    public Double getTotalAportePatronal() {
        return totalAportePatronal;
    }

    public void setTotalAportePatronal(Double totalAportePatronal) {
        this.totalAportePatronal = totalAportePatronal;
    }

    public Double getTotalAportes() {
        return totalAportes;
    }

    public void setTotalAportes(Double totalAportes) {
        this.totalAportes = totalAportes;
    }

    public Double getContribucionCcc() {
        return contribucionCcc;
    }

    public void setContribucionCcc(Double contribucionCcc) {
        this.contribucionCcc = contribucionCcc;
    }

    public Double getTotalSeguroTiempoParcial() {
        return totalSeguroTiempoParcial;
    }

    public void setTotalSeguroTiempoParcial(Double totalSeguroTiempoParcial) {
        this.totalSeguroTiempoParcial = totalSeguroTiempoParcial;
    }

    public Double getTotalComprobante() {
        return totalComprobante;
    }

    public void setTotalComprobante(Double totalComprobante) {
        this.totalComprobante = totalComprobante;
    }

    public List<String> getAvisos() {
        return avisos;
    }

    public void setAvisos(List<String> avisos) {
        this.avisos = avisos;
    }

}
