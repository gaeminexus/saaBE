package com.saa.ejb.crd.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ConfiguracionBandaProductoDaoService;
import com.saa.model.crd.ConfiguracionBandaProducto;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class ConfiguracionBandaProductoDaoServiceImpl extends EntityDaoImpl<ConfiguracionBandaProducto>
        implements ConfiguracionBandaProductoDaoService {

    @PersistenceContext
    EntityManager em;

    /** Centinela de "vigencia abierta" para las comparaciones de traslape. */
    private static final LocalDate FIN_DE_LOS_TIEMPOS = LocalDate.of(9999, 12, 31);

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) ConfiguracionBandaProducto");
        return new String[]{
            "codigo",
            "producto",
            "empresa",
            "tipoCartera",
            "fechaDesde",
            "fechaHasta",
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
    public ConfiguracionBandaProducto selectVigente(Long idProducto, Long idEmpresa,
            Long tipoCartera, LocalDate fecha) throws Throwable {
        System.out.println("Ingresa al metodo selectVigente de ConfiguracionBandaProducto"
                + " - producto: " + idProducto + " empresa: " + idEmpresa
                + " tipoCartera: " + tipoCartera + " fecha: " + fecha);
        Query query = em.createQuery(
                " select c from ConfiguracionBandaProducto c " +
                " where  c.producto.codigo = :idProducto " +
                " and    c.empresa.codigo  = :idEmpresa " +
                " and    c.tipoCartera     = :tipoCartera " +
                " and    c.estado          = :activo " +
                " and    c.fechaDesde     <= :fecha " +
                " and   (c.fechaHasta is null or c.fechaHasta >= :fecha) " +
                " order by c.fechaDesde desc, c.codigo desc");
        query.setParameter("idProducto",  idProducto);
        query.setParameter("idEmpresa",   idEmpresa);
        query.setParameter("tipoCartera", tipoCartera);
        query.setParameter("activo",      Long.valueOf(Estado.ACTIVO));
        query.setParameter("fecha",       fecha);
        List<ConfiguracionBandaProducto> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public List<ConfiguracionBandaProducto> selectVigentesByEmpresa(Long idEmpresa, LocalDate fecha)
            throws Throwable {
        System.out.println("Ingresa al metodo selectVigentesByEmpresa de ConfiguracionBandaProducto"
                + " - empresa: " + idEmpresa + " fecha: " + fecha);
        Query query = em.createQuery(
                " select c from ConfiguracionBandaProducto c " +
                " where  c.empresa.codigo = :idEmpresa " +
                " and    c.estado         = :activo " +
                " and    c.fechaDesde    <= :fecha " +
                " and   (c.fechaHasta is null or c.fechaHasta >= :fecha) " +
                " order by c.producto.codigo, c.tipoCartera, c.fechaDesde desc");
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("activo",    Long.valueOf(Estado.ACTIVO));
        query.setParameter("fecha",     fecha);
        return query.getResultList();
    }

    @Override
    public List<ConfiguracionBandaProducto> selectHistorial(Long idProducto, Long idEmpresa,
            Long tipoCartera) throws Throwable {
        System.out.println("Ingresa al metodo selectHistorial de ConfiguracionBandaProducto"
                + " - producto: " + idProducto + " empresa: " + idEmpresa
                + " tipoCartera: " + tipoCartera);
        Query query = em.createQuery(
                " select c from ConfiguracionBandaProducto c " +
                " where  c.producto.codigo = :idProducto " +
                " and    c.empresa.codigo  = :idEmpresa " +
                " and    c.tipoCartera     = :tipoCartera " +
                " order by c.fechaDesde desc, c.codigo desc");
        query.setParameter("idProducto",  idProducto);
        query.setParameter("idEmpresa",   idEmpresa);
        query.setParameter("tipoCartera", tipoCartera);
        return query.getResultList();
    }

    @Override
    public List<ConfiguracionBandaProducto> selectSolapadas(Long idProducto, Long idEmpresa,
            Long tipoCartera, LocalDate desde, LocalDate hasta) throws Throwable {
        System.out.println("Ingresa al metodo selectSolapadas de ConfiguracionBandaProducto"
                + " - producto: " + idProducto + " empresa: " + idEmpresa
                + " tipoCartera: " + tipoCartera + " desde: " + desde + " hasta: " + hasta);
        // Dos intervalos [a1,a2] y [b1,b2] se solapan si a1 <= b2 y b1 <= a2. El "hasta"
        // nulo es "vigencia abierta" = infinito: en la fila se resuelve con el
        // "c.fechaHasta is null" del JPQL; en el parametro se sustituye por un centinela
        // en Java, porque un ":hasta is null" con parametro nulo no tiene tipo inferible
        // contra Oracle.
        LocalDate hastaEfectivo = (hasta != null ? hasta : FIN_DE_LOS_TIEMPOS);
        Query query = em.createQuery(
                " select c from ConfiguracionBandaProducto c " +
                " where  c.producto.codigo = :idProducto " +
                " and    c.empresa.codigo  = :idEmpresa " +
                " and    c.tipoCartera     = :tipoCartera " +
                " and    c.estado          = :activo " +
                " and    c.fechaDesde     <= :hasta " +
                " and   (c.fechaHasta is null or c.fechaHasta >= :desde) " +
                " order by c.fechaDesde, c.codigo");
        query.setParameter("idProducto",  idProducto);
        query.setParameter("idEmpresa",   idEmpresa);
        query.setParameter("tipoCartera", tipoCartera);
        query.setParameter("activo",      Long.valueOf(Estado.ACTIVO));
        query.setParameter("desde",       desde);
        query.setParameter("hasta",       hastaEfectivo);
        return query.getResultList();
    }
}
