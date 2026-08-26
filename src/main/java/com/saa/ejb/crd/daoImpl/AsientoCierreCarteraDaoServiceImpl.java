package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.AsientoCierreCarteraDaoService;
import com.saa.model.crd.AsientoCierreCartera;
import com.saa.rubros.EstadoAsiento;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class AsientoCierreCarteraDaoServiceImpl extends EntityDaoImpl<AsientoCierreCartera>
        implements AsientoCierreCarteraDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) AsientoCierreCartera");
        return new String[]{
            "codigo",
            "corrida",
            "subProceso",
            "asiento",
            "numeroAsiento",
            "fecha",
            "valor",
            "cantidad",
            "idEstado",
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
    public List<AsientoCierreCartera> selectByCorrida(Long idCorrida) throws Throwable {
        System.out.println("Ingresa al metodo selectByCorrida de AsientoCierreCartera"
                + " - corrida: " + idCorrida);
        Query query = em.createQuery(
                " select a from AsientoCierreCartera a " +
                " where  a.corrida.codigo = :idCorrida " +
                " order by a.subProceso");
        query.setParameter("idCorrida", idCorrida);
        return query.getResultList();
    }

    @Override
    public List<AsientoCierreCartera> selectGeneradosByCorrida(Long idCorrida) throws Throwable {
        System.out.println("Ingresa al metodo selectGeneradosByCorrida de AsientoCierreCartera"
                + " - corrida: " + idCorrida);
        Query query = em.createQuery(
                " select a from AsientoCierreCartera a " +
                " where  a.corrida.codigo = :idCorrida " +
                " and    a.idEstado       = :generado " +
                " order by a.subProceso");
        query.setParameter("idCorrida", idCorrida);
        query.setParameter("generado", Long.valueOf(EstadoAsiento.ACTIVO));
        return query.getResultList();
    }
}
