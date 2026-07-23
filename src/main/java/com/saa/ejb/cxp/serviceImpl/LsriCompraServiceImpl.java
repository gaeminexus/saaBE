package com.saa.ejb.cxp.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.LsriCompraDaoService;
import com.saa.ejb.cxp.service.LsriCompraService;
import com.saa.model.cxp.Lsri;
import com.saa.model.cxp.NombreEntidadesCompra;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación de LsriCompraService (módulo CXP).
 */
@Stateless
public class LsriCompraServiceImpl implements LsriCompraService {

    @EJB
    private LsriCompraDaoService lsriCompraDaoService;

    @Override
    public void save(List<Lsri> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de Lsri CXP service");
        for (Lsri registro : lista) {
            lsriCompraDaoService.save(registro, registro.getId());
        }
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de Lsri CXP service");
        Lsri lsri = new Lsri();
        for (Long id : ids) {
            lsriCompraDaoService.remove(lsri, id);
        }
    }

    @Override
    public List<Lsri> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo (selectAll) Lsri CXP");
        List<Lsri> result = lsriCompraDaoService.selectAll(NombreEntidadesCompra.LSRI_COMPRA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda completa de Lsri CXP no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<Lsri> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo (selectByCriteria) Lsri CXP");
        List<Lsri> result = lsriCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.LSRI_COMPRA_ENTITY);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio de Lsri CXP no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public Lsri selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById Lsri CXP con id: " + id);
        return lsriCompraDaoService.selectById(id, NombreEntidadesCompra.LSRI_COMPRA);
    }

    @Override
    public Lsri saveSingle(Lsri lsri) throws Throwable {
        System.out.println("saveSingle - Lsri CXP");
        return lsriCompraDaoService.save(lsri, lsri.getId());
    }
}
