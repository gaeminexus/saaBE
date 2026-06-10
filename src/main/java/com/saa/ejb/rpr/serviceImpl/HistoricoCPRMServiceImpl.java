package com.saa.ejb.rpr.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.HistoricoCPRMDaoService;
import com.saa.ejb.rpr.service.HistoricoCPRMService;
import com.saa.model.rpr.HistoricoCPRM;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class HistoricoCPRMServiceImpl implements HistoricoCPRMService {

    @EJB
    private HistoricoCPRMDaoService historicoCPRMDaoService;

    @Override
    public HistoricoCPRM selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById HistoricoCPRM con id: " + id);
        return historicoCPRMDaoService.selectById(id, NombreEntidadesReporte.HISTORICO_CPRM);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de HistoricoCPRMService");
        HistoricoCPRM entidad = new HistoricoCPRM();
        for (Long registro : id) {
            historicoCPRMDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<HistoricoCPRM> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de HistoricoCPRMService");
        for (HistoricoCPRM registro : lista) {
            historicoCPRMDaoService.save(registro, null);
        }
    }

    @Override
    public List<HistoricoCPRM> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll HistoricoCPRMService");
        List<HistoricoCPRM> result = historicoCPRMDaoService.selectAll(NombreEntidadesReporte.HISTORICO_CPRM);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total HistoricoCPRM no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public HistoricoCPRM saveSingle(HistoricoCPRM entidad) throws Throwable {
        System.out.println("saveSingle - HistoricoCPRM");
        entidad = historicoCPRMDaoService.save(entidad, null);
        return entidad;
    }

    @Override
    public List<HistoricoCPRM> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria HistoricoCPRMService");
        List<HistoricoCPRM> result = historicoCPRMDaoService.selectByCriteria(datos, NombreEntidadesReporte.HISTORICO_CPRM);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio HistoricoCPRM no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<HistoricoCPRM> selectByIdentificacion(String identificacion) throws Throwable {
        return historicoCPRMDaoService.selectByIdentificacion(identificacion);
    }
}
