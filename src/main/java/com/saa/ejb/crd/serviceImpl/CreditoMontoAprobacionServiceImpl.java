package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.CreditoMontoAprobacionDaoService;
import com.saa.ejb.crd.service.CreditoMontoAprobacionService;
import com.saa.model.crd.CreditoMontoAprobacion;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CreditoMontoAprobacionServiceImpl implements CreditoMontoAprobacionService {
	
	@EJB
    private CreditoMontoAprobacionDaoService CreditoMontoAprobacionDaoService;

    /**
     * Recupera un registro de CreditoMontoAprobacion por su ID.
     */
    @Override
    public CreditoMontoAprobacion selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return CreditoMontoAprobacionDaoService.selectById(id, NombreEntidadesCredito.CREDITO_MONTO_APROBACION);
    }

    /**
     * Elimina uno o varios registros de CreditoMontoAprobacion.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CreditoMontoAprobacionService ... depurado");
        CreditoMontoAprobacion creditoMontoAprobacion = new CreditoMontoAprobacion();
        for (Long registro : id) {
            CreditoMontoAprobacionDaoService.remove(creditoMontoAprobacion, registro);
        }
    }

    /**
     * Guarda una lista de registros de CreditoMontoAprobacion.
     */
    @Override
    public void save(List<CreditoMontoAprobacion> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CreditoMontoAprobacionService");
        for (CreditoMontoAprobacion registro : lista) {
            CreditoMontoAprobacionDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de CreditoMontoAprobacion.
     */
    @Override
    public List<CreditoMontoAprobacion> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll CreditoMontoAprobacionService");
        List<CreditoMontoAprobacion> result = CreditoMontoAprobacionDaoService.selectAll(NombreEntidadesCredito.CREDITO_MONTO_APROBACION);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total CreditoMontoAprobacion no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de CreditoMontoAprobacion.
     */
    @Override
    public CreditoMontoAprobacion saveSingle(CreditoMontoAprobacion creditoMontoAprobacion) throws Throwable {
    	System.out.println("saveSingle - CreditoMontoAprobacion");
    	if(creditoMontoAprobacion.getCodigo() == null){
    		creditoMontoAprobacion.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
    	creditoMontoAprobacion = CreditoMontoAprobacionDaoService.save(creditoMontoAprobacion, creditoMontoAprobacion.getCodigo());
    	return creditoMontoAprobacion;
    }


    /**
     * Recupera registros de CreditoMontoAprobacion segun criterios de búsqueda.
     */
    @Override
    public List<CreditoMontoAprobacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria CreditoMontoAprobacionService");
        List<CreditoMontoAprobacion> result = CreditoMontoAprobacionDaoService.selectByCriteria(datos, NombreEntidadesCredito.CREDITO_MONTO_APROBACION);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CreditoMontoAprobacion no devolvio ningun registro");
        }
        return result;
    }

}
