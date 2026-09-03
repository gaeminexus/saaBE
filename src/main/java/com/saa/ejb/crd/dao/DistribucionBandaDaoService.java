package com.saa.ejb.crd.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.ejb.crd.service.dto.FiltroDetalleDistribucionBanda;
import com.saa.model.crd.DistribucionBanda;

import jakarta.ejb.Local;

/**
 * DAO de CRD.DSBN — ver PLAN-AUDITORIA-BANDAS.md. Todo lo de esta interfaz es de solo
 * lectura salvo {@link #eliminarPorOrigen} (parte del reemplazo idempotente) y
 * {@link #actualizarAsientoPorOrigen} (enganche con contabilidad, posterior a la escritura).
 */
@Local
public interface DistribucionBandaDaoService extends EntityDao<DistribucionBanda> {

    /** Todas las filas de un origen, sin filtrar — para el cuadre y para reconstruir el detalle. */
    List<DistribucionBanda> selectByOrigen(String origen, Long idOrigen) throws Throwable;

    /**
     * Borra todas las filas de un origen — mitad del reemplazo idempotente
     * (PLAN-AUDITORIA-BANDAS.md §5.1 punto 2): reprocesar un origen REEMPLAZA sus filas, no
     * las duplica.
     *
     * @return cuántas filas borró
     */
    int eliminarPorOrigen(String origen, Long idOrigen) throws Throwable;

    /**
     * Estampa el asiento en TODAS las filas de un origen, después de que contabilidad genera
     * el suyo — el único enganche con CNT (ASNTCDGO).
     */
    void actualizarAsientoPorOrigen(String origen, Long idOrigen, Long idAsiento) throws Throwable;

    /**
     * Orígenes con distribución registrada, del más reciente al más antiguo — alimenta
     * {@code GET /rest/dsbn/origenes}. Cada fila es {@code [origen, idOrigen, fechaMax]}.
     */
    List<Object[]> selectOrigenesDistintos(String origenFiltro, LocalDate fechaDesde,
            LocalDate fechaHasta, int limite) throws Throwable;

    /** Detalle filtrado y paginado — {@code POST /rest/dsbn/detalle}. */
    List<DistribucionBanda> selectDetalleFiltrado(FiltroDetalleDistribucionBanda filtro) throws Throwable;

    /** Cuántas filas matchea el filtro, sin paginar — para {@code totalFilas}. */
    long contarDetalleFiltrado(FiltroDetalleDistribucionBanda filtro) throws Throwable;

    /**
     * Vista RESUMEN de {@code POST /rest/dsbn/detalle} (API-AUDITORIA-BANDAS.md "Las DOS
     * vistas", 2026-09-02): agregado con GROUP BY sobre el conjunto FILTRADO COMPLETO, mismo
     * filtro que {@link #selectDetalleFiltrado} pero sin paginar — el resumen no puede salir de
     * sumar en Java las 50 filas de una página cuando el filtro trae miles.
     *
     * Cada fila es {@code [concepto, idBanda, etiqueta, cuentaContable, nombreCuenta,
     * idTipoPrestamo, idTipoAporte, sumaValor, cantidadFilas]}. {@code idBanda}/{@code etiqueta}
     * vienen null en conceptos sin banda (todo salvo CAPITAL); {@code cuentaContable}/
     * {@code nombreCuenta} vienen null sin CNT conectado, y también en los conceptos sin banda
     * (su cuenta se resuelve al LEER por {@code idTipoPrestamo}/{@code idTipoAporte} — ver
     * {@code DistribucionBandaServiceImpl#resolverLineaPlantilla}) — en todos los casos el
     * GROUP BY sigue agrupando correctamente porque el null es constante dentro del grupo.
     */
    List<Object[]> selectResumenJerarquicoFiltrado(FiltroDetalleDistribucionBanda filtro) throws Throwable;
}
