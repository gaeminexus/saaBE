package com.saa.ejb.rpr.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.CreditoG40DaoService;
import com.saa.ejb.rpr.service.CreditoG40Service;
import com.saa.model.rpr.CreditoG40;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CreditoG40ServiceImpl implements CreditoG40Service {

    @EJB
    private CreditoG40DaoService creditoG40DaoService;

    @Override
    public CreditoG40 selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById CreditoG40 con id: " + id);
        return creditoG40DaoService.selectById(id, NombreEntidadesReporte.CREDITO_G40);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CreditoG40Service");
        CreditoG40 entidad = new CreditoG40();
        for (Long registro : id) {
            creditoG40DaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<CreditoG40> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CreditoG40Service");
        for (CreditoG40 registro : lista) {
            creditoG40DaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CreditoG40> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll CreditoG40Service");
        List<CreditoG40> result = creditoG40DaoService.selectAll(NombreEntidadesReporte.CREDITO_G40);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total CreditoG40 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public CreditoG40 saveSingle(CreditoG40 entidad) throws Throwable {
        System.out.println("saveSingle - CreditoG40");
        entidad = creditoG40DaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<CreditoG40> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria CreditoG40Service");
        List<CreditoG40> result = creditoG40DaoService.selectByCriteria(datos, NombreEntidadesReporte.CREDITO_G40);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CreditoG40 no devolvio ningun registro");
        }
        return result;
    }
}
