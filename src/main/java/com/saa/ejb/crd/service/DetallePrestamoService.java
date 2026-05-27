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

	/** G48: Obtiene la menor cuota activa (estados 2,3,4,5,6) con fechaVencimiento <= fechaCorte */
	DetallePrestamo selectMenorCuotaActiva(Long codigoPrestamo, LocalDateTime fechaCorte) throws Throwable;

	/** G48 GRUPO 1: Obtiene cuotas con fechaVencimiento en el mes y estado != 7 (canceladoAnticipado) */
	List<DetallePrestamo> selectCuotasDelMes(Long codigoPrestamo, LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable;

	/** G48 GRUPO 2: Obtiene menor cuota anterior al mes con estado 2,3,5,6 si préstamo está en estado 2,8,11 */
	DetallePrestamo selectMenorCuotaAnteriorAlMes(Long codigoPrestamo, LocalDateTime fechaInicio) throws Throwable;

	/** G48 GRUPO 1 GLOBAL: Todas las cuotas del mes (estado!=7) de todos los préstamos en un solo query */
	List<DetallePrestamo> selectCuotasDelMesGlobal(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable;

	/** G48 GRUPO 2 GLOBAL: La menor cuota por préstamo anterior al mes, estado 2,3,5,6, préstamo 2,8,11 */
	List<DetallePrestamo> selectMenorCuotaAnteriorAlMesGlobal(LocalDateTime fechaInicio) throws Throwable;

	/** G49 GRUPO 1: Préstamos cuya cuota con mayor numeroCuota esté en el mes y sea pagada (estado=4). */
	List<DetallePrestamo> selectMaxCuotaPagadaDelMesGlobal(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable;

	/** G49 GRUPO 2: Préstamos en estadoPrestamo=4 (cancelado anticipado) cuya máxima cuota pagada esté en el mes. */
	List<DetallePrestamo> selectMaxCuotaPagadaCanceladoAnticipadoDelMesGlobal(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Throwable;

	/** G48 GRUPO 2: Suma capital e interés de todas las cuotas desde la cuota incluida hasta la máxima cuota con fechaVencimiento <= fechaFin. */
	Object[] selectSumaCapitalInteresGrupo2(Long codigoPrestamo, Double numeroCuotaInicio, LocalDateTime fechaFin) throws Throwable;

	/**
	 * Calcula el interés por mora acumulado según normativa ecuatoriana (tasa mora = interesNominal * 1.5, base 360 días).
	 * Suma el interés por mora de todas las cuotas desde la cuota indicada hasta la máxima cuota
	 * con fechaVencimiento <= fechaHasta.
	 * Fórmula por cuota: capital × (interesNominal × 1.5 / 360) × diasMora
	 * donde diasMora = días entre fechaVencimiento de la cuota y fechaHasta (mínimo 0).
	 * @param codigoCuotaOrigen Código (PK) de la cuota de inicio (inclusive)
	 * @param fechaHasta        Último día del período hasta el que se calcula la mora
	 * @return Sumatoria del interés por mora de todas las cuotas en el rango
	 */
	Double calcularInteresMora(Long codigoCuotaOrigen, LocalDateTime fechaHasta) throws Throwable;
}
