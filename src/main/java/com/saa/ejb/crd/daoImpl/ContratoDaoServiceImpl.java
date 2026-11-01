package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ContratoDaoService;
import com.saa.model.crd.Contrato;
import com.saa.rubros.EstadoContrato;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class ContratoDaoServiceImpl extends EntityDaoImpl<Contrato> implements ContratoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public Long selectTipoContratoMasFrecuenteAportantesCarga(Long idCarga) throws Throwable {
        System.out.println("ContratoDaoService.selectTipoContratoMasFrecuenteAportantesCarga carga " + idCarga);
        Query query = em.createNativeQuery(
            " select c.TPCNCDGO " +
            " from   CRD.CNTR c " +
            " join   CRD.ENTD e on e.ENTDCDGO = c.ENTDCDGO " +
            " join   CRD.PXCA x on TRIM(x.PXCACDPT) = TO_CHAR(e.ENTDRLPC) " +
            " join   CRD.DTCA t on t.DTCACDGO = x.DTCACDGO " +
            " where  t.CRARCDGO = :idCarga " +
            "   and  TRIM(t.DTCACDPP) = 'AH' " +
            "   and  c.CNTRESTD = " + EstadoContrato.ACTIVO +
            "   and  c.TPCNCDGO is not null " +
            " group by c.TPCNCDGO " +
            " order by count(*) desc, c.TPCNCDGO " +
            " fetch first 1 rows only ");
        query.setParameter("idCarga", idCarga);
        List<?> resultado = query.getResultList();
        return resultado.isEmpty() || resultado.get(0) == null ? null : ((Number) resultado.get(0)).longValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Contrato selectVigenteAlCortePorEntidad(Long idEntidad, java.time.LocalDateTime corte) throws Throwable {
        System.out.println("ContratoDaoService.selectVigenteAlCortePorEntidad entidad " + idEntidad + " corte " + corte);
        Query query = em.createQuery(
            " select c from Contrato c " +
            " where  c.entidad.codigo = :idEntidad " +
            "   and  c.fechaInicio <= :corte " +
            "   and  (c.fechaTerminacion is null or c.fechaTerminacion > :corte) " +
            " order by c.fechaInicio desc, c.codigo desc");
        query.setParameter("idEntidad", idEntidad);
        query.setParameter("corte", corte);
        query.setMaxResults(1);
        List<Contrato> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @SuppressWarnings("unchecked")
	@Override
    public List<Contrato> selectByEntidad(Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectByEntidad de Contrato con idEntidad: " + idEntidad);
        Query query = em.createQuery(
            " select c from Contrato c " +
            " where  c.entidad.codigo = :idEntidad " +
            " order by c.codigo desc");
        query.setParameter("idEntidad", idEntidad);
        return query.getResultList();
    }

    @Override
    public Contrato selectActivoPorEntidad(Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectActivoPorEntidad de Contrato con idEntidad: " + idEntidad);
        try {
            Query query = em.createQuery(
                " select c from Contrato c " +
                " where  c.entidad.codigo = :idEntidad " +
                "   and  c.estado = :activo " +
                " order by c.codigo desc");
            query.setParameter("idEntidad", idEntidad);
            query.setParameter("activo", Long.valueOf(EstadoContrato.ACTIVO));
            query.setMaxResults(1);
            @SuppressWarnings("unchecked")
			List<Contrato> resultado = query.getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } catch (NoResultException e) {
            return null;
        }
    }
}
