package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.PagoPensionComplementaria;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA
 *         Interface DAO para la entidad PagoPensionComplementaria (CRD.PGPC).
 */
@Local
public interface PagoPensionComplementariaDaoService extends EntityDao<PagoPensionComplementaria> {

    /**
     * El pago de un jubilado para un período — a lo sumo uno, por
     * {@code UK_PGPC_ENTD_ANIO_MES}. Es el chequeo de idempotencia del proceso de generación
     * mensual: si devuelve algo, ese período ya se generó y no se vuelve a generar.
     *
     * @param idEntidad : Código de la entidad (CRD.ENTD)
     * @param anio      : Año del período
     * @param mes       : Mes del período (1-12)
     * @return          : El pago existente, o {@code null} si el período no se generó todavía
     * @throws Throwable : Excepcion
     */
    PagoPensionComplementaria selectByEntidadYPeriodo(Long idEntidad, Long anio, Long mes) throws Throwable;

    /**
     * Todos los pagos de un jubilado, del más reciente al más antiguo. Para historial y
     * auditoría.
     *
     * @param idEntidad : Código de la entidad (CRD.ENTD)
     * @return          : Listado; VACÍO si nunca se le generó un pago
     * @throws Throwable : Excepcion
     */
    List<PagoPensionComplementaria> selectByEntidad(Long idEntidad) throws Throwable;

    /**
     * Los pagos REGISTRADA o EN_PAGO — los que todavía no cerraron su ciclo. Es la consulta
     * que alimenta el reconciliador, mismo criterio que
     * {@code DevolucionAporteDaoService.selectPendientesConciliacion}.
     *
     * @return          : Listado; VACÍO si no hay ninguno pendiente
     * @throws Throwable : Excepcion
     */
    List<PagoPensionComplementaria> selectPendientesConciliacion() throws Throwable;

    /**
     * Todos los pagos de un período, sin importar el jubilado. Es el informe mensual completo:
     * la rama YA_EXISTIA de {@code generarPagosDelMes} sólo arma un renglón liviano de cinco
     * campos en una segunda corrida, así que este método es lo que lo recupera si el operador
     * cierra la pantalla.
     *
     * @param anio : Año del período
     * @param mes  : Mes del período (1-12)
     * @return     : Listado ordenado por el nombre del partícipe; VACÍO si el período no tiene pagos
     * @throws Throwable : Excepcion
     */
    List<PagoPensionComplementaria> selectByPeriodo(Long anio, Long mes) throws Throwable;
}
