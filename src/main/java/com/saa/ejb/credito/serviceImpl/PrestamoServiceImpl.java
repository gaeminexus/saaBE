package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.PrestamoDaoService;
import com.saa.ejb.credito.service.PrestamoService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class PrestamoServiceImpl implements PrestamoService {

    @EJB
    private PrestamoDaoService prestamoDaoService;

    /**
     * Recupera un registro de Prestamo por su ID.
     */
    @Override
    public Prestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return prestamoDaoService.selectById(id, NombreEntidadesCredito.PRESTAMO);
    }

    /**
     * Elimina uno o varios registros de Prestamo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de PrestamoService ... depurado");
        Prestamo prestamo = new Prestamo();
        for (Long registro : id) {
            prestamoDaoService.remove(prestamo, registro);
        }
    }

    /**
     * Guarda una lista de registros de Prestamo.
     */
    @Override
    public void save(List<Prestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de PrestamoService");
        for (Prestamo registro : lista) {
            prestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Prestamo.
     */
    @Override
    public List<Prestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll PrestamoService");
        List<Prestamo> result = prestamoDaoService.selectAll(NombreEntidadesCredito.PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Prestamo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Prestamo.
     */
    @Override
    public Prestamo saveSingle(Prestamo prestamo) throws Throwable {
        System.out.println("saveSingle - Prestamo");
        if(prestamo.getCodigo() == null){
        	prestamo.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        prestamo = prestamoDaoService.save(prestamo, prestamo.getCodigo());
        return prestamo;
    }

    /**
     * Recupera registros de Prestamo segun criterios de búsqueda.
     */
    @Override
    public List<Prestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria PrestamoService");
        List<Prestamo> result = prestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Prestamo no devolvio ningun registro");
        }
        return result;
    }
}
