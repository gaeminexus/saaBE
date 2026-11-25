package com.saa.ejb.crd.service.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.crd.Prestamo;

/**
 * Lote 15 (D-195): lo que {@code ReversoMoraCancelacionService.calcular} devuelve: las líneas que van al asiento del evento que cancela
 * el préstamo (D 5.1.02 / H 1.4.02 por la mora devengada y no cobrada; D 1.4.99.05 / H 4.7.05.10 por su provisión) y el detalle por cuota
 * con el que, después de generar el asiento, {@code registrar} escribe los movimientos del libro (CRD.MVIC).
 */
public class ResultadoReversoMora implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Mora de una cuota a reversar. */
    public static class Cuota implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Long idCuota;
        private final double devengo;
        private final double provision;

        public Cuota(Long idCuota, double devengo, double provision) {
            this.idCuota = idCuota;
            this.devengo = devengo;
            this.provision = provision;
        }

        public Long getIdCuota() { return idCuota; }
        /** Mora devengada y no cobrada de la cuota (MVIC tipo 6). */
        public double getDevengo() { return devengo; }
        /** Saldo provisionado de mora de la cuota (MVIC tipo 3). */
        public double getProvision() { return provision; }
    }

    private Prestamo prestamo;
    private LocalDate fecha;
    private String origen;
    private Long idOrigen;
    private double totalDevengo;
    private double totalProvision;
    private List<DetalleAsiento> lineas = new ArrayList<DetalleAsiento>();
    private List<Cuota> cuotas = new ArrayList<Cuota>();

    /** {@code true} si no hay nada que reversar (sin líneas). */
    public boolean estaVacio() {
        return lineas.isEmpty();
    }

    public Prestamo getPrestamo() { return prestamo; }
    public void setPrestamo(Prestamo prestamo) { this.prestamo = prestamo; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public Long getIdOrigen() { return idOrigen; }
    public void setIdOrigen(Long idOrigen) { this.idOrigen = idOrigen; }
    public double getTotalDevengo() { return totalDevengo; }
    public void setTotalDevengo(double totalDevengo) { this.totalDevengo = totalDevengo; }
    public double getTotalProvision() { return totalProvision; }
    public void setTotalProvision(double totalProvision) { this.totalProvision = totalProvision; }
    public List<DetalleAsiento> getLineas() { return lineas; }
    public void setLineas(List<DetalleAsiento> lineas) { this.lineas = lineas; }
    public List<Cuota> getCuotas() { return cuotas; }
    public void setCuotas(List<Cuota> cuotas) { this.cuotas = cuotas; }
}
