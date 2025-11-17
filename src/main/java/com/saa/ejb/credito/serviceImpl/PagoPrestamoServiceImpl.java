package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.PagoPrestamoDaoService;
import com.saa.ejb.credito.service.PagoPrestamoService;
import com.saa.model.credito.PagoPrestamo;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class PagoPrestamoServiceImpl implements PagoPrestamoService {

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    /**
     * Recupera un registro de PagoPrestamo por su ID.
     */
    @Override
    public PagoPrestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return pagoPrestamoDaoService.selectById(id, NombreEntidadesCredito.PAGO_PRESTAMO);
    }

    /**
     * Elimina uno o varios registros de PagoPrestamo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de PagoPrestamoService ... depurado");
        PagoPrestamo pago = new PagoPrestamo();
        for (Long registro : id) {
            pagoPrestamoDaoService.remove(pago, registro);
        }
    }

    /**
     * Guarda una lista de registros de PagoPrestamo.
     */
    @Override
    public void save(List<PagoPrestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de PagoPrestamoService");
        for (PagoPrestamo registro : lista) {
            pagoPrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de PagoPrestamo.
     */
    @Override
    public List<PagoPrestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll PagoPrestamoService");
        List<PagoPrestamo> result = pagoPrestamoDaoService.selectAll(NombreEntidadesCredito.PAGO_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total PagoPrestamo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de PagoPrestamo.
     */
    @Override
    public PagoPrestamo saveSingle(PagoPrestamo pago) throws Throwable {
        System.out.println("saveSingle - PagoPrestamo");
        pago = pagoPrestamoDaoService.save(pago, pago.getCodigo());
        return pago;
    }

    /**
     * Recupera registros de PagoPrestamo segun criterios de búsqueda.
     */
    @Override
    public List<PagoPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria PagoPrestamoService");
        List<PagoPrestamo> result = pagoPrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.PAGO_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio PagoPrestamo no devolvio ningun registro");
        }
        return result;
    }
}
