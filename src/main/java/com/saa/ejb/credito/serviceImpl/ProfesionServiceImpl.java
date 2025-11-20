package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.ProfesionDaoService;
import com.saa.ejb.credito.service.ProfesionService;
import com.saa.model.credito.Profesion;
import com.saa.rubros.Estado;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ProfesionServiceImpl implements ProfesionService {

    @EJB
    private ProfesionDaoService profesionDaoService;

    /**
     * Recupera un registro de Profesion por su ID.
     */
    @Override
    public Profesion selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById Profesion con id: " + id);
        return profesionDaoService.selectById(id, NombreEntidadesCredito.PROFESION);
    }

    /**
     * Elimina uno o varios registros de Profesion.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ProfesionService ... depurado");
        Profesion profesion = new Profesion();
        for (Long registro : id) {
            profesionDaoService.remove(profesion, registro);
        }
    }

    /**
     * Guarda una lista de registros de Profesion.
     */
    @Override
    public void save(List<Profesion> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ProfesionService");
        for (Profesion registro : lista) {
            profesionDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Profesion.
     */
    @Override
    public List<Profesion> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ProfesionService");
        List<Profesion> result = profesionDaoService.selectAll(NombreEntidadesCredito.PROFESION);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Profesion no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Profesion.
     */
    @Override
    public Profesion saveSingle(Profesion profesion) throws Throwable {
        System.out.println("saveSingle - Profesion");
        if(profesion.getCodigo() == null){
        	profesion.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        profesion = profesionDaoService.save(profesion, profesion.getCodigo());
        return profesion;
    }

    /**
     * Recupera registros de Profesion segun criterios de búsqueda.
     */
    @Override
    public List<Profesion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria ProfesionService");
        List<Profesion> result = profesionDaoService.selectByCriteria(datos, NombreEntidadesCredito.PROFESION);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Profesion no devolvio ningun registro");
        }
        return result;
    }
}
