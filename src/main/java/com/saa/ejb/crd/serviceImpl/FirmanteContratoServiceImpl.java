package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.FirmanteContratoDaoService;
import com.saa.ejb.crd.service.FirmanteContratoService;
import com.saa.model.crd.FirmanteContrato;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class FirmanteContratoServiceImpl implements FirmanteContratoService {

    @EJB
    private FirmanteContratoDaoService firmanteContratoDaoService;

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - FirmanteContrato");
        FirmanteContrato entidad = new FirmanteContrato();
        for (Long registro : id) {
            firmanteContratoDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<FirmanteContrato> lista) throws Throwable {
        System.out.println("save list - FirmanteContrato");
        for (FirmanteContrato registro : lista) {
            firmanteContratoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public FirmanteContrato saveSingle(FirmanteContrato firmante) throws Throwable {
        System.out.println("saveSingle - FirmanteContrato");
        return firmanteContratoDaoService.save(firmante, firmante.getCodigo());
    }

    @Override
    public List<FirmanteContrato> selectAll() throws Throwable {
        System.out.println("selectAll - FirmanteContrato");
        List<FirmanteContrato> result = firmanteContratoDaoService.selectAll(NombreEntidadesCredito.FIRMANTE_CONTRATO);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros FirmanteContrato");
        }
        return result;
    }

    @Override
    public FirmanteContrato selectById(Long id) throws Throwable {
        System.out.println("selectById - FirmanteContrato: " + id);
        return firmanteContratoDaoService.selectById(id, NombreEntidadesCredito.FIRMANTE_CONTRATO);
    }

    @Override
    public List<FirmanteContrato> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - FirmanteContrato");
        List<FirmanteContrato> result = firmanteContratoDaoService.selectByCriteria(datos, NombreEntidadesCredito.FIRMANTE_CONTRATO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio FirmanteContrato no devolvio registros");
        }
        return result;
    }

    @Override
    public List<FirmanteContrato> selectByFirma(Long idFirma) throws Throwable {
        System.out.println("selectByFirma - FirmanteContrato: " + idFirma);
        return firmanteContratoDaoService.selectByFirma(idFirma);
    }
}
