package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.CorridaCierreCarteraDaoService;
import com.saa.model.crd.CorridaCierreCartera;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCorridaCierreCartera;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class CorridaCierreCarteraDaoServiceImpl extends EntityDaoImpl<CorridaCierreCartera>
        implements CorridaCierreCarteraDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) CorridaCierreCartera");
        return new String[]{
            "codigo",
            "empresa",
            "anio",
            "mes",
            "fechaCorte",
            "fechaProceso",
            "idEstado",
            "observacion",
            "fechaRegistro",
            "usuarioRegistro",
            "ipRegistro",
            "fechaModificacion",
            "usuarioModificacion",
            "ipModificacion",
            "estado"
        };
    }

    @Override
    public CorridaCierreCartera selectVivaByPeriodo(Long idEmpresa, Long anio, Long mes)
            throws Throwable {
        System.out.println("Ingresa al metodo selectVivaByPeriodo de CorridaCierreCartera"
                + " - empresa: " + idEmpresa + " periodo: " + anio + "-" + mes);
        Query query = em.createQuery(
                " select c from CorridaCierreCartera c " +
                " where  c.empresa.codigo = :idEmpresa " +
                " and    c.anio           = :anio " +
                " and    c.mes            = :mes " +
                " and    c.estado         = :activo " +
                " and    c.idEstado in (:preparada, :ejecutada) " +
                " order by c.codigo desc");
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("anio",      anio);
        query.setParameter("mes",       mes);
        query.setParameter("activo",    Long.valueOf(Estado.ACTIVO));
        query.setParameter("preparada", Long.valueOf(EstadoCorridaCierreCartera.PREPARADA));
        query.setParameter("ejecutada", Long.valueOf(EstadoCorridaCierreCartera.EJECUTADA));
        List<CorridaCierreCartera> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public CorridaCierreCartera selectUltimaEjecutadaAntesDe(Long idEmpresa, Long anio, Long mes)
            throws Throwable {
        System.out.println("Ingresa al metodo selectUltimaEjecutadaAntesDe de CorridaCierreCartera"
                + " - empresa: " + idEmpresa + " periodo: " + anio + "-" + mes);
        // El periodo se compara como un entero anio*100+mes para no arrastrar un OR anidado
        // que Hibernate resuelve mal cuando el mes cruza de anio.
        Long periodo = Long.valueOf(anio.longValue() * 100L + mes.longValue());
        Query query = em.createQuery(
                " select c from CorridaCierreCartera c " +
                " where  c.empresa.codigo = :idEmpresa " +
                " and    c.estado         = :activo " +
                " and    c.idEstado       = :ejecutada " +
                " and   (c.anio * 100 + c.mes) < :periodo " +
                " order by c.anio desc, c.mes desc, c.codigo desc");
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("activo",    Long.valueOf(Estado.ACTIVO));
        query.setParameter("ejecutada", Long.valueOf(EstadoCorridaCierreCartera.EJECUTADA));
        query.setParameter("periodo",   periodo);
        query.setMaxResults(1);
        List<CorridaCierreCartera> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public List<CorridaCierreCartera> selectByEmpresa(Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo selectByEmpresa de CorridaCierreCartera"
                + " - empresa: " + idEmpresa);
        Query query = em.createQuery(
                " select c from CorridaCierreCartera c " +
                " where  c.empresa.codigo = :idEmpresa " +
                " order by c.anio desc, c.mes desc, c.codigo desc");
        query.setParameter("idEmpresa", idEmpresa);
        return query.getResultList();
    }
}
