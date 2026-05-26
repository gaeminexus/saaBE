package com.saa.ejb.rpr.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.GarantiaRealG51DaoService;
import com.saa.ejb.rpr.service.GarantiaRealG51Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.GarantiaRealG51;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GarantiaRealG51ServiceImpl implements GarantiaRealG51Service {

    @EJB
    private GarantiaRealG51DaoService garantiaRealG51DaoService;

    @Override
    public GarantiaRealG51 selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById GarantiaRealG51 con id: " + id);
        return garantiaRealG51DaoService.selectById(id, NombreEntidadesReporte.GARANTIA_REAL_G51);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de GarantiaRealG51Service");
        GarantiaRealG51 entidad = new GarantiaRealG51();
        for (Long registro : id) {
            garantiaRealG51DaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<GarantiaRealG51> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de GarantiaRealG51Service");
        for (GarantiaRealG51 registro : lista) {
            garantiaRealG51DaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<GarantiaRealG51> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll GarantiaRealG51Service");
        List<GarantiaRealG51> result = garantiaRealG51DaoService.selectAll(NombreEntidadesReporte.GARANTIA_REAL_G51);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total GarantiaRealG51 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public GarantiaRealG51 saveSingle(GarantiaRealG51 entidad) throws Throwable {
        System.out.println("saveSingle - GarantiaRealG51");
        entidad = garantiaRealG51DaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<GarantiaRealG51> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria GarantiaRealG51Service");
        List<GarantiaRealG51> result = garantiaRealG51DaoService.selectByCriteria(datos, NombreEntidadesReporte.GARANTIA_REAL_G51);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio GarantiaRealG51 no devolvio ningun registro");
        }
        return result;
    }
}
