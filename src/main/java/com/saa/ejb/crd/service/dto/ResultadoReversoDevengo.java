package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.crd.Prestamo;

/**
 * Lote 20: el reverso del devengo de INTERÉS de una operación (abono, precancelación, acuerdo): las líneas del asiento (D ingreso / H interés por
 * cobrar) y el detalle por cuota que se graba en el libro (MVIC tipo 12) con el MISMO asiento. La suma de las cuotas es el valor de las líneas, al
 * centavo. Vacío si el devengo completo no está activo o no hay nada que reversar.
 */
public class ResultadoReversoDevengo {

    /** Una cuota y lo que se le reversa del devengo. */
    public static class Cuota {
        private final Long idCuota;
        private final double valor;

        public Cuota(Long idCuota, double valor) {
            this.idCuota = idCuota;
            this.valor = valor;
        }

        public Long getIdCuota() { return idCuota; }
        public double getValor() { return valor; }
    }

    private final Prestamo prestamo;
    private final String origen;
    private final Long idOrigen;
    private final List<DetalleAsiento> lineas = new ArrayList<DetalleAsiento>();
    private final List<Cuota> cuotas = new ArrayList<Cuota>();

    public ResultadoReversoDevengo(Prestamo prestamo, String origen, Long idOrigen) {
        this.prestamo = prestamo;
        this.origen = origen;
        this.idOrigen = idOrigen;
    }

    public Prestamo getPrestamo() { return prestamo; }
    public String getOrigen() { return origen; }
    public Long getIdOrigen() { return idOrigen; }
    public List<DetalleAsiento> getLineas() { return lineas; }
    public List<Cuota> getCuotas() { return cuotas; }
}
