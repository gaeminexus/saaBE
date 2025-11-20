package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.EstadoParticipeDaoService;
import com.saa.ejb.credito.service.EstadoParticipeService;
import com.saa.model.credito.EstadoParticipe;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class EstadoParticipeServiceImpl implements EstadoParticipeService {
	
	@EJB
    private EstadoParticipeDaoService EstadoParticipeDaoService;

    /**
     * Recupera un registro de EstadoParticipe por su ID.
     */
    @Override
    public EstadoParticipe selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return EstadoParticipeDaoService.selectById(id, NombreEntidadesCredito.ESTADO_PARTICIPE);
    }

    /**
     * Elimina uno o varios registros de EstadoParticipe.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de EstadoParticipeService ... depurado");
        EstadoParticipe estadoParticipe = new EstadoParticipe();
        for (Long registro : id) {
            EstadoParticipeDaoService.remove(estadoParticipe, registro);
        }
    }

    /**
     * Guarda una lista de registros de EstadoParticipe.
     */
    @Override
    public void save(List<EstadoParticipe> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de EstadoParticipeService");
        for (EstadoParticipe registro : lista) {
            EstadoParticipeDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de EstadoParticipe.
     */
    @Override
    public List<EstadoParticipe> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll EstadoParticipeService");
        List<EstadoParticipe> result = EstadoParticipeDaoService.selectAll(NombreEntidadesCredito.ESTADO_PARTICIPE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total EstadoParticipe no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de EstadoParticipe.
     */
    @Override
    public EstadoParticipe saveSingle(EstadoParticipe estadoParticipe) throws Throwable {
    	System.out.println("saveSingle - EstadoParticipe");
    	if(estadoParticipe.getCodigo() == null){
    		estadoParticipe.setIdEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
    	estadoParticipe = EstadoParticipeDaoService.save(estadoParticipe, estadoParticipe.getCodigo());
    	return estadoParticipe;
    }


    /**
     * Recupera registros de EstadoParticipe segun criterios de búsqueda.
     */
    @Override
    public List<EstadoParticipe> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria EstadoParticipeService");
        List<EstadoParticipe> result = EstadoParticipeDaoService.selectByCriteria(datos, NombreEntidadesCredito.ESTADO_PARTICIPE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio EstadoParticipe no devolvio ningun registro");
        }
        return result;
    }

}
