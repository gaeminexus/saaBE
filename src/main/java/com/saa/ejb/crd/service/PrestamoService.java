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
	Prestamo cargarTablaAmortizacionDesdeExcel(Long idPrestamo, java.io.InputStream archivoExcel) throws Throwable;

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
	 * Cuenta los préstamos de una entidad en estado vigente (2), en mora (8) o plazo vencido (11).
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

}
