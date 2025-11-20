package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.EstadoCesantiaDaoService;
import com.saa.ejb.credito.service.EstadoCesantiaService;
import com.saa.model.credito.EstadoCesantia;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class EstadoCesantiaServiceImpl implements EstadoCesantiaService {
	
	@EJB
    private EstadoCesantiaDaoService EstadoCesantiaDaoService;

    /**
     * Recupera un registro de EstadoCesantia por su ID.
     */
    @Override
    public EstadoCesantia selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return EstadoCesantiaDaoService.selectById(id, NombreEntidadesCredito.ESTADO_CESANTIA);
    }

    /**
     * Elimina uno o varios registros de EstadoCesantia.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de EstadoCesantiaService ... depurado");
        EstadoCesantia estadoCesantia = new EstadoCesantia();
        for (Long registro : id) {
            EstadoCesantiaDaoService.remove(estadoCesantia, registro);
        }
    }

    /**
     * Guarda una lista de registros de EstadoCesantia.
     */
    @Override
    public void save(List<EstadoCesantia> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de EstadoCesantiaService");
        for (EstadoCesantia registro : lista) {
            EstadoCesantiaDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de EstadoCesantia.
     */
    @Override
    public List<EstadoCesantia> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll EstadoCesantiaService");
        List<EstadoCesantia> result = EstadoCesantiaDaoService.selectAll(NombreEntidadesCredito.ESTADO_CESANTIA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total EstadoCesantia no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de EstadoCesantia.
     */
    @Override
    public EstadoCesantia saveSingle(EstadoCesantia estadoCesantia) throws Throwable {
    	System.out.println("saveSingle - EstadoCesantia");
    	if(estadoCesantia.getCodigo() == null){
    		estadoCesantia.setIdEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
    	estadoCesantia = EstadoCesantiaDaoService.save(estadoCesantia, estadoCesantia.getCodigo());
    	return estadoCesantia;
    }


    /**
     * Recupera registros de EstadoCesantia segun criterios de búsqueda.
     */
    @Override
    public List<EstadoCesantia> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria EstadoCesantiaService");
        List<EstadoCesantia> result = EstadoCesantiaDaoService.selectByCriteria(datos, NombreEntidadesCredito.ESTADO_CESANTIA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio EstadoCesantia no devolvio ningun registro");
        }
        return result;
    }

}
