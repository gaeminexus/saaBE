package com.saa.ejb.rpr.serviceImpl;

import com.saa.ejb.rpr.dao.CreditoCuotasPrestamosMensualDaoService;
import com.saa.ejb.rpr.dao.CreditoJubiladosMensualDaoService;
import com.saa.ejb.rpr.dao.CreditoParticipesMensualDaoService;
import com.saa.ejb.rpr.service.LimpiezaReportesService;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless
public class LimpiezaReportesServiceImpl implements LimpiezaReportesService {

    @EJB private CreditoCuotasPrestamosMensualDaoService   ccpmDaoService;
    @EJB private CreditoJubiladosMensualDaoService        cjbmDaoService;
    @EJB private CreditoParticipesMensualDaoService        cprmDaoService;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void limpiarDatosReportes(Long codigoEjecucion) {
        System.out.println("Limpiando datos de reportes para ejecución: " + codigoEjecucion);
        try {
            int borradosCCPM = ccpmDaoService.deleteByEjecucion(codigoEjecucion);
            System.out.println("Registros CCPM eliminados: " + borradosCCPM);

            int borradosCJBM = cjbmDaoService.deleteByEjecucion(codigoEjecucion);
            System.out.println("Registros CJBM eliminados: " + borradosCJBM);

            int borradosCPRM = cprmDaoService.deleteByEjecucion(codigoEjecucion);
            System.out.println("Registros CPRM eliminados: " + borradosCPRM);
        } catch (Throwable e) {
            System.err.println("Error al limpiar los datos de reportes: " + e.getMessage());
            // Lanzar una excepción de runtime para que la transacción haga rollback si es necesario
            throw new RuntimeException("Fallo en la limpieza de reportes para ejecución " + codigoEjecucion, e);
        }
    }
}
