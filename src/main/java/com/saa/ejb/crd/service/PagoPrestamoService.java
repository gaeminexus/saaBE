package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.PagoPrestamo;

import jakarta.ejb.Local;

@Local
public interface PagoPrestamoService extends EntityService<PagoPrestamo>{

    /**
     * Reconstruye el saldo de capital pendiente sumando, sobre cada cuota de {@code cuotas}, el
     * capital ORIGINALMENTE programado ({@code DTPRCPTL}, {@link DetallePrestamo#getCapital()})
     * menos lo efectivamente pagado con pagos VIGENTES ({@code PGPRANUL = 0}) de esa cuota
     * específica ({@code CRD.PGPR}, nunca {@code DTPRCPPG} ni {@code DTPRSICP}), sin bajar de
     * cero por cuota.
     *
     * Único lugar donde vive este cálculo (pedido 8, segunda ola, 2026-08-27): tanto la
     * reestructuración ({@code SimulacionPrestamoServiceImpl}) como la precancelación
     * ({@code ProcesoPagoPrestamoServiceImpl}) lo llaman en vez de duplicarlo. Pasar la lista de
     * cuotas que corresponda a cada caso (todas las pendientes, solo las exigibles, etc.) — el
     * método no decide cuáles pendientes son, solo suma sobre las que reciba.
     *
     * @param cuotas cuotas sobre las que sumar (normalmente el resultado de
     *               {@code DetallePrestamoDaoService.selectCuotasNoPagadasByPrestamo} o
     *               {@code selectCuotasPendientesByPrestamoOrdenadas} — mismo filtro JPQL)
     * @return la suma, redondeada a 2 decimales; 0.0 si {@code cuotas} es nula o vacía
     * @throws Throwable Si ocurre algún error
     */
    double calcularSaldoCapitalPendiente(List<DetallePrestamo> cuotas) throws Throwable;

}
