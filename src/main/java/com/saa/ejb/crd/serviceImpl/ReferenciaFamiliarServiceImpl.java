package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.ReferenciaFamiliarDaoService;
import com.saa.ejb.crd.service.ReferenciaFamiliarService;
import com.saa.model.crd.ReferenciaFamiliar;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ReferenciaFamiliarServiceImpl implements ReferenciaFamiliarService {

    @EJB
    private ReferenciaFamiliarDaoService referenciaFamiliarDaoService;

    @Override
    public ReferenciaFamiliar selectById(Long id) throws Throwable {
        System.out.println("selectById - ReferenciaFamiliar: " + id);
        return referenciaFamiliarDaoService.selectById(id, NombreEntidadesCredito.REFERENCIA_FAMILIAR);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - ReferenciaFamiliar");
        ReferenciaFamiliar entidad = new ReferenciaFamiliar();
        for (Long registro : id) {
            referenciaFamiliarDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<ReferenciaFamiliar> lista) throws Throwable {
        System.out.println("save list - ReferenciaFamiliar");
        for (ReferenciaFamiliar registro : lista) {
            referenciaFamiliarDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<ReferenciaFamiliar> selectAll() throws Throwable {
        System.out.println("selectAll - ReferenciaFamiliar");
        List<ReferenciaFamiliar> result = referenciaFamiliarDaoService.selectAll(NombreEntidadesCredito.REFERENCIA_FAMILIAR);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros ReferenciaFamiliar");
        }
        return result;
    }

    @Override
    public ReferenciaFamiliar saveSingle(ReferenciaFamiliar ref) throws Throwable {
        System.out.println("saveSingle - ReferenciaFamiliar");
        if (ref.getCodigo() == null) {
            ref.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        return referenciaFamiliarDaoService.save(ref, ref.getCodigo());
    }

    @Override
    public List<ReferenciaFamiliar> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - ReferenciaFamiliar");
        List<ReferenciaFamiliar> result = referenciaFamiliarDaoService.selectByCriteria(datos, NombreEntidadesCredito.REFERENCIA_FAMILIAR);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio ReferenciaFamiliar no devolvio registros");
        }
        return result;
    }

    @Override
    public List<ReferenciaFamiliar> selectByParent(Long idEntidad) throws Throwable {
        System.out.println("selectByParent ReferenciaFamiliarService idEntidad: " + idEntidad);
        return referenciaFamiliarDaoService.selectByParent(idEntidad);
    }
}
