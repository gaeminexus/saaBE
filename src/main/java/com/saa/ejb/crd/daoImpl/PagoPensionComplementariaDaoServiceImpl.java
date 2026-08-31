package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.PagoPensionComplementariaDaoService;
import com.saa.model.crd.PagoPensionComplementaria;
import com.saa.rubros.EstadoPagoPensionComplementaria;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class PagoPensionComplementariaDaoServiceImpl extends EntityDaoImpl<PagoPensionComplementaria>
        implements PagoPensionComplementariaDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo",
            "entidad",
            "filial",
            "anio",
            "mes",
            "valorPension",
            "valorSeguro",
            "valor",
            "fecha",
            "estado",
            "idPagoProgramado",
            "idAporte",
            "numeroAsiento",
            "usuarioRegistro",
            "fechaRegistro",
            "fechaPago",
            "usuarioAnulacion",
            "fechaAnulacion",
            "motivoAnulacion"
        };
    }

    @Override
    public PagoPensionComplementaria selectByEntidadYPeriodo(Long idEntidad, Long anio, Long mes) throws Throwable {
        System.out.println("Ingresa al metodo selectByEntidadYPeriodo de PagoPensionComplementaria"
                + " - entidad: " + idEntidad + " anio: " + anio + " mes: " + mes);
        Query query = em.createQuery(
                " select p from PagoPensionComplementaria p " +
                " where  p.entidad.codigo = :idEntidad " +
                " and    p.anio = :anio " +
                " and    p.mes  = :mes ");
        query.setParameter("idEntidad", idEntidad);
        query.setParameter("anio", anio);
        query.setParameter("mes", mes);
        List<PagoPensionComplementaria> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public List<PagoPensionComplementaria> selectByEntidad(Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectByEntidad de PagoPensionComplementaria - entidad: " + idEntidad);
        Query query = em.createQuery(
                " select p from PagoPensionComplementaria p " +
                " where  p.entidad.codigo = :idEntidad " +
                " order by p.anio desc, p.mes desc");
        query.setParameter("idEntidad", idEntidad);
        return query.getResultList();
    }

    @Override
    public List<PagoPensionComplementaria> selectPendientesConciliacion() throws Throwable {
        System.out.println("Ingresa al metodo selectPendientesConciliacion de PagoPensionComplementaria");
        Query query = em.createQuery(
                " select p from PagoPensionComplementaria p " +
                " where  p.estado in (:registrada, :enPago) " +
                " and    p.idPagoProgramado is not null " +
                " order by p.codigo");
        query.setParameter("registrada", Long.valueOf(EstadoPagoPensionComplementaria.REGISTRADA));
        query.setParameter("enPago",     Long.valueOf(EstadoPagoPensionComplementaria.EN_PAGO));
        return query.getResultList();
    }
}
