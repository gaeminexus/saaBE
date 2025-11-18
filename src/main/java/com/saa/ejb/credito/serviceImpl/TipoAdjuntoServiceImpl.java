package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TipoAdjuntoDaoService;
import com.saa.ejb.credito.service.TipoAdjuntoService;
import com.saa.model.credito.TipoAdjunto;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TipoAdjuntoServiceImpl implements TipoAdjuntoService {

    @EJB
    private TipoAdjuntoDaoService tipoAdjuntoDaoService;

    /**
     * Recupera un registro de TipoAdjunto por su ID.
     */
    @Override
    public TipoAdjunto selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById TipoAdjunto con id: " + id);
        return tipoAdjuntoDaoService.selectById(id, NombreEntidadesCredito.TIPO_ADJUNTO);
    }

    /**
     * Elimina uno o varios registros de TipoAdjunto.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TipoAdjuntoService ... depurado");
        TipoAdjunto tipoAdjunto = new TipoAdjunto();
        for (Long registro : id) {
            tipoAdjuntoDaoService.remove(tipoAdjunto, registro);
        }
    }

    /**
     * Guarda una lista de registros de TipoAdjunto.
     */
    @Override
    public void save(List<TipoAdjunto> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TipoAdjuntoService");
        for (TipoAdjunto registro : lista) {
            tipoAdjuntoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de TipoAdjunto.
     */
    @Override
    public List<TipoAdjunto> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TipoAdjuntoService");
        List<TipoAdjunto> result = tipoAdjuntoDaoService.selectAll(NombreEntidadesCredito.TIPO_ADJUNTO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total TipoAdjunto no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de TipoAdjunto.
     */
    @Override
    public TipoAdjunto saveSingle(TipoAdjunto tipoAdjunto) throws Throwable {
        System.out.println("saveSingle - TipoAdjunto");
        tipoAdjunto = tipoAdjuntoDaoService.save(tipoAdjunto, tipoAdjunto.getCodigo());
        return tipoAdjunto;
    }

    /**
     * Recupera registros de TipoAdjunto según criterios de búsqueda.
     */
    @Override
    public List<TipoAdjunto> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TipoAdjuntoService");
        List<TipoAdjunto> result = tipoAdjuntoDaoService.selectByCriteria(datos, NombreEntidadesCredito.TIPO_ADJUNTO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio TipoAdjunto no devolvio ningun registro");
        }
        return result;
    }
}
