package com.saa.ejb.rpr.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.ParticipeActivoG41DaoService;
import com.saa.ejb.rpr.service.ParticipeActivoG41Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.ParticipeActivoG41;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ParticipeActivoG41ServiceImpl implements ParticipeActivoG41Service {

    @EJB
    private ParticipeActivoG41DaoService participeActivoG41DaoService;

    @Override
    public ParticipeActivoG41 selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById ParticipeActivoG41 con id: " + id);
        return participeActivoG41DaoService.selectById(id, NombreEntidadesReporte.PARTICIPE_ACTIVO_G41);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ParticipeActivoG41Service");
        ParticipeActivoG41 entidad = new ParticipeActivoG41();
        for (Long registro : id) {
            participeActivoG41DaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<ParticipeActivoG41> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ParticipeActivoG41Service");
        for (ParticipeActivoG41 registro : lista) {
            participeActivoG41DaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<ParticipeActivoG41> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ParticipeActivoG41Service");
        List<ParticipeActivoG41> result = participeActivoG41DaoService.selectAll(NombreEntidadesReporte.PARTICIPE_ACTIVO_G41);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total ParticipeActivoG41 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public ParticipeActivoG41 saveSingle(ParticipeActivoG41 entidad) throws Throwable {
        System.out.println("saveSingle - ParticipeActivoG41");
        entidad = participeActivoG41DaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<ParticipeActivoG41> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria ParticipeActivoG41Service");
        List<ParticipeActivoG41> result = participeActivoG41DaoService.selectByCriteria(datos, NombreEntidadesReporte.PARTICIPE_ACTIVO_G41);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio ParticipeActivoG41 no devolvio ningun registro");
        }
        return result;
    }
}
