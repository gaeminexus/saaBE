package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.CuotaXParticipeGeneracionDaoService;
import com.saa.ejb.crd.service.CuotaXParticipeGeneracionService;
import com.saa.model.crd.CuotaXParticipeGeneracion;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación Service para CuotaXParticipeGeneracion (CXPG).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Stateless
public class CuotaXParticipeGeneracionServiceImpl implements CuotaXParticipeGeneracionService {

    @EJB
    private CuotaXParticipeGeneracionDaoService cuotaXParticipeGeneracionDaoService;

    // ========================================================================
    // MÉTODOS DE EntityService
    // ========================================================================

    @Override
    public CuotaXParticipeGeneracion selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById CuotaXParticipeGeneracion con id: " + id);
        return cuotaXParticipeGeneracionDaoService.selectById(id, NombreEntidadesCredito.CUOTA_X_PARTICIPE_GENERACION);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CuotaXParticipeGeneracionService");
        CuotaXParticipeGeneracion cuota = new CuotaXParticipeGeneracion();
        for (Long id : ids) {
            cuotaXParticipeGeneracionDaoService.remove(cuota, id);
        }
    }

    @Override
    public void save(List<CuotaXParticipeGeneracion> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CuotaXParticipeGeneracionService");
        for (CuotaXParticipeGeneracion registro : lista) {
            cuotaXParticipeGeneracionDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CuotaXParticipeGeneracion> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll CuotaXParticipeGeneracionService");
        List<CuotaXParticipeGeneracion> result = cuotaXParticipeGeneracionDaoService.selectAll(NombreEntidadesCredito.CUOTA_X_PARTICIPE_GENERACION);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total CuotaXParticipeGeneracion no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public CuotaXParticipeGeneracion saveSingle(CuotaXParticipeGeneracion cuota) throws Throwable {
        System.out.println("saveSingle - CuotaXParticipeGeneracion");
        cuota = cuotaXParticipeGeneracionDaoService.save(cuota, cuota.getCodigo());
        return cuota;
    }

    @Override
    public List<CuotaXParticipeGeneracion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria CuotaXParticipeGeneracionService");
        List<CuotaXParticipeGeneracion> result = cuotaXParticipeGeneracionDaoService.selectByCriteria(datos, NombreEntidadesCredito.CUOTA_X_PARTICIPE_GENERACION);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CuotaXParticipeGeneracion no devolvio ningun registro");
        }
        return result;
    }

    // ========================================================================
    // MÉTODOS ADICIONALES
    // ========================================================================

    @Override
    public CuotaXParticipeGeneracion crear(CuotaXParticipeGeneracion cuota) throws Throwable {
        System.out.println("Service: Creando cuota x partícipe generación");
        return cuotaXParticipeGeneracionDaoService.save(cuota, null);
    }

    @Override
    public CuotaXParticipeGeneracion actualizar(CuotaXParticipeGeneracion cuota) throws Throwable {
        System.out.println("Service: Actualizando cuota: " + cuota.getCodigo());
        return cuotaXParticipeGeneracionDaoService.save(cuota, cuota.getCodigo());
    }

    @Override
    public CuotaXParticipeGeneracion buscarPorId(Long codigo) throws Throwable {
        System.out.println("Service: Buscando cuota por ID: " + codigo);
        return cuotaXParticipeGeneracionDaoService.selectById(codigo, NombreEntidadesCredito.CUOTA_X_PARTICIPE_GENERACION);
    }

    @Override
    public List<CuotaXParticipeGeneracion> listarPorParticipe(Long codigoParticipe) throws Throwable {
        System.out.println("Service: Listando cuotas del partícipe: " + codigoParticipe);
        return cuotaXParticipeGeneracionDaoService.selectByParticipe(codigoParticipe);
    }

    @Override
    public List<CuotaXParticipeGeneracion> listarPorPrestamoYGeneracion(Long codigoPrestamo, Long codigoGeneracion) throws Throwable {
        System.out.println("Service: Listando cuotas - Préstamo: " + codigoPrestamo + " - Generación: " + codigoGeneracion);
        return cuotaXParticipeGeneracionDaoService.selectByPrestamoYGeneracion(codigoPrestamo, codigoGeneracion);
    }
}
