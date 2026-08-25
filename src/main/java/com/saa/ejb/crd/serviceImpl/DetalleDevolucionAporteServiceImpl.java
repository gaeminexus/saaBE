package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetalleDevolucionAporteDaoService;
import com.saa.ejb.crd.service.DetalleDevolucionAporteService;
import com.saa.model.crd.DetalleDevolucionAporte;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DetalleDevolucionAporteServiceImpl implements DetalleDevolucionAporteService {

    @EJB
    private DetalleDevolucionAporteDaoService detalleDevolucionAporteDaoService;

    @Override
    public DetalleDevolucionAporte selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById DetalleDevolucionAporte con id: " + id);
        return detalleDevolucionAporteDaoService.selectById(id,
                NombreEntidadesCredito.DETALLE_DEVOLUCION_APORTE);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de DetalleDevolucionAporteService");
        DetalleDevolucionAporte entidad = new DetalleDevolucionAporte();
        for (Long registro : id) {
            detalleDevolucionAporteDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<DetalleDevolucionAporte> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de DetalleDevolucionAporteService");
        for (DetalleDevolucionAporte registro : lista) {
            detalleDevolucionAporteDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<DetalleDevolucionAporte> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll DetalleDevolucionAporteService");
        List<DetalleDevolucionAporte> result = detalleDevolucionAporteDaoService.selectAll(
                NombreEntidadesCredito.DETALLE_DEVOLUCION_APORTE);
        if (result.isEmpty()) {
            throw new IncomeException(
                    "Busqueda total DetalleDevolucionAporte no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public DetalleDevolucionAporte saveSingle(DetalleDevolucionAporte detalle) throws Throwable {
        System.out.println("saveSingle - DetalleDevolucionAporte");
        // La entidad no tiene campo estado: no hay bloque setEstado.
        return detalleDevolucionAporteDaoService.save(detalle, detalle.getCodigo());
    }

    @Override
    public List<DetalleDevolucionAporte> selectByCriteria(List<DatosBusqueda> datos)
            throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria DetalleDevolucionAporteService");
        List<DetalleDevolucionAporte> result = detalleDevolucionAporteDaoService.selectByCriteria(
                datos, NombreEntidadesCredito.DETALLE_DEVOLUCION_APORTE);
        if (result.isEmpty()) {
            throw new IncomeException(
                    "Busqueda por criterio DetalleDevolucionAporte no devolvio ningun registro");
        }
        return result;
    }
}
