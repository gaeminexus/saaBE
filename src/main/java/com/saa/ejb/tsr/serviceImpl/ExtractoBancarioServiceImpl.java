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
import com.saa.ejb.tsr.dao.ExtractoBancarioDaoService;
import com.saa.ejb.tsr.service.ExtractoBancarioService;
import com.saa.model.tsr.ExtractoBancario;
import com.saa.model.tsr.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ExtractoBancarioService.
 * Contiene los servicios relacionados con la entidad ExtractoBancario.</p>
 */
@Stateless
public class ExtractoBancarioServiceImpl implements ExtractoBancarioService {

    @EJB
    private ExtractoBancarioDaoService extractoBancarioDaoService;

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ExtractoBancario service");
        ExtractoBancario extractoBancario = new ExtractoBancario();
        for (Long registro : id) {
            extractoBancarioDaoService.remove(extractoBancario, registro);
        }
    }

    @Override
    public void save(List<ExtractoBancario> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ExtractoBancario service");
        for (ExtractoBancario registro : lista) {
            extractoBancarioDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<ExtractoBancario> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo (selectAll) ExtractoBancarioService");
        List<ExtractoBancario> result = extractoBancarioDaoService
                .selectAll(NombreEntidadesTesoreria.EXTRACTO_BANCARIO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total ExtractoBancario no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<ExtractoBancario> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo (selectByCriteria) ExtractoBancario");
        List<ExtractoBancario> result = extractoBancarioDaoService
                .selectByCriteria(datos, NombreEntidadesTesoreria.EXTRACTO_BANCARIO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda selectByCriteria ExtractoBancario no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public ExtractoBancario selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById ExtractoBancario con id: " + id);
        return extractoBancarioDaoService.selectById(id, NombreEntidadesTesoreria.EXTRACTO_BANCARIO);
    }

    @Override
    public ExtractoBancario saveSingle(ExtractoBancario extractoBancario) throws Throwable {
        System.out.println("saveSingle - ExtractoBancario");
        // Si codigo llega como 0 desde el cliente, se trata como nuevo registro (INSERT)
        if (extractoBancario.getCodigo() != null && extractoBancario.getCodigo() == 0L) {
            extractoBancario.setCodigo(null);
        }
        return extractoBancarioDaoService.save(extractoBancario, extractoBancario.getCodigo());
    }
}
