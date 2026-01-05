package com.saa.ejb.credito.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.DetallePrestamo;

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

}
