package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TasaPrestamoDaoService;
import com.saa.ejb.credito.service.TasaPrestamoService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.TasaPrestamo;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TasaPrestamoServiceImpl implements TasaPrestamoService {

    @EJB
    private TasaPrestamoDaoService tasaPrestamoDaoService;

    /**
     * Recupera un registro de TasaPrestamo por su ID.
     */
    @Override
    public TasaPrestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById TasaPrestamo con id: " + id);
        return tasaPrestamoDaoService.selectById(id, NombreEntidadesCredito.TASA_PRESTAMO);
    }

    /**
     * Elimina uno o varios registros de TasaPrestamo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TasaPrestamoService ... depurado");
        TasaPrestamo tasaPrestamo = new TasaPrestamo();
        for (Long registro : id) {
            tasaPrestamoDaoService.remove(tasaPrestamo, registro);
        }
    }

    /**
     * Guarda una lista de registros de TasaPrestamo.
     */
    @Override
    public void save(List<TasaPrestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TasaPrestamoService");
        for (TasaPrestamo registro : lista) {
            tasaPrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de TasaPrestamo.
     */
    @Override
    public List<TasaPrestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TasaPrestamoService");
        List<TasaPrestamo> result = tasaPrestamoDaoService.selectAll(NombreEntidadesCredito.TASA_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total TasaPrestamo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de TasaPrestamo.
     */
    @Override
    public TasaPrestamo saveSingle(TasaPrestamo tasaPrestamo) throws Throwable {
        System.out.println("saveSingle - TasaPrestamo");
        if (tasaPrestamo.getCodigo() == null) {
            tasaPrestamo.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
        }
        tasaPrestamo = tasaPrestamoDaoService.save(tasaPrestamo, tasaPrestamo.getCodigo());
        return tasaPrestamo;
    }

    /**
     * Recupera registros de TasaPrestamo segun criterios de búsqueda.
     */
    @Override
    public List<TasaPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TasaPrestamoService");
        List<TasaPrestamo> result = tasaPrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.TASA_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio TasaPrestamo no devolvio ningun registro");
        }
        return result;
    }
}
