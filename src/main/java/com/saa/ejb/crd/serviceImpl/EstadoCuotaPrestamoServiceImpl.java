package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.EstadoCuotaPrestamoDaoService;
import com.saa.ejb.crd.service.EstadoCuotaPrestamoService;
import com.saa.model.crd.EstadoCuotaPrestamo;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación del servicio para EstadoCuotaPrestamo.
 * @author GaemiSoft
 */
@Stateless
public class EstadoCuotaPrestamoServiceImpl implements EstadoCuotaPrestamoService {
    
    @EJB
    private EstadoCuotaPrestamoDaoService estadoCuotaPrestamoDaoService;
    
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de EstadoCuotaPrestamo service");
        EstadoCuotaPrestamo estadoCuotaPrestamo = new EstadoCuotaPrestamo();
        for (Long registro : id) {
            estadoCuotaPrestamoDaoService.remove(estadoCuotaPrestamo, registro);
        }
    }
    
    @Override
    public void save(List<EstadoCuotaPrestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de EstadoCuotaPrestamo service");
        for (int i = 0; i < lista.size(); i++) {
            EstadoCuotaPrestamo estadoCuotaPrestamo = lista.get(i);
            estadoCuotaPrestamo = estadoCuotaPrestamoDaoService.save(estadoCuotaPrestamo, estadoCuotaPrestamo.getCodigo());
            lista.set(i, estadoCuotaPrestamo);
        }
    }
    
    @Override
    public List<EstadoCuotaPrestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll de EstadoCuotaPrestamo service");
        List<EstadoCuotaPrestamo> result = estadoCuotaPrestamoDaoService.selectAll(NombreEntidadesCredito.ESTADO_CUOTA_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total de EstadoCuotaPrestamo no devolvio ningun registro");
        }
        return result;
    }
    
    @Override
    public List<EstadoCuotaPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria de EstadoCuotaPrestamo");
        List<EstadoCuotaPrestamo> result = estadoCuotaPrestamoDaoService.selectByCriteria(
            datos, NombreEntidadesCredito.ESTADO_CUOTA_PRESTAMO
        );
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio de EstadoCuotaPrestamo no devolvio ningun registro");
        }
        return result;
    }
    
    @Override
    public EstadoCuotaPrestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById de EstadoCuotaPrestamo con id: " + id);
        return estadoCuotaPrestamoDaoService.selectById(id, NombreEntidadesCredito.ESTADO_CUOTA_PRESTAMO);
    }
    
    @Override
    public EstadoCuotaPrestamo saveSingle(EstadoCuotaPrestamo estadoCuotaPrestamo) throws Throwable {
        System.out.println("saveSingle - EstadoCuotaPrestamoService");
        estadoCuotaPrestamo = estadoCuotaPrestamoDaoService.save(estadoCuotaPrestamo, estadoCuotaPrestamo.getCodigo());
        return estadoCuotaPrestamo;
    }
}
