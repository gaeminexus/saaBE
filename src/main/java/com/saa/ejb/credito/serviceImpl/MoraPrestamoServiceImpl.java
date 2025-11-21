package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.MoraPrestamoDaoService;
import com.saa.ejb.credito.service.MoraPrestamoService;
import com.saa.model.credito.MoraPrestamo;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class MoraPrestamoServiceImpl implements MoraPrestamoService {

    @EJB
    private MoraPrestamoDaoService moraPrestamoDaoService;

    /**
     * Recupera un registro de MoraPrestamo por su ID.
     */
    @Override
    public MoraPrestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById MoraPrestamo con id: " + id);
        return moraPrestamoDaoService.selectById(id, NombreEntidadesCredito.MORA_PRESTAMO);
    }

    /**
     * Elimina uno o varios registros de MoraPrestamo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de MoraPrestamoService ... depurado");
        MoraPrestamo mora = new MoraPrestamo();
        for (Long registro : id) {
            moraPrestamoDaoService.remove(mora, registro);
        }
    }

    /**
     * Guarda una lista de registros de MoraPrestamo.
     */
    @Override
    public void save(List<MoraPrestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de MoraPrestamoService");
        for (MoraPrestamo registro : lista) {
            moraPrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de MoraPrestamo.
     */
    @Override
    public List<MoraPrestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll MoraPrestamoService");
        List<MoraPrestamo> result = moraPrestamoDaoService.selectAll(NombreEntidadesCredito.MORA_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total MoraPrestamo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de MoraPrestamo.
     */
    @Override
    public MoraPrestamo saveSingle(MoraPrestamo mora) throws Throwable {
        System.out.println("saveSingle - MoraPrestamo");
        if (mora.getCodigo() == null) {
       }
        mora = moraPrestamoDaoService.save(mora, mora.getCodigo());
        return mora;
    }

    /**
     * Recupera registros de MoraPrestamo segun criterios de búsqueda.
     */
    @Override
    public List<MoraPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria MoraPrestamoService");
        List<MoraPrestamo> result = moraPrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.MORA_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio MoraPrestamo no devolvio ningun registro");
        }
        return result;
    }
}
