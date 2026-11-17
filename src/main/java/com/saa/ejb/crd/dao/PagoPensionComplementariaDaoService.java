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
     * Pensión retenida pendiente de pago, agrupada por jubilado (PGPC REGISTRADA, sin orden, PGPCVLRT &gt; 0): una fila
     * {@code {idEntidad, suma de PGPCVLRT, cantidad de meses}} por jubilado. Una sola consulta: ni el proceso de
     * pendientes ni la lectura en bloque recorren el padrón.
     */
    List<Object[]> selectResumenRetenidosPendientes() throws Throwable;

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

    /**
     * C32b (G44, D-113): pensión retenida EN ORIGEN de un período, por jubilado: PGPC del período con PGPCVLRT &gt; 0 y
     * PGPCDVRT = 0 (no se debitó ni devengó en su mes por no tener cuenta certificada), no anulado. Cuenta aunque después
     * lo haya pagado {@code pagarRetenidos} (ese débito se fecha el día del pago y la G44 no lo suma en ese mes).
     * Los PGPCDVRT = 1 (rechazo de un pago ya debitado en su mes) no van: su débito original sigue en el mes.
     * Una fila {@code {idEntidad, suma de PGPCVLRT}} por jubilado.
     */
    List<Object[]> selectSumaRetenidoEnOrigenPorPeriodo(Long anio, Long mes) throws Throwable;
}
