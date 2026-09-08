package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.CorridaJubiladosDaoService;
import com.saa.ejb.crd.service.CorridaJubiladosService;
import com.saa.model.crd.CorridaJubilados;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CorridaJubiladosServiceImpl implements CorridaJubiladosService {

    @EJB
    private CorridaJubiladosDaoService corridaJubiladosDaoService;

    @Override
    public CorridaJubilados selectByPeriodo(Long idEmpresa, Long anio, Long mes) throws Throwable {
        System.out.println("CorridaJubiladosService.selectByPeriodo - empresa: " + idEmpresa
                + " - periodo: " + mes + "/" + anio);
        return corridaJubiladosDaoService.selectByPeriodo(idEmpresa, anio, mes);
    }

    @Override
    public CorridaJubilados selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById CorridaJubilados con id: " + id);
        return corridaJubiladosDaoService.selectById(id, NombreEntidadesCredito.CORRIDA_JUBILADOS);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CorridaJubiladosService");
        CorridaJubilados corrida = new CorridaJubilados();
        for (Long registro : id) {
            corridaJubiladosDaoService.remove(corrida, registro);
        }
    }

    @Override
    public void save(List<CorridaJubilados> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CorridaJubiladosService");
        for (CorridaJubilados registro : lista) {
            corridaJubiladosDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CorridaJubilados> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll CorridaJubiladosService");
        List<CorridaJubilados> result = corridaJubiladosDaoService.selectAll(NombreEntidadesCredito.CORRIDA_JUBILADOS);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total CorridaJubilados no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public CorridaJubilados saveSingle(CorridaJubilados corrida) throws Throwable {
        System.out.println("saveSingle - CorridaJubilados");
        return corridaJubiladosDaoService.save(corrida, corrida.getCodigo());
    }

    @Override
    public List<CorridaJubilados> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria CorridaJubiladosService");
        List<CorridaJubilados> result = corridaJubiladosDaoService.selectByCriteria(datos,
                NombreEntidadesCredito.CORRIDA_JUBILADOS);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CorridaJubilados no devolvio ningun registro");
        }
        return result;
    }
}
