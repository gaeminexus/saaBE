package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.CargaArchivo;

import jakarta.ejb.Local;

@Local
public interface CargaArchivoDaoService extends EntityDao<CargaArchivo>{
	
	/** Recupera los archivos cargados en un año determinado
	 * @param :anio
	 * @return Lista de CargaArchivo
	 */
	List<CargaArchivo> selectByAnio(Long anio) throws Throwable;
	
	/**
	 * Busca todas las cargas con estado específico (estado 3 = procesada)
	 * @param estado Estado de la carga
	 * @return Lista de CargaArchivo encontradas
	 * @throws Throwable Si ocurre un error
	 */
	List<CargaArchivo> selectByEstado(Long estado) throws Throwable;

	/**
	 * OPTIMIZADO: Busca la última carga procesada (MAX año/mes) con estado específico
	 * @param estado Estado de la carga (3 = procesada)
	 * @return Última CargaArchivo procesada o null
	 * @throws Throwable Si ocurre un error
	 */
	CargaArchivo selectUltimaCargaProcesada(Long estado) throws Throwable;

	/**
	 * Cargas de un MES DE AFECTACION en un estado dado, de la mas reciente a la mas antigua.
	 *
	 * <p>El mes de afectacion ({@code CRARANAF}/{@code CRARMSAF}) es el mes al que pertenece
	 * el descuento, no el mes en que se subio el archivo: la carga de julio 2026 se subio el
	 * 4 de agosto. Filtrar por {@code CRARFCCR} daria el mes equivocado.</p>
	 *
	 * <p>Lo usa el control de archivo del cierre de cartera: no se cierra un mes sin su
	 * archivo Petro procesado, porque sin el los aportes del mes no existen en
	 * {@code CRD.APRT} y el neteo los reversaria como no cobrados.</p>
	 *
	 * @param anio			: Anio de afectacion
	 * @param mes			: Mes de afectacion, 1 a 12
	 * @param estado		: Estado de la carga; ver {@link com.saa.rubros.CrdEstadoCargaArchivo}
	 * @return				: Listado de cargas; VACIO si no hay ninguna
	 * @throws Throwable	: Excepcion
	 */
	List<CargaArchivo> selectByPeriodoAfectacionYEstado(Long anio, Long mes, Long estado)
			throws Throwable;

}