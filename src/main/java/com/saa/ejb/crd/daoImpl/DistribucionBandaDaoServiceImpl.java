package com.saa.ejb.crd.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DistribucionBandaDaoService;
import com.saa.ejb.crd.service.dto.FiltroDetalleDistribucionBanda;
import com.saa.model.crd.DistribucionBanda;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

@SuppressWarnings("unchecked")
@Stateless
public class DistribucionBandaDaoServiceImpl extends EntityDaoImpl<DistribucionBanda>
        implements DistribucionBandaDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "origen", "idOrigen", "concepto", "valor",
            "entidad", "prestamo", "detallePrestamo", "producto", "tipoPrestamo", "tipoAporte",
            "tipoCartera", "dias", "banda", "etiqueta",
            "fechaVencimiento", "fechaAplicacion", "idAsiento",
            "fechaRegistro", "usuarioRegistro", "estado"
        };
    }

    @Override
    public List<DistribucionBanda> selectByOrigen(String origen, Long idOrigen) throws Throwable {
        System.out.println("DistribucionBandaDaoService.selectByOrigen - " + origen + "/" + idOrigen);
        TypedQuery<DistribucionBanda> query = em.createQuery(
            "select d from DistribucionBanda d where d.origen = :origen and d.idOrigen = :idOrigen"
                + " order by d.codigo", DistribucionBanda.class);
        query.setParameter("origen", origen);
        query.setParameter("idOrigen", idOrigen);
        return query.getResultList();
    }

    @Override
    public int eliminarPorOrigen(String origen, Long idOrigen) throws Throwable {
        System.out.println("DistribucionBandaDaoService.eliminarPorOrigen - " + origen + "/" + idOrigen);
        Query query = em.createQuery(
            "delete from DistribucionBanda d where d.origen = :origen and d.idOrigen = :idOrigen");
        query.setParameter("origen", origen);
        query.setParameter("idOrigen", idOrigen);
        return query.executeUpdate();
    }

    @Override
    public void actualizarAsientoPorOrigen(String origen, Long idOrigen, Long idAsiento) throws Throwable {
        System.out.println("DistribucionBandaDaoService.actualizarAsientoPorOrigen - " + origen
            + "/" + idOrigen + " -> asiento " + idAsiento);
        Query query = em.createQuery(
            "update DistribucionBanda d set d.idAsiento = :idAsiento"
                + " where d.origen = :origen and d.idOrigen = :idOrigen");
        query.setParameter("idAsiento", idAsiento);
        query.setParameter("origen", origen);
        query.setParameter("idOrigen", idOrigen);
        query.executeUpdate();
    }

    @Override
    public List<Object[]> selectOrigenesDistintos(String origenFiltro, LocalDate fechaDesde,
            LocalDate fechaHasta, int limite) throws Throwable {
        System.out.println("DistribucionBandaDaoService.selectOrigenesDistintos - origen=" + origenFiltro
            + " desde=" + fechaDesde + " hasta=" + fechaHasta);

        StringBuilder jpql = new StringBuilder(
            "select d.origen, d.idOrigen, max(d.fechaAplicacion) "
                + "from DistribucionBanda d where 1 = 1 ");
        if (origenFiltro != null) {
            jpql.append("and d.origen = :origen ");
        }
        if (fechaDesde != null) {
            jpql.append("and d.fechaAplicacion >= :fechaDesde ");
        }
        if (fechaHasta != null) {
            jpql.append("and d.fechaAplicacion <= :fechaHasta ");
        }
        jpql.append("group by d.origen, d.idOrigen order by max(d.fechaAplicacion) desc");

        Query query = em.createQuery(jpql.toString());
        if (origenFiltro != null) {
            query.setParameter("origen", origenFiltro);
        }
        if (fechaDesde != null) {
            query.setParameter("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            query.setParameter("fechaHasta", fechaHasta);
        }
        query.setMaxResults(limite > 0 ? limite : 50);
        return query.getResultList();
    }

    @Override
    public List<DistribucionBanda> selectDetalleFiltrado(FiltroDetalleDistribucionBanda filtro) throws Throwable {
        System.out.println("DistribucionBandaDaoService.selectDetalleFiltrado - "
            + filtro.getOrigen() + "/" + filtro.getIdOrigen());
        Query query = em.createQuery(construirJpqlDetalle(filtro, false));
        aplicarParametrosDetalle(query, filtro);

        int pagina = filtro.getPagina() != null ? Math.max(0, filtro.getPagina()) : 0;
        int tamanio = filtro.getTamanio() != null && filtro.getTamanio() > 0 ? filtro.getTamanio() : 50;
        query.setFirstResult(pagina * tamanio);
        query.setMaxResults(tamanio);

        return query.getResultList();
    }

    @Override
    public long contarDetalleFiltrado(FiltroDetalleDistribucionBanda filtro) throws Throwable {
        Query query = em.createQuery(construirJpqlDetalle(filtro, true));
        aplicarParametrosDetalle(query, filtro);
        Object resultado = query.getSingleResult();
        return resultado != null ? ((Number) resultado).longValue() : 0L;
    }

    /**
     * Arma el JPQL dinámico del detalle. Los arreglos son OR interno (IN) y AND entre sí
     * (API-AUDITORIA-BANDAS.md §2). {@code cuentasContables} filtra por la cuenta de la banda
     * (solo tiene sentido en filas CAPITAL, que son las únicas con {@code banda} no nulo).
     */
    private String construirJpqlDetalle(FiltroDetalleDistribucionBanda filtro, boolean paraContar) {
        StringBuilder jpql = new StringBuilder(paraContar
            ? "select count(d) from DistribucionBanda d where "
            : "select d from DistribucionBanda d where ");
        jpql.append(construirWhereDetalle(filtro, "d.banda", "d.banda.planCuenta"));

        if (!paraContar) {
            String ordenarPor = campoOrden(filtro.getOrdenarPor());
            String orden = "asc".equalsIgnoreCase(filtro.getOrden()) ? "asc" : "desc";
            jpql.append("order by ").append(ordenarPor).append(' ').append(orden);
        }
        return jpql.toString();
    }

    /**
     * Condiciones WHERE compartidas por {@link #construirJpqlDetalle} y
     * {@link #selectResumenJerarquicoFiltrado} — mismo filtro, misma semántica, un solo lugar
     * (2026-09-02). {@code aliasBanda}/{@code aliasPlanCuenta} parametrizan solo el camino hacia
     * banda: el detalle navega el camino implícito {@code d.banda}/{@code d.banda.planCuenta}
     * (como siempre), y el resumen jerárquico navega sus JOIN explícitos ({@code b}/{@code pc})
     * porque necesita LEFT JOIN para no perder los conceptos sin banda (todo salvo CAPITAL) ni
     * las bandas sin cuenta contable (CNT desconectado).
     */
    private String construirWhereDetalle(FiltroDetalleDistribucionBanda filtro, String aliasBanda,
            String aliasPlanCuenta) {
        StringBuilder jpql = new StringBuilder("d.origen = :origen and d.idOrigen = :idOrigen ");

        if (filtro.getConceptos() != null && !filtro.getConceptos().isEmpty()) {
            jpql.append("and d.concepto in :conceptos ");
        }
        if (filtro.getIdsBanda() != null && !filtro.getIdsBanda().isEmpty()) {
            jpql.append("and ").append(aliasBanda).append(".codigo in :idsBanda ");
        }
        if (filtro.getIdsProducto() != null && !filtro.getIdsProducto().isEmpty()) {
            jpql.append("and d.producto.codigo in :idsProducto ");
        }
        if (filtro.getIdsTipoPrestamo() != null && !filtro.getIdsTipoPrestamo().isEmpty()) {
            jpql.append("and d.tipoPrestamo in :idsTipoPrestamo ");
        }
        if (filtro.getIdsTipoAporte() != null && !filtro.getIdsTipoAporte().isEmpty()) {
            jpql.append("and d.tipoAporte in :idsTipoAporte ");
        }
        if (filtro.getIdsEntidad() != null && !filtro.getIdsEntidad().isEmpty()) {
            jpql.append("and d.entidad.codigo in :idsEntidad ");
        }
        if (filtro.getCuentasContables() != null && !filtro.getCuentasContables().isEmpty()) {
            jpql.append("and ").append(aliasPlanCuenta).append(".cuentaContable in :cuentasContables ");
        }
        if (filtro.getFechaDesde() != null) {
            jpql.append("and d.fechaAplicacion >= :fechaDesde ");
        }
        if (filtro.getFechaHasta() != null) {
            jpql.append("and d.fechaAplicacion <= :fechaHasta ");
        }
        return jpql.toString();
    }

    @Override
    public List<Object[]> selectResumenJerarquicoFiltrado(FiltroDetalleDistribucionBanda filtro) throws Throwable {
        System.out.println("DistribucionBandaDaoService.selectResumenJerarquicoFiltrado - "
            + filtro.getOrigen() + "/" + filtro.getIdOrigen());

        // LEFT JOIN, no implícito: un concepto sin banda (todo salvo CAPITAL) o una banda sin
        // plan de cuenta (CNT desconectado) tienen que seguir sumando, no desaparecer del
        // agregado — un JOIN implícito de JPA sobre una asociación opcional se traduce INNER.
        // tipoPrestamo/tipoAporte van en el GROUP BY (2026-09-02): la cuenta de los conceptos
        // sin banda se resuelve al LEER por (concepto, tipo) — DistribucionBandaServiceImpl los
        // necesita para no fusionar, por ejemplo, seguro de incendio hipotecario y prendario
        // (cuentas distintas) en un solo grupo.
        String jpql = "select d.concepto, b.codigo, d.etiqueta, pc.cuentaContable, pc.nombre, "
            + "d.tipoPrestamo, d.tipoAporte, sum(d.valor), count(d) "
            + "from DistribucionBanda d left join d.banda b left join b.planCuenta pc where "
            + construirWhereDetalle(filtro, "b", "pc")
            + "group by d.concepto, b.codigo, d.etiqueta, pc.cuentaContable, pc.nombre, "
            + "d.tipoPrestamo, d.tipoAporte "
            + "order by d.concepto";

        Query query = em.createQuery(jpql);
        aplicarParametrosDetalle(query, filtro);
        return query.getResultList();
    }

    /** Solo columnas propias (no relaciones) — evita inyectar JPQL arbitrario desde el filtro. */
    private String campoOrden(String ordenarPor) {
        if (ordenarPor == null) {
            return "d.codigo";
        }
        switch (ordenarPor) {
            case "valor": return "d.valor";
            case "fechaAplicacion": return "d.fechaAplicacion";
            case "concepto": return "d.concepto";
            default: return "d.codigo";
        }
    }

    private void aplicarParametrosDetalle(Query query, FiltroDetalleDistribucionBanda filtro) {
        query.setParameter("origen", filtro.getOrigen());
        query.setParameter("idOrigen", filtro.getIdOrigen());
        if (filtro.getConceptos() != null && !filtro.getConceptos().isEmpty()) {
            query.setParameter("conceptos", filtro.getConceptos());
        }
        if (filtro.getIdsBanda() != null && !filtro.getIdsBanda().isEmpty()) {
            query.setParameter("idsBanda", filtro.getIdsBanda());
        }
        if (filtro.getIdsProducto() != null && !filtro.getIdsProducto().isEmpty()) {
            query.setParameter("idsProducto", filtro.getIdsProducto());
        }
        if (filtro.getIdsTipoPrestamo() != null && !filtro.getIdsTipoPrestamo().isEmpty()) {
            query.setParameter("idsTipoPrestamo", filtro.getIdsTipoPrestamo());
        }
        if (filtro.getIdsTipoAporte() != null && !filtro.getIdsTipoAporte().isEmpty()) {
            query.setParameter("idsTipoAporte", filtro.getIdsTipoAporte());
        }
        if (filtro.getIdsEntidad() != null && !filtro.getIdsEntidad().isEmpty()) {
            query.setParameter("idsEntidad", filtro.getIdsEntidad());
        }
        if (filtro.getCuentasContables() != null && !filtro.getCuentasContables().isEmpty()) {
            query.setParameter("cuentasContables", filtro.getCuentasContables());
        }
        if (filtro.getFechaDesde() != null) {
            query.setParameter("fechaDesde", filtro.getFechaDesde());
        }
        if (filtro.getFechaHasta() != null) {
            query.setParameter("fechaHasta", filtro.getFechaHasta());
        }
    }
}
