package com.saa.ejb.rpr.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.CreditoJubiladosMensualDaoService;
import com.saa.ejb.rpr.service.CreditoJubiladosMensualService;
import com.saa.model.rpr.CreditoJubiladosMensual;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CreditoJubiladosMensualServiceImpl implements CreditoJubiladosMensualService {

    @EJB
    private CreditoJubiladosMensualDaoService creditoJubiladosMensualDaoService;

    @Override
    public CreditoJubiladosMensual selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById CreditoJubiladosMensual con id: " + id);
        return creditoJubiladosMensualDaoService.selectById(id, NombreEntidadesReporte.CREDITO_JUBILADOS_MENSUAL);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CreditoJubiladosMensualService");
        CreditoJubiladosMensual entidad = new CreditoJubiladosMensual();
        for (Long registro : id) {
            creditoJubiladosMensualDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<CreditoJubiladosMensual> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CreditoJubiladosMensualService");
        for (CreditoJubiladosMensual registro : lista) {
            creditoJubiladosMensualDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CreditoJubiladosMensual> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll CreditoJubiladosMensualService");
        List<CreditoJubiladosMensual> result = creditoJubiladosMensualDaoService.selectAll(NombreEntidadesReporte.CREDITO_JUBILADOS_MENSUAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total CreditoJubiladosMensual no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public CreditoJubiladosMensual saveSingle(CreditoJubiladosMensual entidad) throws Throwable {
        System.out.println("saveSingle - CreditoJubiladosMensual");
        entidad = creditoJubiladosMensualDaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<CreditoJubiladosMensual> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria CreditoJubiladosMensualService");
        List<CreditoJubiladosMensual> result = creditoJubiladosMensualDaoService.selectByCriteria(datos, NombreEntidadesReporte.CREDITO_JUBILADOS_MENSUAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CreditoJubiladosMensual no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<CreditoJubiladosMensual> selectByEjecucion(Long codigoEjecucion) throws Throwable {
        System.out.println("selectByEjecucion CreditoJubiladosMensual codigoEjecucion: " + codigoEjecucion);
        return creditoJubiladosMensualDaoService.selectByEjecucion(codigoEjecucion);
    }
}
