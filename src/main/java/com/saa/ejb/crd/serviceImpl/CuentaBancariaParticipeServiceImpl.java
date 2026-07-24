package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.CuentaBancariaParticipeDaoService;
import com.saa.ejb.crd.service.CuentaBancariaParticipeService;
import com.saa.model.crd.CuentaBancariaParticipe;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CuentaBancariaParticipeServiceImpl implements CuentaBancariaParticipeService {

    @EJB
    private CuentaBancariaParticipeDaoService cuentaBancariaParticipeDaoService;

    @Override
    public CuentaBancariaParticipe selectById(Long id) throws Throwable {
        System.out.println("selectById - CuentaBancariaParticipe: " + id);
        return cuentaBancariaParticipeDaoService.selectById(id, NombreEntidadesCredito.CUENTA_BANCARIA_PARTICIPE);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - CuentaBancariaParticipe");
        CuentaBancariaParticipe entidad = new CuentaBancariaParticipe();
        for (Long registro : id) {
            cuentaBancariaParticipeDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<CuentaBancariaParticipe> lista) throws Throwable {
        System.out.println("save list - CuentaBancariaParticipe");
        for (CuentaBancariaParticipe registro : lista) {
            cuentaBancariaParticipeDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CuentaBancariaParticipe> selectAll() throws Throwable {
        System.out.println("selectAll - CuentaBancariaParticipe");
        List<CuentaBancariaParticipe> result = cuentaBancariaParticipeDaoService.selectAll(NombreEntidadesCredito.CUENTA_BANCARIA_PARTICIPE);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros CuentaBancariaParticipe");
        }
        return result;
    }

    @Override
    public CuentaBancariaParticipe saveSingle(CuentaBancariaParticipe cuenta) throws Throwable {
        System.out.println("saveSingle - CuentaBancariaParticipe");
        if (cuenta.getCodigo() == null) {
            cuenta.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        return cuentaBancariaParticipeDaoService.save(cuenta, cuenta.getCodigo());
    }

    @Override
    public List<CuentaBancariaParticipe> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - CuentaBancariaParticipe");
        List<CuentaBancariaParticipe> result = cuentaBancariaParticipeDaoService.selectByCriteria(datos, NombreEntidadesCredito.CUENTA_BANCARIA_PARTICIPE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CuentaBancariaParticipe no devolvio registros");
        }
        return result;
    }

    @Override
    public List<CuentaBancariaParticipe> selectByParent(Long idEntidad) throws Throwable {
        System.out.println("selectByParent CuentaBancariaParticipeService idEntidad: " + idEntidad);
        return cuentaBancariaParticipeDaoService.selectByParent(idEntidad);
    }
}
