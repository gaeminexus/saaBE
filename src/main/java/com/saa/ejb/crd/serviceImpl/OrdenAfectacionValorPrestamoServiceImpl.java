package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.OrdenAfectacionValorPrestamoDaoService;
import com.saa.ejb.crd.service.OrdenAfectacionValorPrestamoService;
import com.saa.model.crd.OrdenAfectacionValorPrestamo;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación del servicio para OrdenAfectacionValorPrestamo.
 * @author GaemiSoft
 */
@Stateless
public class OrdenAfectacionValorPrestamoServiceImpl implements OrdenAfectacionValorPrestamoService {
    
    @EJB
    private OrdenAfectacionValorPrestamoDaoService ordenAfectacionValorPrestamoDaoService;
    
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de OrdenAfectacionValorPrestamo service");
        OrdenAfectacionValorPrestamo ordenAfectacion = new OrdenAfectacionValorPrestamo();
        for (Long registro : id) {
            ordenAfectacionValorPrestamoDaoService.remove(ordenAfectacion, registro);
        }
    }
    
    @Override
    public void save(List<OrdenAfectacionValorPrestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de OrdenAfectacionValorPrestamo service");
        for (int i = 0; i < lista.size(); i++) {
            OrdenAfectacionValorPrestamo ordenAfectacion = lista.get(i);
            ordenAfectacion = ordenAfectacionValorPrestamoDaoService.save(ordenAfectacion, ordenAfectacion.getCodigo());
            lista.set(i, ordenAfectacion);
        }
    }
    
    @Override
    public List<OrdenAfectacionValorPrestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll de OrdenAfectacionValorPrestamo service");
        List<OrdenAfectacionValorPrestamo> result = ordenAfectacionValorPrestamoDaoService.selectAll(
            NombreEntidadesCredito.ORDEN_AFECTACION_VALOR_PRESTAMO
        );
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total de OrdenAfectacionValorPrestamo no devolvio ningun registro");
        }
        return result;
    }
    
    @Override
    public List<OrdenAfectacionValorPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria de OrdenAfectacionValorPrestamo");
        List<OrdenAfectacionValorPrestamo> result = ordenAfectacionValorPrestamoDaoService.selectByCriteria(
            datos, NombreEntidadesCredito.ORDEN_AFECTACION_VALOR_PRESTAMO
        );
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio de OrdenAfectacionValorPrestamo no devolvio ningun registro");
        }
        return result;
    }
    
    @Override
    public OrdenAfectacionValorPrestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById de OrdenAfectacionValorPrestamo con id: " + id);
        return ordenAfectacionValorPrestamoDaoService.selectById(id, NombreEntidadesCredito.ORDEN_AFECTACION_VALOR_PRESTAMO);
    }
    
    @Override
    public List<OrdenAfectacionValorPrestamo> selectAllOrdenado() throws Throwable {
        System.out.println("Ingresa al selectAllOrdenado de OrdenAfectacionValorPrestamo");
        List<OrdenAfectacionValorPrestamo> result = ordenAfectacionValorPrestamoDaoService.selectAllOrdenado();
        if (result.isEmpty()) {
            throw new IncomeException("No se encontraron registros de OrdenAfectacionValorPrestamo");
        }
        return result;
    }
    
    @Override
    public OrdenAfectacionValorPrestamo saveSingle(OrdenAfectacionValorPrestamo ordenAfectacion) throws Throwable {
        System.out.println("saveSingle - OrdenAfectacionValorPrestamoService");
        ordenAfectacion = ordenAfectacionValorPrestamoDaoService.save(ordenAfectacion, ordenAfectacion.getCodigo());
        return ordenAfectacion;
    }
}
