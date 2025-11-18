package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TipoParticipeDaoService;
import com.saa.ejb.credito.service.TipoParticipeService;
import com.saa.model.credito.TipoParticipe;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TipoParticipeServiceImpl implements TipoParticipeService {

    @EJB
    private TipoParticipeDaoService tipoParticipeDaoService;

    /**
     * Recupera un registro de TipoParticipe por su ID.
     */
    @Override
    public TipoParticipe selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById TipoParticipe con id: " + id);
        return tipoParticipeDaoService.selectById(id, NombreEntidadesCredito.TIPO_PARTICIPE);
    }

    /**
     * Elimina uno o varios registros de TipoParticipe.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TipoParticipeService ... depurado");
        TipoParticipe tipoParticipe = new TipoParticipe();
        for (Long registro : id) {
            tipoParticipeDaoService.remove(tipoParticipe, registro);
        }
    }

    /**
     * Guarda una lista de registros de TipoParticipe.
     */
    @Override
    public void save(List<TipoParticipe> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TipoParticipeService");
        for (TipoParticipe registro : lista) {
            tipoParticipeDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de TipoParticipe.
     */
    @Override
    public List<TipoParticipe> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TipoParticipeService");
        List<TipoParticipe> result = tipoParticipeDaoService.selectAll(NombreEntidadesCredito.TIPO_PARTICIPE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total TipoParticipe no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de TipoParticipe.
     */
    @Override
    public TipoParticipe saveSingle(TipoParticipe tipoParticipe) throws Throwable {
        System.out.println("saveSingle - TipoParticipe");
        tipoParticipe = tipoParticipeDaoService.save(tipoParticipe, tipoParticipe.getCodigo());
        return tipoParticipe;
    }

    /**
     * Recupera registros de TipoParticipe segun criterios de búsqueda.
     */
    @Override
    public List<TipoParticipe> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TipoParticipeService");
        List<TipoParticipe> result = tipoParticipeDaoService.selectByCriteria(datos, NombreEntidadesCredito.TIPO_PARTICIPE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio TipoParticipe no devolvio ningun registro");
        }
        return result;
    }
}
