package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.ExterDaoService;
import com.saa.ejb.crd.service.ExterService;
import com.saa.model.crd.Exter;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ExterServiceImpl implements ExterService {
	
	@EJB
    private ExterDaoService exterDaoService;

    /**
     * Recupera un registro de Exter por su ID.
     */
    @Override
    public Exter selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return exterDaoService.selectById(id, NombreEntidadesCredito.EXTER);
    }

    /**
     * Elimina uno o varios registros de Exter.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ExterService ... depurado");
        Exter exter = new Exter();
        for (Long registro : id) {
        	exterDaoService.remove(exter, registro);
        }
    }

    /**
     * Guarda una lista de registros de Exter.
     */
    @Override
    public void save(List<Exter> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ExterService");
        for (Exter registro : lista) {
        	exterDaoService.save(registro, Long.valueOf(registro.getEstado()));
        }
    }

    /**
     * Recupera todos los registros de Exter.
     */
    @Override
    public List<Exter> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ExterService");
        List<Exter> result = exterDaoService.selectAll(NombreEntidadesCredito.EXTER);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Exter no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Exter.
     */
    @Override
    public Exter saveSingle(Exter exter) throws Throwable {
    	System.out.println("saveSingle - Exter");
    	exter = exterDaoService.save(exter, Long.valueOf(exter.getEstado()));
    	return exter;
    }


    /**
     * Recupera registros de Exter segun criterios de búsqueda.
     */
    @Override
    public List<Exter> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria ExterService");
        List<Exter> result = exterDaoService.selectByCriteria(datos, NombreEntidadesCredito.EXTER);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Exter no devolvio ningun registro");
        }
        return result;
    }

}
