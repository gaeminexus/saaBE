package com.saa.ejb.cxp.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.TsriCompraDaoService;
import com.saa.ejb.cxp.service.TsriCompraService;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.model.cxp.Tsri;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación de TsriCompraService (módulo CXP).
 */
@Stateless
public class TsriCompraServiceImpl implements TsriCompraService {

    @EJB
    private TsriCompraDaoService tsriCompraDaoService;

    @Override
    public void save(List<Tsri> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de Tsri CXP service");
        for (Tsri registro : lista) {
            tsriCompraDaoService.save(registro, registro.getId());
        }
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de Tsri CXP service");
        Tsri tsri = new Tsri();
        for (Long id : ids) {
            tsriCompraDaoService.remove(tsri, id);
        }
    }

    @Override
    public List<Tsri> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo (selectAll) Tsri CXP");
        List<Tsri> result = tsriCompraDaoService.selectAll(NombreEntidadesCompra.TSRI_COMPRA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda completa de Tsri CXP no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<Tsri> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo (selectByCriteria) Tsri CXP");
        List<Tsri> result = tsriCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.TSRI_COMPRA_ENTITY);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio de Tsri CXP no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public Tsri selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById Tsri CXP con id: " + id);
        return tsriCompraDaoService.selectById(id, NombreEntidadesCompra.TSRI_COMPRA);
    }

    @Override
    public Tsri saveSingle(Tsri tsri) throws Throwable {
        System.out.println("saveSingle - Tsri CXP");
        return tsriCompraDaoService.save(tsri, tsri.getId());
    }
}
