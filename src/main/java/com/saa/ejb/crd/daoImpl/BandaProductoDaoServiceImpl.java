package com.saa.ejb.crd.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.BandaProductoDaoService;
import com.saa.model.crd.BandaProducto;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class BandaProductoDaoServiceImpl extends EntityDaoImpl<BandaProducto>
        implements BandaProductoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) BandaProducto");
        return new String[]{
            "codigo",
            "configuracion",
            "numero",
            "periodos",
            "planCuenta",
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
    public List<BandaProducto> selectByConfiguracion(Long idConfiguracion) throws Throwable {
        System.out.println("Ingresa al metodo selectByConfiguracion de BandaProducto"
                + " - configuracion: " + idConfiguracion);
        Query query = em.createQuery(
                " select b from BandaProducto b " +
                " where  b.configuracion.codigo = :idConfiguracion " +
                " and    b.estado               = :activo " +
                " order by b.numero");
        query.setParameter("idConfiguracion", idConfiguracion);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        return query.getResultList();
    }

    @Override
    public List<BandaProducto> selectTodasByConfiguracion(Long idConfiguracion) throws Throwable {
        System.out.println("Ingresa al metodo selectTodasByConfiguracion de BandaProducto"
                + " - configuracion: " + idConfiguracion);
        Query query = em.createQuery(
                " select b from BandaProducto b " +
                " where  b.configuracion.codigo = :idConfiguracion " +
                " order by b.numero");
        query.setParameter("idConfiguracion", idConfiguracion);
        return query.getResultList();
    }

    @Override
    public List<BandaProducto> selectByConfiguraciones(List<Long> idsConfiguracion) throws Throwable {
        System.out.println("Ingresa al metodo selectByConfiguraciones de BandaProducto"
                + " - configuraciones: "
                + (idsConfiguracion != null ? idsConfiguracion.size() : 0));
        if (idsConfiguracion == null || idsConfiguracion.isEmpty()) {
            return new ArrayList<BandaProducto>();
        }
        Query query = em.createQuery(
                " select b from BandaProducto b " +
                " where  b.configuracion.codigo in (:idsConfiguracion) " +
                " and    b.estado               = :activo " +
                " order by b.configuracion.codigo, b.numero");
        query.setParameter("idsConfiguracion", idsConfiguracion);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        return query.getResultList();
    }

    @Override
    public int deleteByConfiguracion(Long idConfiguracion) throws Throwable {
        System.out.println("Ingresa al metodo deleteByConfiguracion de BandaProducto"
                + " - configuracion: " + idConfiguracion);
        Query query = em.createQuery(
                " delete from BandaProducto b " +
                " where  b.configuracion.codigo = :idConfiguracion");
        query.setParameter("idConfiguracion", idConfiguracion);
        int eliminadas = query.executeUpdate();
        // El DELETE masivo va a la base de inmediato, antes de los INSERT de las bandas
        // nuevas: por eso el reemplazo del juego de bandas no choca con UK_BNDP_CBPR_NMRO.
        // No se hace em.clear(): desprenderia la configuracion que el servicio sigue usando.
        em.flush();
        return eliminadas;
    }
}
