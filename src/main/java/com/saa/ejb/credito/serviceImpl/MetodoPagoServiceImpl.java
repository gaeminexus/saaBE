package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.MetodoPagoDaoService;
import com.saa.ejb.credito.service.MetodoPagoService;
import com.saa.model.credito.MetodoPago;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class MetodoPagoServiceImpl implements MetodoPagoService {

    @EJB
    private MetodoPagoDaoService metodoPagoDaoService;

    /**
     * Recupera un registro de MetodoPago por su ID.
     */
    @Override
    public MetodoPago selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById MetodoPago con id: " + id);
        return metodoPagoDaoService.selectById(id, NombreEntidadesCredito.METODO_PAGO);
    }

    /**
     * Elimina uno o varios registros de MetodoPago.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de MetodoPagoService ... depurado");
        MetodoPago metodoPago = new MetodoPago();
        for (Long registro : id) {
            metodoPagoDaoService.remove(metodoPago, registro);
        }
    }

    /**
     * Guarda una lista de registros de MetodoPago.
     */
    @Override
    public void save(List<MetodoPago> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de MetodoPagoService");
        for (MetodoPago registro : lista) {
            metodoPagoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de MetodoPago.
     */
    @Override
    public List<MetodoPago> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll MetodoPagoService");
        List<MetodoPago> result = metodoPagoDaoService.selectAll(NombreEntidadesCredito.METODO_PAGO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total MetodoPago no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de MetodoPago.
     */
    @Override
    public MetodoPago saveSingle(MetodoPago metodoPago) throws Throwable {
        System.out.println("saveSingle - MetodoPago");
        if(metodoPago.getCodigo() == null){
        	metodoPago.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        metodoPago = metodoPagoDaoService.save(metodoPago, metodoPago.getCodigo());
        return metodoPago;
    }

    /**
     * Recupera registros de MetodoPago segun criterios de búsqueda.
     */
    @Override
    public List<MetodoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria MetodoPagoService");
        List<MetodoPago> result = metodoPagoDaoService.selectByCriteria(datos, NombreEntidadesCredito.METODO_PAGO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio MetodoPago no devolvio ningun registro");
        }
        return result;
    }
}
