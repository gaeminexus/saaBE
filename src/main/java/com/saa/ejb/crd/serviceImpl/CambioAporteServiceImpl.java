package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.CambioAporteDaoService;
import com.saa.ejb.crd.service.CambioAporteService;
import com.saa.model.crd.CambioAporte;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CambioAporteServiceImpl implements CambioAporteService {

    @EJB
    private CambioAporteDaoService cambioAporteDaoService;

    @Override
    public CambioAporte selectById(Long id) throws Throwable {
        System.out.println("selectById - CambioAporte id: " + id);
        return cambioAporteDaoService.selectById(id, NombreEntidadesCredito.CAMBIO_APORTE);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - CambioAporte");
        CambioAporte cambio = new CambioAporte();
        for (Long registro : id) {
            cambioAporteDaoService.remove(cambio, registro);
        }
    }

    @Override
    public void save(List<CambioAporte> lista) throws Throwable {
        System.out.println("save list - CambioAporte");
        for (CambioAporte registro : lista) {
            cambioAporteDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CambioAporte> selectAll() throws Throwable {
        System.out.println("selectAll - CambioAporte");
        List<CambioAporte> result = cambioAporteDaoService.selectAll(NombreEntidadesCredito.CAMBIO_APORTE);
        if (result.isEmpty()) {
            throw new IncomeException("No se encontraron registros de CambioAporte");
        }
        return result;
    }

    @Override
    public CambioAporte saveSingle(CambioAporte cambio) throws Throwable {
        System.out.println("saveSingle - CambioAporte");
        if (cambio.getCodigo() == null) {
            cambio.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        cambio = cambioAporteDaoService.save(cambio, cambio.getCodigo());
        return cambio;
    }

    @Override
    public List<CambioAporte> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - CambioAporte");
        List<CambioAporte> result =
                cambioAporteDaoService.selectByCriteria(datos, NombreEntidadesCredito.CAMBIO_APORTE);

        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CambioAporte no devolvió registros");
        }
        return result;
    }
}
