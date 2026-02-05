package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.ParticipeDaoService;
import com.saa.ejb.credito.service.ParticipeService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Participe;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ParticipeServiceImpl implements ParticipeService {

    @EJB
    private ParticipeDaoService participeDaoService;

    /**
     * Recupera un registro de Participe por su ID.
     */
    @Override
    public Participe selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById Participe con id: " + id);
        return participeDaoService.selectById(id, NombreEntidadesCredito.PARTICIPE);
    }

    /**
     * Elimina uno o varios registros de Participe.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ParticipeService ... depurado");
        Participe participe = new Participe();
        for (Long registro : id) {
            participeDaoService.remove(participe, registro);
        }
    }

    /**
     * Guarda una lista de registros de Participe.
     */
    @Override
    public void save(List<Participe> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ParticipeService");
        for (Participe registro : lista) {
            participeDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Participe.
     */
    @Override
    public List<Participe> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ParticipeService");
        List<Participe> result = participeDaoService.selectAll(NombreEntidadesCredito.PARTICIPE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Participe no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Participe.
     */
    @Override
    public Participe saveSingle(Participe participe) throws Throwable {
        System.out.println("saveSingle - Participe");
        if(participe.getCodigo() == null){
        	participe.setIdEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        participe = participeDaoService.save(participe, participe.getCodigo());
        return participe;
    }

    /**
     * Recupera registros de Participe segun criterios de búsqueda.
     */
    @Override
    public List<Participe> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria ParticipeService");
        List<Participe> result = participeDaoService.selectByCriteria(datos, NombreEntidadesCredito.PARTICIPE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Participe no devolvio ningun registro");
        }
        return result;
    }
}
