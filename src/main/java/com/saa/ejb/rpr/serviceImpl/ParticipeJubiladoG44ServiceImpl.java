package com.saa.ejb.rpr.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.ParticipeJubiladoG44DaoService;
import com.saa.ejb.rpr.service.ParticipeJubiladoG44Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.ParticipeJubiladoG44;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ParticipeJubiladoG44ServiceImpl implements ParticipeJubiladoG44Service {

    @EJB
    private ParticipeJubiladoG44DaoService participeJubiladoG44DaoService;

    @Override
    public ParticipeJubiladoG44 selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById ParticipeJubiladoG44 con id: " + id);
        return participeJubiladoG44DaoService.selectById(id, NombreEntidadesReporte.PARTICIPE_JUBILADO_G44);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ParticipeJubiladoG44Service");
        ParticipeJubiladoG44 entidad = new ParticipeJubiladoG44();
        for (Long registro : id) {
            participeJubiladoG44DaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<ParticipeJubiladoG44> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ParticipeJubiladoG44Service");
        for (ParticipeJubiladoG44 registro : lista) {
            participeJubiladoG44DaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<ParticipeJubiladoG44> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ParticipeJubiladoG44Service");
        List<ParticipeJubiladoG44> result = participeJubiladoG44DaoService.selectAll(NombreEntidadesReporte.PARTICIPE_JUBILADO_G44);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total ParticipeJubiladoG44 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public ParticipeJubiladoG44 saveSingle(ParticipeJubiladoG44 entidad) throws Throwable {
        System.out.println("saveSingle - ParticipeJubiladoG44");
        entidad = participeJubiladoG44DaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<ParticipeJubiladoG44> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria ParticipeJubiladoG44Service");
        List<ParticipeJubiladoG44> result = participeJubiladoG44DaoService.selectByCriteria(datos, NombreEntidadesReporte.PARTICIPE_JUBILADO_G44);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio ParticipeJubiladoG44 no devolvio ningun registro");
        }
        return result;
    }
}
