package com.saa.ejb.crd.dao;

import java.sql.Date;
import java.util.List;

import jakarta.ejb.Local;

/**
 * Consultas de SOLO LECTURA del reporte de préstamos cancelados
 * (contrato: docs/logica-negocio/crd/API-REPORTE-PRESTAMOS-CANCELADOS.md).
 *
 * <p>No mapea una entidad (no extiende EntityDao): son consultas nativas, con las columnas explícitas
 * (nunca {@code SELECT *}), que devuelven {@code Object[]} en el orden que documenta cada método. Las
 * consultas "en lote" reciben una lista de ids y la parten en trozos de 1000 (límite del IN de Oracle):
 * el servicio nunca consulta por préstamo dentro de un bucle.</p>
 *
 * <p>Reglas que comparten todas: el estado del préstamo es {@code PRSTIDST} (nunca {@code ESPSCDGO});
 * un evento vigente es {@code EVPRESTD = 1}; un pago vigente es {@code NVL(PGPRANUL,0) = 0} (nunca
 * {@code PGPRESTD}: el reverso lo deja en 1); un PGAP vigente además exige {@code NVL(PGAPIDST,1) <> 0}.</p>
 */
@Local
public interface PrestamoCanceladoDaoService {

    /**
     * Préstamos en estado terminal cuya fecha de cancelación derivada (§3 del contrato; es la misma regla
     * del bloque 2 de {@code sql/381_MEDICION_PRESTAMOS_CANCELADOS.sql}) cae en el rango, inclusivo.
     *
     * @param desde      inicio del rango (inclusivo)
     * @param hasta      fin del rango (inclusivo)
     * @param idProducto {@code PRST.PRDCCDGO}, o null para todos
     * @param idFilial   {@code PRST.FLLLCDGO}, o null para todas
     * @param estados    estados {@code PRSTIDST} a incluir (no vacío)
     * @return filas {@code [PRSTCDGO, PRSTIDST, fechaCancelacion (DATE, truncada), origenFecha
     *         ('SUSTITUCION' | 'EVENTO' | 'PAGO' | 'CUOTA'), f_evento, f_pago]}, ordenadas por PRSTCDGO.
     *         'PAGO' es un pago sin evento (carga o migración): el servicio lo distingue por el tipo del pago.
     */
    List<Object[]> candidatos(Date desde, Date hasta, Long idProducto, Long idFilial, List<Long> estados);

    /**
     * Cuenta los préstamos en estado terminal que pasan los demás filtros y a los que no se les pudo derivar
     * ninguna fecha de cancelación (§2.3 {@code canceladosSinFecha}).
     *
     * @param idProducto {@code PRST.PRDCCDGO}, o null
     * @param idFilial   {@code PRST.FLLLCDGO}, o null
     * @param estados    estados {@code PRSTIDST} a incluir (no vacío)
     * @return cantidad
     */
    long contarSinFecha(Long idProducto, Long idFilial, List<Long> estados);

    /**
     * Datos del préstamo con su entidad, filial y producto.
     *
     * @param ids códigos de préstamo
     * @return filas {@code [PRSTCDGO, PRSTIDAS, ENTDNMID, ENTDRZNS, FLLLNMBR, PRDCNMBR, PRSTIDST, PRSTFCHA,
     *         PRSTFCIN, PRSTFCFN, PRSTPLZO, PRSTTSAA, PRSTVLCT, PRSTMNSL, PRSTMNLD, ENTDCDGO]}
     */
    List<Object[]> datosPrestamos(List<Long> ids);

    /**
     * Agregados de los pagos vigentes ({@code NVL(PGPRANUL,0)=0}) de toda la vida del préstamo.
     *
     * @param ids códigos de préstamo
     * @return filas por préstamo con pagos {@code [PRSTCDGO, cantidad, MIN(PGPRFCHA), MAX(PGPRFCHA),
     *         SUM(CPPG), SUM(INPG), SUM(MRPG), SUM(INVP), SUM(DSGR), SUM(VLSI), SUM(SLOT), SUM(VLRR),
     *         cantidad de pagos MIGRACION o DEP]}
     */
    List<Object[]> agregadosPagos(List<Long> ids);

    /**
     * Agregados de las cuotas ({@code DTPR}).
     *
     * @param ids códigos de préstamo
     * @return filas por préstamo con cuotas {@code [PRSTCDGO, cuotas con DTPRESTD=4, cuotas con DTPRESTD=7,
     *         MAX(DTPRNMCT) de las DTPRESTD=4 (null si ninguna)]}
     */
    List<Object[]> agregadosCuotas(List<Long> ids);

    /**
     * Aportes usados por pago: los PGAP vigentes (PGAPIDST distinto de 0) de los PGPR vigentes, con la parte
     * del garante (la entidad del {@code APRT} es distinta del deudor del préstamo).
     *
     * @param ids códigos de préstamo
     * @return filas por PGPR con aportes {@code [PRSTCDGO, PGPRCDGO, EVPRCDGO (null si no hay evento),
     *         PGPRFCHA, SUM(PGAPVLRR), parte del garante]}
     */
    List<Object[]> aportesPorPago(List<Long> ids);

    /**
     * La operación final de cada préstamo (§4.1): la más reciente entre sus EVPR vigentes (sin los de
     * novación/reestructuración) y sus PGPR vigentes sin evento; fecha descendente y, a igual fecha, código
     * descendente. Los PGPR con evento no compiten: los representa su EVPR.
     *
     * @param ids códigos de préstamo
     * @return una fila por préstamo con operación {@code [PRSTCDGO, 'E' (evento) | 'P' (pago sin evento),
     *         código del EVPR o del PGPR, fecha]}
     */
    List<Object[]> operacionFinal(List<Long> ids);

    /**
     * El último evento de sustitución vigente ({@code EVPRESTD=1}, {@code NOVACION} o
     * {@code REESTRUCTURACION}) de cada préstamo. Para los estados 5/13 es la cancelación; en un 3/4 es una
     * inconsistencia que el reporte advierte.
     *
     * @param ids códigos de préstamo
     * @return una fila por préstamo con evento de sustitución {@code [PRSTCDGO, EVPRCDGO]}
     */
    List<Object[]> eventosSustitucion(List<Long> ids);

    /**
     * Detalle de los EVPR que son operación final.
     *
     * @param idsEvento códigos de EVPR
     * @return filas {@code [EVPRCDGO, EVPRTPOO, EVPRFCHA, EVPRVLRR, EVPRNMAS, EVPROBSR, EVPRUSAR]}
     */
    List<Object[]> detalleEventos(List<Long> idsEvento);

    /**
     * Detalle de los PGPR sin evento que son operación final, con el valor de la operación (suma de los PGPR
     * vigentes sin evento del mismo préstamo y la misma fecha) y la filial de la carga.
     *
     * @param idsPago códigos de PGPR
     * @return filas {@code [PGPRCDGO, PGPRTPOO, PGPRFCHA, valor de la operación, PGPRUSRG, PGPROBSR,
     *         CRARCDGO, FLLLCDGO de la carga (null si no hay carga), PGPRASNT]}
     */
    List<Object[]> detallePagosSinEvento(List<Long> idsPago);

    /**
     * El cobro de caja de cada evento ({@code DCBC.EVPRCDGO} → {@code CBCR}).
     *
     * @param idsEvento códigos de EVPR
     * @return filas {@code [EVPRCDGO, CBCRCDGO, CBCRTPOO, CBCRASN2 (asiento real del cobro)]}
     */
    List<Object[]> cobrosPorEvento(List<Long> idsEvento);

    /**
     * El acuerdo de condonación de cada evento ({@code ACCN.EVPRCDGO}).
     *
     * @param idsEvento códigos de EVPR
     * @return filas {@code [EVPRCDGO, ACCNCDGO, ACCNVLPG, ACCNVLCN, ACCNVLAP, ACCNVLDP, CBCRCDGO]}
     */
    List<Object[]> acuerdosPorEvento(List<Long> idsEvento);

    /**
     * Préstamos nuevos que sustituyen a los dados ({@code PRSTPROR} = original), sin los anulados.
     *
     * @param idsOriginal códigos de préstamo original
     * @return filas {@code [PRSTPROR, PRSTCDGO, PRSTIDAS, PRSTMNSL, PRSTVLSS]}
     */
    List<Object[]> nuevosPorOriginal(List<Long> idsOriginal);

    /**
     * Préstamos por código (el préstamo nuevo de la marca D-021, que no tiene {@code PRSTPROR}).
     *
     * @param ids códigos de préstamo
     * @return filas {@code [null, PRSTCDGO, PRSTIDAS, PRSTMNSL, PRSTVLSS]} (mismo orden que
     *         {@link #nuevosPorOriginal})
     */
    List<Object[]> prestamosPorCodigo(List<Long> ids);
}
