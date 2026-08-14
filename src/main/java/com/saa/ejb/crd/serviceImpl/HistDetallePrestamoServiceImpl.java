package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.HistDetallePrestamoDaoService;
import com.saa.ejb.crd.service.HistDetallePrestamoService;
import com.saa.model.crd.HistDetallePrestamo;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación Service para HistDetallePrestamo (HDTP).
 *
 * @author Sistema SAA
 * @since 2026-08-13
 */
@Stateless
public class HistDetallePrestamoServiceImpl implements HistDetallePrestamoService {

    @EJB
    private HistDetallePrestamoDaoService histDetallePrestamoDaoService;

    @Override
    public HistDetallePrestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById HistDetallePrestamo con id: " + id);
        return histDetallePrestamoDaoService.selectById(id, NombreEntidadesCredito.HIST_DETALLE_PRESTAMO);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de HistDetallePrestamoService");
        HistDetallePrestamo historico = new HistDetallePrestamo();
        for (Long id : ids) {
            histDetallePrestamoDaoService.remove(historico, id);
        }
    }

    @Override
    public void save(List<HistDetallePrestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de HistDetallePrestamoService");
        for (HistDetallePrestamo registro : lista) {
            histDetallePrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<HistDetallePrestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll HistDetallePrestamoService");
        List<HistDetallePrestamo> result = histDetallePrestamoDaoService.selectAll(NombreEntidadesCredito.HIST_DETALLE_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total HistDetallePrestamo no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public HistDetallePrestamo saveSingle(HistDetallePrestamo historico) throws Throwable {
        System.out.println("saveSingle - HistDetallePrestamo");
        historico = histDetallePrestamoDaoService.save(historico, historico.getCodigo());
        return historico;
    }

    @Override
    public List<HistDetallePrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria HistDetallePrestamoService");
        List<HistDetallePrestamo> result = histDetallePrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.HIST_DETALLE_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio HistDetallePrestamo no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<HistDetallePrestamo> listarPorEvento(Long codigoEvento) throws Throwable {
        System.out.println("Service: Listando cuotas historizadas del evento: " + codigoEvento);
        return histDetallePrestamoDaoService.selectByEvento(codigoEvento);
    }

    @Override
    public List<HistDetallePrestamo> listarPorPrestamo(Long codigoPrestamo) throws Throwable {
        System.out.println("Service: Listando cuotas historizadas del préstamo: " + codigoPrestamo);
        return histDetallePrestamoDaoService.selectByPrestamo(codigoPrestamo);
    }
}
