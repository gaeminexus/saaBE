package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DireccionDaoService;
import com.saa.ejb.crd.service.DireccionService;
import com.saa.model.crd.Direccion;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DireccionServiceImpl implements DireccionService {

    @EJB
    private DireccionDaoService direccionDaoService;

    @Override
    public Direccion selectById(Long id) throws Throwable {
        System.out.println("selectById - Direccion: " + id);
        return direccionDaoService.selectById(id, NombreEntidadesCredito.DIRECCION);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - Direccion");
        Direccion dir = new Direccion();
        for (Long registro : id) {
            direccionDaoService.remove(dir, registro);
        }
    }

    @Override
    public void save(List<Direccion> lista) throws Throwable {
        System.out.println("save list - Direccion");
        for (Direccion registro : lista) {
            direccionDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<Direccion> selectAll() throws Throwable {
        System.out.println("selectAll - Direccion");
        List<Direccion> result =
            direccionDaoService.selectAll(NombreEntidadesCredito.DIRECCION);

        if (result.isEmpty()) {
            throw new IncomeException("No existen registros Direccion");
        }
        return result;
    }

    @Override
    public Direccion saveSingle(Direccion dir) throws Throwable {
        System.out.println("saveSingle - Direccion");
        if (dir.getCodigo() == null) {
            dir.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        return direccionDaoService.save(dir, dir.getCodigo());
    }

    @Override
    public List<Direccion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - Direccion");
        List<Direccion> result =
            direccionDaoService.selectByCriteria(datos, NombreEntidadesCredito.DIRECCION);

        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Direccion no devolvió registros");
        }
        return result;
    }
}
