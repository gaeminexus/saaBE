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
	 * Para G44 ex-jubilados: SUM de cuota de cuotas en estado pagado (estado=4)
	 * cuya fechaVencimiento esté entre fechaInicio y fechaFin, agrupado por entidad (via prestamo).
	 * Retorna Object[]{Long codigoEntidad, Double sumaCuotas}.
	 */
	List<Object[]> selectSumaCuotasPagadasPorEntidad(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable;

	/**
	 * Para G48: Obtiene la menor cuota (MIN numeroCuota) de un préstamo con estado activo
	 * (2=activa, 3=emitida, 4=pagada, 5=en mora, 6=parcial) y fechaVencimiento <= fechaCorte.
	 * @param codigoPrestamo Código del préstamo
	 * @param fechaCorte Fecha de corte (último día del mes de ejecución)
	 * @return DetallePrestamo con menor numeroCuota que cumple condiciones, o null
	 */
	DetallePrestamo selectMenorCuotaActiva(Long codigoPrestamo, LocalDateTime fechaCorte) throws Throwable;

	/**
	 * Para G48 GRUPO 1: Obtiene todas las cuotas con fechaVencimiento dentro del mes de ejecución
	 * y estado diferente de 7 (canceladoAnticipado).
	 * @param codigoPrestamo Código del préstamo
	 * @param fechaInicio Primer día del mes de ejecución
	 * @param fechaFin Último día del mes de ejecución
	 * @return Lista de cuotas que cumplen condiciones
	 */
	List<DetallePrestamo> selectCuotasDelMes(Long codigoPrestamo, LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable;

	/**
	 * Para G48 GRUPO 2: Obtiene la menor cuota (MIN numeroCuota) con fechaVencimiento anterior
	 * al primer día del mes de ejecución y estado en (2=activa, 3=emitida, 5=en mora, 6=parcial).
	 * Solo aplica si el préstamo está en estado 8 (plazo vencido), 2 (vigente) u 11 (en mora).
	 * @param codigoPrestamo Código del préstamo
	 * @param fechaInicio Primer día del mes de ejecución
	 * @return DetallePrestamo con menor numeroCuota que cumple condiciones, o null
	 */
	DetallePrestamo selectMenorCuotaAnteriorAlMes(Long codigoPrestamo, LocalDateTime fechaInicio) throws Throwable;

	/**
	 * G48 GRUPO 1 GLOBAL: Todas las cuotas con fechaVencimiento en el mes (BETWEEN) y estado = 4 (pagada).
	 * Un query a BD devuelve todos los registros necesarios con el préstamo ya cargado.
	 */
	List<DetallePrestamo> selectCuotasDelMesGlobal(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable;

	/**
	 * G48 GRUPO 2 GLOBAL: La menor cuota por préstamo (MIN numeroCuota) con fechaVencimiento < fechaInicio,
	 * estado IN (2,3,5,6) y préstamo.estadoPrestamo IN (2,8,11).
	 * Usa subquery MIN para traer solo una cuota por préstamo.
	 */
	List<DetallePrestamo> selectMenorCuotaAnteriorAlMesGlobal(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable;

	/**
	 * Obtiene solo las cuotas NO pagadas ni canceladas anticipadamente de un préstamo.
	 * ✅ OPTIMIZACIÓN: Filtra directamente en la BD en lugar de traer todas las cuotas a memoria.
	 * @param codigoPrestamo Código del préstamo
	 * @return Lista de cuotas pendientes ordenadas por número de cuota
	 * @throws Throwable Si ocurre algún error
	 */
	List<DetallePrestamo> selectCuotasNoPagadasByPrestamo(Long codigoPrestamo) throws Throwable;

	/**
	 * Obtiene la mínima cuota NO pagada (por número de cuota) de un préstamo.
	 * ✅ OPTIMIZACIÓN: Usa subconsulta con MIN() para traer solo la primera cuota pendiente.
	 * @param codigoPrestamo Código del préstamo
	 * @return Lista con la cuota pendiente más antigua (por número de cuota), o lista vacía si no hay pendientes
	 * @throws Throwable Si ocurre algún error
	 */
	List<DetallePrestamo> selectMinCuotaNoPagadaByPrestamo(Long codigoPrestamo) throws Throwable;

	/**
	 * G49 GRUPO 1: Préstamos cuya cuota con mayor numeroCuota esté dentro del mes
	 * y sea de estado pagada (4). Indica que la última cuota del préstamo fue pagada en el mes → cancelación normal.
	 */
	List<DetallePrestamo> selectMaxCuotaPagadaDelMesGlobal(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable;

	/**
	 * G49 GRUPO 2: Préstamos en estadoPrestamo=4 (cancelado anticipado) cuya cuota
	 * con mayor numeroCuota pagada (estado=4) esté dentro del mes.
	 */
	List<DetallePrestamo> selectMaxCuotaPagadaCanceladoAnticipadoDelMesGlobal(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable;

	/**
	 * G48 GRUPO 2: Suma capital e interés de todas las cuotas desde la cuota incluida
	 * hasta la máxima cuota con fechaVencimiento <= fechaFin (último día del período).
	 * @param codigoPrestamo Código del préstamo
	 * @param numeroCuotaInicio Número de cuota desde donde iniciar la suma (incluida)
	 * @param fechaFin Último día del período de ejecución
	 * @return Object[]{Double sumaCapital, Double sumaInteres}
	 */
	Object[] selectSumaCapitalInteresGrupo2(Long codigoPrestamo, Double numeroCuotaInicio, LocalDateTime fechaFin) throws Throwable;

	/**
	 * Calculo de interés por mora: obtiene todas las cuotas desde la cuota de origen
	 * (inclusive) hasta la máxima cuota con fechaVencimiento <= fechaHasta.
	 * Retorna para cada cuota: capital, fechaVencimiento y tasa de interés nominal del préstamo.
	 * @param codigoCuotaOrigen Código (PK) de la cuota desde la que se comienza a sumar
	 * @param fechaHasta        Fecha hasta la que se considera el vencimiento (último día del período)
	 * @return Lista de Object[]{Double capital, LocalDateTime fechaVencimiento, Double interesNominal}
	 */
	List<Object[]> selectCuotasParaMora(Long codigoCuotaOrigen, LocalDateTime fechaHasta) throws Throwable;

}
