package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TransaccionesDaoService;
import com.saa.ejb.credito.service.TransaccionesService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.Transacciones;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TransaccionesServiceImpl implements TransaccionesService {

    @EJB
    private TransaccionesDaoService transaccionesDaoService;

    /**
     * Recupera un registro de Transacciones por su ID.
     */
    @Override
    public Transacciones selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById Transacciones con id: " + id);
        return transaccionesDaoService.selectById(id, NombreEntidadesCredito.TRANSACCIONES);
    }

    /**
     * Elimina uno o varios registros de Transacciones.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TransaccionesService ...");
        Transacciones transacciones = new Transacciones();
        for (Long registro : id) {
            transaccionesDaoService.remove(transacciones, registro);
        }
    }

    /**
     * Guarda una lista de registros de Transacciones.
     */
    @Override
    public void save(List<Transacciones> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TransaccionesService");
        for (Transacciones registro : lista) {
            transaccionesDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Transacciones.
     */
    @Override
    public List<Transacciones> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TransaccionesService");
        List<Transacciones> result =
                transaccionesDaoService.selectAll(NombreEntidadesCredito.TRANSACCIONES);

        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Transacciones no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Transacciones.
     */
    @Override
    public Transacciones saveSingle(Transacciones transacciones) throws Throwable {
        System.out.println("saveSingle - Transacciones");
        if(transacciones.getCodigo() == null)	
        transacciones = transaccionesDaoService.save(transacciones, transacciones.getCodigo());  
        return transacciones;
    }
	
    /**
     * Recupera registros de Transacciones según criterios de búsqueda.
     */
    @Override
    public List<Transacciones> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TransaccionesService");

        List<Transacciones> result =
                transaccionesDaoService.selectByCriteria(datos, NombreEntidadesCredito.TRANSACCIONES);

        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Transacciones no devolvio ningun registro");
        }
        return result;
    }
}
