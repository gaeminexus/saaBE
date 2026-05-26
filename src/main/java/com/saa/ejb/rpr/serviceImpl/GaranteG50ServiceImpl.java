package com.saa.ejb.rpr.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.GaranteG50DaoService;
import com.saa.ejb.rpr.service.GaranteG50Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.GaranteG50;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GaranteG50ServiceImpl implements GaranteG50Service {

    @EJB
    private GaranteG50DaoService garanteG50DaoService;

    @Override
    public GaranteG50 selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById GaranteG50 con id: " + id);
        return garanteG50DaoService.selectById(id, NombreEntidadesReporte.GARANTE_G50);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de GaranteG50Service");
        GaranteG50 entidad = new GaranteG50();
        for (Long registro : id) {
            garanteG50DaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<GaranteG50> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de GaranteG50Service");
        for (GaranteG50 registro : lista) {
            garanteG50DaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<GaranteG50> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll GaranteG50Service");
        List<GaranteG50> result = garanteG50DaoService.selectAll(NombreEntidadesReporte.GARANTE_G50);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total GaranteG50 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public GaranteG50 saveSingle(GaranteG50 entidad) throws Throwable {
        System.out.println("saveSingle - GaranteG50");
        entidad = garanteG50DaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<GaranteG50> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria GaranteG50Service");
        List<GaranteG50> result = garanteG50DaoService.selectByCriteria(datos, NombreEntidadesReporte.GARANTE_G50);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio GaranteG50 no devolvio ningun registro");
        }
        return result;
    }
}
