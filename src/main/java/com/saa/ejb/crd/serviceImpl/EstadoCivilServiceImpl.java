package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.EstadoCivilDaoService;
import com.saa.ejb.crd.service.EstadoCivilService;
import com.saa.model.crd.EstadoCivil;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class EstadoCivilServiceImpl implements EstadoCivilService {
	
	@EJB
    private EstadoCivilDaoService EstadoCivilDaoService;

    /**
     * Recupera un registro de EstadoCivil por su ID.
     */
    @Override
    public EstadoCivil selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return EstadoCivilDaoService.selectById(id, NombreEntidadesCredito.ESTADO_CIVIL);
    }

    /**
     * Elimina uno o varios registros de EstadoCivil.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de EstadoCivilService ... depurado");
        EstadoCivil estadoCivil = new EstadoCivil();
        for (Long registro : id) {
            EstadoCivilDaoService.remove(estadoCivil, registro);
        }
    }

    /**
     * Guarda una lista de registros de EstadoCivil.
     */
    @Override
    public void save(List<EstadoCivil> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de EstadoCivilService");
        for (EstadoCivil registro : lista) {
            EstadoCivilDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de EstadoCivil.
     */
    @Override
    public List<EstadoCivil> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll EstadoCivilService");
        List<EstadoCivil> result = EstadoCivilDaoService.selectAll(NombreEntidadesCredito.ESTADO_CIVIL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total EstadoCivil no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de EstadoCivil.
     */
    @Override
    public EstadoCivil saveSingle(EstadoCivil estadoCivil) throws Throwable {
    	System.out.println("saveSingle - EstadoCivil");
    	if(estadoCivil.getCodigo() == null){
    		estadoCivil.setIdEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
    	estadoCivil = EstadoCivilDaoService.save(estadoCivil, estadoCivil.getCodigo());
    	return estadoCivil;
    }


    /**
     * Recupera registros de EstadoCivil segun criterios de búsqueda.
     */
    @Override
    public List<EstadoCivil> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria EstadoCivilService");
        List<EstadoCivil> result = EstadoCivilDaoService.selectByCriteria(datos, NombreEntidadesCredito.ESTADO_CIVIL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio EstadoCivil no devolvio ningun registro");
        }
        return result;
    }

}
