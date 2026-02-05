package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.CiudadDaoService;
import com.saa.ejb.crd.service.CiudadService;
import com.saa.model.crd.Ciudad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CiudadServiceImpl implements CiudadService {

    @EJB
    private CiudadDaoService ciudadDaoService;

    @Override
    public Ciudad selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById Ciudad con id: " + id);
        return ciudadDaoService.selectById(id, NombreEntidadesCredito.CIUDAD);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CiudadService ... depurado");
        Ciudad ciudad = new Ciudad();
        for (Long registro : id) {
            ciudadDaoService.remove(ciudad, registro);
        }
    }

    @Override
    public void save(List<Ciudad> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CiudadService");
        for (Ciudad registro : lista) {
            ciudadDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<Ciudad> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll CiudadService");
        List<Ciudad> result = ciudadDaoService.selectAll(NombreEntidadesCredito.CIUDAD);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Ciudad no devolvió ningún registro");
        }
        return result;
    }

    @Override
    public Ciudad saveSingle(Ciudad ciudad) throws Throwable {
        System.out.println("saveSingle - Ciudad");
        if (ciudad.getCodigo() == null) {
            ciudad.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        ciudad = ciudadDaoService.save(ciudad, ciudad.getCodigo());
        return ciudad;
    }

    @Override
    public List<Ciudad> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria CiudadService");
        List<Ciudad> result = ciudadDaoService.selectByCriteria(datos, NombreEntidadesCredito.CIUDAD);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Ciudad no devolvió ningún registro");
        }
        return result;
    }
}
