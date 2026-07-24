package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.ConyugeDaoService;
import com.saa.ejb.crd.service.ConyugeService;
import com.saa.model.crd.Conyuge;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ConyugeServiceImpl implements ConyugeService {

    @EJB
    private ConyugeDaoService conyugeDaoService;

    @Override
    public Conyuge selectById(Long id) throws Throwable {
        System.out.println("selectById - Conyuge: " + id);
        return conyugeDaoService.selectById(id, NombreEntidadesCredito.CONYUGE);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - Conyuge");
        Conyuge entidad = new Conyuge();
        for (Long registro : id) {
            conyugeDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<Conyuge> lista) throws Throwable {
        System.out.println("save list - Conyuge");
        for (Conyuge registro : lista) {
            conyugeDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<Conyuge> selectAll() throws Throwable {
        System.out.println("selectAll - Conyuge");
        List<Conyuge> result = conyugeDaoService.selectAll(NombreEntidadesCredito.CONYUGE);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros Conyuge");
        }
        return result;
    }

    @Override
    public Conyuge saveSingle(Conyuge conyuge) throws Throwable {
        System.out.println("saveSingle - Conyuge");
        if (conyuge.getCodigo() == null) {
            conyuge.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        return conyugeDaoService.save(conyuge, conyuge.getCodigo());
    }

    @Override
    public List<Conyuge> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - Conyuge");
        List<Conyuge> result = conyugeDaoService.selectByCriteria(datos, NombreEntidadesCredito.CONYUGE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Conyuge no devolvio registros");
        }
        return result;
    }

    @Override
    public List<Conyuge> selectByParent(Long idEntidad) throws Throwable {
        System.out.println("selectByParent ConyugeService idEntidad: " + idEntidad);
        return conyugeDaoService.selectByParent(idEntidad);
    }
}
