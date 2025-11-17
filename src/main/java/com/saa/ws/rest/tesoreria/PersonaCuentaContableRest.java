package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.PersonaCuentaContableDaoService;
import com.saa.ejb.tesoreria.service.PersonaCuentaContableService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.PersonaCuentaContable;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

@Path("prcc")
public class PersonaCuentaContableRest {

    @EJB
    private PersonaCuentaContableDaoService personaCuentaContableDaoService;

    @EJB
    private PersonaCuentaContableService personaCuentaContableService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public PersonaCuentaContableRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de PersonaCuentaContable.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<PersonaCuentaContable> getAll() throws Throwable {
        return personaCuentaContableDaoService.selectAll(NombreEntidadesTesoreria.PERSONA_CUENTA_CONTABLE);
    }

    /**
     * Recupera un registro de PersonaCuentaContable por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public PersonaCuentaContable getId(@PathParam("id") Long id) throws Throwable {
        return personaCuentaContableDaoService.selectById(id, NombreEntidadesTesoreria.PERSONA_CUENTA_CONTABLE);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public PersonaCuentaContable put(PersonaCuentaContable registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT PERSONA CUENTA CONTABLE");
        return personaCuentaContableService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public PersonaCuentaContable post(PersonaCuentaContable registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST PERSONA CUENTA CONTABLE");
        return personaCuentaContableService.saveSingle(registro);
    }

    /**
     * Selecciona registros de PersonaCuentaContable por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<PersonaCuentaContable> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA PERSONA CUENTA CONTABLE: " + test);
        return personaCuentaContableDaoService.selectAll(NombreEntidadesTesoreria.PERSONA_CUENTA_CONTABLE);
    }

    /**
     * Elimina un registro de PersonaCuentaContable por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE PERSONA CUENTA CONTABLE");
        PersonaCuentaContable elimina = new PersonaCuentaContable();
        personaCuentaContableDaoService.remove(elimina, id);
    }
}
