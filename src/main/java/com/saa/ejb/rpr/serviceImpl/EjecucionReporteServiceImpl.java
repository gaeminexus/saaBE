package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.EjecucionReporteDaoService;
import com.saa.ejb.rpr.service.EjecucionReporteService;
import com.saa.model.rpr.EjecucionReporte;
import com.saa.model.rpr.NombreEntidadesReporte;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class EjecucionReporteServiceImpl implements EjecucionReporteService {

    @EJB
    private EjecucionReporteDaoService ejecucionReporteDaoService;

    @Override
    public EjecucionReporte selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById EjecucionReporte con id: " + id);
        return ejecucionReporteDaoService.selectById(id, NombreEntidadesReporte.EJECUCION_REPORTE);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de EjecucionReporteService");
        EjecucionReporte entidad = new EjecucionReporte();
        for (Long registro : id) {
            ejecucionReporteDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<EjecucionReporte> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de EjecucionReporteService");
        for (EjecucionReporte registro : lista) {
            ejecucionReporteDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<EjecucionReporte> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll EjecucionReporteService");
        List<EjecucionReporte> result = ejecucionReporteDaoService.selectAll(NombreEntidadesReporte.EJECUCION_REPORTE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total EjecucionReporte no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public EjecucionReporte saveSingle(EjecucionReporte entidad) throws Throwable {
        System.out.println("saveSingle - EjecucionReporte");
        entidad = ejecucionReporteDaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<EjecucionReporte> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria EjecucionReporteService");
        List<EjecucionReporte> result = ejecucionReporteDaoService.selectByCriteria(datos, NombreEntidadesReporte.EJECUCION_REPORTE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio EjecucionReporte no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<EjecucionReporte> selectByMesAnio(Long mes, Long anio) throws Throwable {
        System.out.println("Ingresa al metodo selectByMesAnio EjecucionReporteService con mes: " + mes + ", anio: " + anio);
        return ejecucionReporteDaoService.selectByMesAnio(mes, anio);
    }
}
