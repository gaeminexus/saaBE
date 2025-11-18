package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.AporteDaoService;
import com.saa.ejb.credito.service.AporteService;
import com.saa.model.credito.Aporte;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class AporteServiceImpl implements AporteService {

    @EJB
    private AporteDaoService aporteDaoService;

    /**
     * Recupera un registro de Aporte por su ID.
     */
    @Override
    public Aporte selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById Aporte con id: " + id);
        return aporteDaoService.selectById(id, NombreEntidadesCredito.APORTE);
    }

    /**
     * Elimina uno o varios registros de Aporte.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de AporteService ... depurado");
        Aporte aporte = new Aporte();
        for (Long registro : id) {
            aporteDaoService.remove(aporte, registro);
        }
    }

    /**
     * Guarda una lista de registros de Aporte.
     */
    @Override
    public void save(List<Aporte> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de AporteService");
        for (Aporte registro : lista) {
            aporteDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Aporte.
     */
    @Override
    public List<Aporte> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll AporteService");
        List<Aporte> result = aporteDaoService.selectAll(NombreEntidadesCredito.APORTE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Aporte no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Aporte.
     */
    @Override
    public Aporte saveSingle(Aporte aporte) throws Throwable {
        System.out.println("saveSingle - Aporte");
        aporte = aporteDaoService.save(aporte, aporte.getCodigo());
        return aporte;
    }

    /**
     * Recupera registros de Aporte segun criterios de búsqueda.
     */
    @Override
    public List<Aporte> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria AporteService");
        List<Aporte> result = aporteDaoService.selectByCriteria(datos, NombreEntidadesCredito.APORTE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Aporte no devolvio ningun registro");
        }
        return result;
    }
}
