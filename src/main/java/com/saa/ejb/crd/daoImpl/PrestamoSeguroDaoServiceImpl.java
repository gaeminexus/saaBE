package com.saa.ejb.crd.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.PrestamoSeguroDaoService;
import com.saa.model.crd.PrestamoSeguro;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class PrestamoSeguroDaoServiceImpl extends EntityDaoImpl<PrestamoSeguro>
        implements PrestamoSeguroDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) PrestamoSeguro");
        return new String[]{
            "codigo", "documento", "prestamo", "novedad", "base", "mesesCubiertos",
            "peso", "valorAsignado", "cuotasRepartidas"
        };
    }

    @Override
    public List<PrestamoSeguro> selectByDocumento(Long idDocumento) throws Throwable {
        System.out.println("PrestamoSeguroDaoServiceImpl.selectByDocumento - idDocumento: " + idDocumento);
        if (idDocumento == null) {
            return new ArrayList<>();
        }
        Query query = em.createQuery(
            "select p from PrestamoSeguro p where p.documento.codigo = :idDocumento order by p.codigo asc");
        query.setParameter("idDocumento", idDocumento);
        return query.getResultList();
    }

    @Override
    public List<PrestamoSeguro> selectByDocumentos(List<Long> idsDocumento) throws Throwable {
        System.out.println("PrestamoSeguroDaoServiceImpl.selectByDocumentos - cantidad: "
            + (idsDocumento != null ? idsDocumento.size() : 0));
        if (idsDocumento == null || idsDocumento.isEmpty()) {
            return new ArrayList<>();
        }
        Query query = em.createQuery(
            "select p from PrestamoSeguro p where p.documento.codigo in :ids order by p.codigo asc");
        query.setParameter("ids", idsDocumento);
        return query.getResultList();
    }
}
