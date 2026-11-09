package com.saa.ejb.crd.service.dto;

/**
 * Respuesta de {@code POST /prst/simularNovacion} (DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md, B6):
 * el préstamo original, la tabla nueva (la misma forma de {@code /simularCreditoNuevo}), lo que el socio recibe
 * (monto − valor de sustitución) y la evaluación de capacidad y cupo SIN el original.
 */
public class ResultadoSimulacionNovacion {

    /** El préstamo que se nova. */
    public static class Original {
        private Long idPrestamo;
        private Long idAsoprep;
        private String producto;
        private Double cuotaActual;
        private Double vs;
        private SustituiblePrestamo.Desglose desgloseVs;

        public Long getIdPrestamo() { return idPrestamo; }
        public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
        public Long getIdAsoprep() { return idAsoprep; }
        public void setIdAsoprep(Long idAsoprep) { this.idAsoprep = idAsoprep; }
        public String getProducto() { return producto; }
        public void setProducto(String producto) { this.producto = producto; }
        public Double getCuotaActual() { return cuotaActual; }
        public void setCuotaActual(Double cuotaActual) { this.cuotaActual = cuotaActual; }
        public Double getVs() { return vs; }
        public void setVs(Double vs) { this.vs = vs; }
        public SustituiblePrestamo.Desglose getDesgloseVs() { return desgloseVs; }
        public void setDesgloseVs(SustituiblePrestamo.Desglose desgloseVs) { this.desgloseVs = desgloseVs; }
    }

    private Original original;
    private ResultadoSimulacionCreditoNuevo simulacion;
    private Double valorLiquido;
    private EvaluacionOtorgamiento evaluacion;
    /** Datos con los que se armó la tabla nueva (los usa el PDF): tasa, tipo de amortización (1 francesa, 2 alemana) y fecha de inicio. */
    private Double tasa;
    private Long tipoAmortizacion;
    private java.time.LocalDateTime fechaInicio;

    public Double getTasa() { return tasa; }
    public void setTasa(Double tasa) { this.tasa = tasa; }
    public Long getTipoAmortizacion() { return tipoAmortizacion; }
    public void setTipoAmortizacion(Long tipoAmortizacion) { this.tipoAmortizacion = tipoAmortizacion; }
    public java.time.LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(java.time.LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public Original getOriginal() { return original; }
    public void setOriginal(Original original) { this.original = original; }
    public ResultadoSimulacionCreditoNuevo getSimulacion() { return simulacion; }
    public void setSimulacion(ResultadoSimulacionCreditoNuevo simulacion) { this.simulacion = simulacion; }
    public Double getValorLiquido() { return valorLiquido; }
    public void setValorLiquido(Double valorLiquido) { this.valorLiquido = valorLiquido; }
    public EvaluacionOtorgamiento getEvaluacion() { return evaluacion; }
    public void setEvaluacion(EvaluacionOtorgamiento evaluacion) { this.evaluacion = evaluacion; }
}
