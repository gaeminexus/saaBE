package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.HistoricoPagoCuentaIndividualDaoService;
import com.saa.ejb.crd.service.HistoricoPagoCuentaIndividualService;
import com.saa.model.crd.HistoricoPagoCuentaIndividual;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class HistoricoPagoCuentaIndividualServiceImpl implements HistoricoPagoCuentaIndividualService {

    @EJB
    private HistoricoPagoCuentaIndividualDaoService historicoPagoCuentaIndividualDaoService;

    @Override
    public HistoricoPagoCuentaIndividual selectById(Long id) throws Throwable {
        System.out.println("selectById - HistoricoPagoCuentaIndividual: " + id);
        return historicoPagoCuentaIndividualDaoService.selectById(id,
                NombreEntidadesCredito.HISTORICO_PAGO_CUENTA_INDIVIDUAL);
    }

    /** ⛔ Solo lectura: la tabla la alimentan los procesos de liquidación. */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - HistoricoPagoCuentaIndividual (bloqueado: solo lectura)");
        throw new IncomeException("El historico de pagos de cuenta individual es de solo lectura");
    }

    /** ⛔ Solo lectura: la tabla la alimentan los procesos de liquidación. */
    @Override
    public void save(List<HistoricoPagoCuentaIndividual> lista) throws Throwable {
        System.out.println("save list - HistoricoPagoCuentaIndividual (bloqueado: solo lectura)");
        throw new IncomeException("El historico de pagos de cuenta individual es de solo lectura");
    }

    /** ⛔ Solo lectura: la tabla la alimentan los procesos de liquidación. */
    @Override
    public HistoricoPagoCuentaIndividual saveSingle(HistoricoPagoCuentaIndividual registro) throws Throwable {
        System.out.println("saveSingle - HistoricoPagoCuentaIndividual (bloqueado: solo lectura)");
        throw new IncomeException("El historico de pagos de cuenta individual es de solo lectura");
    }

    @Override
    public List<HistoricoPagoCuentaIndividual> selectAll() throws Throwable {
        System.out.println("selectAll - HistoricoPagoCuentaIndividual");
        List<HistoricoPagoCuentaIndividual> result = historicoPagoCuentaIndividualDaoService
                .selectAll(NombreEntidadesCredito.HISTORICO_PAGO_CUENTA_INDIVIDUAL);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros HistoricoPagoCuentaIndividual");
        }
        return result;
    }

    @Override
    public List<HistoricoPagoCuentaIndividual> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - HistoricoPagoCuentaIndividual");
        List<HistoricoPagoCuentaIndividual> result = historicoPagoCuentaIndividualDaoService
                .selectByCriteria(datos, NombreEntidadesCredito.HISTORICO_PAGO_CUENTA_INDIVIDUAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio HistoricoPagoCuentaIndividual no devolvio registros");
        }
        return result;
    }

    @Override
    public List<HistoricoPagoCuentaIndividual> selectByCedula(String cedula) throws Throwable {
        System.out.println("selectByCedula - HistoricoPagoCuentaIndividual: " + cedula);
        return historicoPagoCuentaIndividualDaoService.selectByCedula(cedula);
    }
}
