package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.PersonaNaturalDaoService;
import com.saa.ejb.credito.service.PersonaNaturalService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PersonaNatural;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class PersonaNaturalServiceImpl implements PersonaNaturalService {

    @EJB
    private PersonaNaturalDaoService personaNaturalDaoService;

    /**
     * Recupera un registro de PersonaNatural por su ID.
     */
    @Override
    public PersonaNatural selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById PersonaNatural con id: " + id);
        return personaNaturalDaoService.selectById(id, NombreEntidadesCredito.PERSONA_NATURAL);
    }

    /**
     * Elimina uno o varios registros de PersonaNatural.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de PersonaNaturalService ... depurado");
        PersonaNatural persona = new PersonaNatural();
        for (Long registro : id) {
            personaNaturalDaoService.remove(persona, registro);
        }
    }

    /**
     * Guarda una lista de registros de PersonaNatural.
     */
    @Override
    public void save(List<PersonaNatural> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de PersonaNaturalService");
        for (PersonaNatural registro : lista) {
            personaNaturalDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de PersonaNatural.
     */
    @Override
    public List<PersonaNatural> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll PersonaNaturalService");
        List<PersonaNatural> result = personaNaturalDaoService.selectAll(NombreEntidadesCredito.PERSONA_NATURAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total PersonaNatural no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de PersonaNatural.
     */
    @Override
    public PersonaNatural saveSingle(PersonaNatural persona) throws Throwable {
        System.out.println("saveSingle - PersonaNatural");
        if (persona.getCodigo() == null) {
            persona.setEstado(Long.valueOf(Estado.ACTIVO)); // Estado activo
        }
        persona = personaNaturalDaoService.save(persona, persona.getCodigo());
        return persona;
    }

    /**
     * Recupera registros de PersonaNatural segun criterios de búsqueda.
     */
    @Override
    public List<PersonaNatural> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria PersonaNaturalService");
        List<PersonaNatural> result = personaNaturalDaoService.selectByCriteria(datos, NombreEntidadesCredito.PERSONA_NATURAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio PersonaNatural no devolvio ningun registro");
        }
        return result;
    }
}
