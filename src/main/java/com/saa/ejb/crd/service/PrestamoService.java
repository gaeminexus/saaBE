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

	/**
	 * Aplica un abono a capital al préstamo y recalcula la tabla de amortización.
	 * @param idPrestamo ID del préstamo
	 * @param valorAbono Valor del abono a capital
	 * @param opcionRecalculo 1=Mantener plazo/reducir cuota, 2=Reducir plazo/mantener cuota
	 * @return Préstamo con la tabla de amortización recalculada
	 * @throws Throwable Si ocurre algún error durante el proceso
	 */
	Prestamo aplicarAbonoCapital(Long idPrestamo, Double valorAbono, Integer opcionRecalculo) throws Throwable;

}
