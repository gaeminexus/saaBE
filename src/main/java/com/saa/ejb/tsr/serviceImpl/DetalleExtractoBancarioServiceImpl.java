/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tsr.dao.DetalleExtractoBancarioDaoService;
import com.saa.ejb.tsr.service.DetalleExtractoBancarioService;
import com.saa.model.tsr.DetalleExtractoBancario;
import com.saa.model.tsr.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz DetalleExtractoBancarioService.
 * Contiene los servicios relacionados con la entidad DetalleExtractoBancario.</p>
 */
@Stateless
public class DetalleExtractoBancarioServiceImpl implements DetalleExtractoBancarioService {

    @EJB
    private DetalleExtractoBancarioDaoService detalleExtractoBancarioDaoService;

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de DetalleExtractoBancario service");
        DetalleExtractoBancario detalleExtractoBancario = new DetalleExtractoBancario();
        for (Long registro : id) {
            detalleExtractoBancarioDaoService.remove(detalleExtractoBancario, registro);
        }
    }

    @Override
    public void save(List<DetalleExtractoBancario> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de DetalleExtractoBancario service");
        for (DetalleExtractoBancario registro : lista) {
            detalleExtractoBancarioDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<DetalleExtractoBancario> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo (selectAll) DetalleExtractoBancarioService");
        List<DetalleExtractoBancario> result = detalleExtractoBancarioDaoService
                .selectAll(NombreEntidadesTesoreria.DETALLE_EXTRACTO_BANCARIO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total DetalleExtractoBancario no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<DetalleExtractoBancario> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo (selectByCriteria) DetalleExtractoBancario");
        List<DetalleExtractoBancario> result = detalleExtractoBancarioDaoService
                .selectByCriteria(datos, NombreEntidadesTesoreria.DETALLE_EXTRACTO_BANCARIO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda selectByCriteria DetalleExtractoBancario no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public DetalleExtractoBancario selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById DetalleExtractoBancario con id: " + id);
        return detalleExtractoBancarioDaoService.selectById(id, NombreEntidadesTesoreria.DETALLE_EXTRACTO_BANCARIO);
    }

    @Override
    public DetalleExtractoBancario saveSingle(DetalleExtractoBancario detalleExtractoBancario) throws Throwable {
        System.out.println("saveSingle - DetalleExtractoBancario");
        // Si codigo llega como 0 desde el cliente, se trata como nuevo registro (INSERT)
        if (detalleExtractoBancario.getCodigo() != null && detalleExtractoBancario.getCodigo() == 0L) {
            detalleExtractoBancario.setCodigo(null);
        }
        return detalleExtractoBancarioDaoService.save(detalleExtractoBancario, detalleExtractoBancario.getCodigo());
    }
}
