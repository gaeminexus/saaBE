/**
 * Copyright � Gaemi Soft C�a. Ltda. , 2011 Reservados todos los derechos  
 * Jos� Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa est� protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducci�n o la distribuci�n no autorizadas de este programa, o de cualquier parte del mismo, 
 * est� penada por la ley y con severas sanciones civiles y penales, y ser� objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad  
 * con los t�rminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.basico.ejbImpl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.basico.ejb.FechaService;
import com.saa.rubros.FormatoFecha;
import com.saa.rubros.Rubros;

/**
 * @author GaemiSoft.
 *         <p>
 *         Implementaci�n de la interfaz FechaService.
 *         </p>
 */
@Stateless
public class FechaServiceImpl implements FechaService {

	@EJB
	private DetalleRubroDaoService detalleRubroDaoService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.service.FechaService#sumaRestaDias(java.util
	 * .Date, int)
	 */
	public Date sumaRestaDias(Date fecha, int numeroDias) throws Throwable {
		System.out.println("Ingresa al anteriorDia con fecha: " + fecha + ", dias: " + numeroDias);
		DateFormat df = new SimpleDateFormat(
				detalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.FORMATO_FECHA,
						FormatoFecha.EJB_SIN_HORA));
		Calendar anterior = Calendar.getInstance();
		anterior.setTime(fecha);
		anterior.add(Calendar.DAY_OF_YEAR, numeroDias);
		return df.parse(df.format(anterior.getTime()));
	}

	@Override
	public LocalDate sumaRestaDiasLocal(LocalDate fecha, int numeroDias) throws Throwable {
		System.out.println("sumaRestaDiasLocal con fecha: " + fecha + ", dias: " + numeroDias);
		LocalDate resultado = fecha.plusDays(numeroDias);
		return resultado;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.service.FechaService#ultimoDiaPeriodo(java.
	 * lang.Long, java.lang.Long)
	 */
	public Date ultimoDiaMesAnio(Long mes, Long anio) throws Throwable {
		System.out.println("Ingresa al ultimoDiaMesAnio con mes: " + mes + ", y anio: " + anio);
		DateFormat df = new SimpleDateFormat(
				detalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.FORMATO_FECHA,
						FormatoFecha.EJB_SIN_HORA));
		Calendar ultimoDia = Calendar.getInstance();
		ultimoDia.set(Calendar.MONTH, mes.intValue() - 1);
		ultimoDia.set(Calendar.YEAR, anio.intValue());
		ultimoDia.set(Calendar.DATE, ultimoDia.getActualMaximum(Calendar.DATE));
		return df.parse(df.format(ultimoDia.getTime()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.service.FechaService#primerDiaPeriodo(java.
	 * lang.Long, java.lang.Long)
	 */
	public Date primerDiaMesAnio(Long mes, Long anio) throws Throwable {
		System.out.println("Ingresa al primerDiaMesAnio con mes: " + mes + ", y anio: " + anio);
		DateFormat df = new SimpleDateFormat(
				detalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.FORMATO_FECHA,
						FormatoFecha.EJB_SIN_HORA));
		Calendar primerDia = Calendar.getInstance();
		primerDia.set(Calendar.MONTH, mes.intValue() - 1);
		primerDia.set(Calendar.YEAR, anio.intValue());
		primerDia.set(Calendar.DATE, 1);
		return df.parse(df.format(primerDia.getTime()));
	}
	
	@Override
	public LocalDate ultimoDiaMesAnioLocal(Long mes, Long anio) throws Throwable {
		System.out.println("ultimoDiaMesAnioLocal con mes: " + mes + ", y anio: " + anio);
		LocalDate primerDia = LocalDate.of(anio.intValue(), mes.intValue(), 1);
		LocalDate resultado = primerDia.withDayOfMonth(primerDia.lengthOfMonth());
		return resultado;
	}

	@Override
	public LocalDate primerDiaMesAnioLocal(Long mes, Long anio) throws Throwable {
		System.out.println("primerDiaMesAnioLocal con mes: " + mes + ", y anio: " + anio);
		LocalDate resultado = LocalDate.of(anio.intValue(), mes.intValue(), 1);
		return resultado;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.basico.sistema.ejb.service.FechaService#primerDiaSemana(java.
	 * util.Date)
	 */
	public Date primerDiaSemana(Date fecha) throws Throwable {
		System.out.println("Service primerDiaSemana con fecha: " + fecha);
		DateFormat df = new SimpleDateFormat(
				detalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.FORMATO_FECHA,
						FormatoFecha.EJB_SIN_HORA));
		int diferenciaDias;
		Calendar resultado = Calendar.getInstance();
		resultado.setTime(fecha);
		diferenciaDias = resultado.get(Calendar.DAY_OF_WEEK);
		if (diferenciaDias == 1) {
			diferenciaDias = 8;
		}
		resultado.set(Calendar.DAY_OF_YEAR, resultado.get(Calendar.DAY_OF_YEAR) - diferenciaDias + 2);
		return df.parse(df.format(resultado.getTime()));
	}

	@Override
	public LocalDate primerDiaSemanaLocal(LocalDate fecha) throws Throwable {
		System.out.println("primerDiaSemanaLocal con fecha: " + fecha);
		LocalDate resultado = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		return resultado;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.basico.sistema.ejb.service.FechaService#primerDiaMes(java.util.
	 * Date)
	 */
	public Date primerDiaMes(Date fecha) throws Throwable {
		System.out.println("Service primerDiaMes con fecha: " + fecha);
		DateFormat df = new SimpleDateFormat(
				detalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.FORMATO_FECHA,
						FormatoFecha.EJB_SIN_HORA));
		Calendar primerDia = Calendar.getInstance();
		primerDia.setTime(fecha);
		primerDia.set(Calendar.DATE, 1);
		return df.parse(df.format(primerDia.getTime()));
	}

	@Override
	public LocalDate primerDiaMesLocal(LocalDate fecha) throws Throwable {
		System.out.println("primerDiaMesLocal con fecha: " + fecha);
		LocalDate resultado = fecha.withDayOfMonth(1);
		return resultado;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.basico.sistema.ejb.service.FechaService#primerDiaAnio(java.util
	 * .Date)
	 */
	public Date primerDiaAnio(Date fecha) throws Throwable {
		System.out.println("Service primerDiaAnio con fecha: " + fecha);
		DateFormat df = new SimpleDateFormat(
				detalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.FORMATO_FECHA,
						FormatoFecha.EJB_SIN_HORA));
		Calendar primerDia = Calendar.getInstance();
		primerDia.setTime(fecha);
		primerDia.set(Calendar.MONTH, Calendar.JANUARY);
		primerDia.set(Calendar.DATE, 1);
		return df.parse(df.format(primerDia.getTime()));
	}

	@Override
	public LocalDate primerDiaAnioLocal(LocalDate fecha) throws Throwable {
		System.out.println("primerDiaAnioLocal con fecha: " + fecha);
		LocalDate resultado = fecha.withDayOfYear(1);
		return resultado;
	}

	@SuppressWarnings("rawtypes")
	public boolean verificaTraslape(List fechas, Date fechaDesde,
			Date fechaHasta) throws Throwable {
		System.out.println(
				"Ingresa al verificaTraslape con listado de : " + fechas.size() + ", y fechaDesde: " + fechaDesde +
						",fechaHasta :" + fechaHasta);
		boolean traslape = false;
		Object[] recuperados = null;
		DateFormat df = new SimpleDateFormat(
				detalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.FORMATO_FECHA,
						FormatoFecha.EJB_SIN_HORA));
		Date comparaDesde = new Date();
		Date comparaHasta = new Date();
		Calendar fechaFinNula = Calendar.getInstance();
		fechaFinNula.set(Calendar.MONTH, 12);
		fechaFinNula.set(Calendar.YEAR, 2999);
		fechaFinNula.set(Calendar.DATE, 31);
		Date fuenteDesde = df.parse(df.format(fechaDesde));
		Date fuenteHasta = new Date();
		if (fechaHasta == null) {
			fuenteHasta = df.parse(df.format(fechaFinNula.getTime()));
		} else {
			fuenteHasta = df.parse(df.format(fechaHasta));
		}
		for (Object o : fechas) {
			recuperados = (Object[]) o;
			comparaDesde = df.parse(df.format((Date) recuperados[0]));
			if (recuperados[1] == null) {
				comparaHasta = df.parse(df.format(fechaFinNula.getTime()));
			} else {
				comparaHasta = df.parse(df.format((Date) recuperados[1]));
			}
			if ((fuenteDesde.equals(comparaDesde) || fuenteDesde.equals(comparaHasta))
					|| (fuenteHasta.equals(comparaDesde) || fuenteHasta.equals(comparaHasta))) {
				traslape = true;
				break;
			} else {
				if ((fuenteDesde.after(comparaDesde)) && (fuenteDesde.before(comparaHasta))) {
					traslape = true;
					break;
				} else {
					if ((fuenteHasta.after(comparaDesde)) && (fuenteHasta.before(comparaHasta))) {
						traslape = true;
						break;
					} else {
						if ((comparaDesde.after(fuenteDesde)) && (comparaDesde.before(fuenteHasta))) {
							traslape = true;
							break;
						} else {
							if ((comparaHasta.after(fuenteDesde)) && (comparaHasta.before(fuenteHasta))) {
								traslape = true;
								break;
							}
						}
					}
				}
			}
		}
		return traslape;
	}

	@SuppressWarnings("rawtypes")
	public boolean verificaTraslapeActualiza(List fechas, Date fechaDesde,
			Date fechaHasta) throws Throwable {
		System.out.println("Ingresa al verificaTraslapeActualiza con listado de : " + fechas.size() + ", y fechaDesde: "
				+ fechaDesde +
				",fechaHasta :" + fechaHasta);
		boolean traslape = false;
		Object[] recuperados = null;
		DateFormat df = new SimpleDateFormat(
				detalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.FORMATO_FECHA,
						FormatoFecha.EJB_SIN_HORA));
		Date comparaDesde = new Date();
		Date comparaHasta = new Date();
		Calendar fechaFinNula = Calendar.getInstance();
		fechaFinNula.set(Calendar.MONTH, 12);
		fechaFinNula.set(Calendar.YEAR, 2999);
		fechaFinNula.set(Calendar.DATE, 31);
		Date fuenteDesde = df.parse(df.format(fechaDesde));
		Date fuenteHasta = new Date();
		if (fechaHasta == null) {
			fuenteHasta = df.parse(df.format(fechaFinNula.getTime()));
		} else {
			fuenteHasta = df.parse(df.format(fechaHasta));
		}
		for (Object o : fechas) {
			recuperados = (Object[]) o;
			comparaDesde = df.parse(df.format((Date) recuperados[0]));
			if (recuperados[1] == null) {
				if ((fuenteDesde.before(comparaDesde)) && (fechaHasta == null)) {
					traslape = true;
					break;
				} else {
					if (fechaHasta == null) {
						traslape = false;
						break;
					} else {
						comparaHasta = df.parse(df.format(fechaFinNula.getTime()));
					}
				}
			} else {
				comparaHasta = df.parse(df.format((Date) recuperados[1]));
			}
			if ((fuenteDesde.equals(comparaDesde) || fuenteDesde.equals(comparaHasta))
					|| (fuenteHasta.equals(comparaDesde) || fuenteHasta.equals(comparaHasta))) {
				traslape = true;
				break;
			} else {
				if ((fuenteDesde.after(comparaDesde)) && (fuenteDesde.before(comparaHasta))) {
					traslape = true;
					break;
				} else {
					if ((fuenteHasta.after(comparaDesde)) && (fuenteHasta.before(comparaHasta))) {
						traslape = true;
						break;
					} else {
						if ((comparaDesde.after(fuenteDesde)) && (comparaDesde.before(fuenteHasta))) {
							traslape = true;
							break;
						} else {
							if ((comparaHasta.after(fuenteDesde)) && (comparaHasta.before(fuenteHasta))) {
								traslape = true;
								break;
							}
						}
					}
				}
			}
		}
		return traslape;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.basico.sistema.ejb.service.FechaService#sumaRestaMeses(java.
	 * util.Date, int)
	 */
	public Date sumaRestaMeses(Date fecha, int numeroMeses) throws Throwable {
		System.out.println("Service sumaRestaMeses con fecha: " + fecha + ", meses: " + numeroMeses);
		DateFormat df = new SimpleDateFormat(
				detalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.FORMATO_FECHA,
						FormatoFecha.EJB_SIN_HORA));
		Calendar anterior = Calendar.getInstance();
		anterior.setTime(fecha);
		anterior.add(Calendar.MONTH, numeroMeses);
		return df.parse(df.format(anterior.getTime()));
	}

	@Override
	public LocalDate sumaRestaMesesLocal(LocalDate fecha, int numeroMeses) throws Throwable {
		System.out.println("sumaRestaMesesLocal con fecha: " + fecha + ", meses: " + numeroMeses);
		LocalDate resultado = fecha.plusMonths(numeroMeses);
		return resultado;
	}

	public String calculoEdad(Date fechaNacimiento) throws Throwable {
		String resultado;
		int years = 0;
		int months = 0;
		int days = 0;
		// create calendar object for birth day
		Calendar birthDay = Calendar.getInstance();
		birthDay.setTime(fechaNacimiento);
		/*
		 * birthDay.set(Calendar.YEAR, 2005);
		 * birthDay.set(Calendar.MONTH, Calendar.SEPTEMBER);
		 * birthDay.set(Calendar.DATE, 29);
		 */
		// create calendar object for current day
		long currentTime = System.currentTimeMillis();
		Calendar currentDay = Calendar.getInstance();
		currentDay.setTimeInMillis(currentTime);
		// Get difference between years
		years = currentDay.get(Calendar.YEAR) - birthDay.get(Calendar.YEAR);
		int currMonth = currentDay.get(Calendar.MONTH) + 1;
		int birthMonth = birthDay.get(Calendar.MONTH) + 1;
		// Get difference between months
		months = currMonth - birthMonth;
		// if month difference is in negative then reduce years by one and calculate the
		// number of months.
		if (months < 0) {
			years--;
			months = 12 - birthMonth + currMonth;
			if (currentDay.get(Calendar.DATE) < birthDay.get(Calendar.DATE))
				months--;
		} else if (months == 0 && currentDay.get(Calendar.DATE) < birthDay.get(Calendar.DATE)) {
			years--;
			months = 11;
		}
		// Calculate the days
		if (currentDay.get(Calendar.DATE) > birthDay.get(Calendar.DATE))
			days = currentDay.get(Calendar.DATE) - birthDay.get(Calendar.DATE);
		else if (currentDay.get(Calendar.DATE) < birthDay.get(Calendar.DATE)) {
			int today = currentDay.get(Calendar.DAY_OF_MONTH);
			currentDay.add(Calendar.MONTH, -1);
			days = currentDay.getActualMaximum(Calendar.DAY_OF_MONTH) - birthDay.get(Calendar.DAY_OF_MONTH) + today;
		} else {
			days = 0;
			if (months == 12) {
				years++;
				months = 0;
			}
		}
		resultado = years + " a�os, " + months + " meses y " + days + " dias";
		return resultado;
	}

	@Override
	public String calculoEdadLocal(LocalDate fechaNacimiento) throws Throwable {
		System.out.println("calculoEdadLocal con fechaNacimiento: " + fechaNacimiento);
		LocalDate fechaActual = LocalDate.now();
		Period periodo = Period.between(fechaNacimiento, fechaActual);
		String resultado = periodo.getYears() + " a�os, " + periodo.getMonths() + " meses y " + periodo.getDays() + " dias";
		return resultado;
	}

	public Long diferenciaMeses(Date fecha1, Date fecha2) throws Throwable {
		System.out.println("Service diferenciaMeses con fecha1: " + fecha1 + ", fecha2: " + fecha2);
		Long milFec1;
		Long milFec2;
		Long milResult;
		Long result;
		Long result2;
		Calendar fec1 = Calendar.getInstance();
		Calendar fec2 = Calendar.getInstance();
		fec1.setTime(fecha1);
		fec2.setTime(fecha2);
		milFec1 = fec1.getTimeInMillis();
		milFec2 = fec2.getTimeInMillis();
		milResult = milFec2 - milFec1;
		result = milResult / (24 * 60 * 60 * 1000);
		result2 = result / 30;
		return result2;
	}

	@Override
	public Long diferenciaMesesLocal(LocalDate fecha1, LocalDate fecha2) throws Throwable {
		System.out.println("diferenciaMesesLocal con fecha1: " + fecha1 + ", fecha2: " + fecha2);
		Long resultado = ChronoUnit.MONTHS.between(fecha1, fecha2);
		return resultado;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.basico.sistema.ejb.service.FechaService#diferenciaMinutos(java.
	 * util.Date, java.util.Date)
	 */
	public Double diferenciaMinutos(Date fecha1, Date fecha2) throws Throwable {
		System.out.println("Service diferenciaMinutos con fecha1: " + fecha1 + ", fecha2: " + fecha2);
		Long milFec1;
		Long milFec2;
		Long milResult;
		Double result;
		Calendar fec1 = Calendar.getInstance();
		Calendar fec2 = Calendar.getInstance();
		fec1.setTime(fecha1);
		fec2.setTime(fecha2);
		milFec1 = fec1.getTimeInMillis();
		milFec2 = fec2.getTimeInMillis();
		milResult = milFec2 - milFec1;
		result = Double.valueOf(milResult / (60 * 1000));
		return result;
	}

	@Override
	public Double diferenciaMinutosLocal(LocalDateTime fecha1, LocalDateTime fecha2) throws Throwable {
		System.out.println("diferenciaMinutosLocal con fecha1: " + fecha1 + ", fecha2: " + fecha2);
		Long minutos = ChronoUnit.MINUTES.between(fecha1, fecha2);
		Double resultado = minutos.doubleValue();
		return resultado;
	}

}
