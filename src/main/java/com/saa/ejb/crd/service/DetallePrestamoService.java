package com.saa.ejb.crd.service;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.DetallePrestamo;

import jakarta.ejb.Local;

@Local
public interface DetallePrestamoService extends EntityService<DetallePrestamo> {
	
	List<DetallePrestamo> selectByMesAnio(Long mes, Long anio) throws Throwable;

	/** G44 ex-jubilados: SUM de cuotas pagadas (estado=4) con fechaVencimiento en el rango del mes, agrupado por entidad */
	List<Object[]> selectSumaCuotasPagadasPorEntidad(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable;
}
