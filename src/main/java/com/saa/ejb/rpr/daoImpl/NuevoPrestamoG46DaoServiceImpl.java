package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.NuevoPrestamoG46DaoService;
import com.saa.model.rpr.NuevoPrestamoG46;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class NuevoPrestamoG46DaoServiceImpl extends EntityDaoImpl<NuevoPrestamoG46> implements NuevoPrestamoG46DaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "tipoIdentificacion", "identificacion", "numeroOperacion",
            "tipoCredito", "estadoOperacion", "situacionOperacion", "destinoProvincia",
            "destinoCanton", "destinoParroquia", "fechaConcesion", "fechaVencimiento",
            "valorOperacion", "tasaInteresNominal", "periodicidadPago",
            "frecuenciaRevision", "garantias"
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<NuevoPrestamoG46> selectByDetalle(Long codigoDetalle) throws Throwable {
        System.out.println("NuevoPrestamoG46DaoServiceImpl.selectByDetalle codigoDetalle: " + codigoDetalle);
        Query query = em.createQuery(
            " select g from NuevoPrestamoG46 g " +
            " where g.detalleEjecucion.codigo = :codigoDetalle "
        );
        query.setParameter("codigoDetalle", codigoDetalle);
        return query.getResultList();
    }
}
