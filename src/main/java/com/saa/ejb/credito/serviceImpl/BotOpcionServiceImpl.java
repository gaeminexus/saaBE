package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.BotOpcionDaoService;
import com.saa.ejb.credito.service.BotOpcionService;
import com.saa.model.credito.BotOpcion;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class BotOpcionServiceImpl implements BotOpcionService {

    @EJB
    private BotOpcionDaoService BotOpcionDaoService;

    /**
     * Recupera un registro de BotOpcion por su ID.
     */
    @Override
    public BotOpcion selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return BotOpcionDaoService.selectById(id, NombreEntidadesCredito.BOT_OPCION);
    }

    /**
     * Elimina uno o varios registros de BotOpcion.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de BotOpcionService ... depurado");
        BotOpcion botOpcion = new BotOpcion();
        for (Long registro : id) {
            BotOpcionDaoService.remove(botOpcion, registro);
        }
    }

    /**
     * Guarda una lista de registros de BotOpcion.
     */
    @Override
    public void save(List<BotOpcion> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de BotOpcionService");
        for (BotOpcion registro : lista) {
            BotOpcionDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de BotOpcion.
     */
    @Override
    public List<BotOpcion> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll BotOpcionService");
        List<BotOpcion> result = BotOpcionDaoService.selectAll(NombreEntidadesCredito.BOT_OPCION);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total BotOpcion no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de BotOpcion.
     */
    @Override
    public BotOpcion saveSingle(BotOpcion botOpcion) throws Throwable {
    	System.out.println("saveSingle - BotOpcion");
    	botOpcion = BotOpcionDaoService.save(botOpcion, botOpcion.getCodigo());
    	return botOpcion;
    }


    /**
     * Recupera registros de BotOpcion segun criterios de búsqueda.
     */
    @Override
    public List<BotOpcion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria BotOpcionService");
        List<BotOpcion> result = BotOpcionDaoService.selectByCriteria(datos, NombreEntidadesCredito.BOT_OPCION);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio BotOpcion no devolvio ningun registro");
        }
        return result;
    }
}
