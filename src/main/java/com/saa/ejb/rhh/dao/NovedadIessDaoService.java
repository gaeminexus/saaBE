package com.saa.ejb.rhh.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.NovedadIess;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService NovedadIess.
 *
 * <p><b>La ventana de un periodo es siempre la misma</b>, y esto importa mas de lo que
 * parece: una novedad pertenece al periodo cuya fecha de inicio y fecha de fin encierran
 * su <code>NVISFCHC</code> --la fecha del hecho--, no su fecha de registro ni la de
 * reporte. La pantalla de novedades del mes, la regla que impide cerrar con pendientes y
 * el exportador batch tienen que usar esta misma definicion, o cada uno vera un conjunto
 * distinto y el que impide dejara pasar lo que el que lista muestra.</p>
 */
@Local
public interface NovedadIessDaoService extends EntityDao<NovedadIess> {

	/**
	 * Novedades cuya fecha de hecho cae dentro de la ventana, filtradas por estado.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param desde			: Fecha de inicio del periodo
	 * @param hasta			: Fecha de fin del periodo
	 * @param estados		: Estados que interesan; nulo o vacio trae todos
	 * @return				: Las novedades de la ventana, o lista vacia
	 * @throws Throwable	: Excepcion
	 */
	List<NovedadIess> selectByVentana(Long idEmpresa, LocalDate desde, LocalDate hasta,
			List<Long> estados) throws Throwable;

	/**
	 * Novedades de un tipo dentro de la ventana, filtradas por estado. Es la consulta
	 * que usa el exportador batch: un archivo por tipo.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param tipoNovedad	: Codigo alterno del detalle del rubro 204
	 * @param desde			: Fecha de inicio del periodo
	 * @param hasta			: Fecha de fin del periodo
	 * @param estados		: Estados que interesan; nulo o vacio trae todos
	 * @return				: Las novedades del tipo en la ventana, o lista vacia
	 * @throws Throwable	: Excepcion
	 */
	List<NovedadIess> selectByTipoEnVentana(Long idEmpresa, Long tipoNovedad, LocalDate desde,
			LocalDate hasta, List<Long> estados) throws Throwable;

	/**
	 * Novedades de un contrato, de un tipo, dentro de la ventana. Es lo que hace
	 * idempotente a la generacion automatica: antes de crear una novedad se mira si ya
	 * existe la del mismo contrato, tipo y periodo.
	 *
	 * @param idContrato	: Id del contrato
	 * @param tipoNovedad	: Codigo alterno del detalle del rubro 204
	 * @param desde			: Fecha de inicio del periodo
	 * @param hasta			: Fecha de fin del periodo
	 * @return				: Las novedades encontradas, o lista vacia
	 * @throws Throwable	: Excepcion
	 */
	List<NovedadIess> selectByContratoTipoEnVentana(Long idContrato, Long tipoNovedad,
			LocalDate desde, LocalDate hasta) throws Throwable;

}
