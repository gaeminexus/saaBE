package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Sección «Solicitudes» del dashboard de préstamos (DISENO-NOVACION-EN-CURSO-Y-DASHBOARD-SOLICITUDES.md §2): cantidad y
 * monto solicitado por etapa del ciclo de otorgamiento, en un rango de fechas. Solo lectura, una consulta agregada.
 *
 * <p>Fecha de referencia = fecha de registro del préstamo ({@code PRSTFCRG}; si falta, {@code PRSTFCHA} o {@code PRSTFCIN}),
 * salvo {@code desembolsadas}, que usa la fecha del desembolso ({@code PRSTFCAC}; si no la tiene, la de registro).</p>
 */
public class ResumenSolicitudesPrestamo {

    /** Cantidad y monto (monto solicitado, PRSTMNSL). */
    public static class Cifra {
        private long cantidad;
        private double monto;

        public Cifra() { }
        public Cifra(long cantidad, double monto) { this.cantidad = cantidad; this.monto = monto; }
        public long getCantidad() { return cantidad; }
        public void setCantidad(long cantidad) { this.cantidad = cantidad; }
        public double getMonto() { return monto; }
        public void setMonto(double monto) { this.monto = monto; }
    }

    private LocalDate desde;
    private LocalDate hasta;
    private Long idFilial;
    private Long idProducto;
    /** Todas las registradas en el rango, en cualquier estado. */
    private Cifra recibidas = new Cifra();
    /** Estados 6 PENDIENTE DE APROBACIÓN y 1 GENERADO. */
    private Cifra enTramite = new Cifra();
    /** Estado 14 APROBADO, pendiente de desembolso. */
    private Cifra aprobadas = new Cifra();
    /** El dinero salió (estados 2, 3, 4, 5, 8, 9, 10, 11, 13) con fecha de desembolso (o de registro) en el rango. */
    private Cifra desembolsadas = new Cifra();
    /** Estado 7 RECHAZADO. */
    private Cifra rechazadas = new Cifra();
    /** Estado 12 ANULADO. */
    private Cifra anuladas = new Cifra();

    public LocalDate getDesde() { return desde; }
    public void setDesde(LocalDate desde) { this.desde = desde; }
    public LocalDate getHasta() { return hasta; }
    public void setHasta(LocalDate hasta) { this.hasta = hasta; }
    public Long getIdFilial() { return idFilial; }
    public void setIdFilial(Long idFilial) { this.idFilial = idFilial; }
    public Long getIdProducto() { return idProducto; }
    public void setIdProducto(Long idProducto) { this.idProducto = idProducto; }
    public Cifra getRecibidas() { return recibidas; }
    public void setRecibidas(Cifra recibidas) { this.recibidas = recibidas; }
    public Cifra getEnTramite() { return enTramite; }
    public void setEnTramite(Cifra enTramite) { this.enTramite = enTramite; }
    public Cifra getAprobadas() { return aprobadas; }
    public void setAprobadas(Cifra aprobadas) { this.aprobadas = aprobadas; }
    public Cifra getDesembolsadas() { return desembolsadas; }
    public void setDesembolsadas(Cifra desembolsadas) { this.desembolsadas = desembolsadas; }
    public Cifra getRechazadas() { return rechazadas; }
    public void setRechazadas(Cifra rechazadas) { this.rechazadas = rechazadas; }
    public Cifra getAnuladas() { return anuladas; }
    public void setAnuladas(Cifra anuladas) { this.anuladas = anuladas; }
}
