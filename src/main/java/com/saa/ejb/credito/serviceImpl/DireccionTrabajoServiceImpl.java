package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;

import com.saa.ejb.credito.dao.DireccionTrabajoDaoService;
import com.saa.ejb.credito.service.DireccionTrabajoService;

import com.saa.model.credito.DireccionTrabajo;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DireccionTrabajoServiceImpl implements DireccionTrabajoService {

    @EJB
    private DireccionTrabajoDaoService direccionTrabajoDaoService;

    @Override
    public DireccionTrabajo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById DireccionTrabajo con id: " + id);
        return direccionTrabajoDaoService.selectById(id, NombreEntidadesCredito.DIRECCION_TRABAJO);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de DireccionTrabajoService");
        DireccionTrabajo dt = new DireccionTrabajo();
        for (Long registro : ids) {
            direccionTrabajoDaoService.remove(dt, registro);
        }
    }

    @Override
    public void save(List<DireccionTrabajo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de DireccionTrabajoService");
        for (DireccionTrabajo registro : lista) {
            direccionTrabajoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<DireccionTrabajo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll DireccionTrabajoService");
        List<DireccionTrabajo> result = direccionTrabajoDaoService.selectAll(NombreEntidadesCredito.DIRECCION_TRABAJO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total DireccionTrabajo no devolvió ningún registro");
        }
        return result;
    }

    @Override
    public DireccionTrabajo saveSingle(DireccionTrabajo dt) throws Throwable {
        System.out.println("saveSingle - DireccionTrabajo");
        if (dt.getCodigo() == null) {
        	/*  dt.setEstado(Long.valueOf(Estado.ACTIVO)); */
        }
        dt = direccionTrabajoDaoService.save(dt, dt.getCodigo());
        return dt;
    }

    @Override
    public List<DireccionTrabajo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria DireccionTrabajoService");
        List<DireccionTrabajo> result = direccionTrabajoDaoService.selectByCriteria(datos, NombreEntidadesCredito.DIRECCION_TRABAJO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio DireccionTrabajo no devolvió registros");
        }
        return result;
    }
}
