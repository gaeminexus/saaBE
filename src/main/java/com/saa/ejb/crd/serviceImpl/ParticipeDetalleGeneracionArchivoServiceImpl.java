package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.ParticipeDetalleGeneracionArchivoDaoService;
import com.saa.ejb.crd.service.ParticipeDetalleGeneracionArchivoService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.ParticipeDetalleGeneracionArchivo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación Service para ParticipeDetalleGeneracionArchivo (PDGA).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Stateless
public class ParticipeDetalleGeneracionArchivoServiceImpl implements ParticipeDetalleGeneracionArchivoService {

    @EJB
    private ParticipeDetalleGeneracionArchivoDaoService participeDetalleGeneracionArchivoDaoService;

    // ========================================================================
    // MÉTODOS DE EntityService
    // ========================================================================

    @Override
    public ParticipeDetalleGeneracionArchivo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById ParticipeDetalleGeneracionArchivo con id: " + id);
        return participeDetalleGeneracionArchivoDaoService.selectById(id, NombreEntidadesCredito.PARTICIPE_DETALLE_GENERACION_ARCHIVO);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ParticipeDetalleGeneracionArchivoService");
        ParticipeDetalleGeneracionArchivo participe = new ParticipeDetalleGeneracionArchivo();
        for (Long id : ids) {
            participeDetalleGeneracionArchivoDaoService.remove(participe, id);
        }
    }

    @Override
    public void save(List<ParticipeDetalleGeneracionArchivo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ParticipeDetalleGeneracionArchivoService");
        for (ParticipeDetalleGeneracionArchivo registro : lista) {
            participeDetalleGeneracionArchivoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<ParticipeDetalleGeneracionArchivo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ParticipeDetalleGeneracionArchivoService");
        List<ParticipeDetalleGeneracionArchivo> result = participeDetalleGeneracionArchivoDaoService.selectAll(NombreEntidadesCredito.PARTICIPE_DETALLE_GENERACION_ARCHIVO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total ParticipeDetalleGeneracionArchivo no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public ParticipeDetalleGeneracionArchivo saveSingle(ParticipeDetalleGeneracionArchivo participe) throws Throwable {
        System.out.println("saveSingle - ParticipeDetalleGeneracionArchivo");
        participe = participeDetalleGeneracionArchivoDaoService.save(participe, participe.getCodigo());
        return participe;
    }

    @Override
    public List<ParticipeDetalleGeneracionArchivo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria ParticipeDetalleGeneracionArchivoService");
        List<ParticipeDetalleGeneracionArchivo> result = participeDetalleGeneracionArchivoDaoService.selectByCriteria(datos, NombreEntidadesCredito.PARTICIPE_DETALLE_GENERACION_ARCHIVO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio ParticipeDetalleGeneracionArchivo no devolvio ningun registro");
        }
        return result;
    }

    // ========================================================================
    // MÉTODOS ADICIONALES
    // ========================================================================

    @Override
    public ParticipeDetalleGeneracionArchivo crear(ParticipeDetalleGeneracionArchivo participeDetalle) throws Throwable {
        System.out.println("Service: Creando partícipe detalle generación");
        return participeDetalleGeneracionArchivoDaoService.save(participeDetalle, null);
    }

    @Override
    public ParticipeDetalleGeneracionArchivo actualizar(ParticipeDetalleGeneracionArchivo participeDetalle) throws Throwable {
        System.out.println("Service: Actualizando partícipe detalle: " + participeDetalle.getCodigo());
        return participeDetalleGeneracionArchivoDaoService.save(participeDetalle, participeDetalle.getCodigo());
    }

    @Override
    public ParticipeDetalleGeneracionArchivo buscarPorId(Long codigo) throws Throwable {
        System.out.println("Service: Buscando partícipe detalle por ID: " + codigo);
        return participeDetalleGeneracionArchivoDaoService.selectById(codigo, NombreEntidadesCredito.PARTICIPE_DETALLE_GENERACION_ARCHIVO);
    }

    @Override
    public List<ParticipeDetalleGeneracionArchivo> listarPorDetalle(Long codigoDetalle) throws Throwable {
        System.out.println("Service: Listando partícipes del detalle: " + codigoDetalle);
        return participeDetalleGeneracionArchivoDaoService.selectByDetalle(codigoDetalle);
    }

    @Override
    public List<ParticipeDetalleGeneracionArchivo> listarPorEntidad(Long codigoEntidad) throws Throwable {
        System.out.println("Service: Listando registros de la entidad: " + codigoEntidad);
        return participeDetalleGeneracionArchivoDaoService.selectByEntidad(codigoEntidad);
    }

    @Override
    public List<ParticipeDetalleGeneracionArchivo> listarPorGeneracion(Long codigoGeneracion) throws Throwable {
        System.out.println("Service: Listando todos los registros de la generación: " + codigoGeneracion);
        return participeDetalleGeneracionArchivoDaoService.selectByGeneracion(codigoGeneracion);
    }
}
