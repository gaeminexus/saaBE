package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.CuentaTipoAporteDaoService;
import com.saa.model.crd.CuentaTipoAporte;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class CuentaTipoAporteDaoServiceImpl extends EntityDaoImpl<CuentaTipoAporte>
        implements CuentaTipoAporteDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) CuentaTipoAporte");
        return new String[]{
            "codigo",
            "tipoAporte",
            "empresa",
            "cuentaPasivo",
            "cuentaLiquidacion",
            "estado"
        };
    }

    @Override
    public CuentaTipoAporte selectByTipoAporteYEmpresa(Long idTipoAporte, Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo selectByTipoAporteYEmpresa de CuentaTipoAporte"
                + " - tipoAporte: " + idTipoAporte + " empresa: " + idEmpresa);
        Query query = em.createQuery(
                " select c from CuentaTipoAporte c " +
                " where  c.tipoAporte.codigo = :idTipoAporte " +
                " and    c.empresa.codigo    = :idEmpresa " +
                " and    c.estado            = :activo ");
        query.setParameter("idTipoAporte", idTipoAporte);
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        List<CuentaTipoAporte> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public List<CuentaTipoAporte> selectByEmpresa(Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo selectByEmpresa de CuentaTipoAporte - empresa: " + idEmpresa);
        Query query = em.createQuery(
                " select c from CuentaTipoAporte c " +
                " where  c.empresa.codigo = :idEmpresa " +
                " and    c.estado         = :activo " +
                " order by c.tipoAporte.codigo ");
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        return query.getResultList();
    }
}
