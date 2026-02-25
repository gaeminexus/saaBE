package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.BaseInicialParticipesDaoService;
import com.saa.ejb.crd.service.BaseInicialParticipesService;
import com.saa.model.crd.BaseInicialParticipes;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class BaseInicialParticipesServiceImpl implements BaseInicialParticipesService {

    @EJB
    private BaseInicialParticipesDaoService baseInicialParticipesDaoService;

    /**
     * Recupera un registro de BaseInicialParticipes por su ID.
     */
    @Override
    public BaseInicialParticipes selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return baseInicialParticipesDaoService.selectById(id, NombreEntidadesCredito.BASE_INICIAL_PARTICIPES);
    }

    /**
     * Elimina uno o varios registros de BaseInicialParticipes.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de BaseInicialParticipesService ... depurado");
        BaseInicialParticipes baseInicialParticipes = new BaseInicialParticipes();
        for (Long registro : id) {
            baseInicialParticipesDaoService.remove(baseInicialParticipes, registro);
        }
    }

    /**
     * Guarda una lista de registros de BaseInicialParticipes.
     */
    @Override
    public void save(List<BaseInicialParticipes> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de BaseInicialParticipesService");
        for (BaseInicialParticipes registro : lista) {
            baseInicialParticipesDaoService.save(registro, registro.getNumero());
        }
    }

    /**
     * Recupera todos los registros de BaseInicialParticipes.
     */
    @Override
    public List<BaseInicialParticipes> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll BaseInicialParticipesService");
        List<BaseInicialParticipes> result = baseInicialParticipesDaoService.selectAll(NombreEntidadesCredito.BASE_INICIAL_PARTICIPES);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total BaseInicialParticipes no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de BaseInicialParticipes.
     */
    @Override
    public BaseInicialParticipes saveSingle(BaseInicialParticipes baseInicialParticipes) throws Throwable {
        System.out.println("saveSingle - BaseInicialParticipes");
        // Inicializar valores por defecto si es nuevo registro
        if(baseInicialParticipes.getNumero() == null){
            if(baseInicialParticipes.getCesantiaPatronal() == null) {
                baseInicialParticipes.setCesantiaPatronal(0.0);
            }
            if(baseInicialParticipes.getCesantiaPersonal() == null) {
                baseInicialParticipes.setCesantiaPersonal(0.0);
            }
            if(baseInicialParticipes.getCesantiaRetiroVoluntario() == null) {
                baseInicialParticipes.setCesantiaRetiroVoluntario(0.0);
            }
            if(baseInicialParticipes.getJubilacionPatronal() == null) {
                baseInicialParticipes.setJubilacionPatronal(0.0);
            }
            if(baseInicialParticipes.getJubilacionPersonal() == null) {
                baseInicialParticipes.setJubilacionPersonal(0.0);
            }
            if(baseInicialParticipes.getJubilacionRetiroVoluntario() == null) {
                baseInicialParticipes.setJubilacionRetiroVoluntario(0.0);
            }
            if(baseInicialParticipes.getPensionComplementaria() == null) {
                baseInicialParticipes.setPensionComplementaria(0.0);
            }
            if(baseInicialParticipes.getRendimientoCesantiaPatronal() == null) {
                baseInicialParticipes.setRendimientoCesantiaPatronal(0.0);
            }
            if(baseInicialParticipes.getRendimientoCesantiaPersonal() == null) {
                baseInicialParticipes.setRendimientoCesantiaPersonal(0.0);
            }
            if(baseInicialParticipes.getRendimientoJubilacionPatronal() == null) {
                baseInicialParticipes.setRendimientoJubilacionPatronal(0.0);
            }
            if(baseInicialParticipes.getRendimientoJubilacionPersonal() == null) {
                baseInicialParticipes.setRendimientoJubilacionPersonal(0.0);
            }
            if(baseInicialParticipes.getTotalGeneral() == null) {
                baseInicialParticipes.setTotalGeneral(0.0);
            }
        }
        baseInicialParticipes = baseInicialParticipesDaoService.save(baseInicialParticipes, baseInicialParticipes.getNumero());
        return baseInicialParticipes;
    }

    /**
     * Recupera registros de BaseInicialParticipes segun criterios de búsqueda.
     */
    @Override
    public List<BaseInicialParticipes> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria BaseInicialParticipesService");
        List<BaseInicialParticipes> result = baseInicialParticipesDaoService.selectByCriteria(datos, NombreEntidadesCredito.BASE_INICIAL_PARTICIPES);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio BaseInicialParticipes no devolvio ningun registro");
        }
        return result;
    }
}
