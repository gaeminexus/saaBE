package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TipoCalificacionCreditoDaoService;
import com.saa.ejb.credito.service.TipoCalificacionCreditoService;
import com.saa.model.credito.TipoCalificacionCredito;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TipoCalificacionCreditoServiceImpl implements TipoCalificacionCreditoService {

    @EJB
    private TipoCalificacionCreditoDaoService tipoCalificacionCreditoDaoService;

    /**
     * Recupera un registro de TipoCalificacionCredito por su ID.
     */
    @Override
    public TipoCalificacionCredito selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById TipoCalificacionCredito con id: " + id);
        return tipoCalificacionCreditoDaoService.selectById(id, NombreEntidadesCredito.TIPO_CALIFICACION_CREDITO);
    }

    /**
     * Elimina uno o varios registros de TipoCalificacionCredito.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TipoCalificacionCreditoService ... depurado");
        TipoCalificacionCredito tipo = new TipoCalificacionCredito();
        for (Long registro : id) {
            tipoCalificacionCreditoDaoService.remove(tipo, registro);
        }
    }

    /**
     * Guarda una lista de registros de TipoCalificacionCredito.
     */
    @Override
    public void save(List<TipoCalificacionCredito> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TipoCalificacionCreditoService");
        for (TipoCalificacionCredito registro : lista) {
            tipoCalificacionCreditoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de TipoCalificacionCredito.
     */
    @Override
    public List<TipoCalificacionCredito> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TipoCalificacionCreditoService");
        List<TipoCalificacionCredito> result = tipoCalificacionCreditoDaoService.selectAll(NombreEntidadesCredito.TIPO_CALIFICACION_CREDITO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total TipoCalificacionCredito no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de TipoCalificacionCredito.
     */
    @Override
    public TipoCalificacionCredito saveSingle(TipoCalificacionCredito tipo) throws Throwable {
        System.out.println("saveSingle - TipoCalificacionCredito");
        tipo = tipoCalificacionCreditoDaoService.save(tipo, tipo.getCodigo());
        return tipo;
    }

    /**
     * Recupera registros según criterios.
     */
    @Override
    public List<TipoCalificacionCredito> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TipoCalificacionCreditoService");
        List<TipoCalificacionCredito> result = tipoCalificacionCreditoDaoService.selectByCriteria(datos, NombreEntidadesCredito.TIPO_CALIFICACION_CREDITO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio TipoCalificacionCredito no devolvio ningun registro");
        }
        return result;
    }
}
