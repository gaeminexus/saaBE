package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.NuevoPrestamoG46DaoService;
import com.saa.ejb.rpr.service.NuevoPrestamoG46Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.NuevoPrestamoG46;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class NuevoPrestamoG46ServiceImpl implements NuevoPrestamoG46Service {

    @EJB
    private NuevoPrestamoG46DaoService nuevoPrestamoG46DaoService;

    @Override
    public NuevoPrestamoG46 selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById NuevoPrestamoG46 con id: " + id);
        return nuevoPrestamoG46DaoService.selectById(id, NombreEntidadesReporte.NUEVO_PRESTAMO_G46);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de NuevoPrestamoG46Service");
        NuevoPrestamoG46 entidad = new NuevoPrestamoG46();
        for (Long registro : id) {
            nuevoPrestamoG46DaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<NuevoPrestamoG46> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de NuevoPrestamoG46Service");
        for (NuevoPrestamoG46 registro : lista) {
            nuevoPrestamoG46DaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<NuevoPrestamoG46> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll NuevoPrestamoG46Service");
        List<NuevoPrestamoG46> result = nuevoPrestamoG46DaoService.selectAll(NombreEntidadesReporte.NUEVO_PRESTAMO_G46);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total NuevoPrestamoG46 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public NuevoPrestamoG46 saveSingle(NuevoPrestamoG46 entidad) throws Throwable {
        System.out.println("saveSingle - NuevoPrestamoG46");
        entidad = nuevoPrestamoG46DaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<NuevoPrestamoG46> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria NuevoPrestamoG46Service");
        List<NuevoPrestamoG46> result = nuevoPrestamoG46DaoService.selectByCriteria(datos, NombreEntidadesReporte.NUEVO_PRESTAMO_G46);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio NuevoPrestamoG46 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<NuevoPrestamoG46> selectByDetalle(Long codigoDetalle) throws Throwable {
        return nuevoPrestamoG46DaoService.selectByDetalle(codigoDetalle);
    }
}
