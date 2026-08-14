package com.saa.ejb.crd.service;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;


@Local
public interface PrestamoService extends EntityService<Prestamo>{
	
	/**
	 * Genera la tabla de amortización para un préstamo.
	 * @param idPrestamo ID del préstamo
	 * @param tieneCuotaCero Indica si la tabla tiene período de gracia (1=sí, 0=no)
	 * @return Préstamo con la tabla de amortización generada y actualizada
	 * @throws Throwable Si ocurre algún error durante la generación
	 */
	Prestamo generarTablaAmortizacion(Long idPrestamo, Long tieneCuotaCero) throws Throwable;

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

}
