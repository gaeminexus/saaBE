package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.CantonDaoService;
import com.saa.ejb.credito.service.CantonService;
import com.saa.model.credito.Canton;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CantonServiceImpl implements CantonService  {
	
	@EJB
    private CantonDaoService CantonDaoService;

    /**
     * Recupera un registro de Canton por su ID.
     */
    @Override
    public Canton selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return CantonDaoService.selectById(id, NombreEntidadesCredito.CANTON);
    }

    /**
     * Elimina uno o varios registros de Canton.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CantonService ... depurado");
        Canton canton = new Canton();
        for (Long registro : id) {
            CantonDaoService.remove(canton, registro);
        }
    }

    /**
     * Guarda una lista de registros de Canton.
     */
    @Override
    public void save(List<Canton> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CantonService");
        for (Canton registro : lista) {
            CantonDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Canton.
     */
    @Override
    public List<Canton> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll CantonService");
        List<Canton> result = CantonDaoService.selectAll(NombreEntidadesCredito.CANTON);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Canton no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Canton.
     */
    @Override
    public Canton saveSingle(Canton canton) throws Throwable {
    	System.out.println("saveSingle - Canton");
    	canton = CantonDaoService.save(canton, canton.getCodigo());
    	return canton;
    }


    /**
     * Recupera registros de Canton segun criterios de búsqueda.
     */
    @Override
    public List<Canton> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria CantonService");
        List<Canton> result = CantonDaoService.selectByCriteria(datos, NombreEntidadesCredito.CANTON);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Canton no devolvio ningun registro");
        }
        return result;
    }

}
