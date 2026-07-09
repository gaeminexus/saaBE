package com.saa.ejb.rpr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.NuevoParticipeG45DaoService;
import com.saa.model.rpr.NuevoParticipeG45;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class NuevoParticipeG45DaoServiceImpl extends EntityDaoImpl<NuevoParticipeG45> implements NuevoParticipeG45DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion", "tipoParticipe",
            "actividadEconomica", "patrimonio", "provincia", "canton", "parroquia",
            "genero", "estadoCivil", "fechaNacimiento", "profesion",
            "cargasFamiliares", "origenIngresos"
        };
    }
}
