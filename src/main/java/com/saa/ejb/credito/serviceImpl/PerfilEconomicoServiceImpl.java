package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;

import com.saa.ejb.credito.dao.PerfilEconomicoDaoService;
import com.saa.ejb.credito.service.PerfilEconomicoService;

import com.saa.model.credito.PerfilEconomico;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class PerfilEconomicoServiceImpl implements PerfilEconomicoService {

    @EJB
    private PerfilEconomicoDaoService perfilEconomicoDaoService;

    @Override
    public PerfilEconomico selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById PerfilEconomico con id: " + id);
        return perfilEconomicoDaoService.selectById(id, NombreEntidadesCredito.PERFIL_ECONOMICO);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de PerfilEconomicoService");
        PerfilEconomico pe = new PerfilEconomico();
        for (Long registro : ids) {
            perfilEconomicoDaoService.remove(pe, registro);
        }
    }

    @Override
    public void save(List<PerfilEconomico> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de PerfilEconomicoService");
        for (PerfilEconomico registro : lista) {
            perfilEconomicoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<PerfilEconomico> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll PerfilEconomicoService");
        List<PerfilEconomico> result = perfilEconomicoDaoService.selectAll(NombreEntidadesCredito.PERFIL_ECONOMICO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total PerfilEconomico no devolvió ningún registro");
        }
        return result;
    }

    @Override
    public PerfilEconomico saveSingle(PerfilEconomico pe) throws Throwable {
        System.out.println("saveSingle - PerfilEconomico");
        if (pe.getCodigo() == null) {
            pe.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        pe = perfilEconomicoDaoService.save(pe, pe.getCodigo());
        return pe;
    }

    @Override
    public List<PerfilEconomico> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria PerfilEconomicoService");
        List<PerfilEconomico> result = perfilEconomicoDaoService.selectByCriteria(datos, NombreEntidadesCredito.PERFIL_ECONOMICO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio PerfilEconomico no devolvió registros");
        }
        return result;
    }
}
