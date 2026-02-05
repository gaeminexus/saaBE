package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.EstadoPrestamoDaoService;
import com.saa.ejb.credito.service.EstadoPrestamoService;
import com.saa.model.crd.EstadoPrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class EstadoPrestamoServiceImpl implements EstadoPrestamoService {
	
	@EJB
    private EstadoPrestamoDaoService EstadoPrestamoDaoService;

    /**
     * Recupera un registro de EstadoPrestamo por su ID.
     */
    @Override
    public EstadoPrestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return EstadoPrestamoDaoService.selectById(id, NombreEntidadesCredito.ESTADO_PRESTAMO);
    }

    /**
     * Elimina uno o varios registros de EstadoPrestamo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de EstadoPrestamoService ... depurado");
        EstadoPrestamo estadoPrestamo = new EstadoPrestamo();
        for (Long registro : id) {
            EstadoPrestamoDaoService.remove(estadoPrestamo, registro);
        }
    }

    /**
     * Guarda una lista de registros de EstadoPrestamo.
     */
    @Override
    public void save(List<EstadoPrestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de EstadoPrestamoService");
        for (EstadoPrestamo registro : lista) {
            EstadoPrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de EstadoPrestamo.
     */
    @Override
    public List<EstadoPrestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll EstadoPrestamoService");
        List<EstadoPrestamo> result = EstadoPrestamoDaoService.selectAll(NombreEntidadesCredito.ESTADO_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total EstadoPrestamo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de EstadoPrestamo.
     */
    @Override
    public EstadoPrestamo saveSingle(EstadoPrestamo estadoPrestamo) throws Throwable {
    	System.out.println("saveSingle - EstadoPrestamo");
    	if(estadoPrestamo.getCodigo() == null){
    		estadoPrestamo.setIdEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
    	estadoPrestamo = EstadoPrestamoDaoService.save(estadoPrestamo, estadoPrestamo.getCodigo());
    	return estadoPrestamo;
    }


    /**
     * Recupera registros de EstadoPrestamo segun criterios de búsqueda.
     */
    @Override
    public List<EstadoPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria EstadoPrestamoService");
        List<EstadoPrestamo> result = EstadoPrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.ESTADO_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio EstadoPrestamo no devolvio ningun registro");
        }
        return result;
    }

}
