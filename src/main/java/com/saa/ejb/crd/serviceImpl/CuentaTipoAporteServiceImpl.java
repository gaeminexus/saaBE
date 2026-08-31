package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.CuentaTipoAporteDaoService;
import com.saa.ejb.crd.service.CuentaTipoAporteService;
import com.saa.model.crd.CuentaTipoAporte;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CuentaTipoAporteServiceImpl implements CuentaTipoAporteService {

    @EJB
    private CuentaTipoAporteDaoService cuentaTipoAporteDaoService;

    @Override
    public CuentaTipoAporte selectById(Long id) throws Throwable {
        System.out.println("CuentaTipoAporteService.selectById - id: " + id);
        return cuentaTipoAporteDaoService.selectById(id, NombreEntidadesCredito.CUENTA_TIPO_APORTE);
    }

    @Override
    public List<CuentaTipoAporte> selectAll() throws Throwable {
        System.out.println("CuentaTipoAporteService.selectAll");
        return cuentaTipoAporteDaoService.selectAll(NombreEntidadesCredito.CUENTA_TIPO_APORTE);
    }

    @Override
    public List<CuentaTipoAporte> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("CuentaTipoAporteService.selectByCriteria");
        return cuentaTipoAporteDaoService.selectByCriteria(datos, NombreEntidadesCredito.CUENTA_TIPO_APORTE);
    }

    @Override
    public CuentaTipoAporte saveSingle(CuentaTipoAporte registro) throws Throwable {
        System.out.println("CuentaTipoAporteService.saveSingle");
        return cuentaTipoAporteDaoService.save(registro, registro.getCodigo());
    }

    @Override
    public void save(List<CuentaTipoAporte> registros) throws Throwable {
        System.out.println("CuentaTipoAporteService.save - lote: " + registros.size());
        for (CuentaTipoAporte registro : registros) {
            saveSingle(registro);
        }
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("CuentaTipoAporteService.remove - lote: " + ids.size());
        CuentaTipoAporte entidad = new CuentaTipoAporte();
        for (Long id : ids) {
            cuentaTipoAporteDaoService.remove(entidad, id);
        }
    }

    @Override
    public CuentaTipoAporte selectByTipoAporteYEmpresa(Long idTipoAporte, Long idEmpresa) throws Throwable {
        System.out.println("CuentaTipoAporteService.selectByTipoAporteYEmpresa - tipoAporte: "
                + idTipoAporte + " empresa: " + idEmpresa);
        return cuentaTipoAporteDaoService.selectByTipoAporteYEmpresa(idTipoAporte, idEmpresa);
    }

    @Override
    public List<CuentaTipoAporte> listarPorEmpresa(Long idEmpresa) throws Throwable {
        System.out.println("CuentaTipoAporteService.listarPorEmpresa - empresa: " + idEmpresa);
        return cuentaTipoAporteDaoService.selectByEmpresa(idEmpresa);
    }
}
