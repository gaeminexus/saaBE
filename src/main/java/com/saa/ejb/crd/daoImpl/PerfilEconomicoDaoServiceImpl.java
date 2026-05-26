package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.PerfilEconomicoDaoService;
import com.saa.model.crd.PerfilEconomico;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class PerfilEconomicoDaoServiceImpl extends EntityDaoImpl<PerfilEconomico> implements PerfilEconomicoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "entidad", "salarioFijo", "salarioVariable", "origenOtrosIngresos",
            "otrosIngresos", "totalIngresos", "gastosMensuales", "totalBienes",
            "totalVehiculos", "totalOtrosActivos", "totalActivos", "totalDeudas",
            "patrimonioNeto", "fechaActualizacion", "fechaRegistro", "usuarioRegistro", "estado"
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PerfilEconomico> selectByEntidad(Long codigoEntidad) throws Throwable {
        System.out.println("PerfilEconomicoDaoServiceImpl.selectByEntidad: " + codigoEntidad);
        Query query = em.createQuery(
            " select p from PerfilEconomico p " +
            " where  p.entidad.codigo = :codigoEntidad "
        );
        query.setParameter("codigoEntidad", codigoEntidad);
        return query.getResultList();
    }
}