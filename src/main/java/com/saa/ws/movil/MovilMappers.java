package com.saa.ws.movil;

import com.saa.ejb.crd.service.dto.SaldoPrestamoResumen;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.ws.movil.dto.CuotaPrestamoMovilDTO;
import com.saa.ws.movil.dto.PrestamoMovilDTO;

/**
 * Mapeos entidad → DTO compartidos entre recursos de {@code /movil} (evita que
 * {@link PrestamoMovilRest} y {@link CuentaIndividualMovilRest} recorten el mismo
 * {@code Prestamo} cada uno a su manera y terminen divergiendo).
 */
final class MovilMappers {

    private MovilMappers() {
    }

    /**
     * {@code saldo} viene de {@code PrestamoService.calcularSaldosEnLote(...)}, calculado por el
     * llamador (esta clase no puede inyectar EJB). {@code null} si el lote no trajo resultado para
     * este préstamo.
     *
     * <p>{@code saldoCapital}/{@code saldoTotal} NUNCA salen de {@code Prestamo.getSaldoCapital()}/
     * {@code getSaldoTotal()} ({@code PRST.PRSTSLCP}/{@code PRSTSLTT}): son columnas muertas que
     * ninguna línea del backend escribe (CONTRATO-INTRANET-MOVIL.md §8). {@code saldoInteres},
     * {@code saldoPorVencer} y {@code saldoVencido} tampoco los calcula el motor — van en
     * {@code null}, nunca con el valor de esas columnas: un {@code null} es honesto, un número
     * inventado es exactamente el defecto que esto corrige. La app no los usa.</p>
     */
    static PrestamoMovilDTO aDTO(Prestamo prestamo, SaldoPrestamoResumen saldo) {
        PrestamoMovilDTO dto = new PrestamoMovilDTO();
        dto.setCodigo(prestamo.getCodigo());
        dto.setIdAsoprep(prestamo.getIdAsoprep());
        if (prestamo.getProducto() != null) {
            dto.setIdProducto(prestamo.getProducto().getCodigo());
            dto.setNombreProducto(prestamo.getProducto().getNombre());
        }
        dto.setFecha(prestamo.getFecha());
        dto.setFechaInicio(prestamo.getFechaInicio());
        dto.setFechaFin(prestamo.getFechaFin());
        dto.setPlazo(prestamo.getPlazo());
        dto.setMontoSolicitado(prestamo.getMontoSolicitado());
        dto.setValorCuota(prestamo.getValorCuota());
        dto.setTasa(prestamo.getTasa());
        dto.setTotalPagado(prestamo.getTotalPagado());
        dto.setSaldoCapital(saldo != null ? saldo.getSaldoCapital() : null);
        dto.setSaldoTotal(saldo != null ? saldo.getSaldoTotal() : null);
        dto.setSaldoInteres(null);
        dto.setSaldoPorVencer(null);
        dto.setSaldoVencido(null);
        dto.setMoraCalculada(prestamo.getMoraCalculada());
        dto.setDiasVencido(prestamo.getDiasVencido());
        // idEstado - PRSTIDST, el estado vigente. NUNCA estadoPrestamo (ESPSCDGO) - ver CLAUDE.md.
        dto.setIdEstado(prestamo.getIdEstado());
        return dto;
    }

    static CuotaPrestamoMovilDTO aDTO(DetallePrestamo cuota) {
        CuotaPrestamoMovilDTO dto = new CuotaPrestamoMovilDTO();
        dto.setCodigo(cuota.getCodigo());
        dto.setNumeroCuota(cuota.getNumeroCuota());
        dto.setFechaVencimiento(cuota.getFechaVencimiento());
        dto.setCapital(cuota.getCapital());
        dto.setInteres(cuota.getInteres());
        dto.setMora(cuota.getMora());
        dto.setCuota(cuota.getCuota());
        dto.setSaldoCapital(cuota.getSaldoCapital());
        dto.setSaldo(cuota.getSaldo());
        dto.setEstado(cuota.getEstado());
        dto.setFechaPagado(cuota.getFechaPagado());
        dto.setCapitalPagado(cuota.getCapitalPagado());
        dto.setInteresPagado(cuota.getInteresPagado());
        dto.setDiasMora(cuota.getDiasMora());
        return dto;
    }
}
