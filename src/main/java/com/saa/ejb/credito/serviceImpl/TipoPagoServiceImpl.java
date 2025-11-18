package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TipoPagoDaoService;
import com.saa.ejb.credito.service.TipoPagoService;
import com.saa.model.credito.TipoPago;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TipoPagoServiceImpl implements TipoPagoService {

    @EJB
    private TipoPagoDaoService tipoPagoDaoService;

    /**
     * Recupera un registro de TipoPago por su ID.
     */
    @Override
    public TipoPago selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById TipoPago con id: " + id);
        return tipoPagoDaoService.selectById(id, NombreEntidadesCredito.TIPO_PAGO);
    }

    /**
     * Elimina uno o varios registros de TipoPago.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TipoPagoService ... depurado");
        TipoPago tipoPago = new TipoPago();
        for (Long registro : id) {
            tipoPagoDaoService.remove(tipoPago, registro);
        }
    }

    /**
     * Guarda una lista de registros de TipoPago.
     */
    @Override
    public void save(List<TipoPago> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TipoPagoService");
        for (TipoPago registro : lista) {
            tipoPagoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de TipoPago.
     */
    @Override
    public List<TipoPago> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TipoPagoService");
        List<TipoPago> result = tipoPagoDaoService.selectAll(NombreEntidadesCredito.TIPO_PAGO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total TipoPago no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de TipoPago.
     */
    @Override
    public TipoPago saveSingle(TipoPago tipoPago) throws Throwable {
        System.out.println("saveSingle - TipoPago");
        tipoPago = tipoPagoDaoService.save(tipoPago, tipoPago.getCodigo());
        return tipoPago;
    }

    /**
     * Recupera registros de TipoPago segun criterios de búsqueda.
     */
    @Override
    public List<TipoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TipoPagoService");
        List<TipoPago> result = tipoPagoDaoService.selectByCriteria(datos, NombreEntidadesCredito.TIPO_PAGO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio TipoPago no devolvio ningun registro");
        }
        return result;
    }
}
