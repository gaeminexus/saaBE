package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.ejb.crd.service.ContabilidadPrestamoService;
import com.saa.ejb.crd.service.dto.ContextoPago;
import com.saa.ejb.crd.service.dto.MovimientoAporte;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.model.crd.EventoPrestamo;

import jakarta.ejb.Stateless;

/**
 * Implementación NO-OP de los hooks de contabilidad de préstamos (§9.1).
 *
 * Devuelve siempre null: no se genera asiento y el costo del hook es cero. Cuando exista la
 * implementación real (ver §9.3 para los pre-requisitos pendientes), reemplaza a esta clase
 * SIN tocar los orquestadores.
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Stateless
public class ContabilidadPrestamoNoOpImpl implements ContabilidadPrestamoService {

    @Override
    public Long contabilizarPagoCuota(ResultadoAplicacionPago resultado, ContextoPago ctx) throws Throwable {
        System.out.println("Contabilidad de préstamos no activa - hook no-op (contabilizarPagoCuota)");
        return null;
    }

    @Override
    public Long contabilizarPagoConAportes(ResultadoAplicacionPago resultado, List<MovimientoAporte> movimientos,
            ContextoPago ctx) throws Throwable {
        System.out.println("Contabilidad de préstamos no activa - hook no-op (contabilizarPagoConAportes)");
        return null;
    }

    @Override
    public Long contabilizarAbonoCapital(EventoPrestamo evento) throws Throwable {
        System.out.println("Contabilidad de préstamos no activa - hook no-op (contabilizarAbonoCapital)");
        return null;
    }

    @Override
    public Long contabilizarPrecancelacion(EventoPrestamo evento) throws Throwable {
        System.out.println("Contabilidad de préstamos no activa - hook no-op (contabilizarPrecancelacion)");
        return null;
    }

    @Override
    public Long contabilizarReverso(EventoPrestamo eventoAnulado) throws Throwable {
        System.out.println("Contabilidad de préstamos no activa - hook no-op (contabilizarReverso)");
        return null;
    }
}
