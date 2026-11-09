package com.saa.ejb.crd.service;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;


@Local
public interface PrestamoService extends EntityService<Prestamo>{
	
	/**
	 * Genera la tabla de amortización para un préstamo.
	 *
	 * N1 (REVISION-MOTOR-ANTES-DE-OTORGAMIENTO.md, decisión U2): no es idempotente por
	 * accidente. Si el préstamo ya tiene cuotas, se rechaza salvo que {@code regenerar} sea
	 * {@code true}, y aun regenerando, cualquier cuota con pagos vigentes bloquea la operación
	 * entera — una cuota pagada no se toca nunca.
	 *
	 * @param idPrestamo ID del préstamo
	 * @param tieneCuotaCero Indica si la tabla tiene período de gracia (1=sí, 0=no)
	 * @param regenerar Si el préstamo ya tiene tabla, hay que pedir la regeneración explícita
	 *                  con {@code true}; solo procede si ninguna cuota existente tiene pagos.
	 * @return Préstamo con la tabla de amortización generada y actualizada
	 * @throws Throwable Si ocurre algún error durante la generación
	 */
	Prestamo generarTablaAmortizacion(Long idPrestamo, Long tieneCuotaCero, boolean regenerar) throws Throwable;

	/**
	 * Carga la tabla de amortización desde un archivo Excel.
	 * @param idPrestamo ID del préstamo
	 * @param archivoExcel InputStream del archivo Excel
	 * @return Préstamo con la tabla de amortización cargada y actualizada
	 * @throws Throwable Si ocurre algún error durante la carga
	 */
	Prestamo cargarTablaAmortizacionDesdeExcel(Long idPrestamo, java.io.InputStream archivoExcel, String usuario)
			throws Throwable;

	// NOTA: el abono a capital vive ahora en com.saa.ejb.crd.service.AbonoCapitalPrestamoService
	// (simular + aplicar, con re-amortización, historización en CRD.HDTP y EventoPrestamo).
	// Ver docs/logica-negocio/crd/ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §7.3.

	/**
	 * Recalcula los campos derivados del préstamo (valorCuota, fechaFin, totalCapital,
	 * totalInteres, totalPrestamo, tasaNominal y tasaEfectiva) a partir de la tabla de
	 * amortización VIVA en CRD.DTPR, y lo persiste.
	 *
	 * Expone la lógica que ya usaban generarTablaAmortizacion y la carga desde Excel, para que
	 * el abono a capital y su reverso la reutilicen tras re-amortizar (§7.3 paso 7).
	 *
	 * @param idPrestamo ID del préstamo
	 * @return Préstamo con los campos actualizados
	 * @throws Throwable Si ocurre algún error
	 */
	Prestamo actualizarCamposDesdeTabla(Long idPrestamo) throws Throwable;

	/**
	 * Busca préstamos cuya fecha esté entre fechaInicio y fechaFin.
	 */
	java.util.List<Prestamo> selectByRangoFechas(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable;

	/** Retorna todos los préstamos con un estadoPrestamo específico */
	java.util.List<Prestamo> selectByEstado(Long estado) throws Throwable;

	/**
	 * Cuenta los préstamos de una entidad en estado vigente (2), de plazo vencido (8) o en mora (11).
	 * Corregido 2026-09-30 (API-PASE-A-PLAZO-VENCIDO.md §5): el javadoc tenía los códigos al
	 * revés — manda {@code com.saa.rubros.EstadoPrestamo}.
	 */
	long countVigentesMoraVencidosByEntidad(Long codigoEntidad) throws Throwable;

	/**
	 * Cuenta los préstamos de una entidad cuya última cuota (MAX numeroCuota)
	 * tenga fechaVencimiento dentro del período indicado.
	 * Cubre cancelaciones normales y anticipadas en el período.
	 */
	long countPrestamosConUltimaCuotaEnPeriodoByEntidad(Long codigoEntidad, java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable;

	/**
	 * Aprueba un préstamo (PLAN-CICLO-OTORGAMIENTO.md §3, regla 4): GENERADO (1) → VIGENTE (2).
	 * Exige que el préstamo tenga tabla de amortización generada — sin tabla no hay nada que
	 * aprobar. A partir de esta transición la tabla queda congelada (U4): no se regenera más.
	 *
	 * <p>Desde PLAN-DESEMBOLSO-PRESTAMO.md §5 (2026-09-01), aprobar además: registra la orden
	 * de pago del desembolso en CXP ({@code idPagoProgramado}) y escribe el asiento de entrega
	 * con la plantilla del producto — <b>CXP primero</b>: si la orden de pago falla, no se
	 * genera asiento ni se toca el estado del préstamo.
	 *
	 * @param idPrestamo ID del préstamo
	 * @param usuario Usuario que aprueba, se estampa en usuarioAprobacion
	 * @param observacion Si viene, se concatena a la observación existente (no la pisa)
	 * @param idEmpresa Empresa contable del asiento de entrega. Obligatorio
	 * @param idUsuario Quien registra la orden de pago del desembolso en CXP. Obligatorio
	 * @return Préstamo actualizado, ya VIGENTE, con {@code idPagoProgramado} lleno
	 * @throws Throwable Si el préstamo no existe, no está en GENERADO, no tiene cuotas, faltan
	 *                    idEmpresa/idUsuario, el producto no tiene plantilla de entrega, o la
	 *                    orden de pago no se pudo registrar en CXP
	 */
	Prestamo aprobar(Long idPrestamo, String usuario, String observacion, Long idEmpresa, Long idUsuario)
			throws Throwable;

	/**
	 * Igual que {@link #aprobar(Long, String, String, Long, Long)} con una <b>fecha contable</b>
	 * explícita para re-procesar meses pasados (C6, docs/logica-negocio/cnt/FECHA-CONTABLE-REPROCESO.md).
	 *
	 * @param fechaContable Fecha programada de la orden de pago del desembolso en CXP. {@code null}
	 *                      = hoy (comportamiento de siempre). No puede ser futura. El asiento de
	 *                      entrega sigue fechado con {@code PRSTFCIN}, que ya es fecha de negocio.
	 */
	/**
	 * Igual que {@code aprobar} con fecha contable, pero con la APROBACIÓN POR NIVELES (B4,
	 * DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md): devuelve si se completó o solo se aprobó un nivel (el
	 * préstamo sigue GENERADO). Sin reglas de niveles activas es el aprobar de siempre, en un paso. El usuario que aprueba
	 * es {@code usuario} (validado; su código sale del nombre, U-3): tiene que ser de un usuario del nivel pendiente.
	 */
	com.saa.ejb.crd.service.dto.ResultadoAprobacionPrestamo aprobarConNiveles(Long idPrestamo, String usuario,
			String observacion, Long idEmpresa, Long idUsuario, java.time.LocalDate fechaContable) throws Throwable;

	/**
	 * Hook del desembolso (DISENO-ESTADO-APROBADO-VIGENTE-AL-DESEMBOLSO.md): lo llama tesorería (CXP) al confirmarse el pago
	 * de origen {@code CRD_DESEMBOLSO_PRESTAMO}, en la MISMA transacción y antes del asiento del pago. Un préstamo en
	 * APROBADO (14) pasa a VIGENTE (2): asiento de entrega fechado con {@code fechaDesembolso} (o, en novación, asiento de
	 * sustitución + cancelación del original), y {@code PRSTFCAC}/{@code PRSTUSAC} guardan fecha y usuario del desembolso.
	 * <b>Idempotente</b>: si el préstamo no está en 14 (ya VIGENTE, aprobado antes del cambio) no hace nada. El dinero ya
	 * salió, así que NUNCA frena la confirmación por una diferencia de negocio.
	 *
	 * @return una advertencia para quien confirma el pago (p. ej. el VS cambió o el original ya no es novable), o
	 *         {@code null} si no hay nada que avisar
	 */
	/**
	 * Sección «Solicitudes» del dashboard (recibidas, en trámite, aprobadas, desembolsadas, rechazadas y anuladas, con
	 * cantidad y monto) entre {@code desde} y {@code hasta} (inclusive; por defecto el mes en curso), con filial y producto
	 * opcionales. Solo lectura. Ver {@code ResumenSolicitudesPrestamo}.
	 */
	com.saa.ejb.crd.service.dto.ResumenSolicitudesPrestamo resumenSolicitudes(java.time.LocalDate desde,
			java.time.LocalDate hasta, Long idFilial, Long idProducto) throws Throwable;

	String activarPorDesembolso(Long idPrestamo, java.time.LocalDate fechaDesembolso, Long idEmpresa, String usuario)
			throws Throwable;

	/**
	 * Reverso de un pago de desembolso CONFIRMADO: un préstamo activado por desembolso, sin pagos aplicados y que no es
	 * novación, revierte su asiento de entrega y vuelve a APROBADO. Novación o con pagos: lanza (caso manual). Un préstamo
	 * que no fue activado por desembolso (aprobado antes del cambio) no se toca.
	 */
	void revertirDesembolso(Long idPrestamo, String motivo, java.time.LocalDate fechaReverso, String usuario)
			throws Throwable;

	Prestamo aprobar(Long idPrestamo, String usuario, String observacion, Long idEmpresa, Long idUsuario,
			java.time.LocalDate fechaContable) throws Throwable;

	/**
	 * C15 / D-018 (H6): contabiliza el desembolso de un préstamo ya aprobado sin asiento de entrega:
	 * asiento de entrega + orden de pago confirmada con su egreso, todo con la fecha del desembolso real
	 * del extracto. Idempotente por préstamo. Ver CIERRE-CARTERA-A-FECHA-DE-CORTE.md §H6.
	 */
	java.util.Map<String, Object> contabilizarDesembolsoHistorico(Long idPrestamo, Long idEmpresa, Long idUsuario,
			String usuario, java.time.LocalDate fechaContable, Long idCuentaBancaria, Double valor, String referencia)
			throws Throwable;

	/**
	 * C16 (D-021): desembolso NETO. {@code valor} es el neto transferido por el banco; si el préstamo
	 * canceló otro del mismo partícipe ({@code idPrestamoCancelado}), registra esa precancelación con el
	 * motor pagada con el dinero del préstamo nuevo; {@code otrosDescuentos} opcionales. Regla: bruto =
	 * neto + capital + interés + seguro + otros, al centavo. Ver DESEMBOLSO-NETO-CON-PRECANCELACION.md.
	 */
	java.util.Map<String, Object> contabilizarDesembolsoHistorico(Long idPrestamo, Long idEmpresa, Long idUsuario,
			String usuario, java.time.LocalDate fechaContable, Long idCuentaBancaria, Double valor, String referencia,
			Long idPrestamoCancelado, Double capitalCancelado, Double interesCancelado, Double seguroCancelado,
			java.util.List<com.saa.ejb.crd.service.dto.DescuentoDesembolso> otrosDescuentos) throws Throwable;

	/**
	 * Rechaza un préstamo (PLAN-CICLO-OTORGAMIENTO.md §3, regla 5): PENDIENTE_DE_APROBACION (6)
	 * o GENERADO (1) → RECHAZADO (7). NO borra la tabla de amortización: queda como evidencia de
	 * qué se le ofreció al socio, y es inerte para el proceso de mora.
	 *
	 * @param idPrestamo ID del préstamo
	 * @param usuario Usuario que rechaza, se estampa en usuarioRechazo
	 * @param observacion Si viene, se concatena a la observación existente (no la pisa)
	 * @return Préstamo actualizado, ya RECHAZADO
	 * @throws Throwable Si el préstamo no existe o no está en 6 ni en 1
	 */
	Prestamo rechazar(Long idPrestamo, String usuario, String observacion) throws Throwable;

	/**
	 * Saldo vigente de un lote de préstamos, calculado desde sus cuotas pendientes con la
	 * misma lógica del motor de pagos (docs/logica-negocio/crd/API-SALDOS-PRESTAMO.md).
	 * Reemplaza la lectura de {@code Prestamo.saldoTotal}/{@code saldoCapital}, columnas
	 * muertas que ninguna línea del backend escribe.
	 *
	 * Por cada código: recorre {@code selectCuotasPendientesByPrestamoOrdenadas} y suma, por
	 * cuota, {@code MotorPagoPrestamoService.calcularSaldosCuota} (la variante PURA, sin la
	 * autocorrección que persiste estado). Un préstamo que falle al calcular no aborta el
	 * lote: se omite de la respuesta y se registra en el log.
	 *
	 * <p>Un código que no existe en {@code CRD.PRST} no aparece en la respuesta (no es
	 * error). Un préstamo EXISTENTE sin cuotas pendientes — por ejemplo, en estado terminal
	 * (cancelado) — sí aparece, con sus tres campos en 0: no se filtra por estado del
	 * préstamo, la pantalla ya lo muestra.</p>
	 *
	 * @param codigosPrestamo Códigos de préstamo a calcular, máximo 500
	 * @return Un resumen por cada préstamo que se pudo calcular, en cualquier orden
	 * @throws Throwable Si la lista viene nula, vacía o supera los 500 códigos
	 */
	java.util.List<com.saa.ejb.crd.service.dto.SaldoPrestamoResumen> calcularSaldosEnLote(
			java.util.List<Long> codigosPrestamo) throws Throwable;


    /**
     * Motivo por el que un préstamo NO se puede anular, o {@code null} si se puede. Es la ÚNICA
     * fuente de las reglas AN1, AN2, AN4 (asiento), AN5, AN6 y la regla de la orden de CXP (AN3):
     * la usan {@code GET /prst/{id}/puedeAnular} y {@link #anular}, para que nunca discrepen.
     * Solo lectura; no lanza por una regla de negocio.
     * DISENO-ANULACION-PRESTAMO-E-IMPRESOS.md.
     */
    String motivoNoAnulable(Prestamo prestamo) throws Throwable;

    /**
     * Anula un préstamo otorgado (VIGENTE o EN_MORA sin ningún pago): anula la orden de desembolso
     * en CXP, REVERSA el asiento de entrega con la fecha de hoy, pasa las cuotas a ANULADA y el
     * préstamo a ANULADO (12). Todo en una transacción (AN10): si algo falla, no queda nada a medias.
     *
     * @throws IncomeException si no se puede, con el motivo
     */
    Prestamo anular(Long idPrestamo, String usuario, String motivo, Long idEmpresa, Long idUsuario)
            throws Throwable;
}
