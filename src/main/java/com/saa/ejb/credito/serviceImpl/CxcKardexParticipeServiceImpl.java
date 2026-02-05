package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;

import com.saa.ejb.credito.dao.CxcKardexParticipeDaoService;
import com.saa.ejb.credito.service.CxcKardexParticipeService;
import com.saa.model.crd.CxcKardexParticipe;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CxcKardexParticipeServiceImpl implements CxcKardexParticipeService {

    @EJB
    private CxcKardexParticipeDaoService cxcKardexParticipeDaoService;

    @Override
    public CxcKardexParticipe selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById CxcKardexParticipe con id: " + id);
        return cxcKardexParticipeDaoService.selectById(id, NombreEntidadesCredito.CXC_KARDEX_PARTICIPE);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CxcKardexParticipeService");
        CxcKardexParticipe cx = new CxcKardexParticipe();
        for (Long registro : ids) {
            cxcKardexParticipeDaoService.remove(cx, registro);
        }
    }

    @Override
    public void save(List<CxcKardexParticipe> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CxcKardexParticipeService");
        for (CxcKardexParticipe registro : lista) {
            cxcKardexParticipeDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CxcKardexParticipe> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll CxcKardexParticipeService");
        List<CxcKardexParticipe> result = cxcKardexParticipeDaoService.selectAll(NombreEntidadesCredito.CXC_KARDEX_PARTICIPE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total CxcKardexParticipe no devolvió ningún registro");
        }
        return result;
    }

    @Override
    public CxcKardexParticipe saveSingle(CxcKardexParticipe cxcKardexParticipe) throws Throwable {
        System.out.println("saveSingle - CxcKardexParticipe");
        if (cxcKardexParticipe.getCodigo() == null) {
        	/*cxcKardexParticipe.setEstado(Long.valueOf(Estado.ACTIVO));*/
        }
        cxcKardexParticipe = cxcKardexParticipeDaoService.save(cxcKardexParticipe, cxcKardexParticipe.getCodigo());
        return cxcKardexParticipe;
    }

    @Override
    public List<CxcKardexParticipe> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria CxcKardexParticipeService");
        List<CxcKardexParticipe> result =
            cxcKardexParticipeDaoService.selectByCriteria(datos, NombreEntidadesCredito.CXC_KARDEX_PARTICIPE);

        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CxcKardexParticipe no devolvió registros");
        }
        return result;
    }
}
