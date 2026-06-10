package com.saa.ejb.rpr.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.HistoricoCJBMDaoService;
import com.saa.ejb.rpr.service.HistoricoCJBMService;
import com.saa.model.rpr.HistoricoCJBM;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class HistoricoCJBMServiceImpl implements HistoricoCJBMService {

    @EJB
    private HistoricoCJBMDaoService historicoCJBMDaoService;

    @Override
    public HistoricoCJBM selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById HistoricoCJBM con id: " + id);
        return historicoCJBMDaoService.selectById(id, NombreEntidadesReporte.HISTORICO_CJBM);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de HistoricoCJBMService");
        HistoricoCJBM entidad = new HistoricoCJBM();
        for (Long registro : id) {
            historicoCJBMDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<HistoricoCJBM> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de HistoricoCJBMService");
        for (HistoricoCJBM registro : lista) {
            historicoCJBMDaoService.save(registro, null);
        }
    }

    @Override
    public List<HistoricoCJBM> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll HistoricoCJBMService");
        List<HistoricoCJBM> result = historicoCJBMDaoService.selectAll(NombreEntidadesReporte.HISTORICO_CJBM);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total HistoricoCJBM no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public HistoricoCJBM saveSingle(HistoricoCJBM entidad) throws Throwable {
        System.out.println("saveSingle - HistoricoCJBM");
        entidad = historicoCJBMDaoService.save(entidad, null);
        return entidad;
    }

    @Override
    public List<HistoricoCJBM> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria HistoricoCJBMService");
        List<HistoricoCJBM> result = historicoCJBMDaoService.selectByCriteria(datos, NombreEntidadesReporte.HISTORICO_CJBM);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio HistoricoCJBM no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<HistoricoCJBM> selectByIdentificacion(String identificacion) throws Throwable {
        return historicoCJBMDaoService.selectByIdentificacion(identificacion);
    }

    @Override
    public List<HistoricoCJBM> selectByIdentificacionesIn(List<String> identificaciones) throws Throwable {
        System.out.println("selectByIdentificacionesIn HistoricoCJBM - cantidad: " + (identificaciones != null ? identificaciones.size() : 0));
        return historicoCJBMDaoService.selectByIdentificacionesIn(identificaciones);
    }
}
