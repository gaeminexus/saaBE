package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.ejb.crd.service.dto.FiltroDetalleDistribucionBanda;
import com.saa.ejb.crd.service.dto.OrigenDistribucionBandaResumen;
import com.saa.ejb.crd.service.dto.ResultadoClasificacionBanda;
import com.saa.ejb.crd.service.dto.ResultadoCuadreDistribucionBanda;
import com.saa.ejb.crd.service.dto.ResultadoDetalleDistribucionBanda;
import com.saa.ejb.crd.service.dto.ResultadoDiferenciaDistribucionBanda;
import com.saa.model.crd.PagoPrestamo;

import jakarta.ejb.Local;

/**
 * Auditoría de distribución en bandas — PLAN-AUDITORIA-BANDAS.md / API-AUDITORIA-BANDAS.md.
 *
 * <b>Se escribe donde se APLICA el pago, no donde se arma el asiento</b> — la banda es un
 * dato de cartera, no contable. {@link #registrarDistribucionCargaPetro} corre SIEMPRE,
 * exista o no contabilidad activa; el enganche con CNT ({@code idAsiento}) se completa
 * después, vía {@link #actualizarAsiento}, solo cuando el asiento efectivamente se genera.
 */
@Local
public interface DistribucionBandaService {

    /** 404 - No hay distribución registrada para ese origen */
    String ERR_ORIGEN_NO_ENCONTRADO = "ORIGEN_NO_ENCONTRADO";
    /** 422 - {@code origen} fuera del vocabulario de {@link com.saa.rubros.DsbnOrigen} */
    String ERR_ORIGEN_INVALIDO = "ORIGEN_INVALIDO";

    /**
     * Clasifica y persiste la distribución en bandas de los pagos de préstamo de una carga
     * Petro — capital (con banda), interés ordinario, mora, interés vencido, desgravamen y
     * seguro de incendio, uno por {@code PagoPrestamo}. Idempotente: reemplaza cualquier fila
     * previa de este {@code (CARGA_PETRO, idCarga)} en vez de duplicar (reprocesar la carga
     * ya no acumula historial falso).
     *
     * <b>No incluye aportes todavía</b> (cesantía/jubilación de la carga) — alcance de esta
     * entrega, ver PLAN-AUDITORIA-BANDAS.md.
     *
     * <b>Corrección 2026-09-02:</b> devuelve la clasificación de banda de CAPITAL que resolvió
     * para cada pago (código de {@code PagoPrestamo} → resultado), para que
     * {@code CobroPetroContableServiceImpl.contabilizarAplicacion} la reutilice al armar sus
     * propias líneas del asiento en vez de volver a llamar a
     * {@code ClasificadorBandaService.clasificar} por segunda vez para los mismos pagos —
     * antes de esto, un mismo proceso de carga (20+ minutos) clasificaba cada pago DOS veces.
     * Solo trae entrada para los pagos con capital &gt; 0 (los demás no tienen banda).
     *
     * @param pagos     Pagos VIGENTES de la carga (mismos que consume
     *                  {@code CobroPetroContableService.contabilizarAplicacion})
     * @throws Throwable si falta un dato obligatorio para clasificar (producto, tipo de
     *                    préstamo cuando hace falta, fecha de vencimiento de la cuota)
     */
    Map<Long, ResultadoClasificacionBanda> registrarDistribucionCargaPetro(Long idCarga, Long idEmpresa,
            List<PagoPrestamo> pagos, String usuario) throws Throwable;

    /**
     * Mismo núcleo que {@link #registrarDistribucionCargaPetro} (clasifica CAPITAL por banda y
     * escribe interés ordinario/mora/interés vencido/seguro desgravamen/seguro incendio, uno
     * por {@code PagoPrestamo}) para los orígenes NO-Petro — PLAN-AUDITORIA-BANDAS.md §9:
     * {@code COBRO_INDIVIDUAL} (CRD.CBCR), {@code EVENTO_PRESTAMO} (CRD.EVPR) y
     * {@code PAGO_PENSION} (CRD.PGPC). No incluye aportes (eso es específico de Petro, donde sí
     * hay una fuente propia — {@code CRD.APRT} filtrado por carga).
     *
     * <b>SIN idempotencia propia</b> — a diferencia de {@code registrarDistribucionCargaPetro},
     * este método NO borra filas previas del origen antes de escribir. Un mismo
     * {@code (origen, idOrigen)} puede necesitar más de una llamada dentro del mismo proceso
     * (ej. un cobro con varias líneas, cada una con su propio {@code EventoPrestamo} y su
     * propia lista de pagos) — borrar en cada llamada destruiría lo que ya escribió la
     * anterior. El llamador invoca {@link #eliminarDistribucion} UNA sola vez, antes de la
     * primera llamada de la operación, si el origen necesita semántica de reemplazo.
     *
     * @param pagos Pagos de préstamo YA APLICADOS que corresponden a este origen —
     *              habitualmente {@code PagoPrestamoDaoService#selectByEvento(idEvento)}
     * @throws Throwable si falta un dato obligatorio para clasificar (producto, tipo de
     *                    préstamo cuando hace falta, fecha de vencimiento de la cuota)
     */
    Map<Long, ResultadoClasificacionBanda> registrarDistribucionPorPagos(String origen, Long idOrigen,
            Long idEmpresa, List<PagoPrestamo> pagos, String usuario) throws Throwable;

    /**
     * Borra las filas previas de un {@code (origen, idOrigen)} — idempotencia EXPLÍCITA para
     * los llamadores de {@link #registrarDistribucionPorPagos}, que no la incluye (ver su
     * javadoc). {@code registrarDistribucionCargaPetro} sigue haciendo esto por su cuenta, sin
     * cambios.
     *
     * @return cuántas filas borró
     */
    int eliminarDistribucion(String origen, Long idOrigen) throws Throwable;

    /**
     * Estampa el asiento en todas las filas de un origen — se llama DESPUÉS de que
     * contabilidad genera el suyo. No hace nada si {@code idAsiento} es null.
     */
    void actualizarAsiento(String origen, Long idOrigen, Long idAsiento) throws Throwable;

    /**
     * Reescribe la distribución en bandas de una carga Petro YA PROCESADA, sin reprocesar nada
     * — hallazgo 2026-09-03: el WAR desplegado cuando se procesó la carga 449 era anterior a
     * {@code 500079b} (el commit que agregó el registro de aportes en CRD.DSBN), así que su
     * distribución quedó sin esas filas. Reprocesar la carga entera para poblar una tabla de
     * auditoría —20+ minutos, regenerando asientos en producción— no es aceptable.
     *
     * <p><b>SOLO reescribe {@code CRD.DSBN}.</b> Relee los {@code PagoPrestamo} y {@code Aporte}
     * YA EXISTENTES de la carga (no toca asientos, pagos, aportes ni cuotas) y los vuelve a
     * pasar por {@link #registrarDistribucionCargaPetro} — el mismo camino idempotente
     * (borra y reescribe por origen) que corre durante el procesamiento normal. Correrlo dos
     * veces seguidas da el mismo resultado.</p>
     *
     * <p>El {@code idAsiento} de las filas se resuelve del asiento VIGENTE de sub-proceso
     * APLICACIÓN ({@code AsientoCargaPetroDaoService#selectVigenteByCargaYSubProceso}, CRD.ANCP)
     * — el mismo asiento que las filas originales tenían. Si la carga no tiene asiento de
     * aplicación vigente (contabilidad estaba inactiva cuando se procesó), queda {@code null},
     * igual que en el procesamiento normal.</p>
     *
     * <p>Solo {@code CARGA_PETRO}: es el único origen con una fuente de pagos/aportes ya
     * resuelta e idempotente para releer así.</p>
     *
     * @param idCarga : Código de la carga (CRD.CRAR), YA PROCESADA (estado 3)
     * @param usuario : Usuario que ejecuta el recálculo, para auditoría (DSBNUSAR)
     * @return : La clasificación de banda de CAPITAL que se volvió a resolver, por pago —
     *           mismo contrato que {@link #registrarDistribucionCargaPetro}
     * @throws Throwable : {@code IncomeException} si falta {@code idCarga}/{@code usuario}, si
     *                     la carga no existe, o si no se puede resolver su empresa contable
     *                     (sin transferencias vigentes)
     */
    Map<Long, ResultadoClasificacionBanda> recalcularDistribucionCargaPetro(Long idCarga, String usuario)
            throws Throwable;

    /**
     * El encabezado de cuadre de un origen — {@code GET /rest/dsbn/cuadre}.
     *
     * @throws Throwable {@code IncomeException} {@link #ERR_ORIGEN_NO_ENCONTRADO} si no hay
     *                    filas para ese origen, {@link #ERR_ORIGEN_INVALIDO} si {@code origen}
     *                    no es uno de {@link com.saa.rubros.DsbnOrigen}
     */
    ResultadoCuadreDistribucionBanda obtenerCuadre(String origen, Long idOrigen) throws Throwable;

    /**
     * «¿De quién es la diferencia?» — {@code GET /rest/dsbn/diferencia}, API-AUDITORIA-BANDAS.md
     * §4. El cuadre ya dice QUE hay diferencia; esto dice DE QUIÉN, por partícipe: descontado
     * vs. aplicado (préstamos + aportes), con el desglose manual/automático que dice POR DÓNDE
     * entró (usando el prefijo estable de {@code PGPROBSR}, commit {@code e7b76c8}).
     *
     * <b>Solo {@code CARGA_PETRO}</b>: es el único origen con una fuente independiente de
     * "descontado" (CRD.PXCA) contra la cual comparar — mismo motivo por el que su cuadre es
     * {@code null} para los demás orígenes.
     *
     * @throws Throwable {@code IncomeException} si {@code origen} no es {@code CARGA_PETRO}, si
     *                    falta {@code idOrigen}, o si {@code origen} no es válido
     */
    ResultadoDiferenciaDistribucionBanda obtenerDiferencia(String origen, Long idOrigen) throws Throwable;

    /**
     * El detalle filtrable y paginado — {@code POST /rest/dsbn/detalle}. Un origen sin filas
     * devuelve listas vacías con 200, NUNCA 404 ni una excepción: "no hay datos" es un
     * resultado legítimo del filtro, no un fallo.
     */
    ResultadoDetalleDistribucionBanda obtenerDetalle(FiltroDetalleDistribucionBanda filtro) throws Throwable;

    /** Orígenes con distribución registrada, del más reciente al más antiguo — {@code GET /rest/dsbn/origenes}. */
    List<OrigenDistribucionBandaResumen> listarOrigenes(String origen, LocalDate fechaDesde,
            LocalDate fechaHasta, Integer limite) throws Throwable;
}
