package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.BandaCierreCarteraDaoService;
import com.saa.model.crd.BandaCierreCartera;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class BandaCierreCarteraDaoServiceImpl extends EntityDaoImpl<BandaCierreCartera>
        implements BandaCierreCarteraDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) BandaCierreCartera");
        return new String[]{
            "codigo",
            "corrida",
            "producto",
            "tipoCartera",
            "banda",
            "numero",
            "planCuenta",
            "capital",
            "cantidad",
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
    public List<BandaCierreCartera> selectByCorrida(Long idCorrida) throws Throwable {
        System.out.println("Ingresa al metodo selectByCorrida de BandaCierreCartera"
                + " - corrida: " + idCorrida);
        Query query = em.createQuery(
                " select b from BandaCierreCartera b " +
                " where  b.corrida.codigo = :idCorrida " +
                " order by b.producto.codigo, b.tipoCartera, b.numero");
        query.setParameter("idCorrida", idCorrida);
        return query.getResultList();
    }

    @Override
    public int deleteByCorrida(Long idCorrida) throws Throwable {
        System.out.println("Ingresa al metodo deleteByCorrida de BandaCierreCartera"
                + " - corrida: " + idCorrida);
        Query query = em.createQuery(
                " delete from BandaCierreCartera b where b.corrida.codigo = :idCorrida");
        query.setParameter("idCorrida", idCorrida);
        int eliminadas = query.executeUpdate();
        // El DELETE masivo va a la base de inmediato, antes de los INSERT del snapshot
        // nuevo: por eso el recalculo no choca con UK_BDCC_CORRIDA. No se hace em.clear():
        // desprenderia la corrida que el servicio sigue usando.
        em.flush();
        return eliminadas;
    }
}
