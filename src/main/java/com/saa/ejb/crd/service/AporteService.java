package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.EstadoCuentaAportesDTO;
import com.saa.ejb.crd.service.dto.ResultadoJubilacion;
import com.saa.ejb.crd.service.dto.ResultadoRegistroAporte;
import com.saa.ejb.crd.service.dto.SolicitudRegistroAporte;
import com.saa.model.crd.Aporte;

import jakarta.ejb.Local;

@Local
public interface AporteService extends EntityService<Aporte> {

    // ========================================================================
    // Códigos de error de negocio (prefijo del mensaje de IncomeException)
    // ========================================================================

    /** 400 - Falta un parámetro obligatorio o viene malformado */
    String ERR_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
    /** 404 - La entidad (partícipe) no existe */
    String ERR_ENTIDAD_NO_ENCONTRADA = "ENTIDAD_NO_ENCONTRADA";
    /** 422 - El tipo de aporte no existe o no está vigente */
    String ERR_TIPO_APORTE_NO_VIGENTE = "TIPO_APORTE_NO_VIGENTE";
    /** 422 - El valor recibido no es válido */
    String ERR_VALOR_INVALIDO = "VALOR_INVALIDO";
    /** 422 - La fecha recibida no es válida */
    String ERR_FECHA_INVALIDA = "FECHA_INVALIDA";
    /** 404 - El aporte a reversar no existe */
    String ERR_APORTE_NO_ENCONTRADO = "APORTE_NO_ENCONTRADO";
    /** 422 - El partícipe no está en un estado desde el que se pueda procesar la jubilación */
    String ERR_ESTADO_NO_ELEGIBLE = "ESTADO_NO_ELEGIBLE";

    /**
     * Registra un pago de aportes recibido en ventanilla: genera para el partícipe un aporte
     * YA PAGADO del tipo indicado, en UNA transacción.
     *
     * Crea una fila POSITIVA en CRD.APRT con {@code valor = valorPagado = X} y
     * {@code saldo = 0}, estado PAGADA(4), más su {@code PagoAporte}. En el modelo vigente
     * (Fase 1 del plan de devengo de aportes, D1) toda fila nace pagada por construcción: no
     * hay abono posterior ni saldo pendiente que un proceso distinto pueda volver a tocar.
     *
     * El saldo disponible del partícipe sube de inmediato, porque el saldo ES la suma neta de
     * {@code APRTVLRR} (los consumos son filas negativas). Es la operación espejo del pago de
     * préstamo con aportes.
     *
     * @param solicitud Entidad, tipo de aporte, valor, usuario, observación y fecha
     * @return Datos del aporte creado y el saldo del tipo tras el registro
     * @throws Throwable                          Si ocurre un error
     * @throws com.saa.basico.util.IncomeException Ante cualquier fallo de validación (revierte todo)
     */
    ResultadoRegistroAporte registrarAporte(SolicitudRegistroAporte solicitud) throws Throwable;

    /**
     * Reversa un aporte registrado por error — genera una fila NEGATIVA (mismo mecanismo que
     * {@code DevolucionAporteServiceImpl#crearFilaNegativaDevolucion}, pero con
     * {@code tipoMovimiento = REVERSO(5)} en vez de {@code DEVOLUCION(3)}: no es plata que
     * vuelve al partícipe, es la corrección de un cobro que se anuló). El aporte original NO
     * se borra ni se marca: queda como evidencia de que se cobró y luego se corrigió, y el
     * saldo neto del partícipe (suma de {@code APRTVLRR}) vuelve solo al valor previo.
     *
     * Usado por {@code CobroCreditoService#anularCobro} para reversar las líneas de aporte de
     * un cobro {@code COBRO_MIXTO} ya PROCESADO — ver
     * {@code docs/logica-negocio/crd/API-COBROS-APROBACION-CONTABILIDAD.md}.
     *
     * @param idAporte  : Código del aporte original a reversar
     * @param usuario   : Usuario que ejecuta la reversa
     * @param motivo    : Motivo de la reversa (obligatorio, queda en la glosa)
     * @return          : Código del Aporte negativo generado
     * @throws Throwable : {@link #ERR_APORTE_NO_ENCONTRADO} si no existe;
     *                     {@link #ERR_PARAMETRO_INVALIDO} si ya es una fila de reverso (valor &lt; 0)
     */
    Long reversarAporte(Long idAporte, String usuario, String motivo) throws Throwable;

    /** G42 Grupo 1 — Rendimiento: SUM por entidad donde tipoAporte.estado=1 y codigoSBS='RE', fechaTransaccion <= fechaCorte */
    List<Object[]> selectSumaRendimientoPorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G42 Grupo 2 — Patronal: SUM por entidad donde tipoAporte.estado=1 y codigo IN (3,13,14), fechaTransaccion <= fechaCorte */
    List<Object[]> selectSumaPatronalPorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G42 Grupo 3 — Personal: SUM por entidad donde tipoAporte.estado=1, excluyendo grupos 1 y 2, fechaTransaccion <= fechaCorte */
    List<Object[]> selectSumaPersonalPorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G44 — Imposiciones acumuladas: COUNT de aportes con tipoAporte.codigo IN (9, 11), fechaTransaccion <= fechaCorte */
    List<Object[]> selectCountImposicionesJubilacionPorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G44 — Saldo de cuenta: SUM del campo valor de aportes con tipoAporte.codigo = 23, fechaTransaccion <= fechaCorte */
    List<Object[]> selectSumaSaldoCuentaJubilacionPorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G44 ex-jubilados — SUM de aportes tipo 23 en el rango del mes (BETWEEN fechaInicio AND fechaFin) */
    List<Object[]> selectSumaAportesTipo23EnRango(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable;

    /** G42 — Tipo de prestación: Obtiene los códigos de tipoAporte distintos (9, 11) por entidad, fechaTransaccion <= fechaCorte */
    List<Object[]> selectTiposAportePorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G40 — Suma total global de aportes con tipoAporte.codigo = tipoAporte, fechaTransaccion <= fechaCorte */
    Double selectSumaTotalPorTipoAporte(LocalDateTime fechaCorte, Long tipoAporte) throws Throwable;

    /**
     * CPRM — Suma de aportes agrupada por entidad Y tipo de aporte hasta fechaCorte.
     * Retorna Object[]{Long codigoEntidad, Long codigoTipoAporte, String nombreTipoAporte, Double suma}.
     */
    List<Object[]> selectSumaPorEntidadYTipoAporte(LocalDateTime fechaCorte) throws Throwable;

    /**
     * G43 — Imposiciones personales: tipoAporte.codigo IN (9, 11), valor &gt; 0, para una
     * entidad.
     *
     * <p><b>Cambio del 2026-08-27 (decisión del usuario, mismo criterio que G44):</b> cuenta
     * MESES DE DEVENGO distintos ({@code COUNT(DISTINCT} periodo efectivo{@code )}), no
     * filas. "Imposiciones" significa meses aportados — bajo el modelo de devengo filas ≠
     * meses: un partícipe puede tener varias filas del mismo mes (pago parcial completado
     * después, anticipos, ajustes) y una sola fila puede cubrir un mes distinto al de su
     * fecha de caja. Ver {@code PeriodoEfectivoAporteSql}.</p>
     */
    Long selectCountImposicionesPersonalesPorEntidad(Long codigoEntidad) throws Throwable;

    /**
     * G43 — Imposiciones patronales: tipoAporte.codigo IN (13, 14), valor &gt; 0, para una
     * entidad.
     *
     * <p>Mismo cambio del 2026-08-27 que {@link #selectCountImposicionesPersonalesPorEntidad}:
     * cuenta meses de devengo distintos, no filas.</p>
     */
    Long selectCountImposicionesPatronalesPorEntidad(Long codigoEntidad) throws Throwable;

    /**
     * G43 — SUM de aportes con valor < 0, tipoAporte.estado = 1,
     * dentro del mes de ejecucion para una entidad.
     * Retorna Double suma (negativa), o 0.0 si no hay registros.
     */
    Double selectSumaAportesNegativosMesPorEntidad(Long codigoEntidad,
            java.time.LocalDateTime fechaInicio,
            java.time.LocalDateTime fechaFin) throws Throwable;

    /**
     * Estado de cuenta de aportes por devengo (§4.2 del plan de devengo de aportes).
     * Agrupa por PERIODO EFECTIVO (ver {@code PeriodoEfectivoAporteSql}) y tipo de aporte;
     * "esperado" sale de {@link VigenciaContratoService#esperadoPorEntidad}. Los movimientos
     * sin periodo efectivo (histórico sin backfillear, retiros de saldo) van en un grupo con
     * {@code periodo = null} y {@code estado = "SIN PERIODO"} — nunca se esconden.
     *
     * <p>Una entidad sin ningún movimiento en el rango devuelve {@code periodos} vacío, NO
     * lanza (pedido 1: "sin aportes" no es un error).</p>
     *
     * @param idEntidad Código de la entidad (partícipe)
     * @param desde     Primer día del mes de devengo, inclusive
     * @param hasta     Primer día del mes de devengo, inclusive
     * @return Estado de cuenta
     * @throws Throwable                          Si ocurre un error
     * @throws com.saa.basico.util.IncomeException {@link #ERR_ENTIDAD_NO_ENCONTRADA} si la
     *                                             entidad no existe
     */
    EstadoCuentaAportesDTO estadoCuenta(Long idEntidad, LocalDate desde, LocalDate hasta) throws Throwable;

    /**
     * Procesa el traslado de jubilación (J2/J3 de LEVANTAMIENTO-TRES-FRENTES-2026-08-30.md
     * §4.b): mueve TODO el saldo remanente de cesantía (11) y jubilación (9) del partícipe a
     * pensión complementaria (23), y cambia su estado a JUBILADO COMPLEMENTARIO.
     *
     * <p><b>Este método es el paso 3 (traslado) + paso 5 (estado) del flujo de jubilación, NO
     * el flujo completo.</b> El cruce contra préstamos y la devolución en efectivo (paso 2,
     * opcionales) son decisiones previas que la pantalla ya ejecutó, ANTES de llamar acá,
     * usando {@code ProcesoPagoPrestamoService#pagarConAportes} y
     * {@code DevolucionAporteService#registrarDevolucion} — este método no los orquesta ni los
     * reimplementa. Se llama sobre lo que RESTA después de esas decisiones.</p>
     *
     * <p><b>Mecánica (J3):</b> por cada tipo con saldo &gt; $0.01 se crea una fila NEGATIVA en
     * CRD.APRT con glosa indicando que el partícipe se jubiló por el total de esa cuenta a la
     * fecha; si no tiene saldo en un tipo, no se crea fila para ese tipo (no un $0). Se crea
     * una fila POSITIVA en pensión complementaria por la suma de lo trasladado —
     * {@code null}/omitida si el total trasladado es $0 (el partícipe ya no tenía nada en
     * ninguna de las dos cuentas, p. ej. porque todo se cruzó/retiró en el paso 2). Las tres
     * (o menos) filas llevan {@code tipoMovimiento = CrdTipoMovimientoAporte.JUBILACION}.</p>
     *
     * <p><b>Estado:</b> siempre pasa a JUBILADO_COMPLEMENTARIO al final, incluso si el total
     * trasladado es $0 — el paso 5 del flujo no depende de que haya habido movimiento.</p>
     *
     * <p><b>Asiento:</b> §3.1 del levantamiento contable + plantilla alterno 29 (confirmada
     * contra la base 2026-08-31: 5 líneas, aux1 1/2 DEBE cesantía/jubilación, aux1 3/4/5 HABER
     * liquidación cesantía/liquidación jubilación/pensiones por pagar — posicionales, no del
     * catálogo semántico). Este método usa SOLO aux1 1, 2 y 5: el traslado va ÍNTEGRO a
     * pensión complementaria (decisión del usuario 2026-08-31 sobre los rendimientos 12/24,
     * ver más abajo), así que aux1 3/4 (liquidación diferenciada) no tienen monto que
     * registrar en este flujo. Gate de {@code contabilidadActiva()} primero.</p>
     *
     * <p><b>Rendimientos (tipos 12 y 24) — decisión CERRADA del usuario, 2026-08-31, NO
     * volver a proponerla:</b> este método traslada SOLO cesantía personal (11) y jubilación
     * personal (9), que es lo que soporta la plantilla 29 (aux1 1 y 2). Los rendimientos NO se
     * trasladan y quedan en sus cuentas — no es un hueco: la pantalla de jubilación ya permite
     * pedir devolución en efectivo desde CUALQUIER cuenta del partícipe (paso 2 del flujo), así
     * que un jubilado que quiera sus rendimientos los retira por ese camino, no por el traslado
     * a pensión complementaria. Registrado como decisión cerrada en
     * {@code docs/logica-negocio/ESTADO-CRD.md}.</p>
     *
     * @param idEntidad Código de la entidad (partícipe) a jubilar
     * @param usuario   Usuario que ejecuta el traslado (obligatorio, sella la auditoría de Entidad)
     * @param fecha     Fecha de negocio del traslado; {@code null} = hoy. No puede ser futura
     * @param idEmpresa Empresa contable sobre la que se genera el asiento de reclasificación.
     *                  Obligatorio (mismo criterio que el resto del motor de pagos desde la
     *                  Fase 0, API-EMPRESA-CONTABLE-CRD.md: nunca se infiere, viaja explícito)
     * @return Detalle de lo trasladado, los movimientos generados y el estado nuevo
     * @throws Throwable                          Si ocurre un error
     * @throws com.saa.basico.util.IncomeException {@link #ERR_ENTIDAD_NO_ENCONTRADA} si no
     *                                             existe; {@link #ERR_ESTADO_NO_ELEGIBLE} si
     *                                             el partícipe no está ACTIVO ni ACTIVO EN
     *                                             MORA (ya jubilado, cesante, etc.);
     *                                             {@link #ERR_FECHA_INVALIDA} si la fecha es
     *                                             futura
     */
    ResultadoJubilacion procesarJubilacion(Long idEntidad, String usuario, LocalDate fecha, Long idEmpresa)
            throws Throwable;
}
