package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.HistoricoG48DaoService;
import com.saa.ejb.rpr.service.HistoricoG48Service;
import com.saa.model.rpr.HistoricoG48;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class HistoricoG48ServiceImpl implements HistoricoG48Service {

    @EJB
    private HistoricoG48DaoService historicoG48DaoService;

    @Override
    public HistoricoG48 selectById(Long id) throws Throwable {
        return historicoG48DaoService.selectById(id, "HistoricoG48");
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        HistoricoG48 entidad = new HistoricoG48();
        for (Long registro : id) {
            historicoG48DaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<HistoricoG48> lista) throws Throwable {
        for (HistoricoG48 registro : lista) {
            historicoG48DaoService.save(registro, null);
        }
    }

    @Override
    public List<HistoricoG48> selectAll() throws Throwable {
        List<HistoricoG48> result = historicoG48DaoService.selectAll("HistoricoG48");
        if (result.isEmpty()) {
            throw new IncomeException("selectAll HistoricoG48 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public HistoricoG48 saveSingle(HistoricoG48 entidad) throws Throwable {
        return historicoG48DaoService.save(entidad, null);
    }

    @Override
    public List<HistoricoG48> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        List<HistoricoG48> result = historicoG48DaoService.selectByCriteria(datos, "HistoricoG48");
        if (result.isEmpty()) {
            throw new IncomeException("selectByCriteria HistoricoG48 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public HistoricoG48 selectByNumeroOperacion(String numeroOperacion) throws Throwable {
        return historicoG48DaoService.selectByNumeroOperacion(numeroOperacion);
    }
}
