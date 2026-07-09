package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.NuevoParticipeG45DaoService;
import com.saa.ejb.rpr.service.NuevoParticipeG45Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.NuevoParticipeG45;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class NuevoParticipeG45ServiceImpl implements NuevoParticipeG45Service {

    @EJB
    private NuevoParticipeG45DaoService nuevoParticipeG45DaoService;

    @Override
    public NuevoParticipeG45 selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById NuevoParticipeG45 con id: " + id);
        return nuevoParticipeG45DaoService.selectById(id, NombreEntidadesReporte.NUEVO_PARTICIPE_G45);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de NuevoParticipeG45Service");
        NuevoParticipeG45 entidad = new NuevoParticipeG45();
        for (Long registro : id) {
            nuevoParticipeG45DaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<NuevoParticipeG45> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de NuevoParticipeG45Service");
        for (NuevoParticipeG45 registro : lista) {
            nuevoParticipeG45DaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<NuevoParticipeG45> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll NuevoParticipeG45Service");
        List<NuevoParticipeG45> result = nuevoParticipeG45DaoService.selectAll(NombreEntidadesReporte.NUEVO_PARTICIPE_G45);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total NuevoParticipeG45 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public NuevoParticipeG45 saveSingle(NuevoParticipeG45 entidad) throws Throwable {
        System.out.println("saveSingle - NuevoParticipeG45");
        entidad = nuevoParticipeG45DaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<NuevoParticipeG45> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria NuevoParticipeG45Service");
        List<NuevoParticipeG45> result = nuevoParticipeG45DaoService.selectByCriteria(datos, NombreEntidadesReporte.NUEVO_PARTICIPE_G45);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio NuevoParticipeG45 no devolvio ningun registro");
        }
        return result;
    }
}
