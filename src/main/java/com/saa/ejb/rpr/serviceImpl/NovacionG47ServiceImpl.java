package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.NovacionG47DaoService;
import com.saa.ejb.rpr.service.NovacionG47Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.NovacionG47;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class NovacionG47ServiceImpl implements NovacionG47Service {

    @EJB
    private NovacionG47DaoService novacionG47DaoService;

    @Override
    public NovacionG47 selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById NovacionG47 con id: " + id);
        return novacionG47DaoService.selectById(id, NombreEntidadesReporte.NOVACION_G47);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de NovacionG47Service");
        NovacionG47 entidad = new NovacionG47();
        for (Long registro : id) {
            novacionG47DaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<NovacionG47> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de NovacionG47Service");
        for (NovacionG47 registro : lista) {
            novacionG47DaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<NovacionG47> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll NovacionG47Service");
        List<NovacionG47> result = novacionG47DaoService.selectAll(NombreEntidadesReporte.NOVACION_G47);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total NovacionG47 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public NovacionG47 saveSingle(NovacionG47 entidad) throws Throwable {
        System.out.println("saveSingle - NovacionG47");
        entidad = novacionG47DaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<NovacionG47> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria NovacionG47Service");
        List<NovacionG47> result = novacionG47DaoService.selectByCriteria(datos, NombreEntidadesReporte.NOVACION_G47);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio NovacionG47 no devolvio ningun registro");
        }
        return result;
    }
}
