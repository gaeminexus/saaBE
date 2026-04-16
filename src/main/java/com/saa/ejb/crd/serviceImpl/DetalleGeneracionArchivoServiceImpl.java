package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetalleGeneracionArchivoDaoService;
import com.saa.ejb.crd.service.DetalleGeneracionArchivoService;
import com.saa.model.crd.DetalleGeneracionArchivo;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación Service para DetalleGeneracionArchivo (DTGA).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Stateless
public class DetalleGeneracionArchivoServiceImpl implements DetalleGeneracionArchivoService {

    @EJB
    private DetalleGeneracionArchivoDaoService detalleGeneracionArchivoDaoService;

    // ========================================================================
    // MÉTODOS DE EntityService
    // ========================================================================

    @Override
    public DetalleGeneracionArchivo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById DetalleGeneracionArchivo con id: " + id);
        return detalleGeneracionArchivoDaoService.selectById(id, NombreEntidadesCredito.DETALLE_GENERACION_ARCHIVO);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de DetalleGeneracionArchivoService");
        DetalleGeneracionArchivo detalle = new DetalleGeneracionArchivo();
        for (Long id : ids) {
            detalleGeneracionArchivoDaoService.remove(detalle, id);
        }
    }

    @Override
    public void save(List<DetalleGeneracionArchivo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de DetalleGeneracionArchivoService");
        for (DetalleGeneracionArchivo registro : lista) {
            detalleGeneracionArchivoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<DetalleGeneracionArchivo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll DetalleGeneracionArchivoService");
        List<DetalleGeneracionArchivo> result = detalleGeneracionArchivoDaoService.selectAll(NombreEntidadesCredito.DETALLE_GENERACION_ARCHIVO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total DetalleGeneracionArchivo no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public DetalleGeneracionArchivo saveSingle(DetalleGeneracionArchivo detalle) throws Throwable {
        System.out.println("saveSingle - DetalleGeneracionArchivo");
        detalle = detalleGeneracionArchivoDaoService.save(detalle, detalle.getCodigo());
        return detalle;
    }

    @Override
    public List<DetalleGeneracionArchivo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria DetalleGeneracionArchivoService");
        List<DetalleGeneracionArchivo> result = detalleGeneracionArchivoDaoService.selectByCriteria(datos, NombreEntidadesCredito.DETALLE_GENERACION_ARCHIVO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio DetalleGeneracionArchivo no devolvio ningun registro");
        }
        return result;
    }

    // ========================================================================
    // MÉTODOS ADICIONALES
    // ========================================================================

    @Override
    public DetalleGeneracionArchivo crear(DetalleGeneracionArchivo detalle) throws Throwable {
        System.out.println("Service: Creando detalle generación archivo");
        return detalleGeneracionArchivoDaoService.save(detalle, null);
    }

    @Override
    public DetalleGeneracionArchivo actualizar(DetalleGeneracionArchivo detalle) throws Throwable {
        System.out.println("Service: Actualizando detalle generación: " + detalle.getCodigo());
        return detalleGeneracionArchivoDaoService.save(detalle, detalle.getCodigo());
    }

    @Override
    public DetalleGeneracionArchivo buscarPorId(Long codigo) throws Throwable {
        System.out.println("Service: Buscando detalle por ID: " + codigo);
        return detalleGeneracionArchivoDaoService.selectById(codigo, NombreEntidadesCredito.DETALLE_GENERACION_ARCHIVO);
    }

    @Override
    public List<DetalleGeneracionArchivo> listarPorGeneracion(Long codigoGeneracion) throws Throwable {
        System.out.println("Service: Listando detalles de la generación: " + codigoGeneracion);
        return detalleGeneracionArchivoDaoService.selectByGeneracion(codigoGeneracion);
    }

    @Override
    public DetalleGeneracionArchivo buscarPorGeneracionYProducto(Long codigoGeneracion, String codigoProducto) throws Throwable {
        System.out.println("Service: Buscando detalle - Generación: " + codigoGeneracion + " - Producto: " + codigoProducto);
        return detalleGeneracionArchivoDaoService.selectByGeneracionYProducto(codigoGeneracion, codigoProducto);
    }
}
