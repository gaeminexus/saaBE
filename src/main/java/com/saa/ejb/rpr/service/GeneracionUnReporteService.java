package com.saa.ejb.rpr.service;

import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.EjecucionReporte;

import jakarta.ejb.Local;

/**
 * Ejecuta UN reporte G40-G51 en su propia transacción — extraído de
 * {@code GeneracionReportesServiceImpl} (2026-09-08) porque esa clase es {@code @Stateless} sin
 * {@code @TransactionAttribute} (o sea {@code REQUIRED}), así que los doce reportes corrían en
 * UNA sola transacción: si uno lanzaba {@code IncomeException} (marca la transacción para
 * rollback,{@code @ApplicationException(rollback=true)}), el intento de dejar constancia del
 * fallo en el mismo EJRD reventaba con {@code STATUS_MARKED_ROLLBACK} — un síntoma que tapaba el
 * error real, revertía TODO (incluidos los EJRC/EJRD que ya se habían creado) y no dejaba
 * rastro de cuál de los doce falló ni por qué.
 *
 * <b>Tiene que ser un bean aparte, no un método privado del orquestador.</b> Una llamada de un
 * bean a sí mismo no pasa por el proxy del contenedor, así que {@code REQUIRES_NEW} no tendría
 * ningún efecto — el bug quedaría igual y parecería arreglado.
 *
 * @author Sistema SAA
 * @since 2026-09-08
 */
@Local
public interface GeneracionUnReporteService {

    /**
     * Despacha al {@code GeneracionGxxService} correspondiente según
     * {@link DetalleEjecucionReporte#getTipoReporte()} — el mismo {@code switch} de los doce
     * casos que antes vivía en {@code GeneracionReportesServiceImpl#ejecutarG}, sin cambios en
     * la lógica de ningún G individual.
     *
     * <b>{@code REQUIRES_NEW}</b>: si este reporte falla, su transacción se revierte SOLA — no
     * arrastra a los demás reportes del lote ni a los EJRC/EJRD que el orquestador ya haya
     * grabado en su propia transacción.
     *
     * @param ejrd       : El detalle de ejecución (EJRD) del reporte a generar
     * @return           : Cantidad de registros generados
     * @throws Throwable : Lo que lance el {@code GeneracionGxxService} correspondiente — el
     *                     llamador es responsable de atraparlo y de llamar a
     *                     {@link #marcarResultado} con el resultado, en OTRA transacción
     */
    long generarUno(DetalleEjecucionReporte ejrd) throws Throwable;

    /**
     * Crea el EJRC y los 12 EJRD en estado PENDIENTE, en su PROPIA transacción
     * {@code REQUIRES_NEW}, CONFIRMADA antes de que el orquestador arranque el bucle —
     * defecto real de producción, 2026-10-05 (ejecución NUEVA de septiembre 2026): antes esta
     * creación vivía en la transacción externa (REQUIRED) de
     * {@code GeneracionReportesServiceImpl#ejecutarGeneracion}, sin confirmar todavía cuando
     * {@link #marcarResultado} (REQUIRES_NEW, OTRA conexión/transacción) intentaba
     * {@code ejrdService.selectById(idEjrd)} del primer EJRD — esa fila no existía aún para esa
     * transacción, y la consulta reventaba con {@code NoResultException}. "La ejecución nueva
     * nunca se probó" después del 2026-09-08: la rama de CORRECCIÓN funcionaba porque ahí los
     * EJRD ya estaban confirmados de una corrida anterior.
     *
     * <p>El orquestador, después de llamar a esto, recupera los EJRD con
     * {@code DetalleEjecucionReporteService#selectPendientesYNovedadesByEjecucion(idEjrc)} — la
     * MISMA consulta que ya usaba la rama de corrección — en vez de que este método devuelva la
     * lista: así las dos ramas (nueva y corrección) convergen al mismo código después de este
     * punto, sin necesidad de un segundo tipo de retorno.</p>
     *
     * @param mes     : Mes del reporte (1-12)
     * @param anio    : Año del reporte
     * @param usuario : Usuario que dispara la generación
     * @return         : El EJRC recién creado y CONFIRMADO, con su código ya asignado
     * @throws Throwable : Excepcion
     */
    EjecucionReporte crearEjecucion(Long mes, Long anio, String usuario) throws Throwable;

    /**
     * Graba el resultado (OK o con novedades) de UN reporte, en su PROPIA transacción —
     * {@code REQUIRES_NEW}. Recupera el EJRD de nuevo por id en vez de recibir la entidad: la
     * que tiene el orquestador puede venir de una transacción que {@link #generarUno} acaba de
     * marcar para rollback, y guardar esa instancia intentaría escribir dentro de una
     * transacción muerta — exactamente el {@code STATUS_MARKED_ROLLBACK} que este cambio existe
     * para eliminar.
     *
     * @param idEjrd            : Código del EJRD a actualizar
     * @param estado            : {@code EJRD_OK} o {@code EJRD_CON_NOVEDADES}
     * @param cantidadRegistros : Cantidad de registros generados, o {@code null} si falló
     * @param novedades         : Mensaje de error (con {@code "ERROR: "} y el detalle), o
     *                            {@code null} si salió OK
     * @throws Throwable        : Excepcion
     */
    void marcarResultado(Long idEjrd, Long estado, Long cantidadRegistros, String novedades) throws Throwable;
}
