/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.CobroCredito;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA.
 *         Interface DAO para la entidad CobroCredito (CRD.CBCR): cabecera del cobro
 *         pendiente de autorización de contabilidad.
 */
@Local
public interface CobroCreditoDaoService extends EntityDao<CobroCredito> {

    /**
     * Bandeja de un estado puntual, para las pantallas de crédito y de contabilidad.
     *
     * @param estado     : {@link com.saa.rubros.CrdEstadoCobro}
     * @return           : Listado ordenado por fecha de registro ascendente (FIFO);
     *                     VACÍO si no hay ninguno en ese estado
     * @throws Throwable : Excepcion
     */
    List<CobroCredito> selectByEstado(Long estado) throws Throwable;

    /**
     * Los cobros de una entidad (partícipe), para su ficha.
     *
     * @param idEntidad  : Código de la entidad (CRD.ENTD)
     * @return           : Listado; VACÍO si no tiene ningún cobro registrado
     * @throws Throwable : Excepcion
     */
    List<CobroCredito> selectByEntidad(Long idEntidad) throws Throwable;

    /**
     * Cobros cuya referencia (recortada) coincide con la buscada — para el chequeo de
     * unicidad del registro/corrección de un cobro. MISMA comparación que el índice único
     * {@code CRD.UX_CBCR_REFERENCIA}: {@code TRIM(CBCRRFRN)}, excluyendo los cobros en estado
     * {@link com.saa.rubros.CrdEstadoCobro#ANULADO} — un cobro anulado libera su referencia
     * (2026-09-01, decisión del usuario).
     *
     * @param referenciaTrim  Ya recortada por el llamador — este método NUNCA la recorta, para
     *                        que la comparación sea idéntica a la del índice, nunca una copia
     *                        que pueda divergir.
     * @param idCobroExcluido Se excluye de la búsqueda (para que corregir un cobro no choque
     *                        contra sí mismo); {@code null} si es un registro nuevo.
     * @return                Los cobros en conflicto (normalmente 0 o 1; puede haber más de
     *                        uno si el índice todavía no corrió sobre datos históricos).
     * @throws Throwable      Excepcion
     */
    List<CobroCredito> selectByReferencia(String referenciaTrim, Long idCobroExcluido) throws Throwable;

    /**
     * Cobros cuya fecha del DEPÓSITO (no la de registro) cae en el rango, para la pantalla de
     * seguimiento mensual (API-SEGUIMIENTO-COBROS.md). Trae con {@code JOIN FETCH} la entidad,
     * la cuenta bancaria y los tres asientos — evita el N+1 de tocarlos fila por fila cuando un
     * mes trae cientos de cobros.
     *
     * ⚠️ NO fetchea {@code cuentaBancaria.banco} (nivel dos, que el service necesita para el
     * texto "{banco} - {numeroCuenta}"): JPA no permite aliasear un {@code join fetch}, y ese
     * segundo nivel solo se puede encadenar aliaseando {@code cuentaBancaria}. Hibernate lo
     * carga igual por ser {@code @ManyToOne} EAGER — con un SELECT aparte por banco distinto,
     * no por fila. Ver el comentario del JPQL en el {@code Impl}.
     *
     * @param desde      : Fecha inicial del rango (inclusive)
     * @param hasta      : Fecha final del rango (inclusive)
     * @return           : Listado ordenado por fecha de cobro descendente (más reciente
     *                     primero), y por código descendente entre cobros del mismo día;
     *                     VACÍO si no hay ninguno en el rango
     * @throws Throwable : Excepcion
     */
    List<CobroCredito> selectByRangoFechaCobro(LocalDate desde, LocalDate hasta) throws Throwable;

    /**
     * C45 (DS-01) — bloqueo pesimista de la cabecera del cobro ({@code SELECT … FOR UPDATE
     * NOWAIT} nativo, solo la fila de {@code CRD.CBCR}) para que dos llamadas a
     * {@code procesarCobro} del MISMO cobro no lean las dos el estado APROBADO y lo apliquen
     * dos veces (producción, 10-nov-2026: cobro 244, eventos 1151 y 1152, 88 s de diferencia).
     * Si otra sesión ya lo tiene tomado (ORA-00054) lanza
     * {@link com.saa.basico.util.IncomeException} sin esperar.
     *
     * @param idCobro Código del cobro (CRD.CBCR)
     * @throws Throwable IncomeException si el cobro no existe o está en proceso en otra sesión
     */
    void bloquearParaProcesar(Long idCobro) throws Throwable;

    /**
     * C45 (DS-01) — cobros NO anulados de la misma entidad, cuenta bancaria, fecha de depósito
     * y valor (± medio centavo) con referencia '9'/'09'. Es la clave compuesta que sustituye a la
     * unicidad de la referencia cuando el cobro no tiene número real.
     *
     * @param idCobroExcluido se excluye (corregir un cobro no choca consigo mismo); puede ser null
     */
    List<CobroCredito> selectSinReferenciaCoincidentes(Long idEntidad, Long idCuentaBancaria, LocalDate fecha,
            Double valor, Long idCobroExcluido) throws Throwable;

    /**
     * C45 (DS-01) — eventos VIGENTES del préstamo con la misma fecha (día) y valor (± medio
     * centavo) que NO están enlazados a ninguna línea de cobro ({@code CRD.DCBC.EVPRCDGO}): un
     * pago ya aplicado por otro camino (pago manual directo) que el cobro volvería a aplicar
     * (producción, agosto 2026: evento 42 frente al cobro 69).
     *
     * @return filas {EVPRCDGO, EVPRTPOO}
     */
    List<Object[]> selectEventosSinCobroCoincidentes(Long idPrestamo, LocalDate fecha, Double valor)
            throws Throwable;
}
