package com.saa.ejb.crd.service;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;


@Local
public interface PrestamoService extends EntityService<Prestamo>{
	
	/**
	 * Genera la tabla de amortización para un préstamo.
	 * @param idPrestamo ID del préstamo
	 * @return Préstamo con la tabla de amortización generada
	 * @throws Throwable Si ocurre algún error durante la generación
	 */
	Prestamo generarTablaAmortizacion(Long idPrestamo) throws Throwable;

}
