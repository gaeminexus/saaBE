package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Resumen que devuelve {@code ReprocesoCargaPetroService#reprocesar}: qué se anuló de la
 * aplicación anterior de la carga, qué quedó aplicado con la lógica vigente, y las novedades
 * que el reproceso dejó sin aplicar a propósito (roles Petro sin partícipe en el SAA).
 * Ver {@code docs/logica-negocio/petro/REPROCESO-CARGA-PETRO.md} §5.
 */
public class ResultadoReprocesoCargaPetro {

    private Long idCarga;
    private Long mesAfectacion;
    private Long anioAfectacion;
    /** Último día del mes de la carga: fecha de efecto de pagos, aportes y asientos. */
    private LocalDate fechaContable;
    /** Número de corrida de reproceso de ESTA carga (1 = primera vez que se reprocesa). */
    private int corrida;
    /** Etiqueta estándar estampada en todo lo anulado: {@code "REPROCESO carga N corrida M"}. */
    private String etiqueta;

    // --- Anulado (aplicación anterior) ---
    private int pagosAnulados;
    private double montoPagosAnulados;
    /** De los pagos anulados, cuántos eran del esquema viejo (sin CRARCDGO, identificados por la observación). */
    private int pagosAnuladosSinEnlace;
    private int aportesReversados;
    private double montoAportesReversados;
    /** De los aportes reversados, cuántos eran del esquema viejo (sin APRTIDAS ni CRARCDGO, identificados por la glosa). */
    private int aportesReversadosSinEnlace;
    private int pagosAporteInactivados;
    private int cuotasRecalculadas;
    private int prestamosReabiertos;
    private List<Long> asientosAnulados = new ArrayList<>();
    private Long asientoReProvision;

    // --- Aplicado (lógica vigente) ---
    private int pagosAplicados;
    private double montoPagosAplicados;
    private int aportesAplicados;
    private double montoAportesAplicados;
    private List<Long> asientosGenerados = new ArrayList<>();
    /** Resumen de texto de {@code aplicarPagosArchivoPetro} (contadores, novedades, advertencias). */
    private String resumenAplicacion;

    // --- D-019: operaciones vigentes al corte, hoy canceladas, reabiertas para la cuota del mes ---
    /** Una línea por operación: código, estado anterior, cuotas reabiertas, eventos posteriores marcados. */
    private List<String> cancelacionesPosterioresPendientes = new ArrayList<>();

    public List<String> getCancelacionesPosterioresPendientes() {
        return cancelacionesPosterioresPendientes;
    }

    public void setCancelacionesPosterioresPendientes(List<String> cancelacionesPosterioresPendientes) {
        this.cancelacionesPosterioresPendientes = cancelacionesPosterioresPendientes;
    }

    // --- D-019 ampliada: cuotas del mes saldadas sin pago (migración) reabiertas al corte ---
    private int cuotasSaldadasSinPagoReabiertas;
    /** Una línea por cuota: préstamo, número, estado anterior → nuevo, fecha de pago anterior. */
    private List<String> detalleCuotasSaldadasSinPagoReabiertas = new ArrayList<>();

    public int getCuotasSaldadasSinPagoReabiertas() {
        return cuotasSaldadasSinPagoReabiertas;
    }

    public void setCuotasSaldadasSinPagoReabiertas(int cuotasSaldadasSinPagoReabiertas) {
        this.cuotasSaldadasSinPagoReabiertas = cuotasSaldadasSinPagoReabiertas;
    }

    public List<String> getDetalleCuotasSaldadasSinPagoReabiertas() {
        return detalleCuotasSaldadasSinPagoReabiertas;
    }

    public void setDetalleCuotasSaldadasSinPagoReabiertas(List<String> detalleCuotasSaldadasSinPagoReabiertas) {
        this.detalleCuotasSaldadasSinPagoReabiertas = detalleCuotasSaldadasSinPagoReabiertas;
    }

    // --- D-022 (REPROCESO-CARGA-PETRO.md §4.10): aportes AH con contrato resuelto al corte ---
    /** Aportes de entidades con contrato inactivo hoy y vigente al corte, sin vigencias. */
    private int aportesContratoAlCorte;
    private double montoAportesContratoAlCorte;
    private List<String> detalleAportesContratoAlCorte = new ArrayList<>();
    /** Partícipes sin ningún contrato a los que el reproceso les creó contrato + vigencia. */
    private int contratosCreados;
    private double montoContratosCreados;
    private List<String> detalleContratosCreados = new ArrayList<>();

    public int getAportesContratoAlCorte() {
        return aportesContratoAlCorte;
    }

    public void setAportesContratoAlCorte(int aportesContratoAlCorte) {
        this.aportesContratoAlCorte = aportesContratoAlCorte;
    }

    public double getMontoAportesContratoAlCorte() {
        return montoAportesContratoAlCorte;
    }

    public void setMontoAportesContratoAlCorte(double montoAportesContratoAlCorte) {
        this.montoAportesContratoAlCorte = montoAportesContratoAlCorte;
    }

    public List<String> getDetalleAportesContratoAlCorte() {
        return detalleAportesContratoAlCorte;
    }

    public void setDetalleAportesContratoAlCorte(List<String> detalleAportesContratoAlCorte) {
        this.detalleAportesContratoAlCorte = detalleAportesContratoAlCorte;
    }

    public int getContratosCreados() {
        return contratosCreados;
    }

    public void setContratosCreados(int contratosCreados) {
        this.contratosCreados = contratosCreados;
    }

    public double getMontoContratosCreados() {
        return montoContratosCreados;
    }

    public void setMontoContratosCreados(double montoContratosCreados) {
        this.montoContratosCreados = montoContratosCreados;
    }

    public List<String> getDetalleContratosCreados() {
        return detalleContratosCreados;
    }

    public void setDetalleContratosCreados(List<String> detalleContratosCreados) {
        this.detalleContratosCreados = detalleContratosCreados;
    }

    // --- D-010: excedentes devueltos registrados como aporte EXCEDENTE_PETRO ---
    private int excedentesDevueltos;
    private double montoExcedentesDevueltos;
    /** Una línea por excedente aplicado: entidad, producto, monto, tipo de aporte, APRT creado. */
    private List<String> detalleExcedentesDevueltos = new ArrayList<>();

    public int getExcedentesDevueltos() {
        return excedentesDevueltos;
    }

    public void setExcedentesDevueltos(int excedentesDevueltos) {
        this.excedentesDevueltos = excedentesDevueltos;
    }

    public double getMontoExcedentesDevueltos() {
        return montoExcedentesDevueltos;
    }

    public void setMontoExcedentesDevueltos(double montoExcedentesDevueltos) {
        this.montoExcedentesDevueltos = montoExcedentesDevueltos;
    }

    public List<String> getDetalleExcedentesDevueltos() {
        return detalleExcedentesDevueltos;
    }

    public void setDetalleExcedentesDevueltos(List<String> detalleExcedentesDevueltos) {
        this.detalleExcedentesDevueltos = detalleExcedentesDevueltos;
    }

    // --- Novedades: roles Petro sin partícipe en el SAA (no se aplican, se reportan) ---
    private int filasSinParticipe;
    private double montoSinParticipe;
    private List<String> detalleSinParticipe = new ArrayList<>();

    private String mensaje;

    public Long getIdCarga() { return idCarga; }
    public void setIdCarga(Long idCarga) { this.idCarga = idCarga; }
    public Long getMesAfectacion() { return mesAfectacion; }
    public void setMesAfectacion(Long mesAfectacion) { this.mesAfectacion = mesAfectacion; }
    public Long getAnioAfectacion() { return anioAfectacion; }
    public void setAnioAfectacion(Long anioAfectacion) { this.anioAfectacion = anioAfectacion; }
    public LocalDate getFechaContable() { return fechaContable; }
    public void setFechaContable(LocalDate fechaContable) { this.fechaContable = fechaContable; }
    public int getCorrida() { return corrida; }
    public void setCorrida(int corrida) { this.corrida = corrida; }
    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }
    public int getPagosAnulados() { return pagosAnulados; }
    public void setPagosAnulados(int pagosAnulados) { this.pagosAnulados = pagosAnulados; }
    public double getMontoPagosAnulados() { return montoPagosAnulados; }
    public void setMontoPagosAnulados(double montoPagosAnulados) { this.montoPagosAnulados = montoPagosAnulados; }
    public int getPagosAnuladosSinEnlace() { return pagosAnuladosSinEnlace; }
    public void setPagosAnuladosSinEnlace(int pagosAnuladosSinEnlace) { this.pagosAnuladosSinEnlace = pagosAnuladosSinEnlace; }
    public int getAportesReversados() { return aportesReversados; }
    public void setAportesReversados(int aportesReversados) { this.aportesReversados = aportesReversados; }
    public double getMontoAportesReversados() { return montoAportesReversados; }
    public void setMontoAportesReversados(double montoAportesReversados) { this.montoAportesReversados = montoAportesReversados; }
    public int getAportesReversadosSinEnlace() { return aportesReversadosSinEnlace; }
    public void setAportesReversadosSinEnlace(int aportesReversadosSinEnlace) { this.aportesReversadosSinEnlace = aportesReversadosSinEnlace; }
    public int getPagosAporteInactivados() { return pagosAporteInactivados; }
    public void setPagosAporteInactivados(int pagosAporteInactivados) { this.pagosAporteInactivados = pagosAporteInactivados; }
    public int getCuotasRecalculadas() { return cuotasRecalculadas; }
    public void setCuotasRecalculadas(int cuotasRecalculadas) { this.cuotasRecalculadas = cuotasRecalculadas; }
    public int getPrestamosReabiertos() { return prestamosReabiertos; }
    public void setPrestamosReabiertos(int prestamosReabiertos) { this.prestamosReabiertos = prestamosReabiertos; }
    public List<Long> getAsientosAnulados() { return asientosAnulados; }
    public void setAsientosAnulados(List<Long> asientosAnulados) { this.asientosAnulados = asientosAnulados; }
    public Long getAsientoReProvision() { return asientoReProvision; }
    public void setAsientoReProvision(Long asientoReProvision) { this.asientoReProvision = asientoReProvision; }
    public int getPagosAplicados() { return pagosAplicados; }
    public void setPagosAplicados(int pagosAplicados) { this.pagosAplicados = pagosAplicados; }
    public double getMontoPagosAplicados() { return montoPagosAplicados; }
    public void setMontoPagosAplicados(double montoPagosAplicados) { this.montoPagosAplicados = montoPagosAplicados; }
    public int getAportesAplicados() { return aportesAplicados; }
    public void setAportesAplicados(int aportesAplicados) { this.aportesAplicados = aportesAplicados; }
    public double getMontoAportesAplicados() { return montoAportesAplicados; }
    public void setMontoAportesAplicados(double montoAportesAplicados) { this.montoAportesAplicados = montoAportesAplicados; }
    public List<Long> getAsientosGenerados() { return asientosGenerados; }
    public void setAsientosGenerados(List<Long> asientosGenerados) { this.asientosGenerados = asientosGenerados; }
    public String getResumenAplicacion() { return resumenAplicacion; }
    public void setResumenAplicacion(String resumenAplicacion) { this.resumenAplicacion = resumenAplicacion; }
    public int getFilasSinParticipe() { return filasSinParticipe; }
    public void setFilasSinParticipe(int filasSinParticipe) { this.filasSinParticipe = filasSinParticipe; }
    public double getMontoSinParticipe() { return montoSinParticipe; }
    public void setMontoSinParticipe(double montoSinParticipe) { this.montoSinParticipe = montoSinParticipe; }
    public List<String> getDetalleSinParticipe() { return detalleSinParticipe; }
    public void setDetalleSinParticipe(List<String> detalleSinParticipe) { this.detalleSinParticipe = detalleSinParticipe; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
