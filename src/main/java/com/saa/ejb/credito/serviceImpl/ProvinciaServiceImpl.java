package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.ProvinciaDaoService;
import com.saa.ejb.credito.service.ProvinciaService;
import com.saa.model.credito.Provincia;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ProvinciaServiceImpl implements ProvinciaService {

    @EJB
    private ProvinciaDaoService provinciaDaoService;

    /**
     * Recupera un registro de Provincia por su ID.
     */
    @Override
    public Provincia selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById Provincia con id: " + id);
        return provinciaDaoService.selectById(id, NombreEntidadesCredito.PROVINCIA);
    }

    /**
     * Elimina uno o varios registros de Provincia.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ProvinciaService ... depurado");
        Provincia provincia = new Provincia();
        for (Long registro : id) {
            provinciaDaoService.remove(provincia, registro);
        }
    }

    /**
     * Guarda una lista de registros de Provincia.
     */
    @Override
    public void save(List<Provincia> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ProvinciaService");
        for (Provincia registro : lista) {
            provinciaDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Provincia.
     */
    @Override
    public List<Provincia> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ProvinciaService");
        List<Provincia> result = provinciaDaoService.selectAll(NombreEntidadesCredito.PROVINCIA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Provincia no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Provincia.
     */
    @Override
    public Provincia saveSingle(Provincia provincia) throws Throwable {
        System.out.println("saveSingle - Provincia");
        if (provincia.getCodigo() == null) {
            provincia.setEstado(Long.valueOf(Estado.ACTIVO)); // Activo
        }
        provincia = provinciaDaoService.save(provincia, provincia.getCodigo());
        return provincia;
    }

    /**
     * Recupera registros de Provincia segun criterios de búsqueda.
     */
    @Override
    public List<Provincia> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria ProvinciaService");
        List<Provincia> result = provinciaDaoService.selectByCriteria(datos, NombreEntidadesCredito.PROVINCIA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Provincia no devolvio ningun registro");
        }
        return result;
    }
}
