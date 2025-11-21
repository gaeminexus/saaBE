package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.CesantiaDaoService;
import com.saa.ejb.credito.service.CesantiaService;
import com.saa.model.credito.Cesantia;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CesantiaServiceImpl implements CesantiaService {

    @EJB
    private CesantiaDaoService cesantiaDaoService;

    @Override
    public Cesantia selectById(Long id) throws Throwable {
        System.out.println("selectById - Cesantia: " + id);
        return cesantiaDaoService.selectById(id, NombreEntidadesCredito.CESANTIA);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - Cesantia");
        Cesantia csnt = new Cesantia();
        for (Long registro : id) {
            cesantiaDaoService.remove(csnt, registro);
        }
    }

    @Override
    public void save(List<Cesantia> lista) throws Throwable {
        System.out.println("save list - Cesantia");
        for (Cesantia registro : lista) {
            cesantiaDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<Cesantia> selectAll() throws Throwable {
        System.out.println("selectAll - Cesantia");
        List<Cesantia> result =
            cesantiaDaoService.selectAll(NombreEntidadesCredito.CESANTIA);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros Cesantia");
        }
        return result;
    }

    @Override
    public Cesantia saveSingle(Cesantia csnt) throws Throwable {
        System.out.println("saveSingle - Cesantia");
        if (csnt.getCodigo() == null) {
            csnt.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        return cesantiaDaoService.save(csnt, csnt.getCodigo());
    }

    @Override
    public List<Cesantia> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - Cesantia");
        List<Cesantia> result =
            cesantiaDaoService.selectByCriteria(datos, NombreEntidadesCredito.CESANTIA);

        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Cesantia no devolvió registros");
        }
        return result;
    }
}
