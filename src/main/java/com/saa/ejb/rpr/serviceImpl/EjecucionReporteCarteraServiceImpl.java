package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.CreditoCuotasPrestamosMensualDaoService;
import com.saa.ejb.rpr.dao.CreditoJubiladosMensualDaoService;
import com.saa.ejb.rpr.dao.CreditoParticipesMensualDaoService;
import com.saa.ejb.rpr.dao.EjecucionReporteCarteraDaoService;
import com.saa.ejb.rpr.service.EjecucionReporteCarteraService;
import com.saa.model.rpr.EjecucionReporteCartera;
import com.saa.model.rpr.NombreEntidadesReporte;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class EjecucionReporteCarteraServiceImpl implements EjecucionReporteCarteraService {

    @EJB
    private EjecucionReporteCarteraDaoService ejecucionReporteCarteraDaoService;

    @EJB
    private CreditoJubiladosMensualDaoService cjbmDaoService;

    @EJB
    private CreditoCuotasPrestamosMensualDaoService ccpmDaoService;

    @EJB
    private CreditoParticipesMensualDaoService cprmDaoService;

    @Override
    public EjecucionReporteCartera selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById EjecucionReporteCartera con id: " + id);
        return ejecucionReporteCarteraDaoService.selectById(id, NombreEntidadesReporte.EJECUCION_REPORTE_CARTERA);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de EjecucionReporteCarteraService");
        EjecucionReporteCartera entidad = new EjecucionReporteCartera();
        for (Long registro : id) {
            // Eliminar registros hijos primero para evitar violación FK
            int cprm = cprmDaoService.deleteByEjecucion(registro);
            int ccpm = ccpmDaoService.deleteByEjecucion(registro);
            int cjbm = cjbmDaoService.deleteByEjecucion(registro);
            System.out.println("Eliminados hijos de EjecucionReporteCartera " + registro
                + " → CPRM: " + cprm + ", CCPM: " + ccpm + ", CJBM: " + cjbm);
            ejecucionReporteCarteraDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<EjecucionReporteCartera> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de EjecucionReporteCarteraService");
        for (EjecucionReporteCartera registro : lista) {
            ejecucionReporteCarteraDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<EjecucionReporteCartera> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll EjecucionReporteCarteraService");
        List<EjecucionReporteCartera> result = ejecucionReporteCarteraDaoService.selectAll(NombreEntidadesReporte.EJECUCION_REPORTE_CARTERA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total EjecucionReporteCartera no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public EjecucionReporteCartera saveSingle(EjecucionReporteCartera entidad) throws Throwable {
        System.out.println("saveSingle - EjecucionReporteCartera");
        entidad = ejecucionReporteCarteraDaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<EjecucionReporteCartera> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria EjecucionReporteCarteraService");
        List<EjecucionReporteCartera> result = ejecucionReporteCarteraDaoService.selectByCriteria(datos, NombreEntidadesReporte.EJECUCION_REPORTE_CARTERA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio EjecucionReporteCartera no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<EjecucionReporteCartera> selectByMesAnio(Long mes, Long anio) throws Throwable {
        System.out.println("selectByMesAnio EjecucionReporteCartera mes: " + mes + " anio: " + anio);
        return ejecucionReporteCarteraDaoService.selectByMesAnio(mes, anio);
    }
}
