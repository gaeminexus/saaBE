package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.ReferenciaPersonalDaoService;
import com.saa.ejb.crd.service.ReferenciaPersonalService;
import com.saa.model.crd.ReferenciaPersonal;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ReferenciaPersonalServiceImpl implements ReferenciaPersonalService {

    @EJB
    private ReferenciaPersonalDaoService referenciaPersonalDaoService;

    @Override
    public ReferenciaPersonal selectById(Long id) throws Throwable {
        System.out.println("selectById - ReferenciaPersonal: " + id);
        return referenciaPersonalDaoService.selectById(id, NombreEntidadesCredito.REFERENCIA_PERSONAL);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - ReferenciaPersonal");
        ReferenciaPersonal entidad = new ReferenciaPersonal();
        for (Long registro : id) {
            referenciaPersonalDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<ReferenciaPersonal> lista) throws Throwable {
        System.out.println("save list - ReferenciaPersonal");
        for (ReferenciaPersonal registro : lista) {
            referenciaPersonalDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<ReferenciaPersonal> selectAll() throws Throwable {
        System.out.println("selectAll - ReferenciaPersonal");
        List<ReferenciaPersonal> result = referenciaPersonalDaoService.selectAll(NombreEntidadesCredito.REFERENCIA_PERSONAL);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros ReferenciaPersonal");
        }
        return result;
    }

    @Override
    public ReferenciaPersonal saveSingle(ReferenciaPersonal ref) throws Throwable {
        System.out.println("saveSingle - ReferenciaPersonal");
        if (ref.getCodigo() == null) {
            ref.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        return referenciaPersonalDaoService.save(ref, ref.getCodigo());
    }

    @Override
    public List<ReferenciaPersonal> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - ReferenciaPersonal");
        List<ReferenciaPersonal> result = referenciaPersonalDaoService.selectByCriteria(datos, NombreEntidadesCredito.REFERENCIA_PERSONAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio ReferenciaPersonal no devolvio registros");
        }
        return result;
    }

    @Override
    public List<ReferenciaPersonal> selectByParent(Long idEntidad) throws Throwable {
        System.out.println("selectByParent ReferenciaPersonalService idEntidad: " + idEntidad);
        return referenciaPersonalDaoService.selectByParent(idEntidad);
    }
}
