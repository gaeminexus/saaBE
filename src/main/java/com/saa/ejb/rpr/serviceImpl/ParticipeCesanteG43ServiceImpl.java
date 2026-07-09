package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.ParticipeCesanteG43DaoService;
import com.saa.ejb.rpr.service.ParticipeCesanteG43Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.ParticipeCesanteG43;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ParticipeCesanteG43ServiceImpl implements ParticipeCesanteG43Service {

    @EJB
    private ParticipeCesanteG43DaoService participeCesanteG43DaoService;

    @Override
    public ParticipeCesanteG43 selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById ParticipeCesanteG43 con id: " + id);
        return participeCesanteG43DaoService.selectById(id, NombreEntidadesReporte.PARTICIPE_CESANTE_G43);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ParticipeCesanteG43Service");
        ParticipeCesanteG43 entidad = new ParticipeCesanteG43();
        for (Long registro : id) {
            participeCesanteG43DaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<ParticipeCesanteG43> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ParticipeCesanteG43Service");
        for (ParticipeCesanteG43 registro : lista) {
            participeCesanteG43DaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<ParticipeCesanteG43> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ParticipeCesanteG43Service");
        List<ParticipeCesanteG43> result = participeCesanteG43DaoService.selectAll(NombreEntidadesReporte.PARTICIPE_CESANTE_G43);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total ParticipeCesanteG43 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public ParticipeCesanteG43 saveSingle(ParticipeCesanteG43 entidad) throws Throwable {
        System.out.println("saveSingle - ParticipeCesanteG43");
        entidad = participeCesanteG43DaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<ParticipeCesanteG43> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria ParticipeCesanteG43Service");
        List<ParticipeCesanteG43> result = participeCesanteG43DaoService.selectByCriteria(datos, NombreEntidadesReporte.PARTICIPE_CESANTE_G43);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio ParticipeCesanteG43 no devolvio ningun registro");
        }
        return result;
    }
}
