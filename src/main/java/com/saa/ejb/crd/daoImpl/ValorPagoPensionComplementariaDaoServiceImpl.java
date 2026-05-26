package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ValorPagoPensionComplementariaDaoService;
import com.saa.model.crd.ValorPagoPensionComplementaria;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class ValorPagoPensionComplementariaDaoServiceImpl extends EntityDaoImpl<ValorPagoPensionComplementaria>
        implements ValorPagoPensionComplementariaDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "entidad", "valorPagar", "numeroCuotas", "tienePrestamo",
            "valorSeguro", "estado", "usuarioIngreso", "fechaIngreso",
            "usuarioModificacion", "fechaModificacion"
        };
    }

    /**
     * Retorna todos los registros VPPC asociados a una entidad.
     * Usado en G44 para obtener valorPagar → valorPension y valorNetoRecibir.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<ValorPagoPensionComplementaria> selectByEntidad(Long codigoEntidad) throws Throwable {
        System.out.println("ValorPagoPensionComplementariaDaoServiceImpl.selectByEntidad: " + codigoEntidad);
        Query query = em.createQuery(
            " select v from ValorPagoPensionComplementaria v " +
            " where  v.entidad.codigo = :codigoEntidad "
        );
        query.setParameter("codigoEntidad", codigoEntidad);
        return query.getResultList();
    }
}