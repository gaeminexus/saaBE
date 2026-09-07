package com.saa.ejb.crd.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ConfiguracionCalificacionRiesgoDaoService;
import com.saa.model.crd.ConfiguracionCalificacionRiesgo;
import com.saa.model.crd.Producto;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class ConfiguracionCalificacionRiesgoDaoServiceImpl extends EntityDaoImpl<ConfiguracionCalificacionRiesgo>
        implements ConfiguracionCalificacionRiesgoDaoService {

    @PersistenceContext
    EntityManager em;

    /** Centinela de "vigencia abierta" para las comparaciones de traslape — mismo criterio que
     * {@code ConfiguracionBandaProductoDaoServiceImpl}. */
    private static final LocalDate FIN_DE_LOS_TIEMPOS = LocalDate.of(9999, 12, 31);

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "producto", "idEmpresa", "nombre", "fechaInicio", "fechaFin",
            "fechaRegistro", "usuarioRegistro", "estado"
        };
    }

    @Override
    public ConfiguracionCalificacionRiesgo selectVigentePorProducto(Long idProducto, Long idEmpresa,
            LocalDate fecha) throws Throwable {
        System.out.println("ConfiguracionCalificacionRiesgoDaoService.selectVigentePorProducto - producto: "
            + idProducto + " - empresa: " + idEmpresa + " - fecha: " + fecha);
        Query query = em.createQuery(
            "select c from ConfiguracionCalificacionRiesgo c "
                + "where c.producto.codigo = :idProducto "
                + "and (c.idEmpresa is null or c.idEmpresa = :idEmpresa) "
                + "and c.estado = :activo "
                + "and c.fechaInicio <= :fecha "
                + "and (c.fechaFin is null or c.fechaFin >= :fecha) "
                // Sin NULLS LAST (portabilidad JPQL): con la carga inicial (sql/177) hay UNA sola
                // fila por producto, con idEmpresa null — no hay ambigüedad que desempatar todavía.
                // El día que se agregue una configuración específica por empresa, revisar este
                // orden para que la más específica gane sobre la genérica.
                + "order by c.fechaInicio desc, c.codigo desc");
        query.setParameter("idProducto", idProducto);
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("fecha", fecha);
        query.setMaxResults(1);
        List<ConfiguracionCalificacionRiesgo> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public List<Producto> selectProductosSinConfiguracionVigente(LocalDate fecha) throws Throwable {
        System.out.println("ConfiguracionCalificacionRiesgoDaoService.selectProductosSinConfiguracionVigente - fecha: "
            + fecha);
        Query query = em.createQuery(
            "select p from Producto p where not exists ("
                + "select 1 from ConfiguracionCalificacionRiesgo c "
                + "where c.producto = p "
                + "and c.estado = :activo "
                + "and c.fechaInicio <= :fecha "
                + "and (c.fechaFin is null or c.fechaFin >= :fecha)) "
                + "order by p.codigo");
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("fecha", fecha);
        return query.getResultList();
    }

    @Override
    public List<ConfiguracionCalificacionRiesgo> selectVigentesPorEmpresa(Long idEmpresa, LocalDate fecha)
            throws Throwable {
        System.out.println("ConfiguracionCalificacionRiesgoDaoService.selectVigentesPorEmpresa - empresa: "
            + idEmpresa + " - fecha: " + fecha);
        Query query = em.createQuery(
            "select c from ConfiguracionCalificacionRiesgo c "
                + "where (c.idEmpresa is null or c.idEmpresa = :idEmpresa) "
                + "and c.estado = :activo "
                + "and c.fechaInicio <= :fecha "
                + "and (c.fechaFin is null or c.fechaFin >= :fecha) "
                // Mismo orden que selectVigentePorProducto (y la misma ambigüedad anotada ahí
                // para datos historicos): el agrupado en Java por producto se queda con la
                // primera fila que ve.
                + "order by c.producto.codigo, c.fechaInicio desc, c.codigo desc");
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("fecha", fecha);
        return query.getResultList();
    }

    @Override
    public List<ConfiguracionCalificacionRiesgo> selectHistorial(Long idProducto, Long idEmpresa)
            throws Throwable {
        System.out.println("ConfiguracionCalificacionRiesgoDaoService.selectHistorial - producto: "
            + idProducto + " - empresa: " + idEmpresa);
        Query query = em.createQuery(
            "select c from ConfiguracionCalificacionRiesgo c "
                + "where c.producto.codigo = :idProducto "
                + "and (c.idEmpresa is null or c.idEmpresa = :idEmpresa) "
                + "order by c.fechaInicio desc, c.codigo desc");
        query.setParameter("idProducto", idProducto);
        query.setParameter("idEmpresa", idEmpresa);
        return query.getResultList();
    }

    @Override
    public List<ConfiguracionCalificacionRiesgo> selectSolapadas(Long idProducto, Long idEmpresa,
            LocalDate desde, LocalDate hasta) throws Throwable {
        System.out.println("ConfiguracionCalificacionRiesgoDaoService.selectSolapadas - producto: "
            + idProducto + " - empresa: " + idEmpresa + " - desde: " + desde + " - hasta: " + hasta);
        // Dos intervalos se solapan si a1 <= b2 y b1 <= a2 (mismo criterio que
        // ConfiguracionBandaProductoDaoServiceImpl). El "hasta" nulo se resuelve con el centinela
        // en Java por el mismo motivo que ahi: un parametro nulo no tiene tipo inferible.
        LocalDate hastaEfectivo = (hasta != null ? hasta : FIN_DE_LOS_TIEMPOS);
        // La empresa de la configuracion NUEVA decide el filtro, no al reves: si es universal
        // (null), conflictua con CUALQUIER otra del mismo producto -- universal no puede convivir
        // con nada mas en el mismo rango. Si es de una empresa puntual, conflictua con las
        // universales (la ambiguedad que selectVigentePorProducto todavia no resuelve para datos
        // historicos, y que esta validacion evita hacia adelante) y con esa MISMA empresa; nunca
        // con otra empresa especifica distinta.
        String filtroEmpresa = (idEmpresa == null) ? ""
            : "and (c.idEmpresa is null or c.idEmpresa = :idEmpresa) ";
        Query query = em.createQuery(
            "select c from ConfiguracionCalificacionRiesgo c "
                + "where c.producto.codigo = :idProducto "
                + filtroEmpresa
                + "and c.estado = :activo "
                + "and c.fechaInicio <= :hasta "
                + "and (c.fechaFin is null or c.fechaFin >= :desde) "
                + "order by c.fechaInicio, c.codigo");
        query.setParameter("idProducto", idProducto);
        if (idEmpresa != null) {
            query.setParameter("idEmpresa", idEmpresa);
        }
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("desde", desde);
        query.setParameter("hasta", hastaEfectivo);
        return query.getResultList();
    }
}
