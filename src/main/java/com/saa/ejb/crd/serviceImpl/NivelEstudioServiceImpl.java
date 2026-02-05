package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.NivelEstudioDaoService;
import com.saa.ejb.crd.service.NivelEstudioService;
import com.saa.model.crd.NivelEstudio;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class NivelEstudioServiceImpl implements NivelEstudioService {

    @EJB
    private NivelEstudioDaoService nivelEstudioDaoService;

    /**
     * Recupera un registro de NivelEstudio por su ID.
     */
    @Override
    public NivelEstudio selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById NivelEstudio con id: " + id);
        return nivelEstudioDaoService.selectById(id, NombreEntidadesCredito.NIVEL_ESTUDIO);
    }

    /**
     * Elimina uno o varios registros de NivelEstudio.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de NivelEstudioService ... depurado");
        NivelEstudio nivelEstudio = new NivelEstudio();
        for (Long registro : id) {
            nivelEstudioDaoService.remove(nivelEstudio, registro);
        }
    }

    /**
     * Guarda una lista de registros de NivelEstudio.
     */
    @Override
    public void save(List<NivelEstudio> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de NivelEstudioService");
        for (NivelEstudio registro : lista) {
            nivelEstudioDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de NivelEstudio.
     */
    @Override
    public List<NivelEstudio> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll NivelEstudioService");
        List<NivelEstudio> result = nivelEstudioDaoService.selectAll(NombreEntidadesCredito.NIVEL_ESTUDIO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total NivelEstudio no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de NivelEstudio.
     */
    @Override
    public NivelEstudio saveSingle(NivelEstudio nivelEstudio) throws Throwable {
        System.out.println("saveSingle - NivelEstudio");
        if(nivelEstudio.getCodigo() == null){
        	nivelEstudio.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        nivelEstudio = nivelEstudioDaoService.save(nivelEstudio, nivelEstudio.getCodigo());
        return nivelEstudio;
    }

    /**
     * Recupera registros de NivelEstudio segun criterios de búsqueda.
     */
    @Override
    public List<NivelEstudio> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria NivelEstudioService");
        List<NivelEstudio> result = nivelEstudioDaoService.selectByCriteria(datos, NombreEntidadesCredito.NIVEL_ESTUDIO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio NivelEstudio no devolvio ningun registro");
        }
        return result;
    }
}
