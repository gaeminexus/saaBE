package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.AprobacionPrestamoDaoService;
import com.saa.ejb.crd.service.AprobacionPrestamoService;
import com.saa.model.crd.AprobacionPrestamo;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Bitácora de aprobaciones por nivel. La ESCRIBE solo {@code NivelAprobacionService} (reglas de transición, invalidación,
 * mismo usuario en dos niveles); acá solo se lee. No tiene REST propio: se consulta con {@code /prst/estadoAprobacion}.
 */
@Stateless
public class AprobacionPrestamoServiceImpl implements AprobacionPrestamoService {

    @EJB
    private AprobacionPrestamoDaoService dao;

    @Override
    public AprobacionPrestamo selectById(Long id) throws Throwable {
        System.out.println("selectById - AprobacionPrestamo: " + id);
        return dao.selectById(id, NombreEntidadesCredito.APROBACION_PRESTAMO);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        throw new IncomeException("La bitácora de aprobaciones no se borra; se invalida (APPRESTD = 0).");
    }

    @Override
    public void save(List<AprobacionPrestamo> lista) throws Throwable {
        throw new IncomeException("Las aprobaciones las registra el proceso de aprobación del préstamo.");
    }

    @Override
    public List<AprobacionPrestamo> selectAll() throws Throwable {
        System.out.println("selectAll - AprobacionPrestamo");
        List<AprobacionPrestamo> result = dao.selectAll(NombreEntidadesCredito.APROBACION_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros AprobacionPrestamo");
        }
        return result;
    }

    @Override
    public AprobacionPrestamo saveSingle(AprobacionPrestamo registro) throws Throwable {
        throw new IncomeException("Las aprobaciones las registra el proceso de aprobación del préstamo.");
    }

    @Override
    public List<AprobacionPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - AprobacionPrestamo");
        List<AprobacionPrestamo> result = dao.selectByCriteria(datos, NombreEntidadesCredito.APROBACION_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio AprobacionPrestamo no devolvio registros");
        }
        return result;
    }
}
