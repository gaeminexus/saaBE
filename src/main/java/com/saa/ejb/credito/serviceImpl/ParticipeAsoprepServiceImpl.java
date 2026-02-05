package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.ParticipeAsoprepDaoService;
import com.saa.ejb.credito.service.ParticipeAsoprepService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.ParticipeAsoprep;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ParticipeAsoprepServiceImpl implements ParticipeAsoprepService {

    @EJB
    private ParticipeAsoprepDaoService participeAsoprepDaoService;

    /**
     * Recupera un registro de ParticipeAsoprep por su ID.
     */
    @Override
    public ParticipeAsoprep selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return participeAsoprepDaoService.selectById(id, NombreEntidadesCredito.PARTICIPE_ASOPREP);
    }

    /**
     * Elimina uno o varios registros de ParticipeAsoprep.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ParticipeAsoprepService ... depurado");
        ParticipeAsoprep pago = new ParticipeAsoprep();
        for (Long registro : id) {
            participeAsoprepDaoService.remove(pago, registro);
        }
    }

    /**
     * Guarda una lista de registros de ParticipeAsoprep.
     */
    @Override
    public void save(List<ParticipeAsoprep> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ParticipeAsoprepService");
        for (ParticipeAsoprep registro : lista) {
            participeAsoprepDaoService.save(registro, registro.getId());
        }
    }

    /**
     * Recupera todos los registros de ParticipeAsoprep.
     */
    @Override
    public List<ParticipeAsoprep> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ParticipeAsoprepService");
        List<ParticipeAsoprep> result = participeAsoprepDaoService.selectAll(NombreEntidadesCredito.PARTICIPE_ASOPREP);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total ParticipeAsoprep no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de ParticipeAsoprep.
     */
    @Override
    public ParticipeAsoprep saveSingle(ParticipeAsoprep pago) throws Throwable {
        System.out.println("saveSingle - ParticipeAsoprep");
        if (pago.getId() == null) {
            pago.setEstadoParticipante("ACTIVO");   // o "ACTIVO" 
        }

        pago = participeAsoprepDaoService.save(pago, pago.getId());  
        return pago;
    }

    /**
     * Recupera registros de ParticipeAsoprep segun criterios de búsqueda.
     */
    @Override
    public List<ParticipeAsoprep> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria ParticipeAsoprepService");
        List<ParticipeAsoprep> result = participeAsoprepDaoService.selectByCriteria(datos, NombreEntidadesCredito.PARTICIPE_ASOPREP);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio ParticipeAsoprep no devolvio ningun registro");
        }
        return result;
    }
}
