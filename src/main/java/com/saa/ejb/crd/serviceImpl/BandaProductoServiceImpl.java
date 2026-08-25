package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.BandaProductoDaoService;
import com.saa.ejb.crd.service.BandaProductoService;
import com.saa.ejb.crd.service.ClasificadorBandaService;
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.model.crd.BandaProducto;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación del servicio de BandaProducto. Ver {@link BandaProductoService}: el flujo
 * de negocio graba las bandas como juego completo desde
 * {@code ConfiguracionBandaProductoService}, no por aquí.
 */
@Stateless
public class BandaProductoServiceImpl implements BandaProductoService {

    @EJB
    private BandaProductoDaoService bandaProductoDaoService;

    @EJB
    private ClasificadorBandaService clasificadorBandaService;

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de BandaProducto service");
        BandaProducto entidad = new BandaProducto();
        for (Long registro : id) {
            bandaProductoDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<BandaProducto> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de BandaProducto service");
        for (BandaProducto entidad : lista) {
            bandaProductoDaoService.save(entidad, entidad.getCodigo());
        }
    }

    @Override
    public BandaProducto saveSingle(BandaProducto entidad) throws Throwable {
        System.out.println("Ingresa al metodo (saveSingle) BandaProducto Service");
        bandaProductoDaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<BandaProducto> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll BandaProductoService");
        List<BandaProducto> result = bandaProductoDaoService
                .selectAll(NombreEntidadesCredito.BANDA_PRODUCTO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda de BandaProducto no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public BandaProducto selectById(Long id) throws Throwable {
        System.out.println("Ingresa al metodo (selectById) de BandaProducto con id: " + id);
        return bandaProductoDaoService.selectById(id, NombreEntidadesCredito.BANDA_PRODUCTO);
    }

    @Override
    public List<BandaProducto> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo (selectByCriteria) BandaProducto");
        List<BandaProducto> result = bandaProductoDaoService
                .selectByCriteria(datos, NombreEntidadesCredito.BANDA_PRODUCTO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda de BandaProducto no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<BandaProductoDetalle> selectDetalleByConfiguracion(Long idConfiguracion)
            throws Throwable {
        System.out.println("Ingresa al metodo (selectDetalleByConfiguracion) BandaProducto"
                + " - configuracion: " + idConfiguracion);
        if (idConfiguracion == null) {
            throw new IncomeException("La configuracion es obligatoria");
        }
        return clasificadorBandaService.derivarRangos(
                bandaProductoDaoService.selectByConfiguracion(idConfiguracion));
    }

    @Override
    public List<BandaProducto> selectByConfiguracion(Long idConfiguracion) throws Throwable {
        System.out.println("Ingresa al metodo (selectByConfiguracion) BandaProducto"
                + " - configuracion: " + idConfiguracion);
        if (idConfiguracion == null) {
            throw new IncomeException("La configuracion es obligatoria");
        }
        return bandaProductoDaoService.selectByConfiguracion(idConfiguracion);
    }
}
