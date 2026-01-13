package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TransaccionesAsoprepDaoService;
import com.saa.ejb.credito.service.TransaccionesAsoprepService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.TransaccionesAsoprep;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TransaccionesAsoprepServiceImpl implements TransaccionesAsoprepService {

    @EJB
    private TransaccionesAsoprepDaoService transaccionesAsoprepDaoService;

    /**
     * Recupera un registro de Transacciones por su ID.
     */
    @Override
    public TransaccionesAsoprep selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById Transacciones con id: " + id);
        return transaccionesAsoprepDaoService.selectById(id, NombreEntidadesCredito.TRANSACCIONES_ASOPREP);
    }

    /**
     * Elimina uno o varios registros de Transacciones.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TransaccionesService ...");
        TransaccionesAsoprep transaccionesAsoprep = new TransaccionesAsoprep();
        for (Long registro : id) {
            transaccionesAsoprepDaoService.remove(transaccionesAsoprep, registro);
        }
    }

    /**
     * Guarda una lista de registros de Transacciones.
     */
    @Override
    public void save(List<TransaccionesAsoprep> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TransaccionesService");
        for (TransaccionesAsoprep registro : lista) {
            transaccionesAsoprepDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Transacciones.
     */
    @Override
    public List<TransaccionesAsoprep> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TransaccionesService");
        List<TransaccionesAsoprep> result =
                transaccionesAsoprepDaoService.selectAll(NombreEntidadesCredito.TRANSACCIONES_ASOPREP);

        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Transacciones no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Transacciones.
     */
    @Override
    public TransaccionesAsoprep saveSingle(TransaccionesAsoprep transaccionesAsoprep) throws Throwable {
        System.out.println("saveSingle - Transacciones");
        if(transaccionesAsoprep.getCodigo() == null)	
        transaccionesAsoprep = transaccionesAsoprepDaoService.save(transaccionesAsoprep, transaccionesAsoprep.getCodigo());  
        return transaccionesAsoprep;
    }
	
    /**
     * Recupera registros de Transacciones según criterios de búsqueda.
     */
    @Override
    public List<TransaccionesAsoprep> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TransaccionesService");

        List<TransaccionesAsoprep> result =
                transaccionesAsoprepDaoService.selectByCriteria(datos, NombreEntidadesCredito.TRANSACCIONES_ASOPREP);

        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Transacciones no devolvio ningun registro");
        }
        return result;
    }
}
