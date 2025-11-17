package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TipoViviendaDaoService;
import com.saa.ejb.credito.service.TipoViviendaService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.TipoVivienda;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TipoViviendaServiceImpl implements TipoViviendaService {

    @EJB
    private TipoViviendaDaoService tipoViviendaDaoService;

    /**
     * Recupera un registro de TipoVivienda por su ID.
     */
    @Override
    public TipoVivienda selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return tipoViviendaDaoService.selectById(id, NombreEntidadesCredito.TIPO_VIVIENDA);
    }

    /**
     * Elimina uno o varios registros de TipoVivienda.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TipoViviendaService ... depurado");
        TipoVivienda tipoVivienda = new TipoVivienda();
        for (Long registro : id) {
            tipoViviendaDaoService.remove(tipoVivienda, registro);
        }
    }

    /**
     * Guarda una lista de registros de TipoVivienda.
     */
    @Override
    public void save(List<TipoVivienda> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TipoViviendaService");
        for (TipoVivienda registro : lista) {
            tipoViviendaDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de TipoVivienda.
     */
    @Override
    public List<TipoVivienda> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TipoViviendaService");
        List<TipoVivienda> result = tipoViviendaDaoService.selectAll(NombreEntidadesCredito.TIPO_VIVIENDA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total TipoVivienda no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de TipoVivienda.
     */
    @Override
    public TipoVivienda saveSingle(TipoVivienda tipoVivienda) throws Throwable {
        System.out.println("saveSingle - TipoVivienda");
        tipoVivienda = tipoViviendaDaoService.save(tipoVivienda, tipoVivienda.getCodigo());
        return tipoVivienda;
    }

    /**
     * Recupera registros de TipoVivienda segun criterios de búsqueda.
     */
    @Override
    public List<TipoVivienda> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TipoViviendaService");
        List<TipoVivienda> result = tipoViviendaDaoService.selectByCriteria(datos, NombreEntidadesCredito.TIPO_VIVIENDA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio TipoVivienda no devolvio ningun registro");
        }
        return result;
    }
}
