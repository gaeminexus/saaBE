package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.CxcParticipeDaoService;
import com.saa.ejb.credito.service.CxcParticipeService;
import com.saa.model.credito.CxcParticipe;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CxcParticipeServiceImpl implements CxcParticipeService {

    @EJB
    private CxcParticipeDaoService cxcParticipeDaoService;

    /**
     * Recupera un registro de CxcParticipe por su ID.
     */
    @Override
    public CxcParticipe selectById(Long id) throws Throwable {
        System.out.println("selectById - CxcParticipe: " + id);
        return cxcParticipeDaoService.selectById(id, NombreEntidadesCredito.CXC_PARTICIPE);
    }

    /**
     * Elimina uno o varios registros de CxcParticipe.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - CxcParticipe");
        CxcParticipe cxcParticipe = new CxcParticipe();
        for (Long registro : id) {
            cxcParticipeDaoService.remove(cxcParticipe, registro);
        }
    }

    /**
     * Guarda una lista de registros de CxcParticipe.
     */
    @Override
    public void save(List<CxcParticipe> lista) throws Throwable {
        System.out.println("save list - CxcParticipe");
        for (CxcParticipe registro : lista) {
            cxcParticipeDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de CxcParticipe.
     */
    @Override
    public List<CxcParticipe> selectAll() throws Throwable {
        System.out.println("selectAll - CxcParticipe");
        List<CxcParticipe> result =
                cxcParticipeDaoService.selectAll(NombreEntidadesCredito.CXC_PARTICIPE);

        if (result.isEmpty()) {
            throw new IncomeException("No existen registros CxcParticipe");
        }
        return result;
    }

    /**
     * Guarda un solo registro de CxcParticipe.
     */
    @Override
    public CxcParticipe saveSingle(CxcParticipe cxcParticipe) throws Throwable {
        System.out.println("saveSingle - CxcParticipe");
        if (cxcParticipe.getCodigo() == null) {
        	/* cxcParticipe.setEstado(Long.valueOf(Estado.ACTIVO));*/
        }
        return cxcParticipeDaoService.save(cxcParticipe, cxcParticipe.getCodigo());
    }

    /**
     * Recupera registros por criterios de búsqueda.
     */
    @Override
    public List<CxcParticipe> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - CxcParticipe");
        List<CxcParticipe> result =
                cxcParticipeDaoService.selectByCriteria(datos, NombreEntidadesCredito.CXC_PARTICIPE);

        if (result.isEmpty()) {
            throw new IncomeException("Búsqueda por criterio CxcParticipe no devolvió registros");
        }
        return result;
    }
}
