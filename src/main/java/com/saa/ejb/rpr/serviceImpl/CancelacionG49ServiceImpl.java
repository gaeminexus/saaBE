package com.saa.ejb.rpr.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.CancelacionG49DaoService;
import com.saa.ejb.rpr.service.CancelacionG49Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.CancelacionG49;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CancelacionG49ServiceImpl implements CancelacionG49Service {

    @EJB
    private CancelacionG49DaoService cancelacionG49DaoService;

    @Override
    public CancelacionG49 selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById CancelacionG49 con id: " + id);
        return cancelacionG49DaoService.selectById(id, NombreEntidadesReporte.CANCELACION_G49);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CancelacionG49Service");
        CancelacionG49 entidad = new CancelacionG49();
        for (Long registro : id) {
            cancelacionG49DaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<CancelacionG49> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CancelacionG49Service");
        for (CancelacionG49 registro : lista) {
            cancelacionG49DaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CancelacionG49> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll CancelacionG49Service");
        List<CancelacionG49> result = cancelacionG49DaoService.selectAll(NombreEntidadesReporte.CANCELACION_G49);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total CancelacionG49 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public CancelacionG49 saveSingle(CancelacionG49 entidad) throws Throwable {
        System.out.println("saveSingle - CancelacionG49");
        entidad = cancelacionG49DaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<CancelacionG49> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria CancelacionG49Service");
        List<CancelacionG49> result = cancelacionG49DaoService.selectByCriteria(datos, NombreEntidadesReporte.CANCELACION_G49);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CancelacionG49 no devolvio ningun registro");
        }
        return result;
    }
}
