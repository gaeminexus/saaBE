package com.saa.ejb.rpr.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.CreditoParticipesMensualDaoService;
import com.saa.ejb.rpr.service.CreditoParticipesMensualService;
import com.saa.model.rpr.CreditoParticipesMensual;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CreditoParticipesMensualServiceImpl implements CreditoParticipesMensualService {

    @EJB
    private CreditoParticipesMensualDaoService creditoParticipesMensualDaoService;

    @Override
    public CreditoParticipesMensual selectById(Long id) throws Throwable {
        return creditoParticipesMensualDaoService.selectById(id, NombreEntidadesReporte.CREDITO_PARTICIPES_MENSUAL);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        CreditoParticipesMensual entidad = new CreditoParticipesMensual();
        for (Long registro : id) {
            creditoParticipesMensualDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<CreditoParticipesMensual> lista) throws Throwable {
        for (CreditoParticipesMensual registro : lista) {
            creditoParticipesMensualDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CreditoParticipesMensual> selectAll() throws Throwable {
        List<CreditoParticipesMensual> result = creditoParticipesMensualDaoService.selectAll(NombreEntidadesReporte.CREDITO_PARTICIPES_MENSUAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total CreditoParticipesMensual no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public CreditoParticipesMensual saveSingle(CreditoParticipesMensual entidad) throws Throwable {
        entidad = creditoParticipesMensualDaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<CreditoParticipesMensual> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        List<CreditoParticipesMensual> result = creditoParticipesMensualDaoService.selectByCriteria(datos, NombreEntidadesReporte.CREDITO_PARTICIPES_MENSUAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CreditoParticipesMensual no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<CreditoParticipesMensual> selectByEjecucion(Long codigoEjecucion) throws Throwable {
        return creditoParticipesMensualDaoService.selectByEjecucion(codigoEjecucion);
    }
}