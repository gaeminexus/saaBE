package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.DetalleEjecucionReporteDaoService;
import com.saa.ejb.rpr.service.DetalleEjecucionReporteService;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.NombreEntidadesReporte;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DetalleEjecucionReporteServiceImpl implements DetalleEjecucionReporteService {

    @EJB
    private DetalleEjecucionReporteDaoService detalleEjecucionReporteDaoService;

    @Override
    public DetalleEjecucionReporte selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById DetalleEjecucionReporte con id: " + id);
        return detalleEjecucionReporteDaoService.selectById(id, NombreEntidadesReporte.DETALLE_EJECUCION_REPORTE);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de DetalleEjecucionReporteService");
        DetalleEjecucionReporte entidad = new DetalleEjecucionReporte();
        for (Long registro : id) {
            detalleEjecucionReporteDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<DetalleEjecucionReporte> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de DetalleEjecucionReporteService");
        for (DetalleEjecucionReporte registro : lista) {
            detalleEjecucionReporteDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<DetalleEjecucionReporte> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll DetalleEjecucionReporteService");
        List<DetalleEjecucionReporte> result = detalleEjecucionReporteDaoService.selectAll(NombreEntidadesReporte.DETALLE_EJECUCION_REPORTE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total DetalleEjecucionReporte no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public DetalleEjecucionReporte saveSingle(DetalleEjecucionReporte entidad) throws Throwable {
        System.out.println("saveSingle - DetalleEjecucionReporte");
        entidad = detalleEjecucionReporteDaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<DetalleEjecucionReporte> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria DetalleEjecucionReporteService");
        List<DetalleEjecucionReporte> result = detalleEjecucionReporteDaoService.selectByCriteria(datos, NombreEntidadesReporte.DETALLE_EJECUCION_REPORTE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio DetalleEjecucionReporte no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<DetalleEjecucionReporte> selectByEjecucion(Long idEjecucion) throws Throwable {
        System.out.println("Ingresa al metodo selectByEjecucion DetalleEjecucionReporteService con idEjecucion: " + idEjecucion);
        List<DetalleEjecucionReporte> result = detalleEjecucionReporteDaoService.selectByEjecucion(idEjecucion);
        if (result.isEmpty()) {
            throw new IncomeException("No se encontraron detalles para la ejecucion: " + idEjecucion);
        }
        return result;
    }

    @Override
    public List<DetalleEjecucionReporte> selectConNovedadesByEjecucion(Long idEjecucion) throws Throwable {
        System.out.println("Ingresa al metodo selectConNovedadesByEjecucion DetalleEjecucionReporteService con idEjecucion: " + idEjecucion);
        List<DetalleEjecucionReporte> result = detalleEjecucionReporteDaoService.selectConNovedadesByEjecucion(idEjecucion);
        if (result.isEmpty()) {
            throw new IncomeException("No se encontraron detalles con novedades para la ejecucion: " + idEjecucion);
        }
        return result;
    }

    @Override
    public List<DetalleEjecucionReporte> selectPendientesYNovedadesByEjecucion(Long idEjecucion) throws Throwable {
        System.out.println("Ingresa al metodo selectPendientesYNovedadesByEjecucion DetalleEjecucionReporteService con idEjecucion: " + idEjecucion);
        List<DetalleEjecucionReporte> result = detalleEjecucionReporteDaoService.selectPendientesYNovedadesByEjecucion(idEjecucion);
        if (result.isEmpty()) {
            throw new IncomeException("No hay detalles pendientes o con novedades para la ejecucion: " + idEjecucion);
        }
        return result;
    }

    @Override
    public DetalleEjecucionReporte selectByEjecucionYTipo(Long idEjecucion, String tipoReporte) throws Throwable {
        System.out.println("selectByEjecucionYTipo DetalleEjecucionReporteService ejrc: " + idEjecucion + " tipo: " + tipoReporte);
        return detalleEjecucionReporteDaoService.selectByEjecucionYTipo(idEjecucion, tipoReporte);
    }
}
