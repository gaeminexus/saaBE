package com.saa.ejb.credito.serviceImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.EntidadDaoService;
import com.saa.ejb.credito.service.EntidadService;
import com.saa.model.credito.Entidad;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class EntidadServiceImpl implements EntidadService {

    @EJB
    private EntidadDaoService entidadDaoService;

    /**
     * Recupera un registro de Entidad por su ID.
     */
    @Override
    public Entidad selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return entidadDaoService.selectById(id, NombreEntidadesCredito.ENTIDAD);
    }

    /**
     * Elimina uno o varios registros de Entidad.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de EntidadService ... depurado");
        Entidad entidad = new Entidad();
        for (Long registro : id) {
            entidadDaoService.remove(entidad, registro);
        }
    }

    /**
     * Guarda una lista de registros de Entidad.
     */
    @Override
    public void save(List<Entidad> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de EntidadService");
        for (Entidad registro : lista) {
            entidadDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Entidad.
     */
    @Override
    public List<Entidad> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll EntidadService");
        List<Entidad> result = entidadDaoService.selectAll(NombreEntidadesCredito.ENTIDAD);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Entidad no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Entidad.
     */
    @Override
    public Entidad saveSingle(Entidad entidad) throws Throwable {
        System.out.println("saveSingle - Entidad");
        if(entidad.getCodigo() == null){
        	entidad.setIdEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        entidad = entidadDaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    /**
     * Recupera registros de Entidad segun criterios de búsqueda.
     */
    @Override
    public List<Entidad> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria EntidadService");
        List<Entidad> result = entidadDaoService.selectByCriteria(datos, NombreEntidadesCredito.ENTIDAD);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Entidad no devolvio ningun registro");
        }
        return result;
    }

	@Override
	public List<Entidad> selectCoincidenciasByNombre(String nombre) throws Throwable {
		System.out.println("selectCoincidenciasByNombre");
		List<Entidad> entidades = new ArrayList<>();
        List<BigDecimal> result = entidadDaoService.selectCoincidenciasByNombre(nombre);
        if (result.isEmpty()) {
            throw new IncomeException("No existen coincidencias para el nombre proporcionado");
        } else {
        	for (BigDecimal codigo : result) {
				Entidad entidad = entidadDaoService.selectById(codigo.longValue(), NombreEntidadesCredito.ENTIDAD);
				entidades.add(entidad);
			}
        }
        return entidades;
	}

}
