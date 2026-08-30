package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.AsientoCargaPetroDaoService;
import com.saa.model.crd.AsientoCargaPetro;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class AsientoCargaPetroDaoServiceImpl extends EntityDaoImpl<AsientoCargaPetro>
        implements AsientoCargaPetroDaoService {

    /** 1 = vigente, 0 = reversado (ANCPIDST). */
    private static final long VIGENTE = 1L;

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) AsientoCargaPetro");
        return new String[]{
            "codigo",
            "cargaArchivo",
            "subProceso",
            "asiento",
            "numeroAsiento",
            "fecha",
            "valor",
            "cantidad",
            "observacion",
            "idEstado",
            "usuarioRegistro",
            "fechaRegistro",
            "ipRegistro"
        };
    }

    @Override
    public List<AsientoCargaPetro> selectByCarga(Long idCarga) throws Throwable {
        System.out.println("Ingresa al metodo selectByCarga de AsientoCargaPetro"
                + " - carga: " + idCarga);
        Query query = em.createQuery(
                " select a from AsientoCargaPetro a " +
                " where  a.cargaArchivo.codigo = :idCarga " +
                " order by a.subProceso");
        query.setParameter("idCarga", idCarga);
        return query.getResultList();
    }

    @Override
    public AsientoCargaPetro selectVigenteByCargaYSubProceso(Long idCarga, int subProceso)
            throws Throwable {
        System.out.println("Ingresa al metodo selectVigenteByCargaYSubProceso de AsientoCargaPetro"
                + " - carga: " + idCarga + " - subProceso: " + subProceso);
        try {
            Query query = em.createQuery(
                    " select a from AsientoCargaPetro a " +
                    " where  a.cargaArchivo.codigo = :idCarga " +
                    " and    a.subProceso          = :subProceso " +
                    " and    a.idEstado            = :vigente ");
            query.setParameter("idCarga", idCarga);
            query.setParameter("subProceso", Long.valueOf(subProceso));
            query.setParameter("vigente", VIGENTE);
            return (AsientoCargaPetro) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
