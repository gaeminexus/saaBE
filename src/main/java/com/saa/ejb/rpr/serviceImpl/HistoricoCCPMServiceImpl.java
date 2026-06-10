package com.saa.ejb.rpr.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.HistoricoCCPMDaoService;
import com.saa.ejb.rpr.service.HistoricoCCPMService;
import com.saa.model.rpr.HistoricoCCPM;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class HistoricoCCPMServiceImpl implements HistoricoCCPMService {

    @EJB
    private HistoricoCCPMDaoService historicoCCPMDaoService;

    @Override
    public HistoricoCCPM selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById HistoricoCCPM con id: " + id);
        return historicoCCPMDaoService.selectById(id, NombreEntidadesReporte.HISTORICO_CCPM);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de HistoricoCCPMService");
        HistoricoCCPM entidad = new HistoricoCCPM();
        for (Long registro : id) {
            historicoCCPMDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<HistoricoCCPM> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de HistoricoCCPMService");
        for (HistoricoCCPM registro : lista) {
            historicoCCPMDaoService.save(registro, null);
        }
    }

    @Override
    public List<HistoricoCCPM> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll HistoricoCCPMService");
        List<HistoricoCCPM> result = historicoCCPMDaoService.selectAll(NombreEntidadesReporte.HISTORICO_CCPM);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total HistoricoCCPM no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public HistoricoCCPM saveSingle(HistoricoCCPM entidad) throws Throwable {
        System.out.println("saveSingle - HistoricoCCPM");
        entidad = historicoCCPMDaoService.save(entidad, null);
        return entidad;
    }

    @Override
    public List<HistoricoCCPM> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria HistoricoCCPMService");
        List<HistoricoCCPM> result = historicoCCPMDaoService.selectByCriteria(datos, NombreEntidadesReporte.HISTORICO_CCPM);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio HistoricoCCPM no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public HistoricoCCPM selectByNumeroOperacion(String numeroOperacion) throws Throwable {
        return historicoCCPMDaoService.selectByNumeroOperacion(numeroOperacion);
    }
}
