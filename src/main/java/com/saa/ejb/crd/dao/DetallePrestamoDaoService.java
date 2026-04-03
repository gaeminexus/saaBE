package com.saa.ejb.crd.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.DetallePrestamo;

import jakarta.ejb.Local;

@Local
public interface DetallePrestamoDaoService extends EntityDao<DetallePrestamo> {
	
	/**
	 * Consulta los detalles de préstamo en un rango de fechas.
	 * @param fechaDesde: 	Fecha de inicio del rango.
	 * @param fechaHasta: 	Fecha de fin del rango.
	 * @return: 			Lista de DetallePrestamo que caen dentro del rango de fechas especificado.
	 * @throws Throwable: 	Si ocurre algún error durante la consulta.
	 */
	List<DetallePrestamo> selectByRangoFechas(LocalDateTime fechaDesde, LocalDateTime fechaHasta) throws Throwable;

	/**
	 * Busca la cuota de un préstamo en un mes/año específico.
	 * Usado en FASE 2 para encontrar la cuota correspondiente a un registro del archivo.
	 * @param codigoPrestamo Código del préstamo
	 * @param mes Mes de vencimiento (1-12)
	 * @param anio Año de vencimiento
	 * @return Lista de cuotas que vencen en ese mes/año
	 * @throws Throwable Si ocurre algún error
	 */
	List<DetallePrestamo> selectByPrestamoYMesAnio(Long codigoPrestamo, Integer mes, Integer anio) throws Throwable;

	/**
	 * Obtiene todas las cuotas (tabla de amortización) de un préstamo.
	 * @param codigoPrestamo Código del préstamo
	 * @return Lista de todas las cuotas del préstamo ordenadas por número de cuota
	 * @throws Throwable Si ocurre algún error
	 */
	List<DetallePrestamo> selectByPrestamo(Long codigoPrestamo) throws Throwable;

	/**
	 * Obtiene solo las cuotas NO pagadas ni canceladas anticipadamente de un préstamo.
	 * ✅ OPTIMIZACIÓN: Filtra directamente en la BD en lugar de traer todas las cuotas a memoria.
	 * @param codigoPrestamo Código del préstamo
	 * @return Lista de cuotas pendientes ordenadas por número de cuota
	 * @throws Throwable Si ocurre algún error
	 */
	List<DetallePrestamo> selectCuotasNoPagadasByPrestamo(Long codigoPrestamo) throws Throwable;

}
